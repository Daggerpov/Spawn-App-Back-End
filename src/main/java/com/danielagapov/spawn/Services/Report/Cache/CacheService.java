package com.danielagapov.spawn.Services.Report.Cache;

import com.danielagapov.spawn.DTOs.ActivityType.ActivityTypeDTO;
import com.danielagapov.spawn.DTOs.CacheValidationResponseDTO;
import com.danielagapov.spawn.Models.User.User;
import com.danielagapov.spawn.Repositories.User.IUserRepository;
import com.danielagapov.spawn.Services.Activity.IActivityService;
import com.danielagapov.spawn.Services.ActivityType.IActivityTypeService;
import com.danielagapov.spawn.Services.FriendRequest.IFriendRequestService;
import com.danielagapov.spawn.Services.User.IUserService;
import com.danielagapov.spawn.Services.UserInterest.IUserInterestService;
import com.danielagapov.spawn.Services.UserSocialMedia.IUserSocialMediaService;
import com.danielagapov.spawn.Services.UserStats.IUserStatsService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.core.serializer.support.SerializationFailedException;
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
 * <p>
 * IMPORTANT: Notifications are deliberately excluded from caching to ensure real-time delivery.
 * The service will always invalidate any notification cache to prevent stale notifications.
 */
@Service
public class CacheService implements ICacheService {
    private static final Logger logger = LoggerFactory.getLogger(CacheService.class);
    // Define cache categories
    private static final String FRIENDS_CACHE = "friends";
    private static final String EVENTS_CACHE = "events";
    private static final String ACTIVITY_TYPES_CACHE = "activityTypes";
    private static final String PROFILE_PICTURE_CACHE = "profilePicture";
    private static final String OTHER_PROFILES_CACHE = "otherProfiles";
    private static final String RECOMMENDED_FRIENDS_CACHE = "recommendedFriends";
    private static final String FRIEND_REQUESTS_CACHE = "friendRequests";
    private static final String SENT_FRIEND_REQUESTS_CACHE = "sentFriendRequests";
    private static final String RECENTLY_SPAWNED_CACHE = "recentlySpawned";
    private static final String PROFILE_STATS_CACHE = "profileStats";
    private static final String PROFILE_INTERESTS_CACHE = "profileInterests";
    private static final String PROFILE_SOCIAL_MEDIA_CACHE = "profileSocialMedia";
    private static final String PROFILE_EVENTS_CACHE = "profileEvents";
    private final IUserRepository userRepository;
    private final IUserService userService;
    private final IActivityService ActivityService;
    private final IActivityTypeService activityTypeService;
    private final IFriendRequestService friendRequestService;
    private final ObjectMapper objectMapper;
    private final IUserStatsService userStatsService;
    private final IUserInterestService userInterestService;
    private final IUserSocialMediaService userSocialMediaService;
    private final CacheManager cacheManager;

    @Autowired
    public CacheService(
            IUserRepository userRepository,
            IUserService userService,
            IActivityService ActivityService,
            IActivityTypeService activityTypeService,
            IFriendRequestService friendRequestService,
            ObjectMapper objectMapper,
            IUserStatsService userStatsService,
            IUserInterestService userInterestService,
            IUserSocialMediaService userSocialMediaService,
            CacheManager cacheManager) {
        this.userRepository = userRepository;
        this.userService = userService;
        this.ActivityService = ActivityService;
        this.activityTypeService = activityTypeService;
        this.friendRequestService = friendRequestService;
        this.objectMapper = objectMapper;
        this.userStatsService = userStatsService;
        this.userInterestService = userInterestService;
        this.userSocialMediaService = userSocialMediaService;
        this.cacheManager = cacheManager;
    }

