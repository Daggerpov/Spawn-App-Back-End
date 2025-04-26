package com.danielagapov.spawn.Services.Cache;

import com.danielagapov.spawn.DTOs.CacheValidationResponseDTO;
import com.danielagapov.spawn.DTOs.Event.FullFeedEventDTO;
import com.danielagapov.spawn.DTOs.FriendRequest.FetchFriendRequestDTO;
import com.danielagapov.spawn.DTOs.FriendTag.FriendTagDTO;
import com.danielagapov.spawn.DTOs.User.BaseUserDTO;
import com.danielagapov.spawn.DTOs.User.FriendUser.FullFriendUserDTO;
import com.danielagapov.spawn.DTOs.User.FriendUser.RecommendedFriendUserDTO;
import com.danielagapov.spawn.Models.User;
import com.danielagapov.spawn.Repositories.IUserRepository;
import com.danielagapov.spawn.Repositories.IFriendTagRepository;
import com.danielagapov.spawn.Repositories.IUserFriendTagRepository;
import com.danielagapov.spawn.Services.Event.IEventService;
import com.danielagapov.spawn.Services.FriendRequest.IFriendRequestService;
import com.danielagapov.spawn.Services.FriendTag.IFriendTagService;
import com.danielagapov.spawn.Services.User.IUserService;
import com.danielagapov.spawn.Utils.LoggingUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
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
    private final IFriendRequestService friendRequestService;
    private final IFriendTagService friendTagService;
    private final IFriendTagRepository friendTagRepository;
    private final IUserFriendTagRepository userFriendTagRepository;
    private final ObjectMapper objectMapper;
    private final CacheManager cacheManager;
    
    // Define cache categories
    private static final String FRIENDS_CACHE = "friends";
    private static final String EVENTS_CACHE = "events";
    private static final String PROFILE_PICTURE_CACHE = "profilePicture";
    private static final String OTHER_PROFILES_CACHE = "otherProfiles";
    private static final String RECOMMENDED_FRIENDS_CACHE = "recommendedFriends";
    private static final String FRIEND_REQUESTS_CACHE = "friendRequests";
    private static final String USER_TAGS_CACHE = "userTags";
    private static final String TAG_FRIENDS_CACHE = "tagFriends";
    
    @Autowired
    public CacheService(
            IUserRepository userRepository,
            IUserService userService,
            IEventService eventService,
            IFriendRequestService friendRequestService,
            IFriendTagService friendTagService,
            IFriendTagRepository friendTagRepository,
            IUserFriendTagRepository userFriendTagRepository,
            ObjectMapper objectMapper,
            CacheManager cacheManager) {
        this.userRepository = userRepository;
        this.userService = userService;
        this.eventService = eventService;
        this.friendRequestService = friendRequestService;
        this.friendTagService = friendTagService;
        this.friendTagRepository = friendTagRepository;
        this.userFriendTagRepository = userFriendTagRepository;
        this.objectMapper = objectMapper;
        this.cacheManager = cacheManager;
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

        // Validate profile picture cache
        if (clientCacheTimestamps.containsKey(PROFILE_PICTURE_CACHE)) {
            response.put(PROFILE_PICTURE_CACHE, validateProfilePictureCache(user, clientCacheTimestamps.get(PROFILE_PICTURE_CACHE)));
        }
        
        // Validate other profiles cache
        if (clientCacheTimestamps.containsKey(OTHER_PROFILES_CACHE)) {
            response.put(OTHER_PROFILES_CACHE, validateOtherProfilesCache(user, clientCacheTimestamps.get(OTHER_PROFILES_CACHE)));
        }
        
        // Validate recommended friends cache
        if (clientCacheTimestamps.containsKey(RECOMMENDED_FRIENDS_CACHE)) {
            response.put(RECOMMENDED_FRIENDS_CACHE, validateRecommendedFriendsCache(user, clientCacheTimestamps.get(RECOMMENDED_FRIENDS_CACHE)));
        }
        
        // Validate friend requests cache
        if (clientCacheTimestamps.containsKey(FRIEND_REQUESTS_CACHE)) {
            response.put(FRIEND_REQUESTS_CACHE, validateFriendRequestsCache(user, clientCacheTimestamps.get(FRIEND_REQUESTS_CACHE)));
        }
        
        // Validate user tags cache
        if (clientCacheTimestamps.containsKey(USER_TAGS_CACHE)) {
            response.put(USER_TAGS_CACHE, validateUserTagsCache(user, clientCacheTimestamps.get(USER_TAGS_CACHE)));
        }
        
        // Validate tag friends cache
        if (clientCacheTimestamps.containsKey(TAG_FRIENDS_CACHE)) {
            response.put(TAG_FRIENDS_CACHE, validateTagFriendsCache(user, clientCacheTimestamps.get(TAG_FRIENDS_CACHE)));
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
     * Validates the user's own profile picture cache.
     */
    private CacheValidationResponseDTO validateProfilePictureCache(User user, String clientTimestamp) {
        try {
            // Parse the client timestamp
            ZonedDateTime clientTime = ZonedDateTime.parse(clientTimestamp, DateTimeFormatter.ISO_DATE_TIME);
            
            // Check if user's profile picture has been updated since the client's cache timestamp
            if (user.getLastUpdated() != null) {
                ZonedDateTime lastUpdated = ZonedDateTime.ofInstant(user.getLastUpdated(), clientTime.getZone());
                
                // If the user was updated after the client's cache timestamp, refresh the data
                boolean needsUpdate = lastUpdated.isAfter(clientTime);
                
                if (needsUpdate) {
                    try {
                        BaseUserDTO userProfile = userService.getBaseUserById(user.getId());
                        byte[] profileData = objectMapper.writeValueAsBytes(userProfile);
                        
                        if (profileData.length < 100_000) {
                            return new CacheValidationResponseDTO(true, profileData);
                        }
                    } catch (Exception e) {
                        logger.error("Failed to serialize profile picture data", e);
                    }
                    
                    return new CacheValidationResponseDTO(true, null);
                }
                
                return new CacheValidationResponseDTO(false, null);
            } else {
                // If lastUpdated is null (shouldn't happen with new entities but could with existing ones),
                // conservatively return the current profile data
                try {
                    BaseUserDTO userProfile = userService.getBaseUserById(user.getId());
                    byte[] profileData = objectMapper.writeValueAsBytes(userProfile);
                    
                    if (profileData.length < 100_000) {
                        return new CacheValidationResponseDTO(true, profileData);
                    }
                } catch (Exception e) {
                    logger.error("Failed to serialize profile picture data", e);
                }
                
                return new CacheValidationResponseDTO(true, null);
            }
        } catch (Exception e) {
            logger.error("Error validating profile picture cache for user {}: {}", user.getId(), e.getMessage());
            return new CacheValidationResponseDTO(true, null);
        }
    }
    
    /**
     * Validates the cache for other users' profiles that this user has viewed.
     */
    private CacheValidationResponseDTO validateOtherProfilesCache(User user, String clientTimestamp) {
        try {
            // Parse the client timestamp
            ZonedDateTime clientTime = ZonedDateTime.parse(clientTimestamp, DateTimeFormatter.ISO_DATE_TIME);
            
            // Get latest timestamp for any profile updates of user's friends
            Instant latestFriendProfileUpdate = getLatestFriendProfileUpdate(user.getId());
            
            boolean needsUpdate = latestFriendProfileUpdate.isAfter(clientTime.toInstant());
            
            // For other profiles, we'll tell the client to refresh as needed
            // but won't include the data since it could be large with many friends
            return new CacheValidationResponseDTO(needsUpdate, null);
            
        } catch (Exception e) {
            logger.error("Error validating other profiles cache for user {}: {}", user.getId(), e.getMessage());
            return new CacheValidationResponseDTO(true, null);
        }
    }
    
    /**
     * Validates the recommended friends cache.
     */
    private CacheValidationResponseDTO validateRecommendedFriendsCache(User user, String clientTimestamp) {
        try {
            // Parse the client timestamp
            ZonedDateTime clientTime = ZonedDateTime.parse(clientTimestamp, DateTimeFormatter.ISO_DATE_TIME);
            
            // Get the timestamp for latest friend activity (which affects recommendations)
            Instant latestActivity = getLatestFriendActivity(user.getId());
            
            boolean needsUpdate = latestActivity.isAfter(clientTime.toInstant());
            
            if (needsUpdate) {
                try {
                    List<RecommendedFriendUserDTO> recommendedFriends = 
                            userService.getLimitedRecommendedFriendsForUserId(user.getId());
                    byte[] recommendedData = objectMapper.writeValueAsBytes(recommendedFriends);
                    
                    if (recommendedData.length < 100_000) {
                        return new CacheValidationResponseDTO(true, recommendedData);
                    }
                } catch (Exception e) {
                    logger.error("Failed to serialize recommended friends data", e);
                }
                
                return new CacheValidationResponseDTO(true, null);
            }
            
            return new CacheValidationResponseDTO(false, null);
            
        } catch (Exception e) {
            logger.error("Error validating recommended friends cache for user {}: {}", user.getId(), e.getMessage());
            return new CacheValidationResponseDTO(true, null);
        }
    }
    
    /**
     * Validates the friend requests cache.
     */
    private CacheValidationResponseDTO validateFriendRequestsCache(User user, String clientTimestamp) {
        try {
            // Parse the client timestamp
            ZonedDateTime clientTime = ZonedDateTime.parse(clientTimestamp, DateTimeFormatter.ISO_DATE_TIME);
            
            // Friend requests should always be fresh
            // This data is critical for UX, so we'll typically invalidate with updated data
            
            try {
                List<FetchFriendRequestDTO> friendRequests = 
                        friendRequestService.getIncomingFetchFriendRequestsByUserId(user.getId());
                byte[] requestsData = objectMapper.writeValueAsBytes(friendRequests);
                
                if (requestsData.length < 100_000) {
                    return new CacheValidationResponseDTO(true, requestsData);
                }
            } catch (Exception e) {
                logger.error("Failed to serialize friend requests data", e);
            }
            
            return new CacheValidationResponseDTO(true, null);
            
        } catch (Exception e) {
            logger.error("Error validating friend requests cache for user {}: {}", user.getId(), e.getMessage());
            return new CacheValidationResponseDTO(true, null);
        }
    }
    
    /**
     * Validates the user's tags cache.
     */
    private CacheValidationResponseDTO validateUserTagsCache(User user, String clientTimestamp) {
        try {
            // Parse the client timestamp
            ZonedDateTime clientTime = ZonedDateTime.parse(clientTimestamp, DateTimeFormatter.ISO_DATE_TIME);
            
            // Get latest tag modifications
            Instant latestTagActivity = getLatestTagActivity(user.getId());
            
            boolean needsUpdate = latestTagActivity.isAfter(clientTime.toInstant());
            
            if (needsUpdate) {
                try {
                    List<FriendTagDTO> tags = friendTagService.getFriendTagsByOwnerId(user.getId());
                    byte[] tagsData = objectMapper.writeValueAsBytes(tags);
                    
                    if (tagsData.length < 100_000) {
                        return new CacheValidationResponseDTO(true, tagsData);
                    }
                } catch (Exception e) {
                    logger.error("Failed to serialize user tags data", e);
                }
                
                return new CacheValidationResponseDTO(true, null);
            }
            
            return new CacheValidationResponseDTO(false, null);
            
        } catch (Exception e) {
            logger.error("Error validating user tags cache for user {}: {}", user.getId(), e.getMessage());
            return new CacheValidationResponseDTO(true, null);
        }
    }
    
    /**
     * Validates the cache for friends inside tags.
     */
    private CacheValidationResponseDTO validateTagFriendsCache(User user, String clientTimestamp) {
        try {
            // Parse the client timestamp
            ZonedDateTime clientTime = ZonedDateTime.parse(clientTimestamp, DateTimeFormatter.ISO_DATE_TIME);
            
            // Get latest friend-tag associations
            Instant latestTagFriendActivity = getLatestTagFriendActivity(user.getId());
            
            boolean needsUpdate = latestTagFriendActivity.isAfter(clientTime.toInstant());
            
            // For tag friends, we'll tell the client to refresh as needed
            // but won't include the data since there could be many tags with many friends
            return new CacheValidationResponseDTO(needsUpdate, null);
            
        } catch (Exception e) {
            logger.error("Error validating tag friends cache for user {}: {}", user.getId(), e.getMessage());
            return new CacheValidationResponseDTO(true, null);
        }
    }
    
    /**
     * Helper method to evict relevant caches for the blocked user
     */
    private void evictBlockedUserCaches(UUID userId) {
        try {
            logger.info("Evicting caches for user ID: " + userId + " after being blocked/unblocked");
            
            // Evict from recommended friends cache
            Cache recommendedFriendsCache = cacheManager.getCache(RECOMMENDED_FRIENDS_CACHE);
            if (recommendedFriendsCache != null) {
                recommendedFriendsCache.evict(userId);
                logger.debug("Evicted user {} from recommendedFriends cache", userId);
            }
            
            // Evict from other profiles cache
            Cache otherProfilesCache = cacheManager.getCache(OTHER_PROFILES_CACHE);
            if (otherProfilesCache != null) {
                otherProfilesCache.evict(userId);
                logger.debug("Evicted user {} from otherProfiles cache", userId);
            }
            
            // Evict from friends list cache
            Cache friendsListCache = cacheManager.getCache(FRIENDS_CACHE);
            if (friendsListCache != null) {
                friendsListCache.evict(userId);
                logger.debug("Evicted user {} from friends cache", userId);
            }
            
            // Evict from user tags cache
            Cache userTagsCache = cacheManager.getCache(USER_TAGS_CACHE);
            if (userTagsCache != null) {
                userTagsCache.evict(userId);
                logger.debug("Evicted user {} from userTags cache", userId);
            }
            
            // Evict from tag friends cache
            Cache tagFriendsCache = cacheManager.getCache(TAG_FRIENDS_CACHE);
            if (tagFriendsCache != null) {
                tagFriendsCache.evict(userId);
                logger.debug("Evicted user {} from tagFriends cache", userId);
            }
            
            // Also evict the user from any events-related caches
            Cache eventsCache = cacheManager.getCache(EVENTS_CACHE);
            if (eventsCache != null) {
                eventsCache.evict(userId);
                logger.debug("Evicted user {} from events cache", userId);
            }
            
        } catch (Exception e) {
            logger.error("Error evicting caches for blocked user: " + e.getMessage(), e);
            // Don't throw here - this is a best-effort operation
        }
    }
    
    /**
     * Gets the timestamp of the latest friend-related activity for a user.
     * This includes friend requests, acceptances, and any profile updates of friends.
     */
    private Instant getLatestFriendActivity(UUID userId) {
        try {
            // Get the latest friend request involving this user
            Instant latestFriendRequest = friendRequestService.getLatestFriendRequestTimestamp(userId);
            
            // Get the latest profile update of any of the user's friends
            Instant latestFriendProfileUpdate = getLatestFriendProfileUpdate(userId);
            
            // Return the most recent of these timestamps
            if (latestFriendRequest != null && latestFriendProfileUpdate != null) {
                return latestFriendRequest.isAfter(latestFriendProfileUpdate) ? 
                       latestFriendRequest : latestFriendProfileUpdate;
            } else if (latestFriendRequest != null) {
                return latestFriendRequest;
            } else if (latestFriendProfileUpdate != null) {
                return latestFriendProfileUpdate;
            }
            
            // If no activity is found, return the current time to force a refresh
            logger.debug("No friend activity found for user {}, using current time", userId);
            return Instant.now();
        } catch (Exception e) {
            logger.error("Error getting latest friend activity for user {}: {}", userId, e.getMessage(), e);
            // In case of an error, return current time to force a refresh
            return Instant.now();
        }
    }
    
    /**
     * Gets the timestamp of the latest event-related activity relevant to a user.
     * This includes events the user created, is participating in, or was invited to.
     */
    private Instant getLatestEventActivity(UUID userId) {
        try {
            // Get the latest event created by the user
            Instant latestCreatedEvent = eventService.getLatestCreatedEventTimestamp(userId);
            
            // Get the latest event the user was invited to
            Instant latestInvitedEvent = eventService.getLatestInvitedEventTimestamp(userId);
            
            // Get the latest event the user is participating in that was updated
            Instant latestUpdatedEvent = eventService.getLatestUpdatedEventTimestamp(userId);
            
            // Find the most recent timestamp among these three
            Instant latestTimestamp = null;
            
            if (latestCreatedEvent != null) {
                latestTimestamp = latestCreatedEvent;
            }
            
            if (latestInvitedEvent != null && (latestTimestamp == null || latestInvitedEvent.isAfter(latestTimestamp))) {
                latestTimestamp = latestInvitedEvent;
            }
            
            if (latestUpdatedEvent != null && (latestTimestamp == null || latestUpdatedEvent.isAfter(latestTimestamp))) {
                latestTimestamp = latestUpdatedEvent;
            }
            
            if (latestTimestamp != null) {
                return latestTimestamp;
            }
            
            // If no activity is found, return the current time to force a refresh
            logger.debug("No event activity found for user {}, using current time", userId);
            return Instant.now();
        } catch (Exception e) {
            logger.error("Error getting latest event activity for user {}: {}", userId, e.getMessage(), e);
            // In case of an error, return current time to force a refresh
            return Instant.now();
        }
    }
    
    /**
     * Gets the timestamp of the latest profile update from any of the user's friends.
     */
    private Instant getLatestFriendProfileUpdate(UUID userId) {
        try {
            return userRepository.findLatestFriendProfileUpdate(userId);
        } catch (Exception e) {
            logger.error("Error fetching latest friend profile update for user {}: {}", userId, e.getMessage(), e);
            return Instant.now();
        }
    }
    
    /**
     * Gets the timestamp of the latest tag creation or modification by the user.
     */
    private Instant getLatestTagActivity(UUID userId) {
        try {
            return friendTagRepository.findLatestTagActivity(userId);
        } catch (Exception e) {
            logger.error("Error fetching latest tag activity for user {}: {}", userId, e.getMessage(), e);
            return Instant.now();
        }
    }
    
    /**
     * Gets the timestamp of the latest change in tag-friend associations.
     */
    private Instant getLatestTagFriendActivity(UUID userId) {
        try {
            return userFriendTagRepository.findLatestTagFriendActivity(userId);
        } catch (Exception e) {
            logger.error("Error fetching latest tag-friend activity for user {}: {}", userId, e.getMessage(), e);
            return Instant.now();
        }
    }
} 