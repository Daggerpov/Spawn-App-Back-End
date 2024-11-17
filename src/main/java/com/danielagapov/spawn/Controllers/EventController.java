package com.danielagapov.spawn.Controllers;

import com.danielagapov.spawn.Models.Event;
import com.danielagapov.spawn.Services.Event.IEventService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController()
@RequestMapping("/api/v1/events")
public class EventController {
    private final IEventService eventService;

    public EventController(IEventService eventService) {
        this.eventService = eventService;
    }

    @GetMapping("/")
    public String getEvents() {
        return "These are the events: " + eventService.getAllEvents();
    }

    @GetMapping("/mock-endpoint")
    public String getMockEndpoint() {
        return "This is the mock endpoint for events. Everything is working with it.";
    }

    @GetMapping("/{id}")
    public Event getEvent(@PathVariable Long id) {
        return eventService.getEventById(id);
    }

    // get event by tag
    @GetMapping("/tag/{id}")
    public List<Event> getEventsByTagId(@PathVariable Long id) {
        return eventService.getEventsByTagId(id);
    }

    @PostMapping("/")
    public Event createEvent(@RequestBody Event newEvent) {
        return eventService.saveEvent(newEvent);
    }
}