package com.danielagapov.spawn.Services;

import com.danielagapov.spawn.DTOs.CacheValidationResponseDTO;

import java.util.Map;
import java.util.UUID;

/**
 * Service interface for mobile cache validation.
 */
public interface ICacheService {
    
    /**
     * Validates client cache against server data.
     * 
     * @param userId The user ID requesting cache validation
     * @param clientCacheTimestamps Map of cache category names to their last update timestamps
     * @return Map of cache category names to validation response objects
     */
    Map<String, CacheValidationResponseDTO> validateCache(UUID userId, Map<String, String> clientCacheTimestamps);
} 