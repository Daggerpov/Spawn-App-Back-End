package com.danielagapov.spawn.Services.OAuth;


import com.danielagapov.spawn.DTOs.User.AuthResponseDTO;
import com.danielagapov.spawn.DTOs.User.BaseUserDTO;
import com.danielagapov.spawn.DTOs.User.UserCreationDTO;
import com.danielagapov.spawn.DTOs.User.UserDTO;
import com.danielagapov.spawn.Enums.EntityType;
import com.danielagapov.spawn.Enums.OAuthProvider;
import com.danielagapov.spawn.Enums.UserStatus;
import com.danielagapov.spawn.Exceptions.AccountAlreadyExistsException;
import com.danielagapov.spawn.Exceptions.Base.BaseNotFoundException;
import com.danielagapov.spawn.Exceptions.IncorrectProviderException;
import com.danielagapov.spawn.Exceptions.Logger.ILogger;
import com.danielagapov.spawn.Mappers.UserMapper;
import com.danielagapov.spawn.Models.User.User;
import com.danielagapov.spawn.Models.User.UserIdExternalIdMap;
import com.danielagapov.spawn.Repositories.User.IUserIdExternalIdMapRepository;
import com.danielagapov.spawn.Services.User.IUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;


@Service
public class OAuthService implements IOAuthService {
    private final IUserService userService;
    private final IUserIdExternalIdMapRepository externalIdMapRepository;
    private final Map<OAuthProvider, OAuthStrategy> oauthProviders;
    private final ILogger logger;
    
    // Application-level synchronization for OAuth operations per external ID
    private final ConcurrentHashMap<String, Object> externalIdLocks = new ConcurrentHashMap<>();

    @Autowired
    public OAuthService(IUserIdExternalIdMapRepository externalIdMapRepository,
                        IUserService userService,
                        ILogger logger,
                        List<OAuthStrategy> providers) {
        this.externalIdMapRepository = externalIdMapRepository;
        this.userService = userService;
        this.logger = logger;
        this.oauthProviders = providers.stream()
                .collect(Collectors.toMap(OAuthStrategy::getOAuthProvider, strategy -> strategy));
    }

