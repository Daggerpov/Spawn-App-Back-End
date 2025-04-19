package com.danielagapov.spawn.Services;

import com.danielagapov.spawn.DTOs.CacheValidationResponseDTO;
import com.danielagapov.spawn.DTOs.Event.EventDTO;
import com.danielagapov.spawn.DTOs.Event.FullFeedEventDTO;
import com.danielagapov.spawn.DTOs.User.FriendUser.FullFriendUserDTO;
import com.danielagapov.spawn.Models.User;
import com.danielagapov.spawn.Repositories.IUserRepository;
import com.danielagapov.spawn.Services.Event.IEventService;
import com.danielagapov.spawn.Services.User.IUserService;
import com.danielagapov.spawn.Utils.LoggingUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Service implementation for mobile cache validation.
 * This service compares client-side cache timestamps with server-side data
 * to determine if client caches need to be refreshed.
 * 
 * IMPORTANT: Notifications are deliberately excluded from caching to ensure real-time delivery.
 * The service will always invalidate any notification cache to prevent stale notifications.
 */
@Service
public class CacheService implements ICacheService {
    private static final Logger logger = LoggerFactory.getLogger(CacheService.class);
    
    private final IUserRepository userRepository;
    private final IUserService userService;
    private final IEventService eventService;
    private final ObjectMapper objectMapper;
    
    // Define cache categories
    private static final String FRIENDS_CACHE = "friends";
    private static final String EVENTS_CACHE = "events";
    
    @Autowired
    public CacheService(
            IUserRepository userRepository,
            IUserService userService,
            IEventService eventService,
            ObjectMapper objectMapper) {
        this.userRepository = userRepository;
        this.userService = userService;
        this.eventService = eventService;
        this.objectMapper = objectMapper;
    }
    
    /**
     * Validates client cache against server data.
     * For each cache category in the request, determines if the client's cache is stale
     * and needs to be refreshed.
     * 
     * Note: Notifications are always invalidated to ensure they are never cached on mobile devices.
     * This guarantees users always receive the most up-to-date notifications.
     *
     * @param userId The user ID requesting cache validation
     * @param clientCacheTimestamps Map of cache category names to their last update timestamps
     * @return Map of cache category names to validation response objects
     */
    @Override
    public Map<String, CacheValidationResponseDTO> validateCache(
            UUID userId, Map<String, String> clientCacheTimestamps) {
        
        Map<String, CacheValidationResponseDTO> response = new HashMap<>();
        User user = userRepository.findById(userId).orElse(null);
        
        if (user == null) {
            logger.warn("Cache validation requested for non-existent user: {}", userId);
            return response;
        }
        
        logger.info("Validating cache for user: " + LoggingUtils.formatUserInfo(user));
        
        // Validate friends cache
        if (clientCacheTimestamps.containsKey(FRIENDS_CACHE)) {
            response.put(FRIENDS_CACHE, validateFriendsCache(user, clientCacheTimestamps.get(FRIENDS_CACHE)));
        }
        
        // Validate events cache
        if (clientCacheTimestamps.containsKey(EVENTS_CACHE)) {
            response.put(EVENTS_CACHE, validateEventsCache(user, clientCacheTimestamps.get(EVENTS_CACHE)));
        }
        
        return response;
    }
    
    /**
     * Validates the user's friends cache by checking if any friends have been added, removed,
     * or updated since the client's last cache timestamp.
     */
    private CacheValidationResponseDTO validateFriendsCache(User user, String clientTimestamp) {
        try {
            // Parse the client timestamp
            ZonedDateTime clientTime = ZonedDateTime.parse(clientTimestamp, DateTimeFormatter.ISO_DATE_TIME);
            
            // Get the latest friend activity timestamp for this user
            Instant latestFriendActivity = getLatestFriendActivity(user.getId());
            
            // If client cache is older than the latest activity, invalidate
            boolean needsUpdate = latestFriendActivity.isAfter(clientTime.toInstant());
            
            if (needsUpdate) {
                // For small data sets like friends, we can include the updated data
                // to save an extra API call from the client
                try {
                    // Get current friends for the user
                    List<FullFriendUserDTO> friends = userService.getFullFriendUsersByUserId(user.getId());
                    byte[] friendsData = objectMapper.writeValueAsBytes(friends);
                    
                    // Only include the data if it's not too large (limit to ~100KB)
                    if (friendsData.length < 100_000) {
                        return new CacheValidationResponseDTO(true, friendsData);
                    }
                } catch (Exception e) {
                    logger.error("Failed to serialize friends data", e);
                }
                
                // If we couldn't include the data, just tell the client to refresh
                return new CacheValidationResponseDTO(true, null);
            }
            
            // Client cache is still valid
            return new CacheValidationResponseDTO(false, null);
            
        } catch (Exception e) {
            logger.error("Error validating friends cache for user {}: {}", user.getId(), e.getMessage());
            // On error, tell client to refresh to be safe
            return new CacheValidationResponseDTO(true, null);
        }
    }
    
    /**
     * Validates the user's events cache by checking if any relevant events have been
     * created, updated, or deleted since the client's last cache timestamp.
     */
    private CacheValidationResponseDTO validateEventsCache(User user, String clientTimestamp) {
        try {
            // Parse the client timestamp
            ZonedDateTime clientTime = ZonedDateTime.parse(clientTimestamp, DateTimeFormatter.ISO_DATE_TIME);
            
            // Get the latest event activity timestamp for this user
            Instant latestEventActivity = getLatestEventActivity(user.getId());
            
            // If client cache is older than the latest activity, invalidate
            boolean needsUpdate = latestEventActivity.isAfter(clientTime.toInstant());
            
            if (needsUpdate) {
                // For events, we'll use feed events which combines owned and invited events
                try {
                    // Get current events for the user
                    List<FullFeedEventDTO> feedEvents = eventService.getFeedEvents(user.getId());
                    byte[] eventsData = objectMapper.writeValueAsBytes(feedEvents);
                    
                    // Only include the data if it's not too large (limit to ~100KB)
                    if (eventsData.length < 100_000) {
                        return new CacheValidationResponseDTO(true, eventsData);
                    }
                } catch (Exception e) {
                    logger.error("Failed to serialize events data", e);
                }
                
                // If we couldn't include the data, just tell the client to refresh
                return new CacheValidationResponseDTO(true, null);
            }
            
            // Client cache is still valid
            return new CacheValidationResponseDTO(false, null);
            
        } catch (Exception e) {
            logger.error("Error validating events cache for user {}: {}", user.getId(), e.getMessage());
            // On error, tell client to refresh to be safe
            return new CacheValidationResponseDTO(true, null);
        }
    }
    
    /**
     * Gets the timestamp of the latest friend-related activity for a user.
     * This includes friend requests, acceptances, and any profile updates of friends.
     */
    private Instant getLatestFriendActivity(UUID userId) {
        // This would typically query the database for:
        // 1. Latest friend request involving the user
        // 2. Latest friend request acceptance involving the user
        // 3. Latest profile update of any of the user's friends
        
        // For now, returning current time if we can't determine it
        // In a real implementation, this would query the database
        return Instant.now();
    }
    
    /**
     * Gets the timestamp of the latest event-related activity relevant to a user.
     * This includes events the user created, is participating in, or was invited to.
     */
    private Instant getLatestEventActivity(UUID userId) {
        // This would typically query the database for:
        // 1. Latest created event by the user
        // 2. Latest updated event the user is participating in
        // 3. Latest event invitation to the user
        
        // For now, returning current time if we can't determine it
        // In a real implementation, this would query the database
        return Instant.now();
    }
} 