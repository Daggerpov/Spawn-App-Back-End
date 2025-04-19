package com.danielagapov.spawn.DTOs;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * DTO for cache validation requests from mobile clients.
 * Contains a map of cache category names to their last update timestamps.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CacheValidationRequestDTO {
    /**
     * Map of cache category names (e.g., "friends", "events") to their 
     * last update timestamps in ISO-8601 format (e.g., "2023-04-01T10:15:30Z")
     */
    private Map<String, String> timestamps;
} 