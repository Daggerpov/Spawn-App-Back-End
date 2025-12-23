package com.danielagapov.spawn.activity.api;

import com.danielagapov.spawn.activity.api.dto.UserIdActivityTimeDTO;
import com.danielagapov.spawn.shared.util.ParticipationStatus;
import org.springframework.data.domain.Limit;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Public API for Activity module - exposes read-only operations
 * for other modules to access activity data without direct repository access.
 * 
 * This interface provides a clean module boundary, allowing other modules
 * (User, Chat, etc.) to query activity participation data without coupling
 * to internal Activity module implementation details.
 * 
 * Part of Phase 3: Shared Data Resolution in Spring Modulith refactoring.
 */
public interface IActivityService {
    
    // ==================== Participant Queries ====================
    
    /**
     * Get all participant user IDs for an activity with a specific status.
     * 
     * @param activityId The activity ID
     * @param status The participation status to filter by
     * @return List of user IDs with the specified participation status
     */
    List<UUID> getParticipantUserIdsByActivityIdAndStatus(UUID activityId, ParticipationStatus status);
    
    /**
     * Get all activity IDs a user is participating in with a specific status.
     * 
     * @param userId The user ID
     * @param status The participation status to filter by
     * @return List of activity IDs the user has the specified status for
     */
    List<UUID> getActivityIdsByUserIdAndStatus(UUID userId, ParticipationStatus status);
    
    /**
     * Check if a user is a participant in an activity with a specific status.
     * 
     * @param activityId The activity ID
     * @param userId The user ID
     * @param status The participation status to check
     * @return true if the user has the specified status for the activity
     */
    boolean isUserParticipantWithStatus(UUID activityId, UUID userId, ParticipationStatus status);
    
    /**
     * Get participant count for an activity with a specific status.
     * 
     * @param activityId The activity ID
     * @param status The participation status to filter by
     * @return The count of participants with the specified status
     */
    int getParticipantCountByStatus(UUID activityId, ParticipationStatus status);
    
    // ==================== Activity History Queries ====================
    
    /**
     * Get past activity IDs for a user.
     * Used for "recently spawned with" feature.
     * 
     * @param userId The user ID
     * @param status The participation status
     * @param now Current time for comparison
     * @param limit Maximum number of results
     * @return List of past activity IDs
     */
    List<UUID> getPastActivityIdsForUser(UUID userId, ParticipationStatus status, OffsetDateTime now, Limit limit);
    
    /**
     * Get other user IDs from a list of activities, excluding a specific user.
     * Used for "recently spawned with" recommendations.
     * 
     * @param activityIds List of activity IDs
     * @param excludeUserId User ID to exclude from results
     * @param status The participation status to filter by
     * @return List of user IDs with their most recent activity time
     */
    List<UserIdActivityTimeDTO> getOtherUserIdsByActivityIds(List<UUID> activityIds, UUID excludeUserId, ParticipationStatus status);
    
    // ==================== Shared Activities Queries ====================
    
    /**
     * Get count of activities two users have participated in together.
     * Used for friend recommendation scoring.
     * 
     * @param userId1 First user ID
     * @param userId2 Second user ID  
     * @param status The participation status to filter by
     * @return Number of shared activities
     */
    int getSharedActivitiesCount(UUID userId1, UUID userId2, ParticipationStatus status);
    
    // ==================== Activity Creator Queries ====================
    
    /**
     * Get the creator ID for an activity.
     * 
     * @param activityId The activity ID
     * @return The creator's user ID, or null if activity not found
     */
    UUID getActivityCreatorId(UUID activityId);
    
    /**
     * Get all activity IDs created by a user.
     * 
     * @param userId The creator's user ID
     * @return List of activity IDs created by the user
     */
    List<UUID> getActivityIdsCreatedByUser(UUID userId);
}