    @Override
    public BaseUserDTO makeUser(UserDTO user, String externalUserId, byte[] profilePicture, OAuthProvider provider) {
        try {
            logger.info(String.format("Making user: {user: %s, externalUserId: %s}", user, externalUserId));
            
            // Check if this external user already exists
            boolean existsByExternalId = mappingExistsByExternalId(externalUserId);

            // Check if a user exists with this email
            boolean existsByEmail = userService.existsByEmail(user.getEmail());

            // Case 1: There's already a mapping for this externalId
            if (existsByExternalId) {
                logger.info("Existing user detected in makeUser, mapping already exists");
                User existingUser = getMapping(externalUserId).getUser();
                
                // Only delete users that have non-active statuses (null is treated as active for backward compatibility)
                if (existingUser.getStatus() != null && existingUser.getStatus() != UserStatus.ACTIVE) {
                    logger.info("Found incomplete user account (status: " + existingUser.getStatus() + "). Allowing re-creation.");
                    // Delete the incomplete user and their mapping to allow fresh creation
                    userService.deleteUserById(existingUser.getId());
                    // The mapping will be explicitly deleted with the user
                } else {
                    logger.info("Returning existing active user");
                    return UserMapper.toDTO(existingUser);
                }
            }
            
            // Case 2: There's already a Spawn user with this email address, but no mapping with this external id
            // In this case, the user signed in with a different provider initially, so we should not allow creation
            // with this provider
            if (existsByEmail) {
                logger.info("Existing user detected in makeUser, email already exists");
                try {
                    UserIdExternalIdMap externalIdMap = getMappingByUserEmail(user.getEmail());
                    User existingUser = externalIdMap.getUser();
                    
                    // Only delete users that have non-active statuses (null is treated as active for backward compatibility)
                    if (existingUser.getStatus() != null && existingUser.getStatus() != UserStatus.ACTIVE) {
                        logger.info("Found incomplete user account with email (status: " + existingUser.getStatus() + "). Allowing re-creation.");
                        // Delete the incomplete user and their mapping to allow fresh creation
                        userService.deleteUserById(existingUser.getId());
                        // The mapping will be explicitly deleted with the user
                    } else {
                        logger.info("Returning existing active user with different provider");
                        return UserMapper.toDTO(existingUser);
                    }
                } catch (BaseNotFoundException e) {
                    logger.warn("User email exists but no mapping found - this may be due to data inconsistency. Attempting graceful repair in makeUser.");
                    
                    // Attempt to repair the data inconsistency gracefully
                    try {
                        User orphanedUser = userService.getUserByEmail(user.getEmail());
                        logger.info("Found orphaned user for email: " + user.getEmail() + ", user ID: " + orphanedUser.getId());
                        
                        // For users with reasonable data, attempt to create a mapping instead of deleting
                        if (orphanedUser.getStatus() != null && 
                            (orphanedUser.getStatus() == UserStatus.ACTIVE || 
                             orphanedUser.getStatus() == UserStatus.USERNAME_AND_PHONE_NUMBER ||
                             orphanedUser.getStatus() == UserStatus.NAME_AND_PHOTO ||
                             orphanedUser.getStatus() == UserStatus.CONTACT_IMPORT)) {
                            
                            // This appears to be a legitimate user - attempt to create missing OAuth mapping
                            logger.info("Attempting to create missing OAuth mapping for legitimate user in makeUser: " + orphanedUser.getId());
                            
                            try {
                                createAndSaveMapping(orphanedUser, externalUserId, provider);
                                logger.info("Successfully created missing OAuth mapping in makeUser for user: " + orphanedUser.getId());
                                
                                // Return the repaired user
                                logger.info("Returning repaired existing user with different provider");
                                return UserMapper.toDTO(orphanedUser);
                                
                            } catch (Exception mappingException) {
                                logger.warn("Failed to create OAuth mapping in makeUser for orphaned user: " + mappingException.getMessage());
                                // Fall through to cleanup logic below
                            }
                        }
                        
                        // If we couldn't repair the mapping or user has incomplete data, delete the orphaned user
                        logger.info("Cleaning up orphaned user to allow new user creation: " + orphanedUser.getId() + " with email: " + orphanedUser.getEmail());
                        userService.deleteUserById(orphanedUser.getId());
                        logger.info("Orphaned user deleted: " + orphanedUser.getId() + " with email: " + orphanedUser.getEmail());
                        
                    } catch (Exception repairException) {
                        logger.error("Failed to repair or delete orphaned user: " + repairException.getMessage());
                    }
                    // Continue to Case 3 - treat as new user
                }
            }
            
            // Case 3: This is a new user, neither the externalId nor the email exists in our database
            // OR we deleted an incomplete user above
            // Save the user with profile picture
            UserDTO userDTO = userService.createAndSaveUserWithProfilePicture(user, profilePicture);
            
            // Get the User entity to create the mapping
            User userEntity = userService.getUserEntityById(userDTO.getId());
            
            // Save the mapping for the new user to the external id
            logger.info(String.format("External user detected, saving mapping: {externalUserId: %s, userDTO: %s}", externalUserId, userDTO));
            createAndSaveMapping(userEntity, externalUserId, provider);

            BaseUserDTO baseUserDTO = UserMapper.toBaseDTO(userDTO);
            logger.info(String.format("Returning BaseUserDTO of newly made user: {baseUserDTO: %s}", baseUserDTO));
            return baseUserDTO;
        } catch (DataAccessException e) {
            logger.error("Database error while creating user: " + e.getMessage());
            throw e;
        } catch (Exception e) {
            logger.error("Unexpected error while creating user: " + e.getMessage());
            throw e;
        }
    }

    @Override
    public Optional<AuthResponseDTO> signInUser(String idToken, String email, OAuthProvider provider) {
        logger.info("Checking if user signing in with " + provider + " exists by ID token and email: " + email);
        OAuthStrategy oauthStrategy = oauthProviders.get(provider);

        // Verify the token and extract the user ID
        String userId = oauthStrategy.verifyIdToken(idToken);
        logger.info("Successfully verified " + provider + " ID token and extracted user ID: " + userId);

        // Use the extracted user ID to check if the user exists
        logger.info("Checking if user exists with " + provider + " user ID: " + userId);
        return getUserIfExistsbyExternalId(userId, email);
    }

