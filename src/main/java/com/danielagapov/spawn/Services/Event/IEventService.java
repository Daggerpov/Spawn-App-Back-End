package com.danielagapov.spawn.Services.Event;

import com.danielagapov.spawn.DTOs.Event.AbstractEventDTO;
import com.danielagapov.spawn.DTOs.Event.EventCreationDTO;
import com.danielagapov.spawn.DTOs.Event.EventDTO;
import com.danielagapov.spawn.DTOs.Event.FullFeedEventDTO;
import com.danielagapov.spawn.DTOs.User.UserDTO;
import com.danielagapov.spawn.Enums.ParticipationStatus;
import com.danielagapov.spawn.Models.Event;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public interface IEventService {
    List<EventDTO> getAllEvents();

    // CRUD operations:
    EventDTO getEventById(UUID id);

    Event getEventEntityById(UUID id);

    AbstractEventDTO saveEvent(AbstractEventDTO event);

    AbstractEventDTO createEvent(EventCreationDTO eventCreationDTO);

    EventDTO replaceEvent(EventDTO event, UUID eventId);

    boolean deleteEventById(UUID id);

    EventDTO getEventDTOByEntity(Event event);

    // Participation-related methods:

    List<EventDTO> getEventsInvitedToByFriendTagId(UUID friendTagId, UUID requestingUserId);

    // Get 'Full' Event Methods:
    List<FullFeedEventDTO> getFullEventsInvitedTo(UUID id);

    FullFeedEventDTO getFullEventByEvent(EventDTO event, UUID requestingUserId, Set<UUID> visitedEvents);

    List<FullFeedEventDTO> getAllFullEvents();

    FullFeedEventDTO getFullEventById(UUID id, UUID requestingUserId);

    List<FullFeedEventDTO> convertEventsToFullFeedEvents(List<EventDTO> events, UUID requestingUserId);

    List<FullFeedEventDTO> convertEventsToFullFeedSelfOwnedEvents(List<EventDTO> events, UUID requestingUserId);

    // return type boolean represents whether the user was already invited or not
    // if false -> invites them
    // if true -> return 400 in Controller to indicate that the user has already
    // been invited, or it is a bad request.
    @Caching(evict = {
            @CacheEvict(value = "eventsInvitedTo", key = "#userId"),
            @CacheEvict(value = "fullEventsInvitedTo", key = "#userId"),
            @CacheEvict(value = "fullEventById", key = "#eventId.toString() + ':' + #userId.toString()"),
            @CacheEvict(value = "feedEvents", key = "#userId")
    })
    boolean inviteUser(UUID eventId, UUID userId);

    List<FullFeedEventDTO> getFeedEvents(UUID requestingUserId);

    List<FullFeedEventDTO> getFilteredFeedEventsByFriendTagId(UUID friendTagFilterId);

    // Additional Methods:
    List<EventDTO> getEventsByFriendTagId(UUID friendTagId);

    List<EventDTO> getEventsByOwnerId(UUID creatorUserId);

    String getFriendTagColorHexCodeForRequestingUser(EventDTO eventDTO, UUID requestingUserId);

    FullFeedEventDTO toggleParticipation(UUID eventId, UUID userId);

    List<EventDTO> getEventsInvitedTo(UUID userId);

    ParticipationStatus getParticipationStatus(UUID eventId, UUID userId);

    List<UserDTO> getParticipatingUsersByEventId(UUID id);
}
