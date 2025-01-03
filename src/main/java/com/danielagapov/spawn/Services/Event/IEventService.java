package com.danielagapov.spawn.Services.Event;

import com.danielagapov.spawn.DTOs.EventDTO;

import java.util.List;
import java.util.UUID;

public interface IEventService {
    public List<EventDTO> getAllEvents();
    public EventDTO getEventById(UUID id);
    public List<EventDTO> getEventsByTagId(UUID tagId);
    public EventDTO saveEvent(EventDTO event);
    public List<EventDTO> getEventsByUserId(UUID userId);
    public EventDTO replaceEvent(EventDTO event, UUID eventId);
    public boolean deleteEventById(UUID id);
}
