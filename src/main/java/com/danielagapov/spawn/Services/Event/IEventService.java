package com.danielagapov.spawn.Services.Event;

import java.util.List;

import com.danielagapov.spawn.DTOs.EventDTO;

public interface IEventService {
    public List<EventDTO> getAllEvents();
    public EventDTO getEventById(Long id);
    public List<EventDTO> getEventsByTagId(Long tagId);
    public EventDTO saveEvent(EventDTO event);
    public List<EventDTO> getEventsByUserId(Long userId);
    public EventDTO replaceEvent(EventDTO event, Long eventId);
}
