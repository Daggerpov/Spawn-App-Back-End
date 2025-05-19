package com.danielagapov.spawn.Services.OAuth;


import com.danielagapov.spawn.DTOs.User.BaseUserDTO;
import com.danielagapov.spawn.DTOs.User.UserCreationDTO;
import com.danielagapov.spawn.DTOs.User.UserDTO;
import com.danielagapov.spawn.Enums.OAuthProvider;
import com.danielagapov.spawn.Exceptions.Logger.ILogger;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Collections;
import java.util.Optional;

@Service
public class GoogleOAuthStrategy implements OAuthStrategy {
    private final ILogger logger;
    private final IOAuthService oauthService;
    private GoogleIdTokenVerifier verifier;

    @Value("${google.client.id}")
    private String googleClientId;


    @Autowired
    @Lazy
    public GoogleOAuthStrategy(ILogger logger, IOAuthService oauthService) {
        this.logger = logger;
        this.oauthService = oauthService;
        this.verifier = new GoogleIdTokenVerifier.Builder(new NetHttpTransport(), new GsonFactory()).build();
    }


    /**
     * Verifies a Google ID token and returns the user if they exist
     *
     * @param idToken Google ID token for authentication and verification
     * @param email user email
     * @return a BaseUserDTO if user exists, null otherwise
     */
    @Override
    public Optional<BaseUserDTO> getUserIfExistsByToken(String idToken, String email) {
        logger.info("Checking if user exists by Google ID token and email: " + email);

        // Verify the token and extract the user ID
        String userId = verifyGoogleIdToken(idToken);
        logger.info("Successfully verified Google ID token and extracted user ID: " + userId);

        // Use the extracted user ID to check if the user exists
        logger.info("Checking if user exists with Google user ID: " + userId);
        return oauthService.getUserIfExistsbyExternalId(userId, email);
    }

    @Override
    public OAuthProvider getOAuthProvider() {
        return OAuthProvider.google;
    }

    /**
     * Creates a user account using Google ID token for authentication
     *
     * @param userCreationDTO given from mobile, containing profile picture data
     * @param idToken        Google ID token for authentication and verification
     * @return returns the fully-created user
     */
    @Override
    public BaseUserDTO createUserWithToken(UserCreationDTO userCreationDTO, String idToken) {
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
        BaseUserDTO result = oauthService.makeUser(newUser, userId, userCreationDTO.getProfilePictureData(), OAuthProvider.google);
        logger.info("User creation with Google token completed successfully. New user ID: " + result.getId());
        return result;
    }

    /**
     * Verifies a Google ID token and extracts the subject (user ID)
     *
     * @param idToken Google ID token to verify
     * @return the subject (user ID) extracted from the token
     */
    private String verifyGoogleIdToken(String idToken) {
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
            GoogleIdToken.Payload payload = googleIdToken.getPayload();
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
