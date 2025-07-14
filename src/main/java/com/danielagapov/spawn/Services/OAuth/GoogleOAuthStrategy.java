package com.danielagapov.spawn.Services.OAuth;


import com.danielagapov.spawn.Enums.OAuthProvider;
import com.danielagapov.spawn.Exceptions.Logger.ILogger;
import com.danielagapov.spawn.Exceptions.TokenExpiredException;
import com.danielagapov.spawn.Exceptions.OAuthProviderUnavailableException;
import com.danielagapov.spawn.Util.RetryHelper;
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

@Service
public class GoogleOAuthStrategy implements OAuthStrategy {
    private final ILogger logger;
    private GoogleIdTokenVerifier verifier;

    @Value("${google.client.id}")
    private String googleClientId;


    @Autowired
    @Lazy
    public GoogleOAuthStrategy(ILogger logger) {
        this.logger = logger;
        this.verifier = new GoogleIdTokenVerifier.Builder(new NetHttpTransport(), new GsonFactory()).build();
    }

    @Override
    public OAuthProvider getOAuthProvider() {
        return OAuthProvider.google;
    }

    /**
     * Verifies a Google ID token and extracts the subject (user ID)
     *
     * @param idToken Google ID token to verify
     * @return the subject (user ID) extracted from the token
     */
    @Override
    public String verifyIdToken(String idToken) {
        try {
            logger.info("Attempting to verify Google ID token");
            logger.info("Using client ID: " + googleClientId);

            // Use retry helper for token verification
            return RetryHelper.executeOAuthWithRetry(() -> {
                try {
                    // Verify the token
                    GoogleIdToken googleIdToken = verifier.verify(idToken);
                    if (googleIdToken == null) {
                        logger.error("Token verification failed - invalid token");
                        throw new SecurityException("Invalid Google ID token - token may be expired or malformed");
                    }

                    logger.info("Token verified successfully");
                    // Get payload data
                    GoogleIdToken.Payload payload = googleIdToken.getPayload();
                    String userId = payload.getSubject();  // Get the user's ID
                    logger.info("Extracted user ID: " + userId);

                    // Check token expiration
                    Long expiration = payload.getExpirationTimeSeconds();
                    if (expiration != null && expiration < System.currentTimeMillis() / 1000) {
                        logger.error("Token has expired");
                        throw new TokenExpiredException("Google ID token has expired, please sign in again");
                    }

                    // Verify additional claims if needed
                    // For example, verify email is verified
                    Boolean emailVerified = payload.getEmailVerified();
                    if (emailVerified == null || !emailVerified) {
                        logger.error("Email not verified");
                        throw new SecurityException("Google account email is not verified");
                    }

                    return userId;

                } catch (GeneralSecurityException e) {
                    logger.error("Security error during token verification: " + e.getMessage());
                    throw new SecurityException("Security error during Google token verification: " + e.getMessage(), e);
                } catch (IOException e) {
                    logger.error("Network error during token verification: " + e.getMessage());
                    throw new OAuthProviderUnavailableException("Google authentication service is temporarily unavailable. Please try again later.", e);
                }
            });

        } catch (TokenExpiredException e) {
            logger.error("Token expired: " + e.getMessage());
            throw e;
        } catch (OAuthProviderUnavailableException e) {
            logger.error("OAuth provider unavailable: " + e.getMessage());
            throw e;
        } catch (Exception e) {
            logger.error("Unexpected error during token verification: " + e.getMessage());
            logger.error("Token details: " + (idToken != null ? idToken.substring(0, Math.min(20, idToken.length())) + "..." : "null"));
            throw new SecurityException("Unexpected error during Google token verification: " + e.getMessage(), e);
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
