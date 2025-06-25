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
     * Create a new activity type
     * @param activityTypeDTO The activity type to create (contains ownerUserId)
     * @return The created activity type
     */
    ActivityTypeDTO createActivityType(ActivityTypeDTO activityTypeDTO);
    
    /**
     * Batch update activity types (create, update, delete)
     * @param batchActivityTypeUpdateDTO The batch update data (activity types contain ownerUserId)
     * @return The batch update result
     */
    BatchActivityTypeUpdateDTO updateActivityTypes(BatchActivityTypeUpdateDTO batchActivityTypeUpdateDTO);
    
    /**
     * Delete an activity type
     * @param activityTypeId The activity type ID to delete
     */
    void deleteActivityType(UUID activityTypeId);
    
    /**
     * Toggle the pinned status of an activity type
     * @param activityTypeId The activity type ID to toggle pin status
     * @param userId The user ID who owns the activity type (for authorization)
     * @return The updated activity type DTO
     */
    ActivityTypeDTO togglePin(UUID activityTypeId, UUID userId);
    
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