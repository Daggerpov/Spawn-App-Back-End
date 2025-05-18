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
        this.verifier = new GoogleIdTokenVerifier.Builder(new NetHttpTransport(), new GsonFactory()).build();
    }

    @Override
    public BaseUserDTO createUser(UserCreationDTO userCreationDTO, String externalUserId, OAuthProvider provider) {
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

        return makeUser(newUser, externalUserId, userCreationDTO.getProfilePictureData(), provider);
    }
    
    @Override
    public BaseUserDTO createUserWithGoogleToken(UserCreationDTO userCreationDTO, String idToken) {
        // Verify the token and extract the user ID
        String userId = verifyGoogleIdToken(idToken);
        
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

        return makeUser(newUser, userId, userCreationDTO.getProfilePictureData(), OAuthProvider.google);
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
        // Verify the token and extract the user ID
        String userId = verifyGoogleIdToken(idToken);
        
        // Use the regular makeUser method with the extracted user ID
        return makeUser(user, userId, profilePicture, OAuthProvider.google);
    }


    @Override
    public Optional<BaseUserDTO> getUserIfExistsbyExternalId(String externalUserId, String email) {
        boolean existsByExternalId = mappingExistsByExternalId(externalUserId);
        boolean existsByEmail = userService.existsByEmail(email);

        if (existsByExternalId) { // A Spawn account exists with this external id, return the associated `BaseUserDTO`
            User user = getMapping(externalUserId).getUser();
            return Optional.of(UserMapper.toDTO(user));
        } else if (existsByEmail) { // A Spawn account exists with this email but not with the external id which indicates a sign-in with incorrect provider
            UserIdExternalIdMap externalIdMap = getMappingByUserEmail(email);
            String provider = String.valueOf(externalIdMap.getProvider()).equals("google") ? "Google" : "Apple";
            throw new IncorrectProviderException("The email: " + email + " is already associated to a " + provider + " account. Please login through " + provider + " instead");
        } else { // No account exists for this external id or email
            return Optional.empty();
        }
    }
    
    @Override
    public Optional<BaseUserDTO> getUserIfExistsByGoogleToken(String idToken, String email) {
        // Verify the token and extract the user ID
        String userId = verifyGoogleIdToken(idToken);
        
        // Use the extracted user ID to check if the user exists
        return getUserIfExistsbyExternalId(userId, email);
    }
    
    @Override
    public String verifyGoogleIdToken(String idToken) {
        try {
            // Verify the token
            GoogleIdToken googleIdToken = verifier.verify(idToken);
            if (googleIdToken == null) {
                throw new SecurityException("Invalid ID token");
            }
            
            // Get payload data
            Payload payload = googleIdToken.getPayload();
            String userId = payload.getSubject();  // Get the user's ID
            
            // Verify additional claims if needed
            // For example, verify email is verified
            Boolean emailVerified = payload.getEmailVerified();
            if (emailVerified == null || !emailVerified) {
                throw new SecurityException("Email not verified");
            }
            
            return userId;
            
        } catch (GeneralSecurityException | IOException e) {
            logger.error("Error verifying Google ID token: " + e.getMessage());
            throw new SecurityException("Error verifying Google ID token", e);
        }
    }

    /* ------------------------------ HELPERS ------------------------------ */

    private boolean mappingExistsByExternalId(String externalUserId) {
        return externalIdMapRepository.existsById(externalUserId);
    }

    private UserIdExternalIdMap getMapping(String externalId) {
        try {
            return externalIdMapRepository.findById(externalId).orElse(null);
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
        return externalIdMapRepository.findByUserEmail(email).orElseThrow(() -> new BaseNotFoundException(EntityType.ExternalIdMap, email, "email"));
    }

    // Updated method with @PostConstruct to ensure client ID is loaded from properties
    @PostConstruct
    public void initializeGoogleVerifier() {
        // Re-initialize Google ID token verifier with client ID from application properties
        if (googleClientId != null && !googleClientId.isEmpty()) {
            logger.info("Initializing Google token verifier with client ID: " + googleClientId);
            this.verifier = new GoogleIdTokenVerifier.Builder(new NetHttpTransport(), new GsonFactory())
                .setAudience(Collections.singletonList(googleClientId))
                .build();
        } else {
            logger.warn("Google client ID not set, token verification may fail");
        }
    }
}