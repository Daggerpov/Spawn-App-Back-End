package com.danielagapov.spawn.Controllers;

import com.danielagapov.spawn.DTOs.EventDTO;
import com.danielagapov.spawn.DTOs.IEventDTO;
import com.danielagapov.spawn.DTOs.UserDTO;
import com.danielagapov.spawn.Enums.ParticipationStatus;
import com.danielagapov.spawn.Exceptions.Base.BaseNotFoundException;
import com.danielagapov.spawn.Exceptions.Base.BasesNotFoundException;
import com.danielagapov.spawn.Services.Event.IEventService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController()
@RequestMapping("api/v1/events")
public class EventController {
    private final IEventService eventService;

    public EventController(IEventService eventService) {
        this.eventService = eventService;
    }

    // full path: /api/v1/events?full=full
    @GetMapping
    public ResponseEntity<List<? extends IEventDTO>> getEvents(@RequestParam boolean full) {
        try {
            if (full) {
                return new ResponseEntity<>(eventService.getAllFullEvents(), HttpStatus.OK);
            } else {
                return new ResponseEntity<>(eventService.getAllEvents(), HttpStatus.OK);
            }
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // full path: /api/v1/events/{id}?full=full&requestingUserId=requestingUserId
    @GetMapping("{id}")
    public ResponseEntity<IEventDTO> getEventById(@PathVariable UUID id, @RequestParam boolean full, @RequestParam UUID requestingUserId) {
        try {
            if (full && requestingUserId != null) {
                return new ResponseEntity<>(eventService.getFullEventById(id, requestingUserId), HttpStatus.OK);
            } else {
                return new ResponseEntity<>(eventService.getEventById(id), HttpStatus.OK);
            }
        } catch (BaseNotFoundException e) {
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
        return new ResponseEntity<>("This is the mock endpoint for events. Everything is working with it.",
                HttpStatus.OK);
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
    @GetMapping("{id}/users")
    public ResponseEntity<List<UserDTO>> getUsersParticipatingInEvent(@PathVariable UUID id) {
        try {
            return new ResponseEntity<>(eventService.getParticipatingUsersByEventId(id), HttpStatus.OK);
        } catch (BaseNotFoundException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // full path: /api/v1/events/{eventId}/participating?userId={userid}
    @GetMapping("{eventId}/participating?userId={userid}")
    public ResponseEntity<Boolean> isUserParticipating(@PathVariable UUID eventId, @RequestParam UUID userId) {
        try {
            if (eventService.getParticipationStatus(eventId, userId) == ParticipationStatus.participating) {
                return new ResponseEntity<>(true, HttpStatus.OK);
            } else {
                return new ResponseEntity<>(false, HttpStatus.OK);
            }
        } catch (BaseNotFoundException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // full path: /api/v1/events/{eventId}/invited?userId={userid}
    @GetMapping("{eventId}/invited?userId={userid}")
    public ResponseEntity<Boolean> isUserInvited(@PathVariable UUID eventId, @RequestParam UUID userId) {
        try {
            if (eventService.getParticipationStatus(eventId, userId) == ParticipationStatus.invited) {
                return new ResponseEntity<>(true, HttpStatus.OK);
            } else {
                return new ResponseEntity<>(false, HttpStatus.OK);
            }
        } catch (BaseNotFoundException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // this corresponds to the button on the event for invited users
    // full path: /api/v1/events/{eventId}/toggleStatus?userId={userId}
    @GetMapping("{eventId}/toggleStatus?userId={userId}")
    public ResponseEntity<Void> toggleParticipation(@PathVariable UUID eventId, @RequestParam UUID userId) {
        try {
            if (eventService.toggleParticipation(eventId, userId)) {
                return new ResponseEntity<>(HttpStatus.OK);
            } else {
                return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
            }
        } catch (BaseNotFoundException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // full path: /api/v1/events/{userId}/invitedEvents?full=full
    @GetMapping("{userId}/invitedEvents")
    // need this `? extends IEventDTO` instead of simply `IEventDTO`, because of this error:
    // https://stackoverflow.com/questions/27522741/incompatible-types-inference-variable-t-has-incompatible-bounds
    public ResponseEntity<List<? extends IEventDTO>>getEventsInvitedTo(@PathVariable UUID userId, @RequestParam boolean full) {
        try {
            if (full) {
                return new ResponseEntity<>(eventService.getFullEventsInvitedTo(userId), HttpStatus.OK);
            } else {
                return new ResponseEntity<>(eventService.getEventsInvitedTo(userId), HttpStatus.OK);
            }
        } catch (BaseNotFoundException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }
}
