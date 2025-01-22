package com.danielagapov.spawn.Services.Event;

import com.danielagapov.spawn.DTOs.EventDTO;
import com.danielagapov.spawn.DTOs.FullFeedEventDTO;
import com.danielagapov.spawn.DTOs.UserDTO;
import com.danielagapov.spawn.Enums.ParticipationStatus;

import java.util.List;
import java.util.UUID;

public interface IEventService {
    List<EventDTO> getAllEvents();
    EventDTO getEventById(UUID id);
    FullFeedEventDTO getFullEventById(UUID id, UUID requestingUserId);
    List<EventDTO> getEventsByFriendTagId(UUID friendTagId);
    EventDTO saveEvent(EventDTO event);
    List<EventDTO> getEventsByUserId(UUID userId);
    EventDTO replaceEvent(EventDTO event, UUID eventId);
    boolean deleteEventById(UUID id);
    List<UserDTO> getParticipatingUsersByEventId(UUID id);
    ParticipationStatus getParticipationStatus(UUID eventId, UUID userId);
    boolean inviteUser(UUID eventId, UUID userId);
    boolean toggleParticipation(UUID eventId, UUID userId);
    List<EventDTO> getEventsInvitedTo(UUID id);
    List<FullFeedEventDTO> getFullEventsInvitedTo(UUID id);
    FullFeedEventDTO getFullEventByEvent(EventDTO event, UUID requestingUserId);
    String getFriendTagColorHexCodeForRequestingUser(EventDTO eventDTO, UUID requestingUserId);
}
