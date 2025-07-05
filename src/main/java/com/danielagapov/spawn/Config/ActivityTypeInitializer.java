package com.danielagapov.spawn.Config;

import com.danielagapov.spawn.Exceptions.Logger.ILogger;
import com.danielagapov.spawn.Models.User.User;
import com.danielagapov.spawn.Repositories.User.IUserRepository;
import com.danielagapov.spawn.Services.ActivityType.IActivityTypeService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.List;

/**
 * This class is responsible for initializing default activity types for existing users 
 * when the application starts. It ensures all users have the default activity types.
 */
@Configuration
public class ActivityTypeInitializer {

    @Bean
    public CommandLineRunner initializeActivityTypes(
            IUserRepository userRepository,
            IActivityTypeService activityTypeService,
            ILogger logger) {
        
        return args -> {
            try {
                logger.info("Starting activity type initialization for existing users");
                
                // Get all users from the database
                List<User> allUsers = userRepository.findAll();
                logger.info("Found " + allUsers.size() + " users in the database");
                
                int usersInitialized = 0;
                int usersSkipped = 0;
                int usersWithErrors = 0;
                
                for (User user : allUsers) {
                    try {
                        // Check if user has any activity types
                        List<com.danielagapov.spawn.DTOs.ActivityType.ActivityTypeDTO> existingActivityTypes = 
                            activityTypeService.getActivityTypesByUserId(user.getId());
                        
                        if (existingActivityTypes.isEmpty()) {
                            // User has no activity types, initialize them
                            logger.info("Initializing default activity types for user: " + user.getUsername());
                            
                            // Initialize with retry logic for constraint violations
                            try {
                                activityTypeService.initializeDefaultActivityTypesForUser(user);
                                usersInitialized++;
                                logger.info("Successfully initialized activity types for user: " + user.getUsername());
                            } catch (DataIntegrityViolationException e) {
                                // Handle constraint violation more gracefully
                                if (e.getMessage().contains("Duplicate entry") || e.getMessage().contains("constraint")) {
                                    logger.warn("Constraint violation during initialization for user " + user.getUsername() + 
                                        ". Attempting to recover by checking current state...");
                                    
                                    // Re-check if user now has activity types (maybe partial success)
                                    List<com.danielagapov.spawn.DTOs.ActivityType.ActivityTypeDTO> currentActivityTypes = 
                                        activityTypeService.getActivityTypesByUserId(user.getId());
                                    
                                    if (currentActivityTypes.isEmpty()) {
                                        logger.error("User " + user.getUsername() + " still has no activity types after constraint violation. " +
                                            "This may indicate a database constraint issue. Will skip this user for now.");
                                        usersWithErrors++;
                                    } else {
                                        logger.info("User " + user.getUsername() + " now has " + currentActivityTypes.size() + 
                                            " activity types. Initialization appears to have succeeded despite constraint error.");
                                        usersInitialized++;
                                    }
                                } else {
                                    logger.error("Unexpected database constraint violation for user " + user.getUsername() + 
                                        ": " + e.getMessage());
                                    usersWithErrors++;
                                }
                            } catch (Exception e) {
                                logger.error("Unexpected error during initialization for user " + user.getUsername() + 
                                    ": " + e.getMessage());
                                usersWithErrors++;
                            }
                        } else {
                            usersSkipped++;
                        }
                    } catch (Exception e) {
                        logger.error("Error initializing activity types for user " + user.getUsername() + ": " + e.getMessage());
                        usersWithErrors++;
                    }
                }
                
                logger.info("Activity type initialization completed: " + usersInitialized + 
                    " users initialized, " + usersSkipped + " users skipped, " + usersWithErrors + " users with errors");
                
                if (usersWithErrors > 0) {
                    logger.warn("Some users had errors during activity type initialization. " +
                        "This may indicate database constraint issues that need manual intervention.");
                }
                
            } catch (Exception e) {
                logger.error("Error during activity type initialization: " + e.getMessage());
            }
        };
    }
} 