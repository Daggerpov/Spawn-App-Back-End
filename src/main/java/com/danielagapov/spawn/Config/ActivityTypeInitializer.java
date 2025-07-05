package com.danielagapov.spawn.Config;

import com.danielagapov.spawn.Exceptions.Logger.ILogger;
import com.danielagapov.spawn.Models.User.User;
import com.danielagapov.spawn.Repositories.User.IUserRepository;
import com.danielagapov.spawn.Services.ActivityType.IActivityTypeService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

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
                
                for (User user : allUsers) {
                    try {
                        // Check if user has any activity types
                        List<com.danielagapov.spawn.DTOs.ActivityType.ActivityTypeDTO> existingActivityTypes = 
                            activityTypeService.getActivityTypesByUserId(user.getId());
                        
                        if (existingActivityTypes.isEmpty()) {
                            // User has no activity types, initialize them
                            logger.info("Initializing default activity types for user: " + user.getUsername());
                            activityTypeService.initializeDefaultActivityTypesForUser(user);
                            usersInitialized++;
                        } else {
                            logger.info("User " + user.getUsername() + " already has " + 
                                existingActivityTypes.size() + " activity types. Skipping.");
                            usersSkipped++;
                        }
                    } catch (org.springframework.dao.DataIntegrityViolationException e) {
                        logger.error("Database constraint violation for user " + user.getUsername() + 
                            ". This user might have partial activity types or constraint issues: " + e.getMessage());
                        // Try to fetch activity types again to log current state
                        try {
                            List<com.danielagapov.spawn.DTOs.ActivityType.ActivityTypeDTO> currentActivityTypes = 
                                activityTypeService.getActivityTypesByUserId(user.getId());
                            logger.info("User " + user.getUsername() + " now has " + currentActivityTypes.size() + " activity types");
                        } catch (Exception ex) {
                            logger.error("Could not fetch activity types for user " + user.getUsername() + ": " + ex.getMessage());
                        }
                    } catch (Exception e) {
                        logger.error("Error initializing activity types for user " + user.getUsername() + ": " + e.getMessage());
                    }
                }
                
                logger.info("Activity type initialization completed: " + usersInitialized + 
                    " users initialized, " + usersSkipped + " users skipped");
                
            } catch (Exception e) {
                logger.error("Error during activity type initialization: " + e.getMessage());
            }
        };
    }
} 