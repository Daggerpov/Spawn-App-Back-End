package com.danielagapov.spawn.Controllers;

import com.danielagapov.spawn.DTOs.EventDTO;
import com.danielagapov.spawn.DTOs.UserDTO;
import com.danielagapov.spawn.Exceptions.Base.BaseNotFoundException;
import com.danielagapov.spawn.Exceptions.Base.BasesNotFoundException;
import com.danielagapov.spawn.Helpers.UUIDValidator;
import com.danielagapov.spawn.Services.Event.IEventService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.List;
import java.util.UUID;

@RestController()
@RequestMapping("api/v1/events")
public class EventController {
    private final IEventService eventService;

    public EventController(IEventService eventService) {
        this.eventService = eventService;
    }

    // full path: /api/v1/events
    @GetMapping
    public ResponseEntity<List<EventDTO>> getEvents() {
        try {
            return new ResponseEntity<>(eventService.getAllEvents(), HttpStatus.OK);
        } catch (Exception e) {
            System.out.println("entered catch in controller");
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // full path: /api/v1/events/{id}
    @GetMapping("{id}")
    public ResponseEntity<EventDTO> getEventById(@PathVariable UUID id) {
        try {
            UUID validUUID = UUIDValidator.getInstance().validateUUID(String.valueOf(id));
            EventDTO event = eventService.getEventById(validUUID);
            return new ResponseEntity<>(event, HttpStatus.OK);
        } catch (MethodArgumentTypeMismatchException e) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST); // from invalid UUID passed-in
        } catch (BasesNotFoundException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // full path: /api/v1/events/user/{userId}
    @GetMapping("user/{userId}")
    public ResponseEntity<List<EventDTO>> getEventsByUserId(@PathVariable UUID userId) {
        try {
            return new ResponseEntity<>(eventService.getEventsByUserId(userId), HttpStatus.OK);
        } catch (BasesNotFoundException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // full path: /api/v1/events/friendTag/{tagId}
    @GetMapping("friendTag/{tagId}")
    public ResponseEntity<List<EventDTO>> getEventsByFriendTagId(@PathVariable UUID tagId) {
        try {
            return new ResponseEntity<>(eventService.getEventsByFriendTagId(tagId), HttpStatus.OK);
        } catch (BasesNotFoundException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // full path: /api/v1/events/mock-endpoint
    @GetMapping("mock-endpoint")
    public ResponseEntity<String> getMockEndpoint() {
        return new ResponseEntity<>("This is the mock endpoint for events. Everything is working with it.", HttpStatus.OK);
    }

    // full path: /api/v1/events
    @PostMapping
    public ResponseEntity<EventDTO> createEvent(@RequestBody EventDTO newEvent) {
        try {
            return new ResponseEntity<>(eventService.saveEvent(newEvent), HttpStatus.CREATED);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // full path: /api/v1/events/{id}
    @PutMapping("{id}")
    public ResponseEntity<EventDTO> replaceEvent(@RequestBody EventDTO newEvent, @PathVariable UUID id) {
        try {
            return new ResponseEntity<>(eventService.replaceEvent(newEvent, id), HttpStatus.OK);
        } catch (BaseNotFoundException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
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
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // full path: /api/v1/events/{id}/users
    @GetMapping("events/{id}/users")
    public ResponseEntity<List<UserDTO>> getUsersParticipatingInEvent(@PathVariable UUID id) {
        try {
            return new ResponseEntity<>(eventService.getParticipatingUsersByEventId(id), HttpStatus.OK);
        } catch (BaseNotFoundException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