    @Override
    public Optional<AuthResponseDTO> getUserIfExistsbyExternalId(String externalUserId, String email) {
        logger.info("Checking if user exists by external ID: " + externalUserId + " and email: " + email);
        boolean existsByExternalId = mappingExistsByExternalId(externalUserId);
        boolean existsByEmail = userService.existsByEmail(email);
        logger.info("User exists by externalId: " + existsByExternalId + ", exists by email: " + existsByEmail);

        if (existsByExternalId) { // A Spawn account exists with this external id
            logger.info("Found existing user by external ID: " + externalUserId);
            User user = getMapping(externalUserId).getUser();
            
            // Return user regardless of status - client will handle appropriate onboarding
            AuthResponseDTO authResponseDTO = UserMapper.toAuthResponseDTO(user);
            logger.info("Returning user with ID: " + authResponseDTO.getUser().getId() + ", username: " + authResponseDTO.getUser().getUsername() + ", status: " + user.getStatus());
            return Optional.of(authResponseDTO);
        } else if (existsByEmail) { // A Spawn account exists with this email but not with the external id
            logger.info("Found existing user by email but not by external ID.");
            try {
                UserIdExternalIdMap externalIdMap = getMappingByUserEmail(email);
                User user = externalIdMap.getUser();
                
                // For incomplete users, allow them to continue with any provider
                if (user.getStatus() != null && user.getStatus() != UserStatus.ACTIVE) {
                    logger.info("Found user by email but account is not active (status: " + user.getStatus() + "). Returning user for onboarding completion.");
                    AuthResponseDTO authResponseDTO = UserMapper.toAuthResponseDTO(user);
                    return Optional.of(authResponseDTO);
                } else {
                    // For active users, enforce provider consistency
                    OAuthProvider existingProvider = externalIdMap.getProvider();
                    String providerName = existingProvider == OAuthProvider.google ? "Google" : "Apple";
                    logger.info("Expected provider for this email: " + providerName);
                    throw new IncorrectProviderException("The email: " + email + " is already associated to a " + providerName + " account. Please login through " + providerName + " instead");
                }
            } catch (BaseNotFoundException e) {
                logger.warn("User email exists but no mapping found - checking for data inconsistency and attempting cleanup.");
                
                // Get the user by email to check their status
                try {
                    User orphanedUser = userService.getUserByEmail(email);
                    
                    // If user has non-active status (likely EMAIL_VERIFIED), they were likely orphaned during a previous OAuth flow
                    if (orphanedUser.getStatus() != null && orphanedUser.getStatus() != UserStatus.ACTIVE) {
                        logger.info("Found orphaned user with status: " + orphanedUser.getStatus() + ". Cleaning up for re-registration.");
                        
                        // Clean up the orphaned user to allow fresh registration
                        userService.deleteUserById(orphanedUser.getId());
                        logger.info("Orphaned user deleted. Treating as no user found to allow fresh registration.");
                        return Optional.empty();
                    } else {
                        logger.warn("Active user exists without OAuth mapping - possible data corruption. Manual intervention may be required.");
                        return Optional.empty();
                    }
                } catch (Exception cleanupEx) {
                    logger.error("Error during orphaned user cleanup: " + cleanupEx.getMessage());
                    // Fallback: treat as no user found to allow registration to proceed
                    logger.info("Fallback: treating as no user found due to cleanup error.");
                    return Optional.empty();
                }
            }
        } else { // No account exists for this external id or email
            logger.info("No existing user found for external ID: " + externalUserId + " or email: " + email);
            return Optional.empty();
        }
    }

    @Override
    public BaseUserDTO createUserFromOAuth(UserCreationDTO userCreationDTO, String idToken, OAuthProvider provider) {
        try {
            logger.info(String.format("Creating user from OAuth: {username: %s, email: %s, provider: %s}",
                userCreationDTO.getUsername(), userCreationDTO.getEmail(), provider));

            // Get the appropriate OAuth strategy
            OAuthStrategy oauthStrategy = oauthProviders.get(provider);
            if (idToken != null) {
                // Verify the token and extract the user ID
                String userId = oauthStrategy.verifyIdToken(idToken);
                logger.info("Successfully verified " + provider + " ID token and extracted user ID: " + userId);

                UserDTO newUser = UserMapper.toDTOFromCreationUserDTO(userCreationDTO);

                logger.info("Making new user: " + newUser.getUsername());
                return makeUser(newUser, userId, userCreationDTO.getProfilePictureData(), provider);
            } else {
                logger.error("Missing required authentication parameters");
                throw new IllegalArgumentException("Either a valid ID token or external user ID with provider must be provided");
            }
        } catch (SecurityException e) {
            logger.error("Security error during OAuth authentication: " + e.getMessage());
            throw e;
        } catch (Exception e) {
            logger.error("Unexpected error during OAuth user creation: " + e.getMessage());
            throw e;
        }
    }

