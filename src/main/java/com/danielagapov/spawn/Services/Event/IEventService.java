package com.danielagapov.spawn.Services.Event;

import com.danielagapov.spawn.DTOs.EventDTO;
import com.danielagapov.spawn.DTOs.FullFeedEventDTO;
import com.danielagapov.spawn.DTOs.UserDTO;
import com.danielagapov.spawn.Enums.ParticipationStatus;

import java.util.List;
import java.util.UUID;

public interface IEventService {
    List<EventDTO> getAllEvents();
    
    // CRUD operations:
    EventDTO getEventById(UUID id);
    EventDTO saveEvent(EventDTO event);
    EventDTO replaceEvent(EventDTO event, UUID eventId);
    boolean deleteEventById(UUID id);

    // Participation-related methods:
    List<UserDTO> getParticipatingUsersByEventId(UUID id);
    ParticipationStatus getParticipationStatus(UUID eventId, UUID userId);
    boolean inviteUser(UUID eventId, UUID userId);
    boolean toggleParticipation(UUID eventId, UUID userId);
    List<EventDTO> getEventsInvitedTo(UUID id);
   

    // Get 'Full' Event Methods:
    List<FullFeedEventDTO> getFullEventsInvitedTo(UUID id);
    FullFeedEventDTO getFullEventByEvent(EventDTO event, UUID requestingUserId);
    List<FullFeedEventDTO> getAllFullEvents();
    FullFeedEventDTO getFullEventById(UUID id, UUID requestingUserId);
    List<FullFeedEventDTO> convertEventsToFullFeedEvents(List<EventDTO> events, UUID requestingUserId);

    // Additional Methods:
    List<EventDTO> getEventsByFriendTagId(UUID friendTagId);
    List<EventDTO> getEventsByUserId(UUID userId);
    String getFriendTagColorHexCodeForRequestingUser(EventDTO eventDTO, UUID requestingUserId);
}
