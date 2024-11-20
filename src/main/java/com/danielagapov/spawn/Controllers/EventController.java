package com.danielagapov.spawn.Controllers;

import com.danielagapov.spawn.Models.Event.Event;
import com.danielagapov.spawn.Services.Event.IEventService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController()
@RequestMapping("api/v1/events")
public class EventController {
    private final IEventService eventService;

    public EventController(IEventService eventService) {
        this.eventService = eventService;
    }

    // full path: /api/v1/events
    @GetMapping
    public String getEvents() {
        return "These are the events: " + eventService.getAllEvents();
    }

    // full path: /api/v1/events/{id}
    @GetMapping("{id}")
    public Event getEvent(@PathVariable Long id) {
        return eventService.getEventById(id);
    }

    // full path: /api/v1/events/{id}
    @PutMapping("{id}")
    public Event putEvent(@PathVariable Long id) {
        return eventService.updateEventById(id);
    }

    // full path: /api/v1/events/user/{id}
    @GetMapping("user/{userId}")
    public List<Event> getEventsByUserId(@PathVariable Long userId) {
        return eventService.getEventsByUserId(userId);
    }

    // full path: /api/v1/events/friendTag/{friendTagId}
    @GetMapping("friendTag/{tagId}")
    public List<Event> getEventsByFriendTagId(@PathVariable Long tagId) {
        return eventService.getEventsByTagId(tagId);
    }

    // full path: /api/v1/events/mock-endpoint
    @GetMapping("mock-endpoint")
    public String getMockEndpoint() {
        return "This is the mock endpoint for events. Everything is working with it.";
    }

    // full path: /api/v1/events
    @PostMapping
    public Event createEvent(@RequestBody Event newEvent) {
        return eventService.saveEvent(newEvent);
    }
}