package com.danielagapov.spawn.analytics.internal.services;

import com.danielagapov.spawn.shared.config.CacheValidationResponseDTO;

import java.util.Map;
import java.util.UUID;

/**
 * Service interface for mobile cache validation.
 * 
 * Note: This service is designed to exclude notifications from caching
 * to ensure real-time delivery on mobile devices.
 */
public interface ICacheService {
    
    /**
     * Validates client cache against server data.
     * 
     * @param userId The user ID requesting cache validation
     * @param clientCacheTimestamps Map of cache category names to their last update timestamps
     * @return Map of cache category names to validation response objects
     * 
     * Note: Notifications are always invalidated (not cached) to ensure real-time delivery.
     */
    Map<String, CacheValidationResponseDTO> validateCache(UUID userId, Map<String, String> clientCacheTimestamps);
} 