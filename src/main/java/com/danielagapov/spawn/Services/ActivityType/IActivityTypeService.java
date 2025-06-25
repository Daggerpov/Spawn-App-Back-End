package com.danielagapov.spawn.Services.ActivityType;

import com.danielagapov.spawn.DTOs.ActivityType.ActivityTypeDTO;
import com.danielagapov.spawn.DTOs.ActivityType.BatchActivityTypeUpdateDTO;
import com.danielagapov.spawn.Models.ActivityType;
import com.danielagapov.spawn.Models.User.User;

import java.util.List;
import java.util.UUID;

public interface IActivityTypeService {
    
    /**
     * Get all activity types for a user with their pinning status
     * @param userId The user ID
     * @return List of activity types with pinning information
     */
    List<ActivityTypeDTO> getActivityTypesByUserId(UUID userId);
    
    /**
     * Get the IDs of pinned activity types for a user
     * @param userId The user ID
     * @return List of pinned activity type IDs
     */
    List<UUID> getPinnedActivityTypeIds(UUID userId);
    
    /**
     * Toggle the pin status of an activity type for a user
     * @param userId The user ID
     * @param activityTypeId The activity type ID
     * @param isPinned The new pin status
     */
    void toggleActivityTypePin(UUID userId, UUID activityTypeId, Boolean isPinned);
    
    /**
     * Create a new activity type
     * @param userId The user ID who is creating the activity type
     * @param activityTypeDTO The activity type to create
     * @return The created activity type
     */
    ActivityTypeDTO createActivityType(UUID userId, ActivityTypeDTO activityTypeDTO);
    
    /**
     * Batch update activity types (create, update, delete)
     * @param userId The user ID
     * @param batchActivityTypeUpdateDTO The batch update data
     * @return The batch update result
     */
    BatchActivityTypeUpdateDTO updateActivityTypes(UUID userId, BatchActivityTypeUpdateDTO batchActivityTypeUpdateDTO);
    
    /**
     * Delete an activity type
     * @param activityTypeId The activity type ID to delete
     */
    void deleteActivityType(UUID activityTypeId);
    
    /**
     * Initialize default activity types for a new user
     * @param user The user to initialize activity types for
     */
    void initializeDefaultActivityTypesForUser(User user);
    
    /**
     * Set the order number for an activity type
     * @param activityType The activity type to set order for
     */
    void setOrderNumber(ActivityType activityType);
} 