    /**
     * Validates client cache against server data.
     * For each cache category in the request, determines if the client's cache is stale
     * and needs to be refreshed.
     * <p>
     * Note: Notifications are always invalidated to ensure they are never cached on mobile devices.
     * This guarantees users always receive the most up-to-date notifications.
     *
     * @param userId                The user ID requesting cache validation
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

        // Handle null clientCacheTimestamps
        if (clientCacheTimestamps == null) {
            logger.warn("Client cache timestamps is null for user: {}", userId);
            // Return response with all caches marked as needing refresh
            response.put(FRIENDS_CACHE, new CacheValidationResponseDTO(true, null));
            response.put(EVENTS_CACHE, new CacheValidationResponseDTO(true, null));
            response.put(ACTIVITY_TYPES_CACHE, new CacheValidationResponseDTO(true, null));
            response.put(PROFILE_PICTURE_CACHE, new CacheValidationResponseDTO(true, null));
            response.put(OTHER_PROFILES_CACHE, new CacheValidationResponseDTO(true, null));
            response.put(RECOMMENDED_FRIENDS_CACHE, new CacheValidationResponseDTO(true, null));
            response.put(FRIEND_REQUESTS_CACHE, new CacheValidationResponseDTO(true, null));
            response.put(SENT_FRIEND_REQUESTS_CACHE, new CacheValidationResponseDTO(true, null));
            response.put(RECENTLY_SPAWNED_CACHE, new CacheValidationResponseDTO(true, null));
            response.put(PROFILE_STATS_CACHE, new CacheValidationResponseDTO(true, null));
            response.put(PROFILE_INTERESTS_CACHE, new CacheValidationResponseDTO(true, null));
            response.put(PROFILE_SOCIAL_MEDIA_CACHE, new CacheValidationResponseDTO(true, null));
            response.put(PROFILE_EVENTS_CACHE, new CacheValidationResponseDTO(true, null));
            return response;
        }

        // Validate friends cache
        if (clientCacheTimestamps.containsKey(FRIENDS_CACHE)) {
            response.put(FRIENDS_CACHE, validateFriendsCache(user, clientCacheTimestamps.get(FRIENDS_CACHE)));
        }

        // Validate events cache
        if (clientCacheTimestamps.containsKey(EVENTS_CACHE)) {
            response.put(EVENTS_CACHE, validateEventsCache(user, clientCacheTimestamps.get(EVENTS_CACHE)));
        }

        // Validate activity types cache
        if (clientCacheTimestamps.containsKey(ACTIVITY_TYPES_CACHE)) {
            response.put(ACTIVITY_TYPES_CACHE, validateActivityTypesCache(user, clientCacheTimestamps.get(ACTIVITY_TYPES_CACHE)));
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

        // Validate sent friend requests cache
        if (clientCacheTimestamps.containsKey(SENT_FRIEND_REQUESTS_CACHE)) {
            response.put(SENT_FRIEND_REQUESTS_CACHE, validateSentFriendRequestsCache(user, clientCacheTimestamps.get(SENT_FRIEND_REQUESTS_CACHE)));
        }

        // Validate recently-spawned cache
        if (clientCacheTimestamps.containsKey(RECENTLY_SPAWNED_CACHE)) {
            response.put(RECENTLY_SPAWNED_CACHE, validateRecentlySpawnedCache(user, clientCacheTimestamps.get(RECENTLY_SPAWNED_CACHE)));
        }

        // Validate profile stats cache
        if (clientCacheTimestamps.containsKey(PROFILE_STATS_CACHE)) {
            response.put(PROFILE_STATS_CACHE, validateProfileStatsCache(user, clientCacheTimestamps.get(PROFILE_STATS_CACHE)));
        }

        // Validate profile interests cache
        if (clientCacheTimestamps.containsKey(PROFILE_INTERESTS_CACHE)) {
            response.put(PROFILE_INTERESTS_CACHE, validateProfileInterestsCache(user, clientCacheTimestamps.get(PROFILE_INTERESTS_CACHE)));
        }

        // Validate profile social media cache
        if (clientCacheTimestamps.containsKey(PROFILE_SOCIAL_MEDIA_CACHE)) {
            response.put(PROFILE_SOCIAL_MEDIA_CACHE, validateProfileSocialMediaCache(user, clientCacheTimestamps.get(PROFILE_SOCIAL_MEDIA_CACHE)));
        }

        // Validate profile events cache
        if (clientCacheTimestamps.containsKey(PROFILE_EVENTS_CACHE)) {
            response.put(PROFILE_EVENTS_CACHE, validateProfileEventsCache(user, clientCacheTimestamps.get(PROFILE_EVENTS_CACHE)));
        }

        return response;
    }

    // ========== Helper Methods for Cache Validation ==========

    /**
     * Generic cache validation method with timestamp comparison.
     * 
     * @param user The user requesting cache validation
     * @param clientTimestamp The client's cache timestamp
     * @param cacheType The type of cache being validated (for logging)
     * @param timestampSupplier Supplier that returns the latest server timestamp for this cache type
     * @param dataSupplier Supplier that returns the fresh data if cache needs update
     * @return Cache validation response indicating if refresh is needed and optionally including fresh data
     */
    private CacheValidationResponseDTO validateCacheWithTimestamp(
            User user,
            String clientTimestamp,
            String cacheType,
            TimestampSupplier timestampSupplier,
            DataSupplier dataSupplier) {
        try {
            ZonedDateTime clientTime = ZonedDateTime.parse(clientTimestamp, DateTimeFormatter.ISO_DATE_TIME);
            Instant latestServerTimestamp = timestampSupplier.get();
            boolean needsUpdate = latestServerTimestamp.isAfter(clientTime.toInstant());

            if (needsUpdate) {
                return serializeAndCreateResponse(user, cacheType, dataSupplier);
            }

            return new CacheValidationResponseDTO(false, null);
        } catch (Exception e) {
            logger.error("Error validating {} cache for user {}: {}", cacheType, user.getId(), e.getMessage());
            return new CacheValidationResponseDTO(true, null);
        }
    }

