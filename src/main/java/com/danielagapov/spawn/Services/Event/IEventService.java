package com.danielagapov.spawn.Services.Event;

import com.danielagapov.spawn.DTOs.Event.AbstractEventDTO;
import com.danielagapov.spawn.DTOs.Event.EventCreationDTO;
import com.danielagapov.spawn.DTOs.Event.EventDTO;
import com.danielagapov.spawn.DTOs.Event.FullFeedEventDTO;
import com.danielagapov.spawn.DTOs.Event.ProfileEventDTO;
import com.danielagapov.spawn.DTOs.User.UserDTO;
import com.danielagapov.spawn.Enums.ParticipationStatus;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public interface IEventService {
    List<EventDTO> getAllEvents();

    // CRUD operations:
    EventDTO getEventById(UUID id);

    AbstractEventDTO saveEvent(AbstractEventDTO event);

    AbstractEventDTO createEvent(EventCreationDTO eventCreationDTO);

    EventDTO replaceEvent(EventDTO event, UUID eventId);

    boolean deleteEventById(UUID id);

    // Participation-related methods:
    List<UserDTO> getParticipatingUsersByEventId(UUID id);

    ParticipationStatus getParticipationStatus(UUID eventId, UUID userId);

    boolean inviteUser(UUID eventId, UUID userId);

    // returns back the updated event dto, with participants and invited users updated:
    FullFeedEventDTO toggleParticipation(UUID eventId, UUID userId);

    List<EventDTO> getEventsInvitedTo(UUID id);

    List<EventDTO> getEventsInvitedToByFriendTagId(UUID friendTagId, UUID requestingUserId);

    // Get 'Full' Event Methods:
    List<FullFeedEventDTO> getFullEventsInvitedTo(UUID id);

    FullFeedEventDTO getFullEventByEvent(EventDTO event, UUID requestingUserId, Set<UUID> visitedEvents);

    List<FullFeedEventDTO> getAllFullEvents();

    FullFeedEventDTO getFullEventById(UUID id, UUID requestingUserId);

    List<FullFeedEventDTO> convertEventsToFullFeedEvents(List<EventDTO> events, UUID requestingUserId);

    List<FullFeedEventDTO> convertEventsToFullFeedSelfOwnedEvents(List<EventDTO> events, UUID requestingUserId);

    List<FullFeedEventDTO> getFeedEvents(UUID requestingUserId);
    
    /**
     * Gets feed events for a profile. If the profile user has no upcoming events, returns past events
     * that the profile user invited the requesting user to, with a flag indicating they are past events.
     *
     * @param profileUserId The user ID of the profile being viewed
     * @param requestingUserId The user ID of the user viewing the profile
     * @return List of events with a flag indicating if they are past events
     */
    List<ProfileEventDTO> getProfileEvents(UUID profileUserId, UUID requestingUserId);
    
    /**
     * Gets past events where the specified user invited the requesting user
     * 
     * @param inviterUserId The user ID of the person who invited the requesting user
     * @param requestingUserId The user ID of the user viewing the profile
     * @return List of past events where inviterUserId invited requestingUserId
     */
    List<ProfileEventDTO> getPastEventsWhereUserInvited(UUID inviterUserId, UUID requestingUserId);

    List<FullFeedEventDTO> getFilteredFeedEventsByFriendTagId(UUID friendTagFilterId);

    // Additional Methods:
    List<EventDTO> getEventsByFriendTagId(UUID friendTagId);

    List<EventDTO> getEventsByOwnerId(UUID creatorUserId);

    String getFriendTagColorHexCodeForRequestingUser(EventDTO eventDTO, UUID requestingUserId);
    
    /**
     * Gets the timestamp of the latest event created by the user.
     * 
     * @param userId The user ID to get the latest created event timestamp for
     * @return The timestamp of the latest created event, or null if none found
     */
    Instant getLatestCreatedEventTimestamp(UUID userId);
    
    /**
     * Gets the timestamp of the latest event the user was invited to.
     * 
     * @param userId The user ID to get the latest invited event timestamp for
     * @return The timestamp of the latest invited event, or null if none found
     */
    Instant getLatestInvitedEventTimestamp(UUID userId);
    
    /**
     * Gets the timestamp of the latest update to any event the user is participating in.
     * 
     * @param userId The user ID to get the latest updated event timestamp for
     * @return The timestamp of the latest updated event, or null if none found
     */
    Instant getLatestUpdatedEventTimestamp(UUID userId);
}
