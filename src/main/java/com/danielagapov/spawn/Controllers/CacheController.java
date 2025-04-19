package com.danielagapov.spawn.Controllers;

import com.danielagapov.spawn.DTOs.CacheValidationRequestDTO;
import com.danielagapov.spawn.DTOs.CacheValidationResponseDTO;
import com.danielagapov.spawn.Services.ICacheService;
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

    @Autowired
    public CacheController(ICacheService cacheService) {
        this.cacheService = cacheService;
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
            
        Map<String, CacheValidationResponseDTO> response = cacheService.validateCache(userId, request.getTimestamps());
        return ResponseEntity.ok(response);
    }
} 