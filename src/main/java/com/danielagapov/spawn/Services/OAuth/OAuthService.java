package com.danielagapov.spawn.Services.OAuth;

import com.danielagapov.spawn.DTOs.User.BaseUserDTO;
import com.danielagapov.spawn.DTOs.User.UserCreationDTO;
import com.danielagapov.spawn.DTOs.User.UserDTO;
import com.danielagapov.spawn.Enums.EntityType;
import com.danielagapov.spawn.Enums.OAuthProvider;
import com.danielagapov.spawn.Exceptions.Base.BaseNotFoundException;
import com.danielagapov.spawn.Exceptions.IncorrectProviderException;
import com.danielagapov.spawn.Exceptions.Logger.ILogger;
import com.danielagapov.spawn.Mappers.UserMapper;
import com.danielagapov.spawn.Models.User.User;
import com.danielagapov.spawn.Models.User.UserIdExternalIdMap;
import com.danielagapov.spawn.Repositories.User.IUserIdExternalIdMapRepository;
import com.danielagapov.spawn.Services.User.IUserService;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken.Payload;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Service
public class OAuthService implements IOAuthService {
    private final IUserIdExternalIdMapRepository externalIdMapRepository;
    private final IUserService userService;
    private final ILogger logger;
    private GoogleIdTokenVerifier verifier;
    
    @Value("${google.client.id}")
    private String googleClientId;

    public OAuthService(IUserIdExternalIdMapRepository externalIdMapRepository, IUserService userService, ILogger logger) {
        this.externalIdMapRepository = externalIdMapRepository;
        this.userService = userService;
        this.logger = logger;
        
        // Create a temporary verifier that will be replaced in @PostConstruct
        // No audience specified initially - will be set in initializeGoogleVerifier()
        this.verifier = new GoogleIdTokenVerifier.Builder(new NetHttpTransport(), new GsonFactory()).build();
    }

    @Override
    public BaseUserDTO createUser(UserCreationDTO userCreationDTO, String externalUserId, OAuthProvider provider) {
        logger.info(String.format("Creating user with externalUserId: %s, provider: %s, email: %s", 
            externalUserId, provider, userCreationDTO.getEmail()));
        
        UserDTO newUser = new UserDTO(
                userCreationDTO.getId(),
                null,
                userCreationDTO.getUsername(),
                null, // going to set within `makeUser()`
                userCreationDTO.getName(),
                userCreationDTO.getBio(),
                null,
                userCreationDTO.getEmail()
        );

        logger.info("Calling makeUser with UserDTO: " + newUser);
        BaseUserDTO result = makeUser(newUser, externalUserId, userCreationDTO.getProfilePictureData(), provider);
        logger.info("User creation completed successfully. New user ID: " + result.getId());
        return result;
    }
    
