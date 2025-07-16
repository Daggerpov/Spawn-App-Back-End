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
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;


@Service
public class OAuthService implements IOAuthService {
    private final IUserIdExternalIdMapRepository externalIdMapRepository;
    private final IUserService userService;
    private final ILogger logger;
    private final Map<OAuthProvider, OAuthStrategy> oauthProviders;

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
                    // The mapping will be cascade deleted with the user
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
                        // The mapping will be cascade deleted with the user
                    } else {
                        logger.info("Returning existing active user with different provider");
                        return UserMapper.toDTO(existingUser);
                    }
                } catch (BaseNotFoundException e) {
                    logger.warn("User email exists but no mapping found - this may be due to data inconsistency. Cleaning up orphaned user.");
                    // Delete the orphaned user to allow new user creation
                    try {
                        User orphanedUser = userService.getUserByEmail(user.getEmail());
                        userService.deleteUserById(orphanedUser.getId());
                        logger.info("Orphaned user deleted: " + orphanedUser.getId() + " with email: " + orphanedUser.getEmail());
                    } catch (Exception deleteException) {
                        logger.error("Failed to delete orphaned user: " + deleteException.getMessage());
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
                logger.warn("User email exists but no mapping found - this may be due to data inconsistency. Treating as new user.");
                // Return empty Optional to indicate no user found
                return Optional.empty();
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
        // Retry logic to handle concurrent modifications
        int maxRetries = 3;
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                return checkOAuthRegistrationInternal(email, idToken, provider);
            } catch (org.springframework.dao.OptimisticLockingFailureException | 
                     org.hibernate.StaleObjectStateException e) {
                logger.warn("Concurrent modification detected on attempt " + attempt + "/" + maxRetries + 
                           " for external user: " + email + ". " + e.getMessage());
                
                if (attempt == maxRetries) {
                    logger.error("Failed to complete OAuth registration check after " + maxRetries + 
                               " attempts due to concurrent modifications");
                    throw new RuntimeException("Unable to process OAuth registration due to high concurrency. Please try again.");
                }
                
                // Wait briefly before retry to allow other transactions to complete
                try {
                    Thread.sleep(100 * attempt); // Progressive backoff
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Interrupted during OAuth registration retry");
                }
            }
        }
        return null; // Should never reach here
    }
    
    private String checkOAuthRegistrationInternal(String email, String idToken, OAuthProvider provider) {
        try {
            // Get the appropriate OAuth strategy
            OAuthStrategy oauthStrategy = oauthProviders.get(provider);
            if (idToken != null) {
                // Verify the token and extract the user ID
                String externalUserId = oauthStrategy.verifyIdToken(idToken);
                logger.info("Successfully verified " + provider + " ID token and extracted user ID: " + externalUserId);

                // Perform all checks atomically within the transaction
                UserIdExternalIdMap existingMapping = null;
                try {
                    existingMapping = externalIdMapRepository.findById(externalUserId).orElse(null);
                } catch (Exception e) {
                    logger.warn("Error checking existing mapping: " + e.getMessage());
                }

                User existingUserByEmail = null;
                try {
                    existingUserByEmail = userService.existsByEmail(email) ? userService.getUserByEmail(email) : null;
                } catch (Exception e) {
                    logger.warn("Error checking existing user by email: " + e.getMessage());
                }

                // Case 1: There's already a mapping for this externalId
                if (existingMapping != null) {
                    logger.info("Existing user detected in checkOAuthRegistration, mapping already exists");
                    User existingUser = existingMapping.getUser();
                    
                    // Only delete users that have non-active statuses (null is treated as active for backward compatibility)
                    if (existingUser.getStatus() != null && existingUser.getStatus() != UserStatus.ACTIVE) {
                        logger.info("Found incomplete user account (status: " + existingUser.getStatus() + "). Allowing re-registration.");
                        // Delete the incomplete user and their mapping to allow fresh registration
                        try {
                            userService.deleteUserById(existingUser.getId());
                            // The mapping will be cascade deleted with the user
                        } catch (Exception e) {
                            logger.warn("Error deleting incomplete user: " + e.getMessage());
                            // Continue with registration as the user might have been deleted by another transaction
                        }
                    } else {
                        // For ACTIVE users, return the external ID so the registration flow can handle it
                        // This allows the registerUserViaOAuth method to redirect to sign-in behavior
                        logger.info("Found active user attempting to register (status: " + existingUser.getStatus() + "). Returning external ID for sign-in redirection.");
                        return externalUserId;
                    }
                }

                // Case 2: There's already a Spawn user with this email address, but no mapping with this external id
                // In this case, the user signed in with a different provider initially, so we should not allow creation
                // with this provider
                if (existingUserByEmail != null) {
                    logger.info("Existing user detected in checkOAuthRegistration, email already exists");
                    try {
                        UserIdExternalIdMap externalIdMap = getMappingByUserEmail(email);
                        User existingUser = externalIdMap.getUser();
                        
                        // Only delete users that have non-active statuses (null is treated as active for backward compatibility)
                        if (existingUser.getStatus() != null && existingUser.getStatus() != UserStatus.ACTIVE) {
                            logger.info("Found incomplete user account with email (status: " + existingUser.getStatus() + "). Allowing re-registration.");
                            // Delete the incomplete user and their mapping to allow fresh registration
                            try {
                                userService.deleteUserById(existingUser.getId());
                                // The mapping will be cascade deleted with the user
                            } catch (Exception e) {
                                logger.warn("Error deleting incomplete user by email: " + e.getMessage());
                                // Continue with registration as the user might have been deleted by another transaction
                            }
                        } else {
                            OAuthProvider existingProvider = externalIdMap.getProvider();
                            String providerName = existingProvider == OAuthProvider.google ? "Google" : "Apple";
                            throw new IncorrectProviderException("Email already exists for a " + providerName + " account. Please login through " + providerName + " instead");
                        }
                    } catch (BaseNotFoundException e) {
                        logger.warn("User email exists but no mapping found - this may be due to data inconsistency. Cleaning up orphaned user.");
                        // Delete the orphaned user to allow new registration
                        try {
                            userService.deleteUserById(existingUserByEmail.getId());
                            logger.info("Orphaned user deleted: " + existingUserByEmail.getId() + " with email: " + existingUserByEmail.getEmail());
                        } catch (Exception deleteException) {
                            logger.error("Failed to delete orphaned user: " + deleteException.getMessage());
                        }
                        // Continue to Case 3 - treat as new user
                    }
                }

                // Case 3: This is a new user, neither the externalId nor the email exists in our database
                return externalUserId;

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

    @Override
    @Transactional
    public void createAndSaveMapping(User user, String externalUserId, OAuthProvider provider) {
        try {
            // Use upsert pattern by creating a new mapping with the same external ID
            // JPA will merge/update if the ID already exists, or create new if it doesn't
            // This prevents race conditions that occur with separate find-delete-create operations
            UserIdExternalIdMap mapping = new UserIdExternalIdMap(externalUserId, user, provider);
            logger.info(String.format("Upserting mapping for external ID: %s", externalUserId));
            
            UserIdExternalIdMap savedMapping = externalIdMapRepository.save(mapping);
            logger.info("Mapping successfully saved/updated: " + savedMapping);
        } catch (Exception e) {
            logger.error("Error creating/updating mapping: " + e.getMessage());
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
}