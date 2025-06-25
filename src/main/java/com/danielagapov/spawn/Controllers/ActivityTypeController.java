package com.danielagapov.spawn.Controllers;

import com.danielagapov.spawn.DTOs.Activity.ActivityTypeDTO;
import com.danielagapov.spawn.DTOs.Activity.ActivityTypePinUpdateDTO;
import com.danielagapov.spawn.Exceptions.Logger.ILogger;
import com.danielagapov.spawn.Services.ActivityType.IActivityTypeService;
import com.danielagapov.spawn.Util.LoggingUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/activity-type")
public class ActivityTypeController {

    private final IActivityTypeService activityTypeService;
    private final ILogger logger;

    @Autowired
    public ActivityTypeController(IActivityTypeService activityTypeService, ILogger logger) {
        this.activityTypeService = activityTypeService;
        this.logger = logger;
    }

    /**
     * Get all activity types for a user with pinning information
     * GET /api/v1/activity-type/{userId}
     */
    @GetMapping("/{userId}")
    public ResponseEntity<List<ActivityTypeDTO>> getActivityTypesForUser(@PathVariable UUID userId) {
        try {
            logger.info("Fetching activity types for user: " + LoggingUtils.formatUserIdInfo(userId));
            List<ActivityTypeDTO> activityTypes = activityTypeService.getActivityTypesForUser(userId);
            return new ResponseEntity<>(activityTypes, HttpStatus.OK);
        } catch (Exception e) {
            logger.error("Error fetching activity types for user " + LoggingUtils.formatUserIdInfo(userId) + ": " + e.getMessage());
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Get pinned activity type IDs for a user
     * GET /api/v1/activity-type/pinned/{userId}
     */
    @GetMapping("/pinned/{userId}")
    public ResponseEntity<List<UUID>> getPinnedActivityTypes(@PathVariable UUID userId) {
        try {
            logger.info("Fetching pinned activity types for user: " + LoggingUtils.formatUserIdInfo(userId));
            List<UUID> pinnedActivityTypeIds = activityTypeService.getPinnedActivityTypeIds(userId);
            return new ResponseEntity<>(pinnedActivityTypeIds, HttpStatus.OK);
        } catch (Exception e) {
            logger.error("Error fetching pinned activity types for user " + LoggingUtils.formatUserIdInfo(userId) + ": " + e.getMessage());
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Toggle pin status for an activity type
     * PUT /api/v1/activity-type/pin/{userId}
     */
    @PutMapping("/pin/{userId}")
    public ResponseEntity<Void> toggleActivityTypePin(
            @PathVariable UUID userId,
            @RequestBody ActivityTypePinUpdateDTO pinUpdateDTO) {
        try {
            logger.info("Toggling pin status for activity type " + pinUpdateDTO.getActivityTypeId() + 
                       " to " + pinUpdateDTO.getIsPinned() + " for user: " + LoggingUtils.formatUserIdInfo(userId));
            
            activityTypeService.toggleActivityTypePin(userId, pinUpdateDTO.getActivityTypeId(), pinUpdateDTO.getIsPinned());
            return new ResponseEntity<>(HttpStatus.OK);
        } catch (Exception e) {
            logger.error("Error toggling pin status for user " + LoggingUtils.formatUserIdInfo(userId) + ": " + e.getMessage());
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Create a new activity type
     * POST /api/v1/activity-type
     */
    @PostMapping
    public ResponseEntity<ActivityTypeDTO> createActivityType(@RequestBody ActivityTypeDTO activityTypeDTO) {
        try {
            logger.info("Creating new activity type: " + activityTypeDTO.getTitle());
            ActivityTypeDTO createdActivityType = activityTypeService.createActivityType(activityTypeDTO);
            return new ResponseEntity<>(createdActivityType, HttpStatus.CREATED);
        } catch (Exception e) {
            logger.error("Error creating activity type: " + e.getMessage());
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Update an existing activity type
     * PUT /api/v1/activity-type/{activityTypeId}
     */
    @PutMapping("/{activityTypeId}")
    public ResponseEntity<ActivityTypeDTO> updateActivityType(
            @PathVariable UUID activityTypeId,
            @RequestBody ActivityTypeDTO activityTypeDTO) {
        try {
            logger.info("Updating activity type: " + activityTypeId);
            ActivityTypeDTO updatedActivityType = activityTypeService.updateActivityType(activityTypeId, activityTypeDTO);
            return new ResponseEntity<>(updatedActivityType, HttpStatus.OK);
        } catch (Exception e) {
            logger.error("Error updating activity type " + activityTypeId + ": " + e.getMessage());
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Delete an activity type
     * DELETE /api/v1/activity-type/{activityTypeId}
     */
    @DeleteMapping("/{activityTypeId}")
    public ResponseEntity<Void> deleteActivityType(@PathVariable UUID activityTypeId) {
        try {
            logger.info("Deleting activity type: " + activityTypeId);
            activityTypeService.deleteActivityType(activityTypeId);
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        } catch (Exception e) {
            logger.error("Error deleting activity type " + activityTypeId + ": " + e.getMessage());
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
} 