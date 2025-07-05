package com.danielagapov.spawn.Services.ActivityType;

import com.danielagapov.spawn.DTOs.ActivityType.ActivityTypeDTO;
import com.danielagapov.spawn.DTOs.ActivityType.BatchActivityTypeUpdateDTO;
import com.danielagapov.spawn.Models.ActivityType;
import com.danielagapov.spawn.Models.User.User;

import java.util.List;
import java.util.UUID;

public interface IActivityTypeService {
    
    /**
     * Get all activity types owned by a user
     * @param userId The user ID
     * @return List of activity types owned by the user
     */
    List<ActivityTypeDTO> getActivityTypesByUserId(UUID userId);
    
    /**
     * Batch update activity types (create, update, delete)
     * @param userId The user ID who owns the activity types
     * @param batchActivityTypeUpdateDTO The batch update data
     * @return List of updated activity types
     */
    List<ActivityTypeDTO> updateActivityTypes(UUID userId, BatchActivityTypeUpdateDTO batchActivityTypeUpdateDTO);
    
    /**
     * Initialize default activity types for a new user
     * @param user The user to initialize activity types for
     */
    void initializeDefaultActivityTypesForUser(User user);
    
    /**
     * Initialize default activity types for existing users if they don't have any
     * @param userId The user ID to initialize activity types for
     * @return List of activity types after initialization
     */
    List<ActivityTypeDTO> initializeDefaultActivityTypesForExistingUser(UUID userId);
    
    /**
     * Set the order number for an activity type
     * @param activityType The activity type to set order for
     */
    void setOrderNumber(ActivityType activityType);
} 