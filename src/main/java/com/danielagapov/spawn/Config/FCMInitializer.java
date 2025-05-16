package com.danielagapov.spawn.Config;

import com.danielagapov.spawn.Exceptions.Logger.ILogger;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Configuration;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Base64;

@Configuration
public class FCMInitializer {
    private final ILogger logger;

    public FCMInitializer(ILogger logger) {
        this.logger = logger;
    }

    @PostConstruct
    public void initialize() {
        try {
            // First check if app is already initialized
            if (!FirebaseApp.getApps().isEmpty()) {
                logger.info("Firebase application already initialized");
                return;
            }
            
            String credentials = System.getenv("GOOGLE_APPLICATION_CREDENTIALS");
            if (credentials == null || credentials.trim().isEmpty()) {
                logger.error("GOOGLE_APPLICATION_CREDENTIALS environment variable is not set or empty");
                return;
            }

            InputStream credentialsStream;
            // Check if the credential is a Base64 encoded string (common in deployment environments)
            if (credentials.trim().startsWith("{")) {
                // This is a JSON string
                credentialsStream = new ByteArrayInputStream(credentials.getBytes());
            } else {
                // This is likely a Base64 encoded string
                try {
                    byte[] decodedCredentials = Base64.getDecoder().decode(credentials);
                    credentialsStream = new ByteArrayInputStream(decodedCredentials);
                } catch (IllegalArgumentException e) {
                    logger.error("Failed to decode Base64 credentials: " + e.getMessage());
                    return;
                }
            }

            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(credentialsStream))
                    .build();

            FirebaseApp.initializeApp(options);
            logger.info("Firebase application initialized");
        } catch (IOException e) {
            logger.error("Error initializing Firebase: " + e.getMessage());
        }
    }
}
