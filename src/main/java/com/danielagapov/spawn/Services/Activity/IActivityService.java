package com.danielagapov.spawn.Services.Activity;

import com.danielagapov.spawn.DTOs.Activity.*;
import com.danielagapov.spawn.DTOs.ChatMessage.FullActivityChatMessageDTO;
import com.danielagapov.spawn.DTOs.User.UserDTO;
import com.danielagapov.spawn.Enums.ParticipationStatus;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Service interface for managing activities (events) and their related operations.
 * Provides CRUD operations, participation management, feed generation, and activity conversion utilities.
 */
public interface IActivityService {
    
    /**
     * Retrieves all activities from the database.
     * 
     * @return List of ActivityDTO objects representing all activities
     * @throws com.danielagapov.spawn.Exceptions.Base.BasesNotFoundException if database access fails
     */
    List<ActivityDTO> getAllActivities();

    /**
     * Retrieves a specific activity by its unique identifier.
     * 
     * @param id the unique identifier of the activity
     * @return ActivityDTO object representing the requested activity
     * @throws com.danielagapov.spawn.Exceptions.Base.BaseNotFoundException if activity with given ID is not found
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
     * @throws com.danielagapov.spawn.Exceptions.Base.BaseSaveException if saving fails
     */
    AbstractActivityDTO saveActivity(AbstractActivityDTO activity);

    /**
     * Creates a new activity from DTO and handles invitations.
     * 
     * @param activityDTO the DTO containing activity creation data
     * @return the created AbstractActivityDTO
     * @throws com.danielagapov.spawn.Exceptions.Base.BaseSaveException if creation fails
     * @throws com.danielagapov.spawn.Exceptions.Base.BaseNotFoundException if referenced entities don't exist
     */
    AbstractActivityDTO createActivity(ActivityDTO activityDTO);

    /**
     * Updates an existing activity or creates a new one if it doesn't exist.
     * 
     * @param activity the activity data to update with
     * @param activityId the unique identifier of the activity to update
     * @return the updated FullFeedActivityDTO
     * @throws com.danielagapov.spawn.Exceptions.Base.BaseSaveException if updating fails
     */
    FullFeedActivityDTO replaceActivity(ActivityDTO activity, UUID activityId);

    /**
     * Partially updates specific fields of an existing activity.
     * 
     * @param updates a map containing field names and their new values to update
     * @param activityId the unique identifier of the activity to update
     * @return the updated FullFeedActivityDTO
     * @throws com.danielagapov.spawn.Exceptions.Base.BaseNotFoundException if activity doesn't exist
     * @throws IllegalArgumentException if invalid field names or values are provided
     */
    FullFeedActivityDTO partialUpdateActivity(Map<String, Object> updates, UUID activityId);

    /**
     * Deletes an activity by its unique identifier.
     * 
     * @param id the unique identifier of the activity to delete
     * @return true if deletion was successful, false otherwise
     * @throws com.danielagapov.spawn.Exceptions.Base.BaseNotFoundException if activity with given ID is not found
     */
    boolean deleteActivityById(UUID id);

    /**
     * Retrieves all users participating in a specific activity.
     * 
     * @param id the unique identifier of the activity
     * @return List of UserDTO objects representing participating users
     * @throws com.danielagapov.spawn.Exceptions.Base.BaseNotFoundException if activity doesn't exist
     */
    List<UserDTO> getParticipatingUsersByActivityId(UUID id);

    /**
     * Gets the participation status of a user for a specific activity.
     * 
     * @param activityId the unique identifier of the activity
     * @param userId the unique identifier of the user
     * @return ParticipationStatus enum value indicating the user's participation status
     * @throws com.danielagapov.spawn.Exceptions.Base.BaseNotFoundException if activity or user doesn't exist
     */
    ParticipationStatus getParticipationStatus(UUID activityId, UUID userId);

    /**
     * Invites a user to an activity.
     * 
     * @param activityId the unique identifier of the activity
     * @param userId the unique identifier of the user to invite
     * @return true if invitation was successful, false otherwise
     * @throws com.danielagapov.spawn.Exceptions.Base.BaseNotFoundException if activity or user doesn't exist
     */
    boolean inviteUser(UUID activityId, UUID userId);

    /**
     * Toggles a user's participation status for an activity and returns the updated activity.
     * 
     * @param activityId the unique identifier of the activity
     * @param userId the unique identifier of the user
     * @return updated FullFeedActivityDTO with participants and invited users updated
     * @throws com.danielagapov.spawn.Exceptions.Base.BaseNotFoundException if activity or user doesn't exist
     */
    FullFeedActivityDTO toggleParticipation(UUID activityId, UUID userId);

    /**
     * Retrieves all activities that a user has been invited to.
     * 
     * @param id the unique identifier of the user
     * @return List of ActivityDTO objects representing activities the user was invited to
     * @throws com.danielagapov.spawn.Exceptions.Base.BaseNotFoundException if user doesn't exist
     */
    List<ActivityDTO> getActivitiesInvitedTo(UUID id);


    /**
     * Retrieves all activities that a user has been invited to as full feed activities.
     * 
     * @param id the unique identifier of the user
     * @return List of FullFeedActivityDTO objects with complete activity information
     * @throws com.danielagapov.spawn.Exceptions.Base.BaseNotFoundException if user doesn't exist
     */
    List<FullFeedActivityDTO> getFullActivitiesInvitedTo(UUID id);

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
     * @throws com.danielagapov.spawn.Exceptions.Base.BasesNotFoundException if database access fails
     */
    List<FullFeedActivityDTO> getAllFullActivities();

    /**
     * Retrieves a specific activity as a full feed activity for a requesting user.
     * 
     * @param id the unique identifier of the activity
     * @param requestingUserId the unique identifier of the user making the request
     * @return FullFeedActivityDTO with complete activity information
     * @throws com.danielagapov.spawn.Exceptions.Base.BaseNotFoundException if activity doesn't exist
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

    /**
     * Retrieves personalized feed activities for a user.
     * 
     * @param requestingUserId the unique identifier of the user requesting their feed
     * @return List of FullFeedActivityDTO objects representing the user's personalized feed
     * @throws com.danielagapov.spawn.Exceptions.Base.BaseNotFoundException if user doesn't exist
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
     * @throws com.danielagapov.spawn.Exceptions.Base.BaseNotFoundException if user doesn't exist
     */
    List<ActivityDTO> getActivitiesByOwnerId(UUID creatorUserId);

    
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

    /**
     * Retrieves all chat messages associated with a specific activity.
     * 
     * @param activityId the unique identifier of the activity
     * @return List of FullActivityChatMessageDTO objects representing the activity's chat messages
     * @throws com.danielagapov.spawn.Exceptions.Base.BaseNotFoundException if activity doesn't exist
     */
    List<FullActivityChatMessageDTO> getChatMessagesByActivityId(UUID activityId);
    
    /**
     * Auto-joins a user to an activity when they access it via a deep link.
     * If the user is not already invited or participating, they will be invited and automatically set to participating.
     * 
     * @param activityId The ID of the activity to auto-join
     * @param userId The ID of the user to auto-join to the activity
     * @return FullFeedActivityDTO with the updated activity information
     */
    FullFeedActivityDTO autoJoinUserToActivity(UUID activityId, UUID userId);
}
