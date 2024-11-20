package com.danielagapov.spawn.Controllers;

import com.danielagapov.spawn.DTOs.EventDTO;
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
    public EventDTO getEvent(@PathVariable Long id) {
        return eventService.getEventById(id);
    }
    
    
    // full path: /api/v1/events/user/{id}
    @GetMapping("user/{userId}")
    public List<EventDTO> getEventsByUserId(@PathVariable Long userId) {
        return eventService.getEventsByUserId(userId);
    }

    // full path: /api/v1/events/friendTag/{tagId}
    @GetMapping("friendTag/{tagId}")
    public List<EventDTO> getEventsByFriendTagId(@PathVariable Long tagId) {
        return eventService.getEventsByTagId(tagId);
    }

    // full path: /api/v1/events/mock-endpoint
    @GetMapping("mock-endpoint")
    public String getMockEndpoint() {
        return "This is the mock endpoint for events. Everything is working with it.";
    }

    // full path: /api/v1/events
    @PostMapping
    public EventDTO createEvent(@RequestBody EventDTO newEvent) {
        return eventService.saveEvent(newEvent);
    }

    // full path: /api/v1/events/{id}
    @PutMapping("{id}")
    public EventDTO replaceEvent(@RequestBody EventDTO newEvent, @PathVariable Long id) {
        return eventService.replaceEvent(newEvent, id);
    }
}