    /**
     * Cache validation method with nullable timestamp support.
     * If the server timestamp is null, it means no data exists, so the cache is valid (no update needed).
     * 
     * @param user The user requesting cache validation
     * @param clientTimestamp The client's cache timestamp
     * @param cacheType The type of cache being validated (for logging)
     * @param timestampSupplier Supplier that returns the latest server timestamp (can be null)
     * @param dataSupplier Supplier that returns the fresh data if cache needs update
     * @return Cache validation response indicating if refresh is needed and optionally including fresh data
     */
    private CacheValidationResponseDTO validateCacheWithNullableTimestamp(
            User user,
            String clientTimestamp,
            String cacheType,
            TimestampSupplier timestampSupplier,
            DataSupplier dataSupplier) {
        try {
            ZonedDateTime clientTime = ZonedDateTime.parse(clientTimestamp, DateTimeFormatter.ISO_DATE_TIME);
            Instant latestServerTimestamp = timestampSupplier.get();

            // If there's no server data, client cache should be empty (valid)
            if (latestServerTimestamp == null) {
                return new CacheValidationResponseDTO(false, null);
            }

            boolean needsUpdate = latestServerTimestamp.isAfter(clientTime.toInstant());

            if (needsUpdate) {
                return serializeAndCreateResponse(user, cacheType, dataSupplier);
            }

            return new CacheValidationResponseDTO(false, null);
        } catch (Exception e) {
            logger.error("Error validating {} cache for user {}: {}", cacheType, user.getId(), e.getMessage());
            return new CacheValidationResponseDTO(true, null);
        }
    }

    /**
     * Cache validation method using user's lastUpdated timestamp.
     * 
     * @param user The user requesting cache validation
     * @param clientTimestamp The client's cache timestamp
     * @param cacheType The type of cache being validated (for logging)
     * @param dataSupplier Supplier that returns the fresh data if cache needs update
     * @return Cache validation response indicating if refresh is needed and optionally including fresh data
     */
    private CacheValidationResponseDTO validateCacheWithUserLastUpdated(
            User user,
            String clientTimestamp,
            String cacheType,
            DataSupplier dataSupplier) {
        try {
            ZonedDateTime clientTime = ZonedDateTime.parse(clientTimestamp, DateTimeFormatter.ISO_DATE_TIME);

            if (user.getLastUpdated() != null) {
                ZonedDateTime lastUpdated = ZonedDateTime.ofInstant(user.getLastUpdated(), clientTime.getZone());
                boolean needsUpdate = lastUpdated.isAfter(clientTime);

                if (needsUpdate) {
                    return serializeAndCreateResponse(user, cacheType, dataSupplier);
                }
                return new CacheValidationResponseDTO(false, null);
            }
            
            // If lastUpdated is null, conservatively return fresh data
            return serializeAndCreateResponse(user, cacheType, dataSupplier);
        } catch (Exception e) {
            logger.error("Error validating {} cache for user {}: {}", cacheType, user.getId(), e.getMessage());
            return new CacheValidationResponseDTO(true, null);
        }
    }

