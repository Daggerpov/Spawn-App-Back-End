package com.danielagapov.spawn.Services.OAuth;


import com.danielagapov.spawn.DTOs.User.BaseUserDTO;
import com.danielagapov.spawn.DTOs.User.UserCreationDTO;
import com.danielagapov.spawn.DTOs.User.UserDTO;
import com.danielagapov.spawn.Enums.EntityType;
import com.danielagapov.spawn.Enums.OAuthProvider;
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
                return UserMapper.toDTO(existingUser);
            }
            
            // Case 2: There's already a Spawn user with this email address, but no mapping with this external id
            // In this case, the user signed in with a different provider initially, so we should not allow creation
            // with this provider
            if (existsByEmail) {
                logger.info("Existing user detected in makeUser, email already exists");
                UserIdExternalIdMap externalIdMap = getMappingByUserEmail(user.getEmail());
                return UserMapper.toDTO(externalIdMap.getUser());
            }
            
            // Case 3: This is a new user, neither the externalId nor the email exists in our database
            // Save the user with profile picture
            UserDTO userDTO = userService.createAndSaveUserWithProfilePicture(user, profilePicture);
            
            // Save the mapping for the new user to the external id
            logger.info(String.format("External user detected, saving mapping: {externalUserId: %s, userDTO: %s}", externalUserId, userDTO));
            createAndSaveMapping(externalUserId, userDTO, provider);

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
    public Optional<BaseUserDTO> signInUser(String idToken, String email, OAuthProvider provider) {
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
    public Optional<BaseUserDTO> getUserIfExistsbyExternalId(String externalUserId, String email) {
        logger.info("Checking if user exists by external ID: " + externalUserId + " and email: " + email);
        boolean existsByExternalId = mappingExistsByExternalId(externalUserId);
        boolean existsByEmail = userService.existsByEmail(email);
        logger.info("User exists by externalId: " + existsByExternalId + ", exists by email: " + existsByEmail);

        if (existsByExternalId) { // A Spawn account exists with this external id, return the associated `BaseUserDTO`
            logger.info("Found existing user by external ID: " + externalUserId);
            User user = getMapping(externalUserId).getUser();
            BaseUserDTO userDTO = UserMapper.toDTO(user);
            logger.info("Returning user with ID: " + userDTO.getId() + " and username: " + userDTO.getUsername());
            return Optional.of(userDTO);
        } else if (existsByEmail) { // A Spawn account exists with this email but not with the external id which indicates a sign-in with incorrect provider
            logger.info("Found existing user by email but not by external ID. This indicates an incorrect provider login attempt.");
            UserIdExternalIdMap externalIdMap = getMappingByUserEmail(email);
            String provider = String.valueOf(externalIdMap.getProvider()).equals("google") ? "Google" : "Apple";
            logger.info("Expected provider for this email: " + provider);
            throw new IncorrectProviderException("The email: " + email + " is already associated to a " + provider + " account. Please login through " + provider + " instead");
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
    public String checkOAuthRegistration(String email, String idToken, OAuthProvider provider) {
        try {
            // Get the appropriate OAuth strategy
            OAuthStrategy oauthStrategy = oauthProviders.get(provider);
            if (idToken != null) {
                // Verify the token and extract the user ID
                String externalUserId = oauthStrategy.verifyIdToken(idToken);
                logger.info("Successfully verified " + provider + " ID token and extracted user ID: " + externalUserId);

                // Check if this external user already exists
                boolean existsByExternalId = mappingExistsByExternalId(externalUserId);

                // Check if a user exists with this email
                boolean existsByEmail = userService.existsByEmail(email);

                // Case 1: There's already a mapping for this externalId
                if (existsByExternalId) {
                    logger.info("Existing user detected in makeUser, mapping already exists");
                    throw new AccountAlreadyExistsException("External ID already exists");
                }

                // Case 2: There's already a Spawn user with this email address, but no mapping with this external id
                // In this case, the user signed in with a different provider initially, so we should not allow creation
                // with this provider
                if (existsByEmail) {
                    logger.info("Existing user detected in makeUser, email already exists");
                    throw new IncorrectProviderException("Email already exists for a " + provider + " account. Please login through " + provider + " instead");
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
    public void createAndSaveMapping(User user, String externalUserId, OAuthProvider provider) {
        try {
            UserIdExternalIdMap mapping = new UserIdExternalIdMap(externalUserId, user, provider);
            logger.info(String.format("Saving mapping: {mapping: %s}", mapping));
            externalIdMapRepository.save(mapping);
            logger.info("Mapping saved");
        } catch (Exception e) {
            logger.error(e.getMessage());
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

    private void createAndSaveMapping(String externalUserId, UserDTO userDTO, OAuthProvider provider) {
        try {
            User user = UserMapper.toEntity(userDTO);
            UserIdExternalIdMap mapping = new UserIdExternalIdMap(externalUserId, user, provider);
            logger.info(String.format("Saving mapping: {mapping: %s}", mapping));
            externalIdMapRepository.save(mapping);
            logger.info("Mapping saved");
        } catch (Exception e) {
            logger.error(e.getMessage());
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