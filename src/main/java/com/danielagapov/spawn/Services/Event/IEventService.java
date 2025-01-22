package com.danielagapov.spawn.Services.Event;

import com.danielagapov.spawn.DTOs.EventDTO;
import com.danielagapov.spawn.DTOs.FullEventDTO;
import com.danielagapov.spawn.DTOs.UserDTO;

import java.util.List;
import java.util.UUID;

public interface IEventService {
    List<EventDTO> getAllEvents();
    EventDTO getEventById(UUID id);
    FullEventDTO getFullEventById(UUID id);
    List<EventDTO> getEventsByFriendTagId(UUID friendTagId);
    EventDTO saveEvent(EventDTO event);
    List<EventDTO> getEventsByUserId(UUID userId);
    EventDTO replaceEvent(EventDTO event, UUID eventId);
    boolean deleteEventById(UUID id);
    List<UserDTO> getParticipatingUsersByEventId(UUID id);

    FullEventDTO getFullEventByEvent(EventDTO event);
}