    /**
     * Serializes data and creates a cache validation response.
     * If data is small enough (<100KB), includes it in the response.
     * 
     * @param user The user requesting cache validation
     * @param cacheType The type of cache being validated (for logging)
     * @param dataSupplier Supplier that returns the fresh data
     * @return Cache validation response with needsUpdate=true and optionally the serialized data
     */
    private CacheValidationResponseDTO serializeAndCreateResponse(
            User user,
            String cacheType,
            DataSupplier dataSupplier) {
        try {
            Object data = dataSupplier.get();
            byte[] serializedData = objectMapper.writeValueAsBytes(data);

            // Only include data if it's not too large (limit to ~100KB)
            if (serializedData.length < 100_000) {
                return new CacheValidationResponseDTO(true, serializedData);
            }
        } catch (Exception e) {
            logger.error("Failed to serialize {} data for user {}", cacheType, user.getId(), e);
        }

        // If serialization failed or data too large, just tell client to refresh
        return new CacheValidationResponseDTO(true, null);
    }

    // Functional interfaces for suppliers
    @FunctionalInterface
    private interface TimestampSupplier {
        Instant get();
    }

    @FunctionalInterface
    private interface DataSupplier {
        Object get();
    }

    // ========== Individual Cache Validation Methods ==========

    /**
     * Validates the user's friends cache by checking if any friends have been added, removed,
     * or updated since the client's last cache timestamp.
     */
    private CacheValidationResponseDTO validateFriendsCache(User user, String clientTimestamp) {
        return validateCacheWithTimestamp(
                user,
                clientTimestamp,
                "friends",
                () -> getLatestFriendActivity(user.getId()),
                () -> userService.getFullFriendUsersByUserId(user.getId())
        );
    }

    /**
     * Validates the user's events cache by checking if any relevant activities have been
     * created, updated, or deleted since the client's last cache timestamp.
     */
    private CacheValidationResponseDTO validateEventsCache(User user, String clientTimestamp) {
        return validateCacheWithTimestamp(
                user,
                clientTimestamp,
                "activities",
                () -> getLatestActivityActivity(user.getId()),
                () -> ActivityService.getFeedActivities(user.getId())
        );
    }

    /**
     * Validates the user's activity types cache by checking if any activity types have been
     * created, updated, or deleted since the client's last cache timestamp.
     */
    private CacheValidationResponseDTO validateActivityTypesCache(User user, String clientTimestamp) {
        return validateCacheWithTimestamp(
                user,
                clientTimestamp,
                "activity types",
                () -> getLatestActivityTypeUpdate(user.getId()),
                () -> activityTypeService.getActivityTypesByUserId(user.getId())
        );
    }

    /**
     * Validates the user's own profile picture cache.
     */
    private CacheValidationResponseDTO validateProfilePictureCache(User user, String clientTimestamp) {
        return validateCacheWithUserLastUpdated(
                user,
                clientTimestamp,
                "profile picture",
                () -> user.getProfilePictureUrlString()
        );
    }

    /**
     * Validates the cache for other users' profiles that this user has viewed.
     */
    private CacheValidationResponseDTO validateOtherProfilesCache(User user, String clientTimestamp) {
        return validateCacheWithTimestamp(
                user,
                clientTimestamp,
                "other profiles",
                () -> getLatestFriendProfileUpdate(user.getId()),
                () -> {
                    // Get friend user IDs
                    List<UUID> friendIds = userService.getFriendUserIdsByUserId(user.getId());

                    if (friendIds != null && !friendIds.isEmpty()) {
                        // Create a map of user IDs to profile picture URLs
                        Map<UUID, String> friendProfilePictures = new HashMap<>();
                        for (UUID friendId : friendIds) {
                            User friend = userRepository.findById(friendId).orElse(null);
                            if (friend != null) {
                                friendProfilePictures.put(friendId, friend.getProfilePictureUrlString());
                            }
                        }
                        return friendProfilePictures;
                    }
                    return new HashMap<UUID, String>();
                }
        );
    }

