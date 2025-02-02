package com.danielagapov.spawn.Controllers;

import com.danielagapov.spawn.DTOs.EventDTO;
import com.danielagapov.spawn.DTOs.FullFeedEventDTO;
import com.danielagapov.spawn.DTOs.IEventDTO;
import com.danielagapov.spawn.DTOs.IOnboardedUserDTO;
import com.danielagapov.spawn.Enums.ParticipationStatus;
import com.danielagapov.spawn.Exceptions.Base.BaseNotFoundException;
import com.danielagapov.spawn.Exceptions.Base.BasesNotFoundException;
import com.danielagapov.spawn.Services.Event.IEventService;
import com.danielagapov.spawn.Services.User.IUserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController()
@RequestMapping("api/v1/events")
public class EventController {
    private final IEventService eventService;
    private final IUserService userService;

    public EventController(IEventService eventService, IUserService userService) {
        this.eventService = eventService;
        this.userService = userService;
    }

    // full path: /api/v1/events?full=full
    @GetMapping
    public ResponseEntity<List<? extends IEventDTO>> getEvents(@RequestParam(value="full", required=false) boolean full) {
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
    public ResponseEntity<IEventDTO> getEventById(@PathVariable UUID id, @RequestParam(value="full", required=false) boolean full, @RequestParam(required=false) UUID requestingUserId) {
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

    // full path: /api/v1/events/user/{creatorUserId}?full=full
    @GetMapping("user/{creatorUserId}")
    public ResponseEntity<List<? extends IEventDTO>> getEventsCreatedByUserId(@PathVariable UUID creatorUserId, @RequestParam(value="full", required=false) boolean full) {
        try {
            if (full) {
                return new ResponseEntity<>(eventService.convertEventsToFullFeedSelfOwnedEvents(eventService.getEventsByOwnerId(creatorUserId), creatorUserId), HttpStatus.OK);
            } else {
                return new ResponseEntity<>(eventService.getEventsByOwnerId(creatorUserId), HttpStatus.OK);
            }
        } catch (BasesNotFoundException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // full path: /api/v1/events/friendTag/{tagId}?full=full&requestingUserId=requestingUserId
    @GetMapping("friendTag/{tagId}")
    public ResponseEntity<List<? extends IEventDTO>> getEventsByFriendTagId(@PathVariable UUID tagId, @RequestParam(value="full", required=false) boolean full, @RequestParam UUID requestingUserId) {
        try {
            if (full && requestingUserId != null) {
                return new ResponseEntity<>(eventService.convertEventsToFullFeedEvents(eventService.getEventsByFriendTagId(tagId), requestingUserId), HttpStatus.OK);
            } else {
                return new ResponseEntity<>(eventService.getEventsByFriendTagId(tagId), HttpStatus.OK);
            }
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
    public ResponseEntity<IEventDTO> createEvent(@RequestBody FullFeedEventDTO newEvent) {
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

    // full path: /api/v1/events/{id}/users?full=full
    @GetMapping("{id}/users")
    public ResponseEntity<List<? extends IOnboardedUserDTO>> getUsersParticipatingInEvent(@PathVariable UUID id, @RequestParam(value="full", required=false) boolean full) {
        try {
            if (full) {
                return new ResponseEntity<>(userService.convertUsersToFullUsers(eventService.getParticipatingUsersByEventId(id)), HttpStatus.OK);
            } else {
                return new ResponseEntity<>(eventService.getParticipatingUsersByEventId(id), HttpStatus.OK);
            }
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

    // full path: /api/v1/events/invitedEvents/{userId}?full=full
    @GetMapping("invitedEvents/{userId}")
    // need this `? extends IEventDTO` instead of simply `IEventDTO`, because of this error:
    // https://stackoverflow.com/questions/27522741/incompatible-types-inference-variable-t-has-incompatible-bounds
    public ResponseEntity<List<? extends IEventDTO>>getEventsInvitedTo(@PathVariable UUID userId, @RequestParam(value="full", required=false) boolean full) {
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

    // full path: /api/v1/events/feedEvents/{requestingUserId}
    // this method will return the events created by a given user (like in `getEventsCreatedByUserId()`),
    // in the universal accent color, followed by feed events (like in `getEventsInvitedTo()`
    @GetMapping("feedEvents/{requestingUserId}")
    // need this `? extends IEventDTO` instead of simply `IEventDTO`, because of this error:
    // https://stackoverflow.com/questions/27522741/incompatible-types-inference-variable-t-has-incompatible-bounds
    public ResponseEntity<List<? extends IEventDTO>>getFeedEvents(@PathVariable UUID requestingUserId) {
        try {
            return new ResponseEntity<>(eventService.getFeedEvents(requestingUserId), HttpStatus.OK);
        } catch (BaseNotFoundException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }
}
