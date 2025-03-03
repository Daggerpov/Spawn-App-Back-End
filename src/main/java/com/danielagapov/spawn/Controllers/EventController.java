package com.danielagapov.spawn.Controllers;

import com.danielagapov.spawn.DTOs.Event.AbstractEventDTO;
import com.danielagapov.spawn.DTOs.Event.EventCreationDTO;
import com.danielagapov.spawn.DTOs.Event.EventDTO;
import com.danielagapov.spawn.DTOs.Event.FullFeedEventDTO;
import com.danielagapov.spawn.DTOs.User.AbstractUserDTO;
import com.danielagapov.spawn.Enums.ParticipationStatus;
import com.danielagapov.spawn.Exceptions.EventsNotFoundException;
import com.danielagapov.spawn.Exceptions.Base.BaseNotFoundException;
import com.danielagapov.spawn.Exceptions.Base.BasesNotFoundException;
import com.danielagapov.spawn.Exceptions.Logger.ILogger;
import com.danielagapov.spawn.Services.Event.IEventService;
import com.danielagapov.spawn.Services.User.IUserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

@RestController()
@RequestMapping("api/v1/events")
public class EventController {
    private final IEventService eventService;
    private final IUserService userService;
    private final ILogger logger;

    public EventController(IEventService eventService, IUserService userService, ILogger logger) {
        this.eventService = eventService;
        this.userService = userService;
        this.logger = logger;
    }

