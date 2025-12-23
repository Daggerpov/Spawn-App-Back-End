package com.danielagapov.spawn.shared.config;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for cache validation response that indicates whether a client's cached data is stale.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CacheValidationResponseDTO {
    /**
     * Whether the client should invalidate this category of cached data
     */
    private boolean invalidate;
    
    /**
     * Optional updated data that can be sent to avoid additional API calls
     * This is useful for smaller data sets that can be included directly
     */
    private byte[] updatedItems;
} 