package com.danielagapov.spawn.shared.config;

import com.danielagapov.spawn.shared.exceptions.ILogger;
import com.danielagapov.spawn.user.internal.domain.User;
import com.danielagapov.spawn.user.internal.repositories.IUserRepository;
import com.danielagapov.spawn.activity.internal.services.IActivityTypeService;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import org.springframework.boot.CommandLineRunner;
import org.springframework.cache.CacheManager;
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
            CacheManager cacheManager,
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
                int cacheErrorsFixed = 0;
                
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
                        // Check if the root cause is a JSON parsing error (cache corruption)
                        Throwable cause = e;
                        boolean isJsonError = false;
                        while (cause != null && !isJsonError) {
                            if (cause instanceof JsonParseException || cause instanceof JsonMappingException) {
                                isJsonError = true;
                            }
                            // Also check for the specific error message from the logs
                            if (cause.getMessage() != null && cause.getMessage().contains("Could not read JSON")) {
                                isJsonError = true;
                            }
                            cause = cause.getCause();
                        }
                        
                        if (isJsonError) {
                            logger.warn("Cache corruption detected for user " + user.getUsername() + 
                                " (likely due to character encoding issues). Clearing cache and retrying...");
                            
                            try {
                                // Evict the corrupted cache entry
                                if (cacheManager.getCache("activityTypesByUserId") != null) {
                                    cacheManager.getCache("activityTypesByUserId").evict(user.getId());
                                }
                                
                                // Retry fetching activity types (will hit database now)
                                List<com.danielagapov.spawn.DTOs.ActivityType.ActivityTypeDTO> existingActivityTypes = 
                                    activityTypeService.getActivityTypesByUserId(user.getId());
                                
                                if (existingActivityTypes.isEmpty()) {
                                    // Initialize activity types for this user
                                    activityTypeService.initializeDefaultActivityTypesForUser(user);
                                    usersInitialized++;
                                    cacheErrorsFixed++;
                                    logger.info("Successfully recovered from cache corruption and initialized activity types for user: " + 
                                        user.getUsername());
                                } else {
                                    usersSkipped++;
                                    cacheErrorsFixed++;
                                    logger.info("Successfully recovered from cache corruption for user: " + user.getUsername());
                                }
                            } catch (Exception retryException) {
                                logger.error("Failed to recover from cache corruption for user " + user.getUsername() + 
                                    ": " + retryException.getMessage());
                                usersWithErrors++;
                            }
                        } else {
                            logger.error("Error initializing activity types for user " + user.getUsername() + ": " + e.getMessage());
                            usersWithErrors++;
                        }
                    }
                }
                
                logger.info("Activity type initialization completed: " + usersInitialized + 
                    " users initialized, " + usersSkipped + " users skipped, " + usersWithErrors + " users with errors" +
                    (cacheErrorsFixed > 0 ? ", " + cacheErrorsFixed + " cache errors automatically fixed" : ""));
                
                if (usersWithErrors > 0) {
                    logger.warn("Some users had errors during activity type initialization. " +
                        "This may indicate database constraint issues that need manual intervention.");
                }
                
                if (cacheErrorsFixed > 0) {
                    logger.info("Successfully recovered from " + cacheErrorsFixed + " cache corruption errors. " +
                        "The updated Redis cache configuration should prevent these issues in the future.");
                }
                
            } catch (Exception e) {
                logger.error("Error during activity type initialization: " + e.getMessage());
            }
        };
    }
} 