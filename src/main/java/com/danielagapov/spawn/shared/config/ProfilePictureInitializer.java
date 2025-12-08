package com.danielagapov.spawn.shared.config;

import com.danielagapov.spawn.shared.exceptions.ILogger;
import com.danielagapov.spawn.user.internal.domain.User;
import com.danielagapov.spawn.user.internal.repositories.IUserRepository;
import com.danielagapov.spawn.media.internal.services.IS3Service;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * This class is responsible for initializing default profile picture URLs for users 
 * with NULL profile picture URL strings when the application starts.
 * It ensures all users have a valid profile picture URL.
 */
@Configuration
public class ProfilePictureInitializer {

    @Bean
    public CommandLineRunner initializeProfilePictures(
            IUserRepository userRepository,
            IS3Service s3Service,
            ILogger logger) {
        
        return args -> {
            try {
                logger.info("Starting profile picture initialization for users with NULL profile picture URLs");
                
                // Get all users from the database
                List<User> allUsers = userRepository.findAll();
                logger.info("Found " + allUsers.size() + " users in the database");
                
                int usersUpdated = 0;
                int usersSkipped = 0;
                int usersWithErrors = 0;
                
                // Get the default profile picture URL
                String defaultProfilePictureUrl = s3Service.getDefaultProfilePicture();
                
                if (defaultProfilePictureUrl == null || defaultProfilePictureUrl.trim().isEmpty()) {
                    logger.warn("Default profile picture URL is null or empty. Skipping profile picture initialization.");
                    return;
                }
                
                logger.info("Using default profile picture URL: " + defaultProfilePictureUrl);
                
                for (User user : allUsers) {
                    try {
                        String currentProfilePictureUrl = user.getProfilePictureUrlString();
                        
                        // Check if user has a NULL or empty profile picture URL
                        if (currentProfilePictureUrl == null || currentProfilePictureUrl.trim().isEmpty()) {
                            logger.info("Setting default profile picture for user: " + user.getUsername() + " (ID: " + user.getId() + ")");
                            
                            // Set the default profile picture URL
                            user.setProfilePictureUrlString(defaultProfilePictureUrl);
                            
                            // Save the updated user
                            userRepository.save(user);
                            usersUpdated++;
                            
                            logger.info("Successfully set default profile picture for user: " + user.getUsername());
                        } else {
                            usersSkipped++;
                        }
                    } catch (Exception e) {
                        logger.error("Error setting default profile picture for user " + user.getUsername() + 
                            " (ID: " + user.getId() + "): " + e.getMessage());
                        usersWithErrors++;
                    }
                }
                
                logger.info("Profile picture initialization completed: " + usersUpdated + 
                    " users updated, " + usersSkipped + " users skipped, " + usersWithErrors + " users with errors");
                
                if (usersWithErrors > 0) {
                    logger.warn("Some users had errors during profile picture initialization. " +
                        "This may indicate database or S3 service issues that need manual intervention.");
                }
                
            } catch (Exception e) {
                logger.error("Error during profile picture initialization: " + e.getMessage());
            }
        };
    }
}