    // full path: /api/v1/events?full=full
    @GetMapping
    public ResponseEntity<List<? extends AbstractEventDTO>> getEvents(@RequestParam(value = "full", required = false) boolean full) {
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
    public ResponseEntity<AbstractEventDTO> getEventById(@PathVariable UUID id, @RequestParam(value = "full", required = false) boolean full, @RequestParam(required = false) UUID requestingUserId) {
        logger.log("Get event by Id request received");
        try {
            if (full && requestingUserId != null) {
                return new ResponseEntity<>(eventService.getFullEventById(id, requestingUserId), HttpStatus.OK);
            } else {
                return new ResponseEntity<>(eventService.getEventById(id), HttpStatus.OK);
            }
        } catch (BaseNotFoundException e) {
            // catches user or events
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // full path: /api/v1/events/user/{creatorUserId}?full=full
    @GetMapping("user/{creatorUserId}")
    public ResponseEntity<List<? extends AbstractEventDTO>> getEventsCreatedByUserId(@PathVariable UUID creatorUserId, @RequestParam(value = "full", required = false) boolean full) {
        logger.log("Get events created by user id request received");
        try {
            if (full) {
                return new ResponseEntity<>(eventService.convertEventsToFullFeedSelfOwnedEvents(eventService.getEventsByOwnerId(creatorUserId), creatorUserId), HttpStatus.OK);
            } else {
                return new ResponseEntity<>(eventService.getEventsByOwnerId(creatorUserId), HttpStatus.OK);
            }
        } catch (EventsNotFoundException e) {
            // thrown list of events not found for given user id
            // return response with empty list and 200 status
            return new ResponseEntity<>(new ArrayList<>(), HttpStatus.OK);
        } catch (BaseNotFoundException e) {
            // user or event not found
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            // any other exception
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // full path: /api/v1/events/friendTag/{friendTagFilterId}
    @GetMapping("friendTag/{friendTagFilterId}")
    public ResponseEntity<List<FullFeedEventDTO>> getEventsByFriendTag(@PathVariable UUID friendTagFilterId) {
        logger.log("Get events by friend tag filter id request received");
        try {
            return new ResponseEntity<>(eventService.getFilteredFeedEventsByFriendTagId(friendTagFilterId), HttpStatus.OK);
        } catch (EventsNotFoundException e) {
            // list of events not found for tag filter id
            return new ResponseEntity<>(new ArrayList<>(), HttpStatus.OK);
        } catch (BaseNotFoundException e) {
            // friend tag filter not found for friend tag id
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            // any other exception
            return new ResponseEntity<>(new ArrayList<>(), HttpStatus.INTERNAL_SERVER_ERROR);
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
    public ResponseEntity<AbstractEventDTO> createEvent(@RequestBody EventCreationDTO eventCreationDTO) {
        logger.log("Create event request received");
        try {
            AbstractEventDTO createdEvent = eventService.createEvent(eventCreationDTO);
            return new ResponseEntity<>(createdEvent, HttpStatus.CREATED);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // full path: /api/v1/events/{id}
    @PutMapping("{id}")
    public ResponseEntity<EventDTO> replaceEvent(@RequestBody EventDTO newEvent, @PathVariable UUID id) {
        logger.log("Replace event request received");
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
        logger.log("Delete event request received");
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
    public ResponseEntity<List<? extends AbstractUserDTO>> getUsersParticipatingInEvent(@PathVariable UUID id, @RequestParam(value = "full", required = false) boolean full) {
        logger.log("Get users participating in event request received");
        try {
            if (full) {
                return new ResponseEntity<>(userService.convertUsersToFullUsers(eventService.getParticipatingUsersByEventId(id), new HashSet<>()), HttpStatus.OK);
            } else {
                return new ResponseEntity<>(eventService.getParticipatingUsersByEventId(id), HttpStatus.OK);
            }
        } catch (EventsNotFoundException e) {
            // list of events for user not found
            return new ResponseEntity<>(new ArrayList<>(), HttpStatus.OK);
        } catch (BaseNotFoundException e) {
            // user not found
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // full path: /api/v1/events/{eventId}/participating?userId={userid}
    @GetMapping("{eventId}/participating")
    public ResponseEntity<Boolean> isUserParticipating(@PathVariable UUID eventId, @RequestParam UUID userId) {
        logger.log("Is user participating request received");
        try {
            if (eventService.getParticipationStatus(eventId, userId) == ParticipationStatus.participating) {
                return new ResponseEntity<>(true, HttpStatus.OK);
            } else {
                return new ResponseEntity<>(false, HttpStatus.OK);
            }
        } catch (BaseNotFoundException e) {
            // event or user not found
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // full path: /api/v1/events/{eventId}/invited?userId={userid}
    @GetMapping("{eventId}/invited")
    public ResponseEntity<Boolean> isUserInvited(@PathVariable UUID eventId, @RequestParam UUID userId) {
        logger.log("Is user invited request received");
        try {
            if (eventService.getParticipationStatus(eventId, userId) == ParticipationStatus.invited) {
                return new ResponseEntity<>(true, HttpStatus.OK);
            } else {
                return new ResponseEntity<>(false, HttpStatus.OK);
            }
        } catch (BaseNotFoundException e) {
            // event or user not found
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // this corresponds to the button on the event for invited users
    // full path: /api/v1/events/{eventId}/toggleStatus/{userId}
    @PutMapping("{eventId}/toggleStatus/{userId}")
    public ResponseEntity<FullFeedEventDTO> toggleParticipation(@PathVariable UUID eventId, @PathVariable UUID userId) {
        logger.log("Toggle participation request received");
        try {
            FullFeedEventDTO updatedEventAfterParticipationToggle = eventService.toggleParticipation(eventId, userId);
            return new ResponseEntity<>(updatedEventAfterParticipationToggle, HttpStatus.OK);
        } catch (BaseNotFoundException e) {
            // event or user not found
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // full path: /api/v1/events/invitedEvents/{userId}?full=full
    @GetMapping("invitedEvents/{userId}")
    // need this `? extends AbstractEventDTO` instead of simply `AbstractEventDTO`, because of this error:
    // https://stackoverflow.com/questions/27522741/incompatible-types-inference-variable-t-has-incompatible-bounds
    public ResponseEntity<List<? extends AbstractEventDTO>> getEventsInvitedTo(@PathVariable UUID userId, @RequestParam(required = false) boolean full) {
        logger.log("Get events invited to request received");
        try {
            if (full) {
                return new ResponseEntity<>(eventService.getFullEventsInvitedTo(userId), HttpStatus.OK);
            } else {
                return new ResponseEntity<>(eventService.getEventsInvitedTo(userId), HttpStatus.OK);
            }
        } catch (EventsNotFoundException e) {
            // list of events for user id not found
            return new ResponseEntity<>(new ArrayList<>(), HttpStatus.OK);
        } catch (BaseNotFoundException e) {
            // user not found
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // full path: /api/v1/events/feedEvents/{requestingUserId}
    // this method will return the events created by a given user (like in `getEventsCreatedByUserId()`),
    // in the universal accent color, followed by feed events (like in `getEventsInvitedTo()`
    @GetMapping("feedEvents/{requestingUserId}")
    // need this `? extends AbstractEventDTO` instead of simply `AbstractEventDTO`, because of this error:
    // https://stackoverflow.com/questions/27522741/incompatible-types-inference-variable-t-has-incompatible-bounds
    public ResponseEntity<List<FullFeedEventDTO>> getFeedEvents(@PathVariable UUID requestingUserId) {
        logger.log("Get feed events request received");
        try {
            return new ResponseEntity<>(eventService.getFeedEvents(requestingUserId), HttpStatus.OK);
        } catch (EventsNotFoundException e) {
            // list of events for feed not found
            return new ResponseEntity<>(new ArrayList<>(), HttpStatus.OK);
        } catch (BaseNotFoundException e) {
            // user or event not found
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
