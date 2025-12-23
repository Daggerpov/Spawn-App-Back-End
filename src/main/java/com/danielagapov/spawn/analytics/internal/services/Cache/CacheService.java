package com.danielagapov.spawn.analytics.internal.services.Cache;

import com.danielagapov.spawn.activity.api.dto.ActivityTypeDTO;
import com.danielagapov.spawn.shared.config.CacheValidationResponseDTO;
import com.danielagapov.spawn.user.internal.domain.User;
import com.danielagapov.spawn.user.internal.repositories.IUserRepository;
import com.danielagapov.spawn.activity.internal.services.IActivityService;
import com.danielagapov.spawn.activity.internal.services.IActivityTypeService;
import com.danielagapov.spawn.social.internal.services.IFriendRequestService;
import com.danielagapov.spawn.user.internal.services.IUserService;
import com.danielagapov.spawn.user.internal.services.IUserInterestService;
import com.danielagapov.spawn.user.internal.services.IUserSocialMediaService;
import com.danielagapov.spawn.user.internal.services.IUserStatsService;
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
            response.put(CacheType.FRIENDS.getKey(), new CacheValidationResponseDTO(true, null));
            response.put(CacheType.EVENTS.getKey(), new CacheValidationResponseDTO(true, null));
            response.put(CacheType.ACTIVITY_TYPES.getKey(), new CacheValidationResponseDTO(true, null));
            response.put(CacheType.PROFILE_PICTURE.getKey(), new CacheValidationResponseDTO(true, null));
            response.put(CacheType.OTHER_PROFILES.getKey(), new CacheValidationResponseDTO(true, null));
            response.put(CacheType.RECOMMENDED_FRIENDS.getKey(), new CacheValidationResponseDTO(true, null));
            response.put(CacheType.FRIEND_REQUESTS.getKey(), new CacheValidationResponseDTO(true, null));
            response.put(CacheType.SENT_FRIEND_REQUESTS.getKey(), new CacheValidationResponseDTO(true, null));
            response.put(CacheType.RECENTLY_SPAWNED.getKey(), new CacheValidationResponseDTO(true, null));
            response.put(CacheType.PROFILE_STATS.getKey(), new CacheValidationResponseDTO(true, null));
            response.put(CacheType.PROFILE_INTERESTS.getKey(), new CacheValidationResponseDTO(true, null));
            response.put(CacheType.PROFILE_SOCIAL_MEDIA.getKey(), new CacheValidationResponseDTO(true, null));
            response.put(CacheType.PROFILE_EVENTS.getKey(), new CacheValidationResponseDTO(true, null));
            return response;
        }

        // Validate friends cache
        if (clientCacheTimestamps.containsKey(CacheType.FRIENDS.getKey())) {
            response.put(CacheType.FRIENDS.getKey(), validateFriendsCache(user, clientCacheTimestamps.get(CacheType.FRIENDS.getKey())));
        }

        // Validate events cache
        if (clientCacheTimestamps.containsKey(CacheType.EVENTS.getKey())) {
            response.put(CacheType.EVENTS.getKey(), validateEventsCache(user, clientCacheTimestamps.get(CacheType.EVENTS.getKey())));
        }

        // Validate activity types cache
        if (clientCacheTimestamps.containsKey(CacheType.ACTIVITY_TYPES.getKey())) {
            response.put(CacheType.ACTIVITY_TYPES.getKey(), validateActivityTypesCache(user, clientCacheTimestamps.get(CacheType.ACTIVITY_TYPES.getKey())));
        }

        // Validate profile picture cache
        if (clientCacheTimestamps.containsKey(CacheType.PROFILE_PICTURE.getKey())) {
            response.put(CacheType.PROFILE_PICTURE.getKey(), validateProfilePictureCache(user, clientCacheTimestamps.get(CacheType.PROFILE_PICTURE.getKey())));
        }

        // Validate other profiles cache
        if (clientCacheTimestamps.containsKey(CacheType.OTHER_PROFILES.getKey())) {
            response.put(CacheType.OTHER_PROFILES.getKey(), validateOtherProfilesCache(user, clientCacheTimestamps.get(CacheType.OTHER_PROFILES.getKey())));
        }

        // Validate recommended friends cache
        if (clientCacheTimestamps.containsKey(CacheType.RECOMMENDED_FRIENDS.getKey())) {
            response.put(CacheType.RECOMMENDED_FRIENDS.getKey(), validateRecommendedFriendsCache(user, clientCacheTimestamps.get(CacheType.RECOMMENDED_FRIENDS.getKey())));
        }

        // Validate friend requests cache
        if (clientCacheTimestamps.containsKey(CacheType.FRIEND_REQUESTS.getKey())) {
            response.put(CacheType.FRIEND_REQUESTS.getKey(), validateFriendRequestsCache(user, clientCacheTimestamps.get(CacheType.FRIEND_REQUESTS.getKey())));
        }

        // Validate sent friend requests cache
        if (clientCacheTimestamps.containsKey(CacheType.SENT_FRIEND_REQUESTS.getKey())) {
            response.put(CacheType.SENT_FRIEND_REQUESTS.getKey(), validateSentFriendRequestsCache(user, clientCacheTimestamps.get(CacheType.SENT_FRIEND_REQUESTS.getKey())));
        }

        // Validate recently-spawned cache
        if (clientCacheTimestamps.containsKey(CacheType.RECENTLY_SPAWNED.getKey())) {
            response.put(CacheType.RECENTLY_SPAWNED.getKey(), validateRecentlySpawnedCache(user, clientCacheTimestamps.get(CacheType.RECENTLY_SPAWNED.getKey())));
        }

        // Validate profile stats cache
        if (clientCacheTimestamps.containsKey(CacheType.PROFILE_STATS.getKey())) {
            response.put(CacheType.PROFILE_STATS.getKey(), validateProfileStatsCache(user, clientCacheTimestamps.get(CacheType.PROFILE_STATS.getKey())));
        }

        // Validate profile interests cache
        if (clientCacheTimestamps.containsKey(CacheType.PROFILE_INTERESTS.getKey())) {
            response.put(CacheType.PROFILE_INTERESTS.getKey(), validateProfileInterestsCache(user, clientCacheTimestamps.get(CacheType.PROFILE_INTERESTS.getKey())));
        }

        // Validate profile social media cache
        if (clientCacheTimestamps.containsKey(CacheType.PROFILE_SOCIAL_MEDIA.getKey())) {
            response.put(CacheType.PROFILE_SOCIAL_MEDIA.getKey(), validateProfileSocialMediaCache(user, clientCacheTimestamps.get(CacheType.PROFILE_SOCIAL_MEDIA.getKey())));
        }

        // Validate profile events cache
        if (clientCacheTimestamps.containsKey(CacheType.PROFILE_EVENTS.getKey())) {
            response.put(CacheType.PROFILE_EVENTS.getKey(), validateProfileEventsCache(user, clientCacheTimestamps.get(CacheType.PROFILE_EVENTS.getKey())));
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
            CacheType cacheType,
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
            logger.error("Error validating {} cache for user {}: {}", cacheType.getDisplayName(), user.getId(), e.getMessage());
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
            CacheType cacheType,
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
            logger.error("Error validating {} cache for user {}: {}", cacheType.getDisplayName(), user.getId(), e.getMessage());
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
            CacheType cacheType,
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
            logger.error("Error validating {} cache for user {}: {}", cacheType.getDisplayName(), user.getId(), e.getMessage());
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
            CacheType cacheType,
            DataSupplier dataSupplier) {
        try {
            Object data = dataSupplier.get();
            byte[] serializedData = objectMapper.writeValueAsBytes(data);

            // Only include data if it's not too large (limit to ~100KB)
            if (serializedData.length < 100_000) {
                return new CacheValidationResponseDTO(true, serializedData);
            }
        } catch (Exception e) {
            logger.error("Failed to serialize {} data for user {}", cacheType.getDisplayName(), user.getId(), e);
        }

        // If serialization failed or data too large, just tell client to refresh
        return new CacheValidationResponseDTO(true, null);
    }

    /**
     * Validates the user's friends cache by checking if any friends have been added, removed,
     * or updated since the client's last cache timestamp.
     */
    private CacheValidationResponseDTO validateFriendsCache(User user, String clientTimestamp) {
        return validateCacheWithTimestamp(
                user,
                clientTimestamp,
                CacheType.FRIENDS,
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
                CacheType.EVENTS,
                () -> getLatestActivityActivity(user.getId()),
                () -> ActivityService.getFeedActivities(user.getId())
        );
    }

    // ========== Individual Cache Validation Methods ==========

    /**
     * Validates the user's activity types cache by checking if any activity types have been
     * created, updated, or deleted since the client's last cache timestamp.
     */
    private CacheValidationResponseDTO validateActivityTypesCache(User user, String clientTimestamp) {
        return validateCacheWithTimestamp(
                user,
                clientTimestamp,
                CacheType.ACTIVITY_TYPES,
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
                CacheType.PROFILE_PICTURE,
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
                CacheType.OTHER_PROFILES,
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
                CacheType.RECOMMENDED_FRIENDS,
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
                CacheType.FRIEND_REQUESTS,
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
                CacheType.SENT_FRIEND_REQUESTS,
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
                CacheType.PROFILE_STATS,
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
                CacheType.PROFILE_INTERESTS,
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
                CacheType.PROFILE_SOCIAL_MEDIA,
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
                CacheType.PROFILE_EVENTS,
                () -> getLatestActivityActivity(user.getId()),
                () -> ActivityService.getProfileActivities(user.getId(), user.getId())
        );
    }

    private CacheValidationResponseDTO validateRecentlySpawnedCache(User user, String clientTimestamp) {
        return validateCacheWithTimestamp(
                user,
                clientTimestamp,
                CacheType.RECENTLY_SPAWNED,
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

    // Functional interfaces for suppliers
    @FunctionalInterface
    private interface TimestampSupplier {
        Instant get();
    }

    @FunctionalInterface
    private interface DataSupplier {
        Object get();
    }
} 