    /**
     * Verifies the OAuth registration details provided by checking the ID token
     * and determining if the user already exists in the system or is eligible for registration.
     * Uses application-level synchronization to prevent race conditions.
     *
     * @param email the email address provided by the user attempting to register
     * @param idToken the ID token obtained through the OAuth provider for authentication
     * @param provider the OAuthProvider used for the authentication (e.g., GOOGLE, FACEBOOK)
     * @return externalUserId if the user can be registered
     * @throws AccountAlreadyExistsException if the external user ID already exists in the system
     * @throws IncorrectProviderException if the email is already associated with a different provider
     * @throws IllegalArgumentException if required authentication parameters are missing
     * @throws SecurityException if there is a security-related issue during OAuth verification
     */
    @Override
    @Transactional
    public String checkOAuthRegistration(String email, String idToken, OAuthProvider provider) {
        try {
            // Get the appropriate OAuth strategy and verify token first
            OAuthStrategy oauthStrategy = oauthProviders.get(provider);
            if (idToken == null) {
                throw new IllegalArgumentException("ID token must be provided");
            }
            
            String externalUserId = oauthStrategy.verifyIdToken(idToken);
            logger.info("Successfully verified " + provider + " ID token and extracted user ID: " + externalUserId);
            
            // Use application-level synchronization per external ID to prevent race conditions
            Object lock = externalIdLocks.computeIfAbsent(externalUserId, k -> new Object());
            
            synchronized (lock) {
                try {
                    return checkOAuthRegistrationWithLock(email, externalUserId, provider);
                } finally {
                    // Clean up the lock if no other threads are waiting
                    externalIdLocks.remove(externalUserId, lock);
                }
            }
            
        } catch (SecurityException e) {
            logger.error("Security error during OAuth authentication: " + e.getMessage());
            throw e;
        } catch (Exception e) {
            logger.error("Unexpected error during OAuth user creation: " + e.getMessage());
            throw e;
        }
    }
    
