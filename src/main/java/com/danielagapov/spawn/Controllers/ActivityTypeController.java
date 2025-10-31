package com.danielagapov.spawn.Controllers;

import com.danielagapov.spawn.DTOs.ActivityType.ActivityTypeDTO;
import com.danielagapov.spawn.DTOs.ActivityType.BatchActivityTypeUpdateDTO;
import com.danielagapov.spawn.Exceptions.Logger.ILogger;
import com.danielagapov.spawn.Services.ActivityType.IActivityTypeService;
import com.danielagapov.spawn.Util.LoggingUtils;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
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
    private final CacheManager cacheManager;

    @Autowired
    public ActivityTypeController(IActivityTypeService activityTypeService, ILogger logger, CacheManager cacheManager) {
        this.activityTypeService = activityTypeService;
        this.logger = logger;
        this.cacheManager = cacheManager;
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
            // Check if this is a JSON deserialization error from corrupted cache
            if (isJsonDeserializationError(e)) {
                logger.warn("Cache corruption detected for activity types for user: " + LoggingUtils.formatUserIdInfo(userId) + ". Clearing cache and retrying...");
                try {
                    // Evict the corrupted cache entry
                    evictCache("activityTypesByUserId", userId);
                    // Retry the operation
                    List<ActivityTypeDTO> activityTypes = activityTypeService.getActivityTypesByUserId(userId);
                    logger.info("Successfully recovered from cache corruption for activity types for user: " + LoggingUtils.formatUserIdInfo(userId));
                    return new ResponseEntity<>(activityTypes, HttpStatus.OK);
                } catch (Exception retryE) {
                    logger.error("Failed to recover from cache corruption for activity types: " + retryE.getMessage());
                    return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
                }
            }
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

    /**
     * Checks if an exception is a JSON deserialization error, typically caused by
     * corrupted cache data from the old serialization format.
     */
    private boolean isJsonDeserializationError(Exception e) {
        Throwable cause = e;
        while (cause != null) {
            if (cause instanceof JsonParseException || cause instanceof JsonMappingException) {
                return true;
            }
            // Also check for the specific error message patterns
            if (cause.getMessage() != null && 
                (cause.getMessage().contains("Could not read JSON") ||
                 cause.getMessage().contains("Unexpected token"))) {
                return true;
            }
            cause = cause.getCause();
        }
        return false;
    }

    /**
     * Evicts a corrupted cache entry for a given cache name and user ID.
     */
    private void evictCache(String cacheName, UUID userId) {
        try {
            if (cacheManager.getCache(cacheName) != null) {
                cacheManager.getCache(cacheName).evict(userId);
                logger.info("Evicted corrupted cache entry from '" + cacheName + "' for user: " + LoggingUtils.formatUserIdInfo(userId));
            }
        } catch (Exception e) {
            logger.error("Failed to evict cache entry from '" + cacheName + "': " + e.getMessage());
        }
    }
}