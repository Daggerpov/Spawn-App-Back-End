package com.danielagapov.spawn.activity.api;

import com.danielagapov.spawn.activity.api.dto.*;
import com.danielagapov.spawn.chat.api.dto.FullActivityChatMessageDTO;
import com.danielagapov.spawn.user.api.dto.UserDTO;
import com.danielagapov.spawn.shared.util.ParticipationStatus;
import org.springframework.data.domain.Limit;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Public API for Activity module - exposes operations for other modules 
 * to access activity data without direct repository access.
 * 
 * This interface provides a clean module boundary, allowing other modules
 * (User, Chat, Analytics, etc.) to query and manage activity data without 
 * coupling to internal Activity module implementation details.
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
    
    // ==================== Activity CRUD Operations ====================
    
    /**
     * Retrieves all activities from the database.
     * 
     * @return List of ActivityDTO objects representing all activities
     */
    List<ActivityDTO> getAllActivities();

    /**
     * Retrieves a specific activity by its unique identifier.
     * 
     * @param id the unique identifier of the activity
     * @return ActivityDTO object representing the requested activity
     */
    ActivityDTO getActivityById(UUID id);
    
    /**
     * Gets activity information for external invites without requiring authentication
     * 
     * @param id The activity ID
     * @return ActivityInviteDTO with essential information for the invite page
     */
    ActivityInviteDTO getActivityInviteById(UUID id);

    /**
     * Saves an activity to the database.
     * 
     * @param activity the activity data to save
     * @return the saved AbstractActivityDTO
     */
    AbstractActivityDTO saveActivity(AbstractActivityDTO activity);

    /**
     * Creates a new activity from DTO and handles invitations.
     * 
     * @param activityDTO the DTO containing activity creation data
     * @return the created AbstractActivityDTO
     */
    AbstractActivityDTO createActivity(ActivityDTO activityDTO);
    
    /**
     * Creates a new activity from DTO and handles invitations, with friend suggestions.
     * 
     * @param activityDTO the DTO containing activity creation data
     * @return the created FullFeedActivityDTO
     */
    FullFeedActivityDTO createActivityWithSuggestions(ActivityDTO activityDTO);

    /**
     * Updates an existing activity or creates a new one if it doesn't exist.
     * 
     * @param activity the activity data to update with
     * @param activityId the unique identifier of the activity to update
     * @return the updated FullFeedActivityDTO
     */
    FullFeedActivityDTO replaceActivity(ActivityDTO activity, UUID activityId);

    /**
     * Partially updates specific fields of an existing activity.
     * 
     * @param updates the DTO containing field names and their new values to update
     * @param activityId the unique identifier of the activity to update
     * @return the updated FullFeedActivityDTO
     */
    FullFeedActivityDTO partialUpdateActivity(ActivityPartialUpdateDTO updates, UUID activityId);

    /**
     * Deletes an activity by its unique identifier.
     * 
     * @param id the unique identifier of the activity to delete
     * @return true if deletion was successful, false otherwise
     */
    boolean deleteActivityById(UUID id);

    // ==================== Participation Management ====================

    /**
     * Retrieves all users participating in a specific activity.
     * 
     * @param id the unique identifier of the activity
     * @return List of UserDTO objects representing participating users
     */
    List<UserDTO> getParticipatingUsersByActivityId(UUID id);

    /**
     * Gets the participation status of a user for a specific activity.
     * 
     * @param activityId the unique identifier of the activity
     * @param userId the unique identifier of the user
     * @return ParticipationStatus enum value indicating the user's participation status
     */
    ParticipationStatus getParticipationStatus(UUID activityId, UUID userId);

    /**
     * Invites a user to an activity.
     * 
     * @param activityId the unique identifier of the activity
     * @param userId the unique identifier of the user to invite
     * @return true if invitation was successful, false otherwise
     */
    boolean inviteUser(UUID activityId, UUID userId);

    /**
     * Toggles a user's participation status for an activity and returns the updated activity.
     * 
     * @param activityId the unique identifier of the activity
     * @param userId the unique identifier of the user
     * @return updated FullFeedActivityDTO with participants and invited users updated
     */
    FullFeedActivityDTO toggleParticipation(UUID activityId, UUID userId);

    // ==================== Activity Retrieval by User ====================

    /**
     * Retrieves all activities that a user has been invited to.
     * 
     * @param id the unique identifier of the user
     * @return List of ActivityDTO objects representing activities the user was invited to
     */
    List<ActivityDTO> getActivitiesInvitedTo(UUID id);


    /**
     * Retrieves all activities that a user has been invited to as full feed activities.
     * 
     * @param id the unique identifier of the user
     * @return List of FullFeedActivityDTO objects with complete activity information
     */
    List<FullFeedActivityDTO> getFullActivitiesInvitedTo(UUID id);

    /**
     * Retrieves all activities that a user is participating in as full feed activities.
     * 
     * @param id the unique identifier of the user
     * @return List of FullFeedActivityDTO objects with complete activity information
     */
    List<FullFeedActivityDTO> getFullActivitiesParticipatingIn(UUID id);

    // ==================== Full Activity Retrieval ====================

    /**
     * Converts an ActivityDTO to FullFeedActivityDTO with complete information for a requesting user.
     * 
     * @param activity the ActivityDTO to convert
     * @param requestingUserId the unique identifier of the user making the request
     * @param visitedActivities set of activity IDs already visited (for circular reference prevention)
     * @return FullFeedActivityDTO with complete activity information
     */
    FullFeedActivityDTO getFullActivityByActivity(ActivityDTO activity, UUID requestingUserId, Set<UUID> visitedActivities);

    /**
     * Retrieves all activities as full feed activities.
     * 
     * @return List of FullFeedActivityDTO objects representing all activities
     */
    List<FullFeedActivityDTO> getAllFullActivities();

    /**
     * Retrieves a specific activity as a full feed activity for a requesting user.
     * 
     * @param id the unique identifier of the activity
     * @param requestingUserId the unique identifier of the user making the request
     * @return FullFeedActivityDTO with complete activity information
     */
    FullFeedActivityDTO getFullActivityById(UUID id, UUID requestingUserId);

    /**
     * Converts a list of ActivityDTO objects to FullFeedActivityDTO objects for a requesting user.
     * 
     * @param activities the list of ActivityDTO objects to convert
     * @param requestingUserId the unique identifier of the user making the request
     * @return List of FullFeedActivityDTO objects with complete activity information
     */
    List<FullFeedActivityDTO> convertActivitiesToFullFeedActivities(List<ActivityDTO> activities, UUID requestingUserId);

    /**
     * Converts a list of ActivityDTO objects to FullFeedActivityDTO objects for self-owned activities.
     * 
     * @param activities the list of ActivityDTO objects to convert
     * @param requestingUserId the unique identifier of the user making the request
     * @return List of FullFeedActivityDTO objects with complete activity information marked as self-owned
     */
    List<FullFeedActivityDTO> convertActivitiesToFullFeedSelfOwnedActivities(List<ActivityDTO> activities, UUID requestingUserId);

    // ==================== Feed Operations ====================

    /**
     * Retrieves personalized feed activities for a user.
     * 
     * @param requestingUserId the unique identifier of the user requesting their feed
     * @return List of FullFeedActivityDTO objects representing the user's personalized feed
     */
    List<FullFeedActivityDTO> getFeedActivities(UUID requestingUserId);
    
    /**
     * Gets feed activities for a profile. If the profile user has no upcoming activities, returns past activities
     * that the profile user invited the requesting user to, with a flag indicating they are past activities.
     *
     * @param profileUserId The user ID of the profile being viewed
     * @param requestingUserId The user ID of the user viewing the profile
     * @return List of activities with a flag indicating if they are past activities
     */
    List<ProfileActivityDTO> getProfileActivities(UUID profileUserId, UUID requestingUserId);
    
    /**
     * Gets past activities where the specified user invited the requesting user
     * 
     * @param inviterUserId The user ID of the person who invited the requesting user
     * @param requestingUserId The user ID of the user viewing the profile
     * @return List of past activities where inviterUserId invited requestingUserId
     */
    List<ProfileActivityDTO> getPastActivitiesWhereUserInvited(UUID inviterUserId, UUID requestingUserId);

    /**
     * Retrieves all activities created by a specific user.
     * 
     * @param creatorUserId the unique identifier of the user who created the activities
     * @return List of ActivityDTO objects created by the specified user
     */
    List<ActivityDTO> getActivitiesByOwnerId(UUID creatorUserId);

    // ==================== Timestamp Queries ====================
    
    /**
     * Gets the timestamp of the latest activity created by the user.
     * 
     * @param userId The user ID to get the latest created activity timestamp for
     * @return The timestamp of the latest created activity, or null if none found
     */
    Instant getLatestCreatedActivityTimestamp(UUID userId);
    
    /**
     * Gets the timestamp of the latest activity the user was invited to.
     * 
     * @param userId The user ID to get the latest invited activity timestamp for
     * @return The timestamp of the latest invited activity, or null if none found
     */
    Instant getLatestInvitedActivityTimestamp(UUID userId);
    
    /**
     * Gets the timestamp of the latest update to any activity the user is participating in.
     * 
     * @param userId The user ID to get the latest updated activity timestamp for
     * @return The timestamp of the latest updated activity, or null if none found
     */
    Instant getLatestUpdatedActivityTimestamp(UUID userId);

    // ==================== Chat Message Queries ====================

    /**
     * Retrieves all chat messages associated with a specific activity.
     * 
     * @param activityId the unique identifier of the activity
     * @return List of FullActivityChatMessageDTO objects representing the activity's chat messages
     */
    List<FullActivityChatMessageDTO> getChatMessagesByActivityId(UUID activityId);
    
    // ==================== Auto-Join Operations ====================
    
    /**
     * Auto-joins a user to an activity when they access it via a deep link.
     * If the user is not already invited or participating, they will be invited and automatically set to participating.
     * 
     * @param activityId The ID of the activity to auto-join
     * @param userId The ID of the user to auto-join to the activity
     * @return FullFeedActivityDTO with the updated activity information
     */
    FullFeedActivityDTO autoJoinUserToActivity(UUID activityId, UUID userId);
    
    // ==================== Activity Info Queries (for external modules) ====================
    
    /**
     * Get the title of an activity.
     * Used by Chat module for notifications.
     * 
     * @param activityId The activity ID
     * @return The activity title, or null if activity not found
     */
    String getActivityTitle(UUID activityId);
    
    /**
     * Check if an activity exists by ID.
     * Used for validation before creating associated entities like chat messages.
     * 
     * @param activityId The activity ID to check
     * @return true if activity exists, false otherwise
     */
    boolean activityExists(UUID activityId);
}