    /**
     * Internal method that handles OAuth registration logic with proper synchronization.
     * This method runs within a synchronized block to prevent race conditions.
     */
    @Transactional(isolation = Isolation.SERIALIZABLE)
    private String checkOAuthRegistrationWithLock(String email, String externalUserId, OAuthProvider provider) {
        // Perform all checks atomically within the synchronized block
        UserIdExternalIdMap existingMapping = null;
        User existingUserByEmail = null;
        
        try {
            existingMapping = externalIdMapRepository.findById(externalUserId).orElse(null);
            existingUserByEmail = userService.existsByEmail(email) ? userService.getUserByEmail(email) : null;
        } catch (Exception e) {
            logger.warn("Error during initial checks: " + e.getMessage());
        }

        // Case 1: There's already a mapping for this externalId
        if (existingMapping != null) {
            logger.info("Existing user detected in checkOAuthRegistration, mapping already exists");
            User existingUser = existingMapping.getUser();
            
            // Only delete users that have non-active statuses (null is treated as active for backward compatibility)
            if (existingUser.getStatus() != null && existingUser.getStatus() != UserStatus.ACTIVE) {
                logger.info("Found incomplete user account (status: " + existingUser.getStatus() + "). Allowing re-registration.");
                // Delete the incomplete user (cascade will handle the mapping)
                try {
                    userService.deleteUserById(existingUser.getId());
                    logger.info("Successfully deleted incomplete user, proceeding with fresh registration");
                    return externalUserId; // Return immediately to allow fresh registration
                } catch (Exception e) {
                    logger.warn("Error deleting incomplete user: " + e.getMessage());
                    // The user might have been deleted by another transaction
                    // Check again if the mapping still exists
                    if (!externalIdMapRepository.existsById(externalUserId)) {
                        logger.info("Mapping was deleted by another transaction, proceeding with registration");
                        return externalUserId;
                    }
                    // If mapping still exists, fall through to return external ID
                }
            }
            
            // For ACTIVE users or if deletion failed, return the external ID 
            // so the registration flow can handle it appropriately
            logger.info("Found existing user (status: " + existingUser.getStatus() + "). Returning external ID for appropriate handling.");
            return externalUserId;
        }

        // Case 2: There's already a Spawn user with this email address, but no mapping with this external id
        if (existingUserByEmail != null) {
            logger.info("Existing user detected in checkOAuthRegistration, email already exists");
            try {
                UserIdExternalIdMap externalIdMap = getMappingByUserEmail(email);
                User existingUser = externalIdMap.getUser();
                
                // Only delete users that have non-active statuses
                if (existingUser.getStatus() != null && existingUser.getStatus() != UserStatus.ACTIVE) {
                    logger.info("Found incomplete user account with email (status: " + existingUser.getStatus() + "). Allowing re-registration.");
                    try {
                        userService.deleteUserById(existingUser.getId());
                        logger.info("Successfully deleted incomplete user by email, proceeding with registration");
                    } catch (Exception e) {
                        logger.warn("Error deleting incomplete user by email: " + e.getMessage());
                        // Continue with registration as the user might have been deleted by another transaction
                    }
                } else {
                    // For active users, enforce provider consistency
                    OAuthProvider existingProvider = externalIdMap.getProvider();
                    String providerName = existingProvider == OAuthProvider.google ? "Google" : "Apple";
                    throw new IncorrectProviderException("Email already exists for a " + providerName + " account. Please login through " + providerName + " instead");
                }
            } catch (BaseNotFoundException e) {
                logger.warn("User email exists but no mapping found - this may be due to data inconsistency. Attempting graceful repair in registration flow.");
                
                // Attempt to repair the data inconsistency gracefully
                try {
                    logger.info("Found orphaned user for email: " + email + ", user ID: " + existingUserByEmail.getId());
                    
                    // For users with reasonable data, attempt to create a mapping instead of deleting
                    if (existingUserByEmail.getStatus() != null && 
                        (existingUserByEmail.getStatus() == UserStatus.ACTIVE || 
                         existingUserByEmail.getStatus() == UserStatus.USERNAME_AND_PHONE_NUMBER ||
                         existingUserByEmail.getStatus() == UserStatus.NAME_AND_PHOTO ||
                         existingUserByEmail.getStatus() == UserStatus.CONTACT_IMPORT)) {
                        
                        // This appears to be a legitimate user - attempt to create missing OAuth mapping
                        logger.info("Attempting to create missing OAuth mapping for legitimate user during registration: " + existingUserByEmail.getId());
                        
                        // Use the provided external ID and provider to create the mapping
                        try {
                            createAndSaveMapping(existingUserByEmail, externalUserId, provider);
                            logger.info("Successfully created missing OAuth mapping during registration for user: " + existingUserByEmail.getId());
                            
                            // Return the external ID to indicate the mapping now exists
                            return externalUserId;
                            
                        } catch (Exception mappingException) {
                            logger.warn("Failed to create OAuth mapping during registration for orphaned user: " + mappingException.getMessage());
                            // Fall through to cleanup logic below
                        }
                    }
                    
                    // If we couldn't repair the mapping or user has incomplete data, delete the orphaned user
                    logger.info("Cleaning up orphaned user to allow new registration: " + existingUserByEmail.getId() + " with email: " + existingUserByEmail.getEmail());
                    userService.deleteUserById(existingUserByEmail.getId());
                    logger.info("Orphaned user deleted: " + existingUserByEmail.getId() + " with email: " + existingUserByEmail.getEmail());
                    
                } catch (Exception repairException) {
                    logger.error("Failed to repair or delete orphaned user: " + repairException.getMessage());
                }
            }
        }

        // Case 3: This is a new user, neither the externalId nor the email exists in our database
        logger.info("No existing user found, proceeding with new user registration for external ID: " + externalUserId);
        return externalUserId;
    }

