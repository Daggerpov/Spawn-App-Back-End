package com.danielagapov.spawn.Services.Event;

import java.util.List;
import java.util.UUID;

import com.danielagapov.spawn.DTOs.EventDTO;

public interface IEventService {
    public List<EventDTO> getAllEvents();
    public EventDTO getEventById(UUID id);
    public List<EventDTO> getEventsByTagId(UUID tagId);
    public EventDTO saveEvent(EventDTO event);
    public List<EventDTO> getEventsByUserId(UUID userId);
    public EventDTO replaceEvent(EventDTO event, UUID eventId);
}
