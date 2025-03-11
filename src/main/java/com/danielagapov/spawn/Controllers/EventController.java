package com.danielagapov.spawn.Controllers;

import com.danielagapov.spawn.DTOs.Event.AbstractEventDTO;
import com.danielagapov.spawn.DTOs.Event.EventCreationDTO;
import com.danielagapov.spawn.DTOs.Event.EventDTO;
import com.danielagapov.spawn.DTOs.Event.FullFeedEventDTO;
import com.danielagapov.spawn.Enums.EntityType;
import com.danielagapov.spawn.Enums.ParticipationStatus;
import com.danielagapov.spawn.Exceptions.Base.BaseNotFoundException;
import com.danielagapov.spawn.Exceptions.Base.BasesNotFoundException;
import com.danielagapov.spawn.Services.Event.IEventService;
import com.danielagapov.spawn.Services.User.IUserService;
import org.springframework.http.HttpStatus;
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

    public EventController(IEventService eventService, IUserService userService) {
        this.eventService = eventService;
        this.userService = userService;
    }

    // TL;DR: Don't remove this endpoint; it may become useful. 
    @Deprecated(since = "Not being used on mobile currently.")
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

    // TL;DR: Don't remove this endpoint; it may become useful. 
    @Deprecated(since = "Not being used on mobile currently." +
            "This may become a feature, as Owen has suggested, " +
            "with showing a friend's recent events.")
    // full path: /api/v1/events/user/{creatorUserId}?full=full
    @GetMapping("user/{creatorUserId}")
    public ResponseEntity<?> getEventsCreatedByUserId(@PathVariable UUID creatorUserId, @RequestParam(value = "full", required = false) boolean full) {
        try {
            if (full) {
                return new ResponseEntity<>(eventService.convertEventsToFullFeedSelfOwnedEvents(eventService.getEventsByOwnerId(creatorUserId), creatorUserId), HttpStatus.OK);
            } else {
                return new ResponseEntity<>(eventService.getEventsByOwnerId(creatorUserId), HttpStatus.OK);
            }
        } catch (BasesNotFoundException e) {
            // thrown list of events not found for given user id
            // if entities not found is Event: return response with empty list and 200 status
            // otherwise: bad request http status
            if (e.entityType == EntityType.Event) {
                return new ResponseEntity<>(new ArrayList<>(), HttpStatus.OK);
            } else {
                return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
            }
        } catch (BaseNotFoundException e) {
            // user or event not found
            return new ResponseEntity<>(e.entityType, HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            // any other exception
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // full path: /api/v1/events/friendTag/{friendTagFilterId}
    @GetMapping("friendTag/{friendTagFilterId}")
    public ResponseEntity<?> getEventsByFriendTag(@PathVariable UUID friendTagFilterId) {
        if (friendTagFilterId == null) return new ResponseEntity<>(HttpStatus.BAD_REQUEST);

        try {
            return new ResponseEntity<>(eventService.getFilteredFeedEventsByFriendTagId(friendTagFilterId), HttpStatus.OK);
        } catch (BasesNotFoundException e) {
            // thrown list of events not found for given user id
            // if entities not found is Event: return response with empty list and 200 status
            // otherwise: bad request http status
            if (e.entityType == EntityType.Event) {
                return new ResponseEntity<>(new ArrayList<>(), HttpStatus.OK);
            } else {
                return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
            }
        } catch (BaseNotFoundException e) {
            // friend tag filter not found for friend tag id
            return new ResponseEntity<>(e.entityType, HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            // any other exception
            return new ResponseEntity<>(new ArrayList<>(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // full path: /api/v1/events
    @PostMapping
    public ResponseEntity<AbstractEventDTO> createEvent(@RequestBody EventCreationDTO eventCreationDTO) {
        try {
            AbstractEventDTO createdEvent = eventService.createEvent(eventCreationDTO);
            return new ResponseEntity<>(createdEvent, HttpStatus.CREATED);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // TL;DR: Don't remove this endpoint; it may become useful. 
    @Deprecated(since = "Not being used on mobile currently. " +
            "Pending mobile feature implementation, per:" +
            "https://github.com/Daggerpov/Spawn-App-iOS-SwiftUI/issues/142")
    // full path: /api/v1/events/{id}
    @PutMapping("{id}")
    public ResponseEntity<?> replaceEvent(@RequestBody EventDTO newEvent, @PathVariable UUID id) {
        if (id == null) return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        try {
            return new ResponseEntity<>(eventService.replaceEvent(newEvent, id), HttpStatus.OK);
        } catch (BaseNotFoundException e) {
            return new ResponseEntity<>(e.entityType, HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // TL;DR: Don't remove this endpoint; it may become useful. 
    @Deprecated(since = "Not being used on mobile currently. " +
            "Pending mobile feature implementation, per:" +
            "https://github.com/Daggerpov/Spawn-App-iOS-SwiftUI/issues/142")
    // full path: /api/v1/events/{id}
    @DeleteMapping("{id}")
    public ResponseEntity<?> deleteEvent(@PathVariable UUID id) {
        if (id == null) return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        try {
            boolean isDeleted = eventService.deleteEventById(id);
            if (isDeleted) {
                return new ResponseEntity<>(HttpStatus.NO_CONTENT); // Success
            } else {
                return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR); // Deletion failed
            }
        } catch (BaseNotFoundException e) {
            return new ResponseEntity<>(e.entityType, HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // TL;DR: Don't remove this endpoint; it may become useful. 
    @Deprecated(since = "Not being used on mobile currently.")
    // full path: /api/v1/events/{id}/users?full=full
    @GetMapping("{id}/users")
    public ResponseEntity<?> getUsersParticipatingInEvent(@PathVariable UUID id, @RequestParam(value = "full", required = false) boolean full) {
        if (id == null) return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        try {
            if (full) {
                return new ResponseEntity<>(userService.convertUsersToFullUsers(eventService.getParticipatingUsersByEventId(id), new HashSet<>()), HttpStatus.OK);
            } else {
                return new ResponseEntity<>(eventService.getParticipatingUsersByEventId(id), HttpStatus.OK);
            }
        } catch (BasesNotFoundException e) {
            // thrown list of events not found for given user id
            // if entities not found is Event: return response with empty list and 200 status
            // otherwise: bad request http status
            if (e.entityType == EntityType.Event) {
                return new ResponseEntity<>(new ArrayList<>(), HttpStatus.OK);
            } else {
                return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
            }
        } catch (BaseNotFoundException e) {
            // user not found
            return new ResponseEntity<>(e.entityType, HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // TL;DR: Don't remove this endpoint; it may become useful. 
    @Deprecated(since = "Not being used on mobile currently.")
    // full path: /api/v1/events/{eventId}/participating?userId={userid}
    @GetMapping("{eventId}/participating")
    public ResponseEntity<?> isUserParticipating(@PathVariable UUID eventId, @RequestParam UUID userId) {
        if (userId == null) return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        try {
            if (eventService.getParticipationStatus(eventId, userId) == ParticipationStatus.participating) {
                return new ResponseEntity<>(true, HttpStatus.OK);
            } else {
                return new ResponseEntity<>(false, HttpStatus.OK);
            }
        } catch (BaseNotFoundException e) {
            // event or user not found
            return new ResponseEntity<>(e.entityType, HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // TL;DR: Don't remove this endpoint; it may become useful. 
    @Deprecated(since = "Not being used on mobile currently.")
    // full path: /api/v1/events/{eventId}/invited?userId={userid}
    @GetMapping("{eventId}/invited")
    public ResponseEntity<?> isUserInvited(@PathVariable UUID eventId, @RequestParam UUID userId) {
        if (userId == null || eventId == null) return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        try {
            if (eventService.getParticipationStatus(eventId, userId) == ParticipationStatus.invited) {
                return new ResponseEntity<>(true, HttpStatus.OK);
            } else {
                return new ResponseEntity<>(false, HttpStatus.OK);
            }
        } catch (BaseNotFoundException e) {
            // event or user not found
            return new ResponseEntity<>(e.entityType, HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // this corresponds to the button on the event for invited users
    // full path: /api/v1/events/{eventId}/toggleStatus/{userId}
    @PutMapping("{eventId}/toggleStatus/{userId}")
    public ResponseEntity<?> toggleParticipation(@PathVariable UUID eventId, @PathVariable UUID userId) {
        if (userId == null || eventId == null) return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        try {
            FullFeedEventDTO updatedEventAfterParticipationToggle = eventService.toggleParticipation(eventId, userId);
            return new ResponseEntity<>(updatedEventAfterParticipationToggle, HttpStatus.OK);
        } catch (BaseNotFoundException e) {
            // event or user not found
            return new ResponseEntity<>(e.entityType, HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // TL;DR: Don't remove this endpoint; it may become useful. 
    @Deprecated(since = "Not being used on mobile currently.")
    // full path: /api/v1/events/invitedEvents/{userId}?full=full
    @GetMapping("invitedEvents/{userId}")
    // need this `? extends AbstractEventDTO` instead of simply `AbstractEventDTO`, because of this error:
    // https://stackoverflow.com/questions/27522741/incompatible-types-inference-variable-t-has-incompatible-bounds
    public ResponseEntity<?> getEventsInvitedTo(@PathVariable UUID userId, @RequestParam(required = false) boolean full) {
        if (userId == null) return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        try {
            if (full) {
                return new ResponseEntity<>(eventService.getFullEventsInvitedTo(userId), HttpStatus.OK);
            } else {
                return new ResponseEntity<>(eventService.getEventsInvitedTo(userId), HttpStatus.OK);
            }
        } catch (BasesNotFoundException e) {
            // thrown list of events not found for given user id
            // if entities not found is Event: return response with empty list and 200 status
            // otherwise: bad request http status
            if (e.entityType == EntityType.Event) {
                return new ResponseEntity<>(new ArrayList<>(), HttpStatus.OK);
            } else {
                return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
            }
        } catch (BaseNotFoundException e) {
            // user not found
            return new ResponseEntity<>(e.entityType, HttpStatus.NOT_FOUND);
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
    public ResponseEntity<?> getFeedEvents(@PathVariable UUID requestingUserId) {
        if (requestingUserId == null) return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        try {
            return new ResponseEntity<>(eventService.getFeedEvents(requestingUserId), HttpStatus.OK);
        } catch (BasesNotFoundException e) {
            // thrown list of events not found for given user id
            // if entities not found is Event: return response with empty list and 200 status
            // otherwise: bad request http status
            if (e.entityType == EntityType.Event) {
                return new ResponseEntity<>(new ArrayList<>(), HttpStatus.OK);
            } else {
                return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
            }
        } catch (BaseNotFoundException e) {
            // user or event not found
            return new ResponseEntity<>(e.entityType, HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // full path: /api/v1/events/{id}
    @GetMapping("{id}")
    public ResponseEntity<?> getFullEventById(@PathVariable UUID id, @RequestParam UUID requestingUserId) {
        if (id == null || requestingUserId == null) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }

        try {
            return new ResponseEntity<>(eventService.getFullEventById(id, requestingUserId), HttpStatus.OK);
        } catch (BaseNotFoundException e) {
            // Event not found
            return new ResponseEntity<>(e.entityType, HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            // Any other exception
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
