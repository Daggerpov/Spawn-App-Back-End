package com.danielagapov.spawn.Services.Event;

import java.util.List;
import com.danielagapov.spawn.Models.Event.Event;

public interface IEventService {
    public List<Event> getAllEvents();
    public Event getEventById(Long id);
    public List<Event> getEventsByTagId(Long tagId);
    public Event saveEvent(Event event);
    public List<Event> getEventsByUserId(Long userId);
}
