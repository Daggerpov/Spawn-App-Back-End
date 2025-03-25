package com.danielagapov.spawn.Services.Event;

import com.danielagapov.spawn.DTOs.Event.AbstractEventDTO;
import com.danielagapov.spawn.DTOs.Event.EventCreationDTO;
import com.danielagapov.spawn.DTOs.Event.EventDTO;
import com.danielagapov.spawn.DTOs.Event.FullFeedEventDTO;
import com.danielagapov.spawn.Models.Event;

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

    // Participation-related methods:

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

    List<FullFeedEventDTO> getFilteredFeedEventsByFriendTagId(UUID friendTagFilterId);

    // Additional Methods:
    List<EventDTO> getEventsByFriendTagId(UUID friendTagId);

    List<EventDTO> getEventsByOwnerId(UUID creatorUserId);

    String getFriendTagColorHexCodeForRequestingUser(EventDTO eventDTO, UUID requestingUserId);
}
