package com.danielagapov.spawn.activity.internal.services;

import com.danielagapov.spawn.activity.api.dto.ActivityTypeDTO;
import com.danielagapov.spawn.activity.api.dto.ActivityTypeFriendSuggestionDTO;
import com.danielagapov.spawn.activity.api.dto.BatchActivityTypeUpdateDTO;
import com.danielagapov.spawn.activity.internal.domain.ActivityType;
import com.danielagapov.spawn.user.internal.domain.User;

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
     * Set the order number for an activity type
     * @param activityType The activity type to set order for
     */
    void setOrderNumber(ActivityType activityType);
    
    /**
     * Get friend suggestions for an activity type
     * @param activityTypeId The activity type ID
     * @param userId The user ID who owns the activity type
     * @return Friend suggestion data or null if no suggestions
     */
    ActivityTypeFriendSuggestionDTO getFriendSuggestionsForActivityType(UUID activityTypeId, UUID userId);
    
    /**
     * Automatically add all user's friends to an activity type if it has no associated friends
     * @param activityTypeId The activity type ID
     * @param userId The user ID who owns the activity type
     * @return true if friends were added, false otherwise
     */
    boolean autoAddFriendsToActivityType(UUID activityTypeId, UUID userId);
} 