    @Override
    public BaseUserDTO createUserWithGoogleToken(UserCreationDTO userCreationDTO, String idToken) {
        logger.info(String.format("Creating user with Google ID token, email: %s", userCreationDTO.getEmail()));
        
        // Verify the token and extract the user ID
        logger.info("Verifying Google ID token");
        String userId = verifyGoogleIdToken(idToken);
        logger.info("Token verified, extracted user ID: " + userId);
        
        UserDTO newUser = new UserDTO(
                userCreationDTO.getId(),
                null,
                userCreationDTO.getUsername(),
                null, // going to set within `makeUser()`
                userCreationDTO.getName(),
                userCreationDTO.getBio(),
                null,
                userCreationDTO.getEmail()
        );

        logger.info("Calling makeUser with extracted Google user ID: " + userId);
        BaseUserDTO result = makeUser(newUser, userId, userCreationDTO.getProfilePictureData(), OAuthProvider.google);
        logger.info("User creation with Google token completed successfully. New user ID: " + result.getId());
        return result;
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
            UserDTO userDTO = userService.saveUserWithProfilePicture(user, profilePicture);
            
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
    public BaseUserDTO makeUserWithGoogleToken(UserDTO user, String idToken, byte[] profilePicture) {
        logger.info("Making user with Google token for email: " + user.getEmail());
        
        // Verify the token and extract the user ID
        logger.info("Verifying Google ID token");
        String userId = verifyGoogleIdToken(idToken);
        logger.info("Token verified, extracted Google user ID: " + userId);
        
        // Use the regular makeUser method with the extracted user ID
        logger.info("Calling makeUser with Google user ID: " + userId);
        BaseUserDTO result = makeUser(user, userId, profilePicture, OAuthProvider.google);
        logger.info("makeUserWithGoogleToken completed successfully, user ID: " + result.getId());
        return result;
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
    public Optional<BaseUserDTO> getUserIfExistsByGoogleToken(String idToken, String email) {
        logger.info("Checking if user exists by Google ID token and email: " + email);
        
        // Verify the token and extract the user ID
        String userId = verifyGoogleIdToken(idToken);
        logger.info("Successfully verified Google ID token and extracted user ID: " + userId);
        
        // Use the extracted user ID to check if the user exists
        logger.info("Checking if user exists with Google user ID: " + userId);
        return getUserIfExistsbyExternalId(userId, email);
    }
    
    @Override
    public String verifyGoogleIdToken(String idToken) {
        try {
            logger.info("Attempting to verify Google ID token");
            logger.info("Using client ID: " + googleClientId);
            
            // Verify the token
            GoogleIdToken googleIdToken = verifier.verify(idToken);
            if (googleIdToken == null) {
                logger.error("Token verification failed - invalid token");
                throw new SecurityException("Invalid ID token");
            }
            
            logger.info("Token verified successfully");
            // Get payload data
            Payload payload = googleIdToken.getPayload();
            String userId = payload.getSubject();  // Get the user's ID
            logger.info("Extracted user ID: " + userId);
            
            // Verify additional claims if needed
            // For example, verify email is verified
            Boolean emailVerified = payload.getEmailVerified();
            if (emailVerified == null || !emailVerified) {
                logger.error("Email not verified");
                throw new SecurityException("Email not verified");
            }
            
            return userId;
            
        } catch (GeneralSecurityException | IOException e) {
            logger.error("Error verifying Google ID token: " + e.getMessage());
            logger.error("Token details: " + (idToken != null ? idToken.substring(0, Math.min(20, idToken.length())) + "..." : "null"));
            throw new SecurityException("Error verifying Google ID token", e);
        }
    }

    @Override
    public BaseUserDTO createUserFromOAuth(UserCreationDTO userCreationDTO, String externalUserId, String idToken, OAuthProvider provider) {
        try {
            logger.info(String.format("Creating user from OAuth: {username: %s, email: %s}", 
                userCreationDTO.getUsername(), userCreationDTO.getEmail()));
            
            // Handle Google ID token authentication if present
            if (idToken != null && !idToken.isEmpty()) {
                logger.info("Using Google ID token authentication");
                return createUserWithGoogleToken(userCreationDTO, idToken);
            } 
            // Fall back to externalUserId for Apple or backward compatibility
            else if (externalUserId != null && !externalUserId.isEmpty() && provider == OAuthProvider.apple) {
                logger.info("Using Apple external ID authentication");
                return createUser(userCreationDTO, externalUserId, provider);
            } else {
                logger.warn("Missing required authentication parameters");
                throw new IllegalArgumentException("Either Google ID token or Apple external user ID with provider must be provided");
            }
        } catch (SecurityException e) {
            logger.error("Security error during OAuth authentication: " + e.getMessage());
            throw e;
        } catch (Exception e) {
            logger.error("Unexpected error during OAuth user creation: " + e.getMessage());
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

    // Updated method with @PostConstruct to ensure client ID is loaded from properties
    @PostConstruct
    public void initializeGoogleVerifier() {
        // Try to get client ID from property, which should come from env variable
        String clientId = googleClientId;
        logger.info("Retrieved Google client ID from application properties: " + (clientId != null ? (clientId.substring(0, Math.min(10, clientId.length())) + "...") : "null"));
        
        // If not set in property, try to get directly from environment
        if (clientId == null || clientId.isEmpty()) {
            clientId = System.getenv("GOOGLE_CLIENT_ID");
            logger.info("Getting Google client ID directly from environment variable: " + (clientId != null ? (clientId.substring(0, Math.min(10, clientId.length())) + "...") : "null"));
        }
        
        // Re-initialize Google ID token verifier with client ID from properties or environment
        if (clientId != null && !clientId.isEmpty()) {
            logger.info("Initializing Google token verifier with client ID: " + clientId);
            this.verifier = new GoogleIdTokenVerifier.Builder(new NetHttpTransport(), new GsonFactory())
                .setAudience(Collections.singletonList(clientId))
                .build();
            logger.info("Google token verifier successfully initialized");
        } else {
            logger.error("Google client ID not set, token verification will fail. Set GOOGLE_CLIENT_ID in your environment.");
            // Create a dummy verifier that will reject all tokens
            this.verifier = new GoogleIdTokenVerifier.Builder(new NetHttpTransport(), new GsonFactory()).build();
            logger.warn("Created dummy verifier that will reject all tokens");
        }
    }
}