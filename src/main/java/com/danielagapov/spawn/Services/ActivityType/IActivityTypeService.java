package com.danielagapov.spawn.Services.ActivityType;

import com.danielagapov.spawn.DTOs.Activity.ActivityTypeDTO;
import com.danielagapov.spawn.DTOs.ActivityType.BatchActivityTypeUpdateDTO;

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
     * @param activityTypeDTO The activity type to create
     * @return The created activity type
     */
    ActivityTypeDTO createActivityType(ActivityTypeDTO activityTypeDTO);
    
    /**
     * Update an existing activity type
     * @param batchActivityTypeUpdateDTO The updated activity type data
     * @return The updated activity type
     */
    BatchActivityTypeUpdateDTO updateActivityTypes(BatchActivityTypeUpdateDTO batchActivityTypeUpdateDTO);
    
    /**
     * Delete an activity type
     * @param activityTypeId The activity type ID to delete
     */
    void deleteActivityType(UUID activityTypeId);
} 