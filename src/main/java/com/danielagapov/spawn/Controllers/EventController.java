package com.danielagapov.spawn.Controllers;

import com.danielagapov.spawn.DTOs.EventDTO;
import com.danielagapov.spawn.DTOs.UserDTO;
import com.danielagapov.spawn.Exceptions.Base.BaseNotFoundException;
import com.danielagapov.spawn.Services.Event.IEventService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController()
@RequestMapping("api/v1/events")
public class    EventController {
    private final IEventService eventService;

    public EventController(IEventService eventService) {
        this.eventService = eventService;
    }

    // full path: /api/v1/events
    @GetMapping
    public List<EventDTO> getEvents() {
        return eventService.getAllEvents();
    }

    // full path: /api/v1/events/{id}
    @GetMapping("{id}")
    public EventDTO getEvent(@PathVariable UUID id) {
        return eventService.getEventById(id);
    }
    
    
    // full path: /api/v1/events/user/{userId}
    @GetMapping("user/{userId}")
    public List<EventDTO> getEventsByUserId(@PathVariable UUID userId) {
        return eventService.getEventsByUserId(userId);
    }

    // full path: /api/v1/events/friendTag/{tagId}
    @GetMapping("friendTag/{tagId}")
    public List<EventDTO> getEventsByFriendTagId(@PathVariable UUID tagId) {
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
    public EventDTO replaceEvent(@RequestBody EventDTO newEvent, @PathVariable UUID id) {
        return eventService.replaceEvent(newEvent, id);
    }

    // full path: /api/v1/events/{id}
    @DeleteMapping("{id}")
    public ResponseEntity<Void> deleteEvent(@PathVariable UUID id) {
        try {
            boolean isDeleted = eventService.deleteEventById(id);
            if (isDeleted) {
                return new ResponseEntity<>(HttpStatus.NO_CONTENT); // Success
            } else {
                return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR); // Deletion failed
            }
        } catch (BaseNotFoundException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND); // Resource not found
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR); // Unexpected error
        }
    }

    // full path: /api/v1/events/{id}/users
    @GetMapping("events/{id}/users")
    public List<UserDTO> getUsersParticipatingInEvent(@PathVariable UUID id) {
        return eventService.getParticipatingUsersByEventId(id);
    }
}
