package com.danielagapov.spawn.Controllers;

import com.danielagapov.spawn.DTOs.Activity.AbstractActivityDTO;
import com.danielagapov.spawn.DTOs.Activity.ActivityCreationDTO;
import com.danielagapov.spawn.DTOs.Activity.ActivityDTO;
import com.danielagapov.spawn.DTOs.Activity.FullFeedActivityDTO;
import com.danielagapov.spawn.DTOs.Activity.ProfileActivityDTO;
import com.danielagapov.spawn.Enums.EntityType;
import com.danielagapov.spawn.Exceptions.Base.BaseNotFoundException;
import com.danielagapov.spawn.Exceptions.Base.BasesNotFoundException;
import com.danielagapov.spawn.Services.Activity.IActivitieservice;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.UUID;

@RestController()
@RequestMapping("api/v1/Activities")
public class ActivityController {
    private final IActivitieservice Activitieservice;

    public ActivityController(IActivitieservice Activitieservice) {
        this.Activitieservice = Activitieservice;
    }

    // TL;DR: Don't remove this endpoint; it may become useful.
    @Deprecated(since = "Not being used on mobile currently." +
            "This may become a feature, as Owen has suggested, " +
            "with showing a friend's recent Activities.")
    // full path: /api/v1/Activities/user/{creatorUserId}
    @GetMapping("user/{creatorUserId}")
    public ResponseEntity<?> getActivitiesCreatedByUserId(@PathVariable UUID creatorUserId) {
        try {
            return new ResponseEntity<>(Activitieservice.convertActivitiesToFullFeedSelfOwnedActivities(Activitieservice.getActivitiesByOwnerId(creatorUserId), creatorUserId), HttpStatus.OK);
        } catch (BaseNotFoundException e) {
            // user or activity not found
            return new ResponseEntity<>(e.entityType, HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            // any other exception
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    
    // full path: /api/v1/Activities/profile/{profileUserId}?requestingUserId={requestingUserId}
    @GetMapping("profile/{profileUserId}")
    public ResponseEntity<?> getProfileActivities(@PathVariable UUID profileUserId, @RequestParam UUID requestingUserId) {
        if (profileUserId == null || requestingUserId == null) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
        
        try {
            return new ResponseEntity<>(Activitieservice.getProfileActivities(profileUserId, requestingUserId), HttpStatus.OK);
        } catch (BaseNotFoundException e) {
            // User not found - return 404
            return new ResponseEntity<>(e.entityType, HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            // Any other exception
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // full path: /api/v1/Activities/friendTag/{friendTagFilterId}
    @GetMapping("friendTag/{friendTagFilterId}")
    public ResponseEntity<?> getActivitiesByFriendTag(@PathVariable UUID friendTagFilterId) {
        if (friendTagFilterId == null) return new ResponseEntity<>(HttpStatus.BAD_REQUEST);

        try {
            return new ResponseEntity<>(Activitieservice.getFilteredFeedActivitiesByFriendTagId(friendTagFilterId), HttpStatus.OK);
        } catch (BasesNotFoundException e) {
            // thrown list of activities not found for given user id
            // if entities not found is Activity: return response with empty list and 200 status
            // otherwise: bad request http status
            if (e.entityType == EntityType.Activity) {
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

    // full path: /api/v1/Activities
    @PostMapping
    public ResponseEntity<AbstractActivityDTO> createActivity(@RequestBody ActivityCreationDTO activityCreationDTO) {
        try {
            AbstractActivityDTO createdActivity = Activitieservice.createActivity(activityCreationDTO);
            return new ResponseEntity<>(createdActivity, HttpStatus.CREATED);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // TL;DR: Don't remove this endpoint; it may become useful. 
    @Deprecated(since = "Not being used on mobile currently. " +
            "Pending mobile feature implementation, per:" +
            "https://github.com/Daggerpov/Spawn-App-iOS-SwiftUI/issues/142")
    // full path: /api/v1/Activities/{id}
    @PutMapping("{id}")
    public ResponseEntity<?> replaceActivity(@RequestBody ActivityDTO newActivity, @PathVariable UUID id) {
        if (id == null) return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        try {
            return new ResponseEntity<>(Activitieservice.replaceActivity(newActivity, id), HttpStatus.OK);
        } catch (BaseNotFoundException e) {
            // Only return 404 if user doesn't exist, not if activity doesn't exist
            if (e.entityType == EntityType.User) {
                return new ResponseEntity<>(e.entityType, HttpStatus.NOT_FOUND);
            } else if (e.entityType == EntityType.Activity) {
                // Return 404 for activities too, as this is specifically looking up an activity by ID
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
    // full path: /api/v1/Activities/{id}
    @DeleteMapping("{id}")
    public ResponseEntity<?> deleteActivity(@PathVariable UUID id) {
        if (id == null) return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        try {
            boolean isDeleted = Activitieservice.deleteActivityById(id);
            if (isDeleted) {
                return new ResponseEntity<>(HttpStatus.NO_CONTENT); // Success
            } else {
                return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR); // Deletion failed
            }
        } catch (BaseNotFoundException e) {
            // For deletion, it makes sense to return 404 if the activity doesn't exist
            return new ResponseEntity<>(e.entityType, HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // this corresponds to the button on the activity for invited users
    // full path: /api/v1/Activities/{ActivityId}/toggleStatus/{userId}
    @PutMapping("{ActivityId}/toggleStatus/{userId}")
    public ResponseEntity<?> toggleParticipation(@PathVariable UUID ActivityId, @PathVariable UUID userId) {
        if (userId == null || ActivityId == null) return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        try {
            FullFeedActivityDTO updatedActivityAfterParticipationToggle = Activitieservice.toggleParticipation(ActivityId, userId);
            return new ResponseEntity<>(updatedActivityAfterParticipationToggle, HttpStatus.OK);
        } catch (BaseNotFoundException e) {
            // Only return 404 for appropriate entity types
            if (e.entityType == EntityType.User) {
                return new ResponseEntity<>(e.entityType, HttpStatus.NOT_FOUND);
            } else if (e.entityType == EntityType.Activity) {
                return new ResponseEntity<>(e.entityType, HttpStatus.NOT_FOUND);
            } else if (e.entityType == EntityType.ActivityUser) {
                // If the user is not invited to the activity, return 404
                return new ResponseEntity<>(e.entityType, HttpStatus.NOT_FOUND);
            } else {
                return new ResponseEntity<>(e.entityType, HttpStatus.NOT_FOUND);
            }
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // full path: /api/v1/Activities/feedActivities/{requestingUserId}
    // this method will return the activities created by a given user (like in `getActivitiesCreatedByUserId()`),
    // in the universal accent color, followed by feed activities (like in `getActivitiesInvitedTo()`
    @GetMapping("feedActivities/{requestingUserId}")
    // need this `? extends AbstractActivityDTO` instead of simply `AbstractActivityDTO`, because of this error:
    // https://stackoverflow.com/questions/27522741/incompatible-types-inference-variable-t-has-incompatible-bounds
    public ResponseEntity<?> getFeedActivities(@PathVariable UUID requestingUserId) {
        if (requestingUserId == null) return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        try {
            return new ResponseEntity<>(Activitieservice.getFeedActivities(requestingUserId), HttpStatus.OK);
        } catch (BasesNotFoundException e) {
            // thrown list of activities not found for given user id
            // if entities not found is Activity: return response with empty list and 200 status
            // otherwise: bad request http status
            if (e.entityType == EntityType.Activity) {
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

    // full path: /api/v1/Activities/{id}
    @GetMapping("{id}")
    public ResponseEntity<?> getFullActivityById(@PathVariable UUID id, @RequestParam UUID requestingUserId) {
        if (id == null || requestingUserId == null) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }

        try {
            return new ResponseEntity<>(Activitieservice.getFullActivityById(id, requestingUserId), HttpStatus.OK);
        } catch (BaseNotFoundException e) {
            // Activity or User not found - only return 404 if it's the user that's not found
            if (e.entityType == EntityType.User) {
                return new ResponseEntity<>(e.entityType, HttpStatus.NOT_FOUND);
            } else if (e.entityType == EntityType.Activity) {
                // Activity not found for a valid user, return empty response with 200
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