    @Override
    @Transactional
    public void createAndSaveMapping(User user, String externalUserId, OAuthProvider provider) {
        // Use application-level synchronization per external ID
        Object lock = externalIdLocks.computeIfAbsent(externalUserId, k -> new Object());
        
        synchronized (lock) {
            try {
                createAndSaveMappingWithLock(user, externalUserId, provider);
            } finally {
                // Clean up the lock if no other threads are waiting
                externalIdLocks.remove(externalUserId, lock);
            }
        }
    }
    
    /**
     * Internal method to create and save mapping with proper synchronization.
     * Handles orphaned mappings and data inconsistencies by cleaning up stale data.
     */
    @Transactional(isolation = Isolation.SERIALIZABLE)
    private void createAndSaveMappingWithLock(User user, String externalUserId, OAuthProvider provider) {
        try {
            // Check if mapping already exists
            Optional<UserIdExternalIdMap> existingMapping = externalIdMapRepository.findById(externalUserId);
            if (existingMapping.isPresent()) {
                logger.info("Mapping already exists for external ID: " + externalUserId + ". Checking if it belongs to the same user.");
                
                UserIdExternalIdMap existing = existingMapping.get();
                if (existing.getUser().getId().equals(user.getId())) {
                    logger.info("Mapping already exists for the same user, no action needed");
                    return;
                } else {
                    logger.warn("Mapping exists for different user. This indicates a race condition or data inconsistency.");
                    
                    // Check if the existing mapping points to a deleted/non-existent user
                    try {
                        User existingMappedUser = existing.getUser();
                        boolean userStillExists = userService.existsByUserId(existingMappedUser.getId());
                        
                        if (!userStillExists) {
                            logger.warn("Existing mapping points to deleted user. Cleaning up orphaned mapping for external ID: " + externalUserId);
                            externalIdMapRepository.delete(existing);
                            externalIdMapRepository.flush(); // Ensure deletion is committed before proceeding
                        } else {
                            logger.error("Mapping exists for a different valid user. Cannot proceed with mapping creation.");
                            throw new RuntimeException("OAuth mapping conflict: External ID already mapped to a different active user");
                        }
                    } catch (Exception checkEx) {
                        logger.warn("Could not verify existing mapped user, treating as orphaned mapping: " + checkEx.getMessage());
                        // If we can't verify the user exists, assume it's orphaned and delete the mapping
                        externalIdMapRepository.delete(existing);
                        externalIdMapRepository.flush();
                    }
                }
            }
            
            // Create the new mapping - database constraints should now allow this
            UserIdExternalIdMap mapping = new UserIdExternalIdMap(externalUserId, user, provider);
            logger.info("Creating mapping for external ID: " + externalUserId + " and user: " + user.getId());
            
            UserIdExternalIdMap savedMapping = externalIdMapRepository.save(mapping);
            logger.info("Mapping successfully created: " + savedMapping);
            
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            // This can happen if another thread created the mapping concurrently
            logger.warn("Data integrity violation during mapping creation for external ID: " + externalUserId + ". " + e.getMessage());
            
            // Check if the existing mapping belongs to our user
            Optional<UserIdExternalIdMap> existingMapping = externalIdMapRepository.findById(externalUserId);
            if (existingMapping.isPresent() && existingMapping.get().getUser().getId().equals(user.getId())) {
                logger.info("Concurrent mapping creation detected, but mapping exists for correct user. Operation succeeded.");
                return;
            } else {
                logger.error("Failed to create mapping due to data integrity violation: " + e.getMessage());
                throw new RuntimeException("Unable to complete OAuth mapping creation due to data integrity violation. Please try again.");
            }
        } catch (Exception e) {
            logger.error("Unexpected error creating mapping for external ID " + externalUserId + ": " + e.getMessage());
            throw e;
        }
    }

    /* ------------------------------ HELPERS ------------------------------ */

    private boolean mappingExistsByExternalId(String externalUserId) {
        logger.info("Checking if mapping exists for external user ID: " + externalUserId);
        boolean exists = externalIdMapRepository.existsById(externalUserId);
        logger.info("Mapping exists for external user ID " + externalUserId + ": " + exists);
        return exists;
    }

