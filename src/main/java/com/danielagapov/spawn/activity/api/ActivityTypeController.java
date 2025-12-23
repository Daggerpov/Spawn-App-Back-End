package com.danielagapov.spawn.activity.api;

import com.danielagapov.spawn.activity.api.dto.ActivityTypeDTO;
import com.danielagapov.spawn.activity.api.dto.BatchActivityTypeUpdateDTO;
import com.danielagapov.spawn.shared.exceptions.Logger.ILogger;
import com.danielagapov.spawn.activity.internal.services.IActivityTypeService;
import com.danielagapov.spawn.shared.util.LoggingUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users/{userId}/activity-types")
public final class ActivityTypeController {

    private final IActivityTypeService activityTypeService;
    private final ILogger logger;

    @Autowired
    public ActivityTypeController(IActivityTypeService activityTypeService, ILogger logger) {
        this.activityTypeService = activityTypeService;
        this.logger = logger;
    }

    /**
     * Get activity types owned by a user
     * GET /api/v1/users/{userId}/activity-types
     */
    @GetMapping
    public ResponseEntity<List<ActivityTypeDTO>> getOwnedActivityTypesForUser(@PathVariable UUID userId) {
        try {
            logger.info("Fetching owned activity types for user: " + LoggingUtils.formatUserIdInfo(userId));
            List<ActivityTypeDTO> activityTypes = activityTypeService.getActivityTypesByUserId(userId);
            return new ResponseEntity<>(activityTypes, HttpStatus.OK);
        } catch (Exception e) {
            logger.error("Error fetching owned activity types for user " + LoggingUtils.formatUserIdInfo(userId) + ": " + e.getMessage());
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Batch update activity types (create, update, delete)
     * PUT /api/v1/users/{userId}/activity-types
     */
    @PutMapping
    public ResponseEntity<List<ActivityTypeDTO>> updateActivityTypes(
            @PathVariable UUID userId,
            @RequestBody BatchActivityTypeUpdateDTO batchActivityTypeUpdateDTO) {
        try {
            logger.info("Batch updating activity types for user: " + LoggingUtils.formatUserIdInfo(userId));
            
            List<ActivityTypeDTO> updatedActivityTypes = activityTypeService.updateActivityTypes(userId, batchActivityTypeUpdateDTO);
            return new ResponseEntity<>(updatedActivityTypes, HttpStatus.OK);
        } catch (Exception e) {
            logger.error("Error batch updating activity types for user " + LoggingUtils.formatUserIdInfo(userId) + ": " + e.getMessage());
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}