    /**
     * Validates the recommended friends cache.
     */
    private CacheValidationResponseDTO validateRecommendedFriendsCache(User user, String clientTimestamp) {
        return validateCacheWithTimestamp(
                user,
                clientTimestamp,
                "recommended friends",
                () -> getLatestFriendActivity(user.getId()),
                () -> userService.getLimitedRecommendedFriendsForUserId(user.getId())
        );
    }

    /**
     * Validates the friend requests cache.
     */
    private CacheValidationResponseDTO validateFriendRequestsCache(User user, String clientTimestamp) {
        return validateCacheWithNullableTimestamp(
                user,
                clientTimestamp,
                "friend requests",
                () -> friendRequestService.getLatestFriendRequestTimestamp(user.getId()),
                () -> friendRequestService.getIncomingFetchFriendRequestsByUserId(user.getId())
        );
    }

    /**
     * Validates the sent friend requests cache.
     */
    private CacheValidationResponseDTO validateSentFriendRequestsCache(User user, String clientTimestamp) {
        return validateCacheWithNullableTimestamp(
                user,
                clientTimestamp,
                "sent friend requests",
                () -> friendRequestService.getLatestFriendRequestTimestamp(user.getId()),
                () -> friendRequestService.getSentFetchFriendRequestsByUserId(user.getId())
        );
    }

    /**
     * Validates the user's profile stats cache.
     */
    private CacheValidationResponseDTO validateProfileStatsCache(User user, String clientTimestamp) {
        return validateCacheWithUserLastUpdated(
                user,
                clientTimestamp,
                "profile stats",
                () -> userStatsService.getUserStats(user.getId())
        );
    }

    /**
     * Validates the user's profile interests cache.
     */
    private CacheValidationResponseDTO validateProfileInterestsCache(User user, String clientTimestamp) {
        return validateCacheWithUserLastUpdated(
                user,
                clientTimestamp,
                "profile interests",
                () -> userInterestService.getUserInterests(user.getId())
        );
    }

    /**
     * Validates the user's profile social media cache.
     */
    private CacheValidationResponseDTO validateProfileSocialMediaCache(User user, String clientTimestamp) {
        return validateCacheWithUserLastUpdated(
                user,
                clientTimestamp,
                "profile social media",
                () -> userSocialMediaService.getUserSocialMedia(user.getId())
        );
    }

    /**
     * Validates the user's profile events cache.
     */
    private CacheValidationResponseDTO validateProfileEventsCache(User user, String clientTimestamp) {
        return validateCacheWithTimestamp(
                user,
                clientTimestamp,
                "profile activities",
                () -> getLatestActivityActivity(user.getId()),
                () -> ActivityService.getProfileActivities(user.getId(), user.getId())
        );
    }