    private UserIdExternalIdMap getMapping(String externalId) {
        try {
            logger.info("Fetching mapping for external ID: " + externalId);
            UserIdExternalIdMap mapping = externalIdMapRepository.findById(externalId).orElse(null);
            if (mapping != null) {
                logger.info("Found mapping for external ID: " + externalId + ", associated user ID: " + mapping.getUser().getId());
            } else {
                logger.info("No mapping found for external ID: " + externalId);
            }
            return mapping;
        } catch (DataAccessException e) {
            logger.error("Database error while fetching mapping for externalUserId( " + externalId + ") : " + e.getMessage());
            throw e;
        } catch (Exception e) {
            logger.error("Unexpected error while fetching mapping for externalUserId( " + externalId + ") : " + e.getMessage());
            throw e;
        }
    }



    private UserIdExternalIdMap getMappingByUserEmail(String email) {
        logger.info("Searching for user mapping by email: " + email);
        try {
            UserIdExternalIdMap mapping = externalIdMapRepository.findByUserEmail(email)
                .orElseThrow(() -> new BaseNotFoundException(EntityType.ExternalIdMap, email, "email"));
            logger.info("Found mapping for email: " + email + ", associated with provider: " + mapping.getProvider());
            return mapping;
        } catch (BaseNotFoundException e) {
            logger.error("No mapping found for email: " + email);
            throw e;
        }
    }

    /**
     * Performs comprehensive cleanup of orphaned OAuth data that can occur during concurrent operations.
     * This method should be called when data inconsistencies are detected.
     * 
     * @param email The email to check for orphaned data
     * @param externalUserId The external user ID to check for orphaned mappings
     * @return true if cleanup was performed, false if no cleanup was needed
     */
    @Override
    public boolean performDataConsistencyCleanup(String email, String externalUserId) {
        logger.info("Performing data consistency cleanup for email: " + email + " and external ID: " + externalUserId);
        boolean cleanupPerformed = false;
        
        try {
            // Check for orphaned mappings (mappings pointing to deleted users)
            Optional<UserIdExternalIdMap> orphanedMapping = externalIdMapRepository.findById(externalUserId);
            if (orphanedMapping.isPresent()) {
                UserIdExternalIdMap mapping = orphanedMapping.get();
                try {
                    User mappedUser = mapping.getUser();
                    if (!userService.existsByUserId(mappedUser.getId())) {
                        logger.warn("Found orphaned mapping pointing to deleted user. Cleaning up mapping for external ID: " + externalUserId);
                        externalIdMapRepository.delete(mapping);
                        cleanupPerformed = true;
                    }
                } catch (Exception e) {
                    logger.warn("Error checking mapped user existence, deleting potentially orphaned mapping: " + e.getMessage());
                    externalIdMapRepository.delete(mapping);
                    cleanupPerformed = true;
                }
            }
            
            // Check for orphaned users (users without OAuth mappings that should have them)
            if (userService.existsByEmail(email)) {
                try {
                    User user = userService.getUserByEmail(email);
                    
                    // If user has non-active status, they were likely orphaned during a previous OAuth flow
                    // Try to find their OAuth mapping - if none exists, they're orphaned
                    if (user.getStatus() != null && user.getStatus() != UserStatus.ACTIVE) {
                        try {
                            getMappingByUserEmail(email);
                            // If we get here, user has a mapping, so they're not orphaned
                            logger.info("User has OAuth mapping, not orphaned");
                        } catch (BaseNotFoundException e) {
                            // User has no OAuth mapping but exists - this is an orphaned user
                            logger.warn("Found orphaned user with no OAuth mapping and status: " + user.getStatus() + ". Cleaning up user: " + user.getId());
                            userService.deleteUserById(user.getId());
                            cleanupPerformed = true;
                        }
                    }
                } catch (Exception e) {
                    logger.warn("Error during orphaned user cleanup: " + e.getMessage());
                }
            }
            
            if (cleanupPerformed) {
                logger.info("Data consistency cleanup completed for email: " + email);
            } else {
                logger.info("No cleanup needed for email: " + email);
            }
            
        } catch (Exception e) {
            logger.error("Error during data consistency cleanup: " + e.getMessage());
        }
        
        return cleanupPerformed;
    }

    @Override
    public boolean isOAuthUser(UUID userId) {
        return externalIdMapRepository.existsByUserId(userId);
    }
}