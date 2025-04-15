package com.danielagapov.spawn.Controllers;

import com.danielagapov.spawn.DTOs.Event.AbstractEventDTO;
import com.danielagapov.spawn.DTOs.Event.EventCreationDTO;
import com.danielagapov.spawn.DTOs.Event.EventDTO;
import com.danielagapov.spawn.DTOs.Event.FullFeedEventDTO;
import com.danielagapov.spawn.Enums.EntityType;
import com.danielagapov.spawn.Exceptions.Base.BaseNotFoundException;
import com.danielagapov.spawn.Exceptions.Base.BasesNotFoundException;
import com.danielagapov.spawn.Services.Event.IEventService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.UUID;

@RestController()
@RequestMapping("api/v1/events")
public class EventController {
    private final IEventService eventService;

    public EventController(IEventService eventService) {
        this.eventService = eventService;
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
            // Only return 404 if user doesn't exist, not if event doesn't exist
            if (e.entityType == EntityType.User) {
                return new ResponseEntity<>(e.entityType, HttpStatus.NOT_FOUND);
            } else if (e.entityType == EntityType.Event) {
                // Return 404 for events too, as this is specifically looking up an event by ID
                return new ResponseEntity<>(e.entityType, HttpStatus.NOT_FOUND);
            } else {
                return new ResponseEntity<>(e.entityType, HttpStatus.NOT_FOUND);
            }
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
            // For deletion, it makes sense to return 404 if the event doesn't exist
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
            // Only return 404 for appropriate entity types
            if (e.entityType == EntityType.User) {
                return new ResponseEntity<>(e.entityType, HttpStatus.NOT_FOUND);
            } else if (e.entityType == EntityType.Event) {
                return new ResponseEntity<>(e.entityType, HttpStatus.NOT_FOUND);
            } else if (e.entityType == EntityType.EventUser) {
                // If the user is not invited to the event, return 404
                return new ResponseEntity<>(e.entityType, HttpStatus.NOT_FOUND);
            } else {
                return new ResponseEntity<>(e.entityType, HttpStatus.NOT_FOUND);
            }
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
            // user not found - return 404
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
            // Event or User not found - only return 404 if it's the user that's not found
            if (e.entityType == EntityType.User) {
                return new ResponseEntity<>(e.entityType, HttpStatus.NOT_FOUND);
            } else if (e.entityType == EntityType.Event) {
                // Event not found for a valid user, return empty response with 200
                return new ResponseEntity<>(new ArrayList<>(), HttpStatus.OK);
            } else {
                return new ResponseEntity<>(e.entityType, HttpStatus.NOT_FOUND);
            }
        } catch (Exception e) {
            // Any other exception
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
