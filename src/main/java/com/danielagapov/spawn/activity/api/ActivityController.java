package com.danielagapov.spawn.activity.api;

import com.danielagapov.spawn.activity.api.dto.ActivityDTO;
import com.danielagapov.spawn.activity.api.dto.ActivityPartialUpdateDTO;
import com.danielagapov.spawn.activity.api.dto.FullFeedActivityDTO;
import com.danielagapov.spawn.shared.util.EntityType;
import com.danielagapov.spawn.shared.exceptions.ActivityFullException;
import com.danielagapov.spawn.shared.exceptions.Base.BaseNotFoundException;
import com.danielagapov.spawn.shared.exceptions.Base.BasesNotFoundException;
import com.danielagapov.spawn.shared.exceptions.Logger.ILogger;
import com.danielagapov.spawn.activity.internal.services.IActivityService;
import com.danielagapov.spawn.shared.util.LoggingUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.UUID;

@RestController()
@RequestMapping("api/v1/activities")
public final class ActivityController {
    private final IActivityService activityService;
    private final ILogger logger;

    public ActivityController(IActivityService ActivityService, ILogger logger) {
        this.activityService = ActivityService;
        this.logger = logger;
    }

    // TL;DR: Don't remove this endpoint; it may become useful.
    @Deprecated(since = "Not being used on mobile currently." +
            "This may become a feature, as Owen has suggested, " +
            "with showing a friend's recent activities.")
    // full path: /api/v1/activities/user/{creatorUserId}
    @GetMapping("user/{creatorUserId}")
    public ResponseEntity<?> getActivitiesCreatedByUserId(@PathVariable UUID creatorUserId) {
        try {
            return new ResponseEntity<>(activityService.convertActivitiesToFullFeedSelfOwnedActivities(activityService.getActivitiesByOwnerId(creatorUserId), creatorUserId), HttpStatus.OK);
        } catch (BaseNotFoundException e) {
            // user or activity not found
            logger.error("User or activity not found for user: " + LoggingUtils.formatUserIdInfo(creatorUserId) + ": " + e.getMessage());
            return new ResponseEntity<>(e.entityType, HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            // any other exception
            logger.error("Error getting activities created by user: " + LoggingUtils.formatUserIdInfo(creatorUserId) + ": " + e.getMessage());
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    
    // full path: /api/v1/activities/profile/{profileUserId}?requestingUserId={requestingUserId}
    @GetMapping("profile/{profileUserId}")
    public ResponseEntity<?> getProfileActivities(@PathVariable UUID profileUserId, @RequestParam UUID requestingUserId) {
        if (profileUserId == null || requestingUserId == null) {
            logger.error("Invalid parameters: profileUserId or requestingUserId is null");
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
        
        try {
            return new ResponseEntity<>(activityService.getProfileActivities(profileUserId, requestingUserId), HttpStatus.OK);
        } catch (BaseNotFoundException e) {
            // User not found - return 404
            logger.error("User not found for profile activities: " + e.getMessage());
            return new ResponseEntity<>(e.entityType, HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            // Any other exception
            logger.error("Error getting profile activities for user: " + LoggingUtils.formatUserIdInfo(profileUserId) + ": " + e.getMessage());
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // full path: /api/v1/activities
    @PostMapping
    public ResponseEntity<FullFeedActivityDTO> createActivity(@RequestBody ActivityDTO activityDTO) {
        try {
            FullFeedActivityDTO response = activityService.createActivityWithSuggestions(activityDTO);
            return new ResponseEntity<>(response, HttpStatus.CREATED);
        } catch (IllegalArgumentException e) {
            logger.error("Invalid request for activity creation: " + e.getMessage());
            return new ResponseEntity<FullFeedActivityDTO>(HttpStatus.BAD_REQUEST);
        } catch (BaseNotFoundException e) {
            logger.error("Entity not found during activity creation: " + e.getMessage());
            return new ResponseEntity<FullFeedActivityDTO>(HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            logger.error("Error creating activity: " + e.getMessage());
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // full path: /api/v1/activities/{id}
    @PutMapping("{id}")
    public ResponseEntity<?> replaceActivity(@RequestBody ActivityDTO newActivity, @PathVariable UUID id) {
        if (id == null) {
            logger.error("Invalid parameter: activity ID is null");
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
        try {
            return new ResponseEntity<>(activityService.replaceActivity(newActivity, id), HttpStatus.OK);
        } catch (BaseNotFoundException e) {
            // Only return 404 if user doesn't exist, not if activity doesn't exist
            if (e.entityType == EntityType.User) {
                logger.error("User not found for activity replacement: " + e.getMessage());
                return new ResponseEntity<>(e.entityType, HttpStatus.NOT_FOUND);
            } else if (e.entityType == EntityType.Activity) {
                // Return 404 for activities too, as this is specifically looking up an activity by ID
                logger.error("Activity not found for replacement: " + id + ": " + e.getMessage());
                return new ResponseEntity<>(e.entityType, HttpStatus.NOT_FOUND);
            } else {
                logger.error("Entity not found for activity replacement: " + e.getMessage());
                return new ResponseEntity<>(e.entityType, HttpStatus.NOT_FOUND);
            }
        } catch (Exception e) {
            logger.error("Error replacing activity: " + id + ": " + e.getMessage());
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // full path: /api/v1/activities/{id}/partial
    @PatchMapping("{id}/partial")
    public ResponseEntity<?> partialUpdateActivity(@RequestBody ActivityPartialUpdateDTO updates, @PathVariable UUID id) {
        if (id == null) {
            logger.error("Invalid parameter: activity ID is null");
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
        try {
            return new ResponseEntity<>(activityService.partialUpdateActivity(updates, id), HttpStatus.OK);
        } catch (BaseNotFoundException e) {
            if (e.entityType == EntityType.Activity) {
                logger.error("Activity not found for partial update: " + id + ": " + e.getMessage());
                return new ResponseEntity<>("Activity not found", HttpStatus.NOT_FOUND);
            } else {
                logger.error("Entity not found for partial activity update: " + e.getMessage());
                return new ResponseEntity<>("Entity not found: " + e.getMessage(), HttpStatus.NOT_FOUND);
            }
        } catch (IllegalArgumentException e) {
            logger.error("Invalid update data for activity: " + id + ": " + e.getMessage());
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            logger.error("Error partially updating activity: " + id + ": " + e.getMessage());
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // TL;DR: Don't remove this endpoint; it may become useful. 
    @Deprecated(since = "Not being used on mobile currently. " +
            "Pending mobile feature implementation, per:" +
            "https://github.com/Daggerpov/Spawn-App-iOS-SwiftUI/issues/142")
    // full path: /api/v1/activities/{id}
    @DeleteMapping("{id}")
    public ResponseEntity<?> deleteActivity(@PathVariable UUID id) {
        if (id == null) {
            logger.error("Invalid parameter: activity ID is null");
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
        try {
            boolean isDeleted = activityService.deleteActivityById(id);
            if (isDeleted) {
                return new ResponseEntity<>(HttpStatus.NO_CONTENT); // Success
            } else {
                logger.error("Failed to delete activity: " + id);
                return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR); // Deletion failed
            }
        } catch (BaseNotFoundException e) {
            // For deletion, it makes sense to return 404 if the activity doesn't exist
            logger.error("Activity not found for deletion: " + id + ": " + e.getMessage());
            return new ResponseEntity<>(e.entityType, HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            logger.error("Error deleting activity: " + id + ": " + e.getMessage());
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // this corresponds to the button on the activity for invited users
    // full path: /api/v1/activities/{ActivityId}/toggleStatus/{userId}
    @PutMapping("{ActivityId}/toggleStatus/{userId}")
    public ResponseEntity<?> toggleParticipation(@PathVariable UUID ActivityId, @PathVariable UUID userId) {
        if (userId == null || ActivityId == null) {
            logger.error("Invalid parameters: userId or ActivityId is null");
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
        try {
            FullFeedActivityDTO updatedActivityAfterParticipationToggle = activityService.toggleParticipation(ActivityId, userId);
            return new ResponseEntity<>(updatedActivityAfterParticipationToggle, HttpStatus.OK);
        } catch (BaseNotFoundException e) {
            // Only return 404 for appropriate entity types
            if (e.entityType == EntityType.User) {
                logger.error("User not found for participation toggle: " + LoggingUtils.formatUserIdInfo(userId) + ": " + e.getMessage());
                return new ResponseEntity<>(e.entityType, HttpStatus.NOT_FOUND);
            } else if (e.entityType == EntityType.Activity) {
                logger.error("Activity not found for participation toggle: " + ActivityId + ": " + e.getMessage());
                return new ResponseEntity<>(e.entityType, HttpStatus.NOT_FOUND);
            } else if (e.entityType == EntityType.ActivityUser) {
                // If the user is not invited to the activity, return 404
                logger.error("User not invited to activity for participation toggle: " + LoggingUtils.formatUserIdInfo(userId) + " in activity: " + ActivityId + ": " + e.getMessage());
                return new ResponseEntity<>(e.entityType, HttpStatus.NOT_FOUND);
            } else {
                logger.error("Entity not found for participation toggle: " + e.getMessage());
                return new ResponseEntity<>(e.entityType, HttpStatus.NOT_FOUND);
            }
        } catch (ActivityFullException e) {
            logger.error("Activity is full for participation toggle: " + ActivityId + ": " + e.getMessage());
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            logger.error("Error toggling participation for user: " + LoggingUtils.formatUserIdInfo(userId) + " in activity: " + ActivityId + ": " + e.getMessage());
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // full path: /api/v1/activities/feedActivities/{requestingUserId}
    // this method will return the activities created by a given user (like in `getActivitiesCreatedByUserId()`),
    // in the universal accent color, followed by feed activities (like in `getActivitiesInvitedTo()`
    @GetMapping("feedActivities/{requestingUserId}")
    // need this `? extends AbstractActivityDTO` instead of simply `AbstractActivityDTO`, because of this error:
    // https://stackoverflow.com/questions/27522741/incompatible-types-inference-variable-t-has-incompatible-bounds
    public ResponseEntity<?> getFeedActivities(@PathVariable UUID requestingUserId) {
        if (requestingUserId == null) {
            logger.error("Invalid parameter: requestingUserId is null");
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
        try {
            return new ResponseEntity<>(activityService.getFeedActivities(requestingUserId), HttpStatus.OK);
        } catch (BasesNotFoundException e) {
            // thrown list of activities not found for given user id
            // if entities not found is Activity: return response with empty list and 200 status
            // otherwise: bad request http status
            if (e.entityType == EntityType.Activity) {
                return new ResponseEntity<>(new ArrayList<>(), HttpStatus.OK);
            } else {
                logger.error("Bad request for feed activities: " + e.getMessage());
                return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
            }
        } catch (BaseNotFoundException e) {
            // user not found - return 404
            logger.error("User not found for feed activities: " + LoggingUtils.formatUserIdInfo(requestingUserId) + ": " + e.getMessage());
            return new ResponseEntity<>(e.entityType, HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            logger.error("Error getting feed activities for user: " + LoggingUtils.formatUserIdInfo(requestingUserId) + ": " + e.getMessage());
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // full path: /api/v1/activities/{id}
    @GetMapping("{id}")
    public ResponseEntity<?> getFullActivityById(@PathVariable UUID id, 
                                                @RequestParam(required = false) UUID requestingUserId,
                                                @RequestParam(required = false, defaultValue = "false") boolean isActivityExternalInvite,
                                                @RequestParam(required = false, defaultValue = "false") boolean autoJoin) {
        if (id == null) {
            logger.error("Invalid parameter: activity ID is null");
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
        
        // For external invites, we don't require requestingUserId and return simplified DTO
        if (isActivityExternalInvite) {
            try {
                return new ResponseEntity<>(activityService.getActivityInviteById(id), HttpStatus.OK);
            } catch (BaseNotFoundException e) {
                // Activity not found
                if (e.entityType == EntityType.Activity) {
                    logger.error("Activity not found for external invite: " + id + ": " + e.getMessage());
                    return new ResponseEntity<>(HttpStatus.NOT_FOUND);
                } else {
                    logger.error("Entity not found for external invite: " + e.getMessage());
                    return new ResponseEntity<>(e.entityType, HttpStatus.NOT_FOUND);
                }
            } catch (Exception e) {
                logger.error("Error getting activity invite by ID: " + id + ": " + e.getMessage());
                return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }
        
        // Original behavior for authenticated requests
        if (requestingUserId == null) {
            logger.error("Invalid parameter: requestingUserId is required for authenticated requests");
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }

        try {
            // If autoJoin is true, automatically join the user to the activity
            if (autoJoin) {
                logger.info("Auto-joining user " + LoggingUtils.formatUserIdInfo(requestingUserId) + " to activity " + id);
                return new ResponseEntity<>(activityService.autoJoinUserToActivity(id, requestingUserId), HttpStatus.OK);
            } else {
                return new ResponseEntity<>(activityService.getFullActivityById(id, requestingUserId), HttpStatus.OK);
            }
        } catch (BaseNotFoundException e) {
            // Activity or User not found - only return 404 if it's the user that's not found
            if (e.entityType == EntityType.User) {
                logger.error("User not found for full activity: " + LoggingUtils.formatUserIdInfo(requestingUserId) + ": " + e.getMessage());
                return new ResponseEntity<>(e.entityType, HttpStatus.NOT_FOUND);
            } else if (e.entityType == EntityType.Activity) {
                // Activity not found for a valid user, return empty response with 200
                return new ResponseEntity<>(new ArrayList<>(), HttpStatus.OK);
            } else {
                logger.error("Entity not found for full activity: " + e.getMessage());
                return new ResponseEntity<>(e.entityType, HttpStatus.NOT_FOUND);
            }
        } catch (Exception e) {
            // Any other exception
            logger.error("Error getting full activity by ID: " + id + " for user: " + LoggingUtils.formatUserIdInfo(requestingUserId) + ": " + e.getMessage());
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("{activityId}/chats")
    public ResponseEntity<?> getChatMessagesForActivity(@PathVariable UUID activityId) {
        try {
            return new ResponseEntity<>(activityService.getChatMessagesByActivityId(activityId), HttpStatus.OK);
        } catch (Exception e) {
            logger.error("Error getting chat messages for activity: " + activityId + ": " + e.getMessage());
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
