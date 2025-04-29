package com.danielagapov.spawn.Config;

import com.danielagapov.spawn.Exceptions.Logger.ILogger;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Configuration;

import java.io.ByteArrayInputStream;
import java.io.IOException;

@Configuration
public class FCMInitializer {

    @PostConstruct
    public void initialize(ILogger logger) {
        try {
            String credentials = System.getenv("GOOGLE_APPLICATION_CREDENTIALS");
            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(new ByteArrayInputStream(credentials.getBytes())))
                    .build();
            if (FirebaseApp.getApps().isEmpty()) {
                FirebaseApp.initializeApp(options);
                logger.info("Firebase application initialized");
            }
        } catch (IOException e) {
            logger.error("Error initializing Firebase: " + e.getMessage());
        }
    }
}
