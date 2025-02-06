package com.danielagapov.spawn.Services.Event;

import com.danielagapov.spawn.DTOs.*;
import com.danielagapov.spawn.Enums.ParticipationStatus;

import java.util.List;
import java.util.UUID;

public interface IEventService {
    List<EventDTO> getAllEvents();
    
    // CRUD operations:
    EventDTO getEventById(UUID id);
    IEventDTO saveEvent(IEventDTO event);
    IEventDTO createEvent(EventCreationDTO eventCreationDTO);
    EventDTO replaceEvent(EventDTO event, UUID eventId);
    boolean deleteEventById(UUID id);

    // Participation-related methods:
    List<UserDTO> getParticipatingUsersByEventId(UUID id);
    ParticipationStatus getParticipationStatus(UUID eventId, UUID userId);
    boolean inviteUser(UUID eventId, UUID userId);
    // returns back the updated event dto, with participants and invited users updated:
    FullFeedEventDTO toggleParticipation(UUID eventId, UUID userId);
    List<EventDTO> getEventsInvitedTo(UUID id);
   

    // Get 'Full' Event Methods:
    List<FullFeedEventDTO> getFullEventsInvitedTo(UUID id);
    FullFeedEventDTO getFullEventByEvent(EventDTO event, UUID requestingUserId);
    List<FullFeedEventDTO> getAllFullEvents();
    FullFeedEventDTO getFullEventById(UUID id, UUID requestingUserId);
    List<FullFeedEventDTO> convertEventsToFullFeedEvents(List<EventDTO> events, UUID requestingUserId);
    List<FullFeedEventDTO> convertEventsToFullFeedSelfOwnedEvents(List<EventDTO> events, UUID requestingUserId);
    List<FullFeedEventDTO> getFeedEvents(UUID requestingUserId);

    // Additional Methods:
    List<EventDTO> getEventsByFriendTagId(UUID friendTagId);
    List<EventDTO> getEventsByOwnerId(UUID creatorUserId);
    String getFriendTagColorHexCodeForRequestingUser(EventDTO eventDTO, UUID requestingUserId);

    List<FullFeedEventDTO> getFilteredFeedEvents(UUID requestingUserId, UUID friendTagFilterId);
}
