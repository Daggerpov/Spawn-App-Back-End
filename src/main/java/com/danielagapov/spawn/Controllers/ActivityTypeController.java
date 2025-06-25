package com.danielagapov.spawn.Controllers;

import com.danielagapov.spawn.DTOs.ActivityType.ActivityTypeDTO;
import com.danielagapov.spawn.DTOs.ActivityType.BatchActivityTypeUpdateDTO;
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
@RequestMapping("/api/v1/{userId}/activity-types")
public class ActivityTypeController {

    private final IActivityTypeService activityTypeService;
    private final ILogger logger;

    @Autowired
    public ActivityTypeController(IActivityTypeService activityTypeService, ILogger logger) {
        this.activityTypeService = activityTypeService;
        this.logger = logger;
    }

    /**
     * Get activity types owned by a user
     * GET /api/v1/{userId}/activity-types
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
     * Create a new activity type
     * POST /api/v1/{userId}/activity-types
     */
    @PostMapping
    public ResponseEntity<ActivityTypeDTO> createActivityType(
            @PathVariable UUID userId,
            @RequestBody ActivityTypeDTO activityTypeDTO) {
        try {
            logger.info("Creating new activity type: " + activityTypeDTO.getTitle() + " for user: " + LoggingUtils.formatUserIdInfo(userId));
            
            // Create a new DTO with the ownerUserId set from the path parameter
            ActivityTypeDTO dtoWithOwner = new ActivityTypeDTO(
                activityTypeDTO.getId(),
                activityTypeDTO.getTitle(),
                activityTypeDTO.getAssociatedFriends(),
                activityTypeDTO.getIcon(),
                activityTypeDTO.getOrderNum(),
                userId, // Set ownerUserId from path parameter
                activityTypeDTO.getIsPinned() != null ? activityTypeDTO.getIsPinned() : false // Handle isPinned
            );
            
            ActivityTypeDTO createdActivityType = activityTypeService.createActivityType(dtoWithOwner);
            return new ResponseEntity<>(createdActivityType, HttpStatus.CREATED);
        } catch (Exception e) {
            logger.error("Error creating activity type: " + e.getMessage());
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Batch update activity types (create, update, delete)
     * PUT /api/v1/{userId}/activity-types
     */
    @PutMapping
    public ResponseEntity<BatchActivityTypeUpdateDTO> updateActivityTypes(
            @PathVariable UUID userId,
            @RequestBody BatchActivityTypeUpdateDTO batchActivityTypeUpdateDTO) {
        try {
            logger.info("Batch updating activity types for user: " + LoggingUtils.formatUserIdInfo(userId));
            
            // Set ownerUserId for all updated activity types
            List<ActivityTypeDTO> updatedWithOwner = batchActivityTypeUpdateDTO.getUpdatedActivityTypes().stream()
                .map(dto -> new ActivityTypeDTO(
                    dto.getId(),
                    dto.getTitle(),
                    dto.getAssociatedFriends(),
                    dto.getIcon(),
                    dto.getOrderNum(),
                    userId, // Set ownerUserId from path parameter
                    dto.getIsPinned() != null ? dto.getIsPinned() : false // Handle isPinned
                ))
                .toList();
            
            BatchActivityTypeUpdateDTO batchWithOwner = new BatchActivityTypeUpdateDTO(
                updatedWithOwner,
                batchActivityTypeUpdateDTO.getDeletedActivityTypeIds()
            );
            
            BatchActivityTypeUpdateDTO updatedBatch = activityTypeService.updateActivityTypes(batchWithOwner);
            return new ResponseEntity<>(updatedBatch, HttpStatus.OK);
        } catch (Exception e) {
            logger.error("Error batch updating activity types for user " + LoggingUtils.formatUserIdInfo(userId) + ": " + e.getMessage());
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Toggle pin status of an activity type
     * PATCH /api/v1/{userId}/activity-types/{activityTypeId}/pin
     */
    @PatchMapping("/{activityTypeId}/pin")
    public ResponseEntity<ActivityTypeDTO> togglePinActivityType(
            @PathVariable UUID userId,
            @PathVariable UUID activityTypeId) {
        try {
            logger.info("Toggling pin status for activity type: " + activityTypeId + " by user: " + LoggingUtils.formatUserIdInfo(userId));
            ActivityTypeDTO updatedActivityType = activityTypeService.togglePin(activityTypeId, userId);
            return new ResponseEntity<>(updatedActivityType, HttpStatus.OK);
        } catch (SecurityException e) {
            logger.error("Unauthorized pin toggle attempt for activity type " + activityTypeId + " by user " + userId + ": " + e.getMessage());
            return new ResponseEntity<>(HttpStatus.FORBIDDEN);
        } catch (Exception e) {
            logger.error("Error toggling pin for activity type " + activityTypeId + ": " + e.getMessage());
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Delete an activity type
     * DELETE /api/v1/{userId}/activity-types/{activityTypeId}
     */
    @DeleteMapping("/{activityTypeId}")
    public ResponseEntity<Void> deleteActivityType(
            @PathVariable UUID userId,
            @PathVariable UUID activityTypeId) {
        try {
            logger.info("Deleting activity type: " + activityTypeId + " for user: " + LoggingUtils.formatUserIdInfo(userId));
            activityTypeService.deleteActivityType(activityTypeId);
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        } catch (Exception e) {
            logger.error("Error deleting activity type " + activityTypeId + ": " + e.getMessage());
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
} 