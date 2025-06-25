package com.danielagapov.spawn.Services.Activity;

import com.danielagapov.spawn.DTOs.Activity.*;
import com.danielagapov.spawn.DTOs.ChatMessage.FullActivityChatMessageDTO;
import com.danielagapov.spawn.DTOs.User.UserDTO;
import com.danielagapov.spawn.Enums.ParticipationStatus;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public interface IActivityService {
    List<ActivityDTO> getAllActivities();

    // CRUD operations:
    ActivityDTO getActivityById(UUID id);
    
    /**
     * Gets activity information for external invites without requiring authentication
     * 
     * @param id The activity ID
     * @return ActivityInviteDTO with essential information for the invite page
     */
    ActivityInviteDTO getActivityInviteById(UUID id);

    AbstractActivityDTO saveActivity(AbstractActivityDTO activity);

    AbstractActivityDTO createActivity(ActivityCreationDTO activityCreationDTO);

    ActivityDTO replaceActivity(ActivityDTO activity, UUID activityId);

    boolean deleteActivityById(UUID id);

    // Participation-related methods:
    List<UserDTO> getParticipatingUsersByActivityId(UUID id);

    ParticipationStatus getParticipationStatus(UUID activityId, UUID userId);

    boolean inviteUser(UUID activityId, UUID userId);

    // returns back the updated activity dto, with participants and invited users updated:
    FullFeedActivityDTO toggleParticipation(UUID activityId, UUID userId);

    List<ActivityDTO> getActivitiesInvitedTo(UUID id);

    List<ActivityDTO> getActivitiesInvitedToByFriendTagId(UUID friendTagId, UUID requestingUserId);

    // Get 'Full' Activity Methods:
    List<FullFeedActivityDTO> getFullActivitiesInvitedTo(UUID id);

    FullFeedActivityDTO getFullActivityByActivity(ActivityDTO activity, UUID requestingUserId, Set<UUID> visitedActivities);

    List<FullFeedActivityDTO> getAllFullActivities();

    FullFeedActivityDTO getFullActivityById(UUID id, UUID requestingUserId);

    List<FullFeedActivityDTO> convertActivitiesToFullFeedActivities(List<ActivityDTO> activities, UUID requestingUserId);

    List<FullFeedActivityDTO> convertActivitiesToFullFeedSelfOwnedActivities(List<ActivityDTO> activities, UUID requestingUserId);

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

    List<FullFeedActivityDTO> getFilteredFeedActivitiesByFriendTagId(UUID friendTagFilterId);

    // Additional Methods:
    List<ActivityDTO> getActivitiesByFriendTagId(UUID friendTagId);

    List<ActivityDTO> getActivitiesByOwnerId(UUID creatorUserId);

    String getFriendTagColorHexCodeForRequestingUser(ActivityDTO activityDTO, UUID requestingUserId);
    
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

    List<FullActivityChatMessageDTO> getChatMessagesByActivityId(UUID activityId);
}