    private CacheValidationResponseDTO validateRecentlySpawnedCache(User user, String clientTimestamp) {
        return validateCacheWithTimestamp(
                user,
                clientTimestamp,
                "recently spawned",
                () -> getLatestFriendActivity(user.getId()),
                () -> userService.getRecentlySpawnedWithUsers(user.getId())
        );
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
     * Gets the timestamp of the latest activity-related activity relevant to a user.
     * This includes activities the user created, is participating in, or was invited to.
     */
    private Instant getLatestActivityActivity(UUID userId) {
        try {
            // Get the latest activity created by the user
            Instant latestCreatedActivity = ActivityService.getLatestCreatedActivityTimestamp(userId);

            // Get the latest activity the user was invited to
            Instant latestInvitedActivity = ActivityService.getLatestInvitedActivityTimestamp(userId);

            // Get the latest activity the user is participating in that was updated
            Instant latestUpdatedActivity = ActivityService.getLatestUpdatedActivityTimestamp(userId);

            // Find the most recent timestamp among these three
            Instant latestTimestamp = null;

            if (latestCreatedActivity != null) {
                latestTimestamp = latestCreatedActivity;
            }

            if (latestInvitedActivity != null && (latestTimestamp == null || latestInvitedActivity.isAfter(latestTimestamp))) {
                latestTimestamp = latestInvitedActivity;
            }

            if (latestUpdatedActivity != null && (latestTimestamp == null || latestUpdatedActivity.isAfter(latestTimestamp))) {
                latestTimestamp = latestUpdatedActivity;
            }

            if (latestTimestamp != null) {
                return latestTimestamp;
            }

            // If no activity is found, return the current time to force a refresh
            logger.debug("No activity activity found for user {}, using current time", userId);
            return Instant.now();
        } catch (SerializationFailedException e) {
            logger.error("Serialization error getting latest activity activity for user {}: {}. Clearing related caches.", userId, e.getMessage());
            clearActivityRelatedCaches(userId);
            // Return current time to force a refresh after cache clearing
            return Instant.now();
        } catch (Exception e) {
            logger.error("Error getting latest activity activity for user {}: {}", userId, e.getMessage(), e);
            // In case of an error, return current time to force a refresh
            return Instant.now();
        }
    }

    /**
     * Clears activity-related caches for a specific user when serialization issues occur.
     */
    private void clearActivityRelatedCaches(UUID userId) {
        try {
            logger.info("Clearing activity-related caches for user: {}", userId);
            
            // Clear activity caches
            if (cacheManager.getCache("feedActivities") != null) {
                cacheManager.getCache("feedActivities").evict(userId);
            }
            
            if (cacheManager.getCache("fullActivityById") != null) {
                cacheManager.getCache("fullActivityById").clear(); // Clear all entries as keys contain user IDs
            }
            
            if (cacheManager.getCache("ActivitiesByOwnerId") != null) {
                cacheManager.getCache("ActivitiesByOwnerId").evict(userId);
            }
            
            // filteredFeedActivities cache removed in friendship refactor
            
            if (cacheManager.getCache("ActivitiesInvitedTo") != null) {
                cacheManager.getCache("ActivitiesInvitedTo").evict(userId);
            }
            
            if (cacheManager.getCache("fullActivitiesInvitedTo") != null) {
                cacheManager.getCache("fullActivitiesInvitedTo").evict(userId);
            }
            
            logger.info("Successfully cleared activity-related caches for user: {}", userId);
        } catch (Exception e) {
            logger.error("Error clearing activity-related caches for user {}: {}", userId, e.getMessage());
            // Don't throw here - this is a best-effort operation
        }
    }

    /**
     * Gets the timestamp of the latest profile update from any of the user's friends.
     */
    private Instant getLatestFriendProfileUpdate(UUID userId) {
        try {
            Instant result = userRepository.findLatestFriendProfileUpdate(userId);
            // Handle case where repository returns null (no friends or no updates)
            if (result == null) {
                logger.debug("No friend profile updates found for user {}, returning user creation time", userId);
                // Get the user's creation time as fallback
                return userRepository.findById(userId)
                        .map(user -> user.getDateCreated().toInstant())
                        .orElse(Instant.now()); // Fallback to current time if user not found
            }
            return result;
        } catch (Exception e) {
            logger.error("Error fetching latest friend profile update for user {}: {}", userId, e.getMessage(), e);
            return Instant.now();
        }
    }

    /**
     * Gets the latest activity type update timestamp for a user.
     * This checks when any activity type was last created, updated, or deleted.
     */
    private Instant getLatestActivityTypeUpdate(UUID userId) {
        try {
            // Get all activity types for the user
            List<ActivityTypeDTO> activityTypes = activityTypeService.getActivityTypesByUserId(userId);
            
            if (activityTypes == null || activityTypes.isEmpty()) {
                // If no activity types, return epoch so cache is always valid
                return Instant.EPOCH;
            }

            // For now, we'll assume any activity type operation requires a refresh
            // In a more sophisticated implementation, you might track lastModified timestamps
            // on ActivityType entities and check those here
            
            // As a simple implementation, we'll return the current time minus 1 hour
            // This means activity types cache will be refreshed at most once per hour
            return Instant.now().minusSeconds(3600);
            
        } catch (Exception e) {
            logger.error("Error getting latest activity type update for user {}: {}", userId, e.getMessage());
            // On error, return current time to force refresh
            return Instant.now();
        }
    }
} 