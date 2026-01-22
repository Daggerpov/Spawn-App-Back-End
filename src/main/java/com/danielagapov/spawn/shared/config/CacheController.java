package com.danielagapov.spawn.shared.config;

import com.danielagapov.spawn.shared.config.CacheValidationRequestDTO;
import com.danielagapov.spawn.shared.config.CacheValidationResponseDTO;
import com.danielagapov.spawn.shared.exceptions.Logger.ILogger;
import com.danielagapov.spawn.analytics.internal.services.ICacheService;
import com.danielagapov.spawn.activity.internal.services.ICalendarService;
import com.danielagapov.spawn.shared.util.LoggingUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

/**
 * Controller for handling mobile cache validation requests.
 * This controller helps mobile clients determine if their cached data is stale.
 */
@RestController
@RequestMapping("/api/v1/cache")
public class CacheController {

    private final ICacheService cacheService;
    private final ICalendarService calendarService;
    private final ILogger logger;

    @Autowired
    public CacheController(ICacheService cacheService, ICalendarService calendarService, ILogger logger) {
        this.cacheService = cacheService;
        this.calendarService = calendarService;
        this.logger = logger;
    }

    /**
     * Validates client cache timestamps against server data.
     * Clients send a map of data categories and their last update timestamps.
     * The server responds with information about which categories need refreshing.
     *
     * @param userId The ID of the user requesting cache validation
     * @param request DTO containing cache categories and their timestamps
     * @return A map of cache categories and their validation status
     */
    @PostMapping("/validate/{userId}")
    public ResponseEntity<Map<String, CacheValidationResponseDTO>> validateCache(
            @PathVariable UUID userId,
            @RequestBody CacheValidationRequestDTO request) {
        // Calculate the number of categories safely for logging
        // This prevents NullPointerException when request or timestamps is null
        int categoryCount = 0;
        if (request != null && request.getTimestamps() != null) {
            categoryCount = request.getTimestamps().size();
        }
        
        try {
            // Add null checks to prevent NullPointerException
            // This can happen when the mobile client sends an empty request body
            // or when the timestamps field is null
            Map<String, String> timestamps = null;
            if (request != null) {
                timestamps = request.getTimestamps();
            }
            
            Map<String, CacheValidationResponseDTO> response = cacheService.validateCache(userId, timestamps);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error validating cache for user: " + LoggingUtils.formatUserIdInfo(userId) + ": " + e.getMessage());
            throw e;
        }
    }
    
    /**
     * Clears all calendar caches for all users.
     * This endpoint should be called after schema changes to ensure fresh data.
     * 
     * @return A response indicating success or failure
     */
    @PostMapping("/clear-calendar-caches")
    public ResponseEntity<String> clearCalendarCaches() {
        logger.info("Clearing all calendar caches");
        try {
            calendarService.clearAllCalendarCaches();
            return ResponseEntity.ok("All calendar caches cleared successfully");
        } catch (Exception e) {
            logger.error("Error clearing calendar caches: " + e.getMessage());
            return ResponseEntity.status(500).body("Error clearing calendar caches: " + e.getMessage());
        }
    }
} 