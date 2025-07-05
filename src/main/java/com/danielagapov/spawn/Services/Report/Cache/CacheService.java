package com.danielagapov.spawn.Services.Report.Cache;

import com.danielagapov.spawn.DTOs.CacheValidationResponseDTO;
import com.danielagapov.spawn.DTOs.Activity.FullFeedActivityDTO;
import com.danielagapov.spawn.DTOs.Activity.ProfileActivityDTO;
import com.danielagapov.spawn.DTOs.ActivityType.ActivityTypeDTO;
import com.danielagapov.spawn.DTOs.FriendRequest.FetchFriendRequestDTO;
import com.danielagapov.spawn.DTOs.User.FriendUser.FullFriendUserDTO;
import com.danielagapov.spawn.DTOs.User.FriendUser.RecommendedFriendUserDTO;
import com.danielagapov.spawn.DTOs.User.Profile.UserStatsDTO;
import com.danielagapov.spawn.DTOs.User.Profile.UserSocialMediaDTO;
import com.danielagapov.spawn.DTOs.User.RecentlySpawnedUserDTO;
import com.danielagapov.spawn.Models.User.User;
import com.danielagapov.spawn.Repositories.User.IUserRepository;
import com.danielagapov.spawn.Services.Activity.IActivityService;
import com.danielagapov.spawn.Services.ActivityType.IActivityTypeService;
import com.danielagapov.spawn.Services.FriendRequest.IFriendRequestService;
import com.danielagapov.spawn.Services.User.IUserService;
import com.danielagapov.spawn.Services.UserStats.IUserStatsService;
import com.danielagapov.spawn.Services.UserInterest.IUserInterestService;
import com.danielagapov.spawn.Services.UserSocialMedia.IUserSocialMediaService;
import com.danielagapov.spawn.Util.LoggingUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
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
            IUserSocialMediaService userSocialMediaService) {
        this.userRepository = userRepository;
        this.userService = userService;
        this.ActivityService = ActivityService;
        this.activityTypeService = activityTypeService;
        this.friendRequestService = friendRequestService;
        this.objectMapper = objectMapper;
        this.userStatsService = userStatsService;
        this.userInterestService = userInterestService;
        this.userSocialMediaService = userSocialMediaService;
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

        logger.info("Validating cache for user: " + LoggingUtils.formatUserInfo(user));

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
     * Validates the user's events cache by checking if any relevant activities have been
     * created, updated, or deleted since the client's last cache timestamp.
     */
    private CacheValidationResponseDTO validateEventsCache(User user, String clientTimestamp) {
        try {
            // Parse the client timestamp
            ZonedDateTime clientTime = ZonedDateTime.parse(clientTimestamp, DateTimeFormatter.ISO_DATE_TIME);

            // Get the latest activity activity timestamp for this user
            Instant latestActivityActivity = getLatestActivityActivity(user.getId());

            // If client cache is older than the latest activity, invalidate
            boolean needsUpdate = latestActivityActivity.isAfter(clientTime.toInstant());

            if (needsUpdate) {
                // For events, we'll use feed activities which combines owned and invited activities
                try {
                    // Get current activities for the user
                    List<FullFeedActivityDTO> feedActivities = ActivityService.getFeedActivities(user.getId());
                    byte[] activitiesData = objectMapper.writeValueAsBytes(feedActivities);

                    // Only include the data if it's not too large (limit to ~100KB)
                    if (activitiesData.length < 100_000) {
                        return new CacheValidationResponseDTO(true, activitiesData);
                    }
                } catch (Exception e) {
                    logger.error("Failed to serialize activities data", e);
                }

                // If we couldn't include the data, just tell the client to refresh
                return new CacheValidationResponseDTO(true, null);
            }

            // Client cache is still valid
            return new CacheValidationResponseDTO(false, null);

        } catch (Exception e) {
            logger.error("Error validating activities cache for user {}: {}", user.getId(), e.getMessage());
            // On error, tell client to refresh to be safe
            return new CacheValidationResponseDTO(true, null);
        }
    }

    /**
     * Validates the user's activity types cache by checking if any activity types have been
     * created, updated, or deleted since the client's last cache timestamp.
     */
    private CacheValidationResponseDTO validateActivityTypesCache(User user, String clientTimestamp) {
        try {
            // Parse the client timestamp
            ZonedDateTime clientTime = ZonedDateTime.parse(clientTimestamp, DateTimeFormatter.ISO_DATE_TIME);

            // Get the latest activity type update timestamp for this user
            Instant latestActivityTypeUpdate = getLatestActivityTypeUpdate(user.getId());

            // If client cache is older than the latest activity type update, invalidate
            boolean needsUpdate = latestActivityTypeUpdate.isAfter(clientTime.toInstant());

            if (needsUpdate) {
                try {
                    // Get current activity types for the user
                    List<ActivityTypeDTO> activityTypes = activityTypeService.getActivityTypesByUserId(user.getId());
                    byte[] activityTypesData = objectMapper.writeValueAsBytes(activityTypes);

                    // Only include the data if it's not too large (limit to ~100KB)
                    if (activityTypesData.length < 100_000) {
                        return new CacheValidationResponseDTO(true, activityTypesData);
                    }
                } catch (Exception e) {
                    logger.error("Failed to serialize activity types data", e);
                }

                // If we couldn't include the data, just tell the client to refresh
                return new CacheValidationResponseDTO(true, null);
            }

            // Client cache is still valid
            return new CacheValidationResponseDTO(false, null);

        } catch (Exception e) {
            logger.error("Error validating activity types cache for user {}: {}", user.getId(), e.getMessage());
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
                        // Just cache the profile picture data instead of the entire BaseUserDTO
                        String profilePicture = user.getProfilePictureUrlString();
                        byte[] profileData = objectMapper.writeValueAsBytes(profilePicture);

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
                    // Just cache the profile picture data instead of the entire BaseUserDTO
                    String profilePicture = user.getProfilePictureUrlString();
                    byte[] profileData = objectMapper.writeValueAsBytes(profilePicture);

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

            if (needsUpdate) {
                try {
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

                        byte[] profilePicturesData = objectMapper.writeValueAsBytes(friendProfilePictures);

                        if (profilePicturesData.length < 100_000) {
                            return new CacheValidationResponseDTO(true, profilePicturesData);
                        }
                    }
                } catch (Exception e) {
                    logger.error("Failed to serialize other profile pictures data", e);
                }
            }

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
     * Validates the user's profile stats cache.
     */
    private CacheValidationResponseDTO validateProfileStatsCache(User user, String clientTimestamp) {
        try {
            ZonedDateTime clientTime = ZonedDateTime.parse(clientTimestamp, DateTimeFormatter.ISO_DATE_TIME);

            // Check if user's stats have been updated since the client's cache timestamp
            if (user.getLastUpdated() != null) {
                ZonedDateTime lastUpdated = ZonedDateTime.ofInstant(user.getLastUpdated(), clientTime.getZone());
                boolean needsUpdate = lastUpdated.isAfter(clientTime);

                if (needsUpdate) {
                    try {
                        UserStatsDTO stats = userStatsService.getUserStats(user.getId());
                        byte[] statsData = objectMapper.writeValueAsBytes(stats);

                        if (statsData.length < 100_000) {
                            return new CacheValidationResponseDTO(true, statsData);
                        }
                    } catch (Exception e) {
                        logger.error("Failed to serialize profile stats data", e);
                    }
                    return new CacheValidationResponseDTO(true, null);
                }
                return new CacheValidationResponseDTO(false, null);
            }
            return new CacheValidationResponseDTO(true, null);
        } catch (Exception e) {
            logger.error("Error validating profile stats cache for user {}: {}", user.getId(), e.getMessage());
            return new CacheValidationResponseDTO(true, null);
        }
    }

    /**
     * Validates the user's profile interests cache.
     */
    private CacheValidationResponseDTO validateProfileInterestsCache(User user, String clientTimestamp) {
        try {
            ZonedDateTime clientTime = ZonedDateTime.parse(clientTimestamp, DateTimeFormatter.ISO_DATE_TIME);

            // Check if user's interests have been updated since the client's cache timestamp
            if (user.getLastUpdated() != null) {
                ZonedDateTime lastUpdated = ZonedDateTime.ofInstant(user.getLastUpdated(), clientTime.getZone());
                boolean needsUpdate = lastUpdated.isAfter(clientTime);

                if (needsUpdate) {
                    try {
                        List<String> interests = userInterestService.getUserInterests(user.getId());
                        byte[] interestsData = objectMapper.writeValueAsBytes(interests);

                        if (interestsData.length < 100_000) {
                            return new CacheValidationResponseDTO(true, interestsData);
                        }
                    } catch (Exception e) {
                        logger.error("Failed to serialize profile interests data", e);
                    }
                    return new CacheValidationResponseDTO(true, null);
                }
                return new CacheValidationResponseDTO(false, null);
            }
            return new CacheValidationResponseDTO(true, null);
        } catch (Exception e) {
            logger.error("Error validating profile interests cache for user {}: {}", user.getId(), e.getMessage());
            return new CacheValidationResponseDTO(true, null);
        }
    }

    /**
     * Validates the user's profile social media cache.
     */
    private CacheValidationResponseDTO validateProfileSocialMediaCache(User user, String clientTimestamp) {
        try {
            ZonedDateTime clientTime = ZonedDateTime.parse(clientTimestamp, DateTimeFormatter.ISO_DATE_TIME);

            // Check if user's social media has been updated since the client's cache timestamp
            if (user.getLastUpdated() != null) {
                ZonedDateTime lastUpdated = ZonedDateTime.ofInstant(user.getLastUpdated(), clientTime.getZone());
                boolean needsUpdate = lastUpdated.isAfter(clientTime);

                if (needsUpdate) {
                    try {
                        UserSocialMediaDTO socialMedia = userSocialMediaService.getUserSocialMedia(user.getId());
                        byte[] socialMediaData = objectMapper.writeValueAsBytes(socialMedia);

                        if (socialMediaData.length < 100_000) {
                            return new CacheValidationResponseDTO(true, socialMediaData);
                        }
                    } catch (Exception e) {
                        logger.error("Failed to serialize profile social media data", e);
                    }
                    return new CacheValidationResponseDTO(true, null);
                }
                return new CacheValidationResponseDTO(false, null);
            }
            return new CacheValidationResponseDTO(true, null);
        } catch (Exception e) {
            logger.error("Error validating profile social media cache for user {}: {}", user.getId(), e.getMessage());
            return new CacheValidationResponseDTO(true, null);
        }
    }

    /**
     * Validates the user's profile events cache.
     */
    private CacheValidationResponseDTO validateProfileEventsCache(User user, String clientTimestamp) {
        try {
            ZonedDateTime clientTime = ZonedDateTime.parse(clientTimestamp, DateTimeFormatter.ISO_DATE_TIME);

            // Get latest activity activity for this user's profile
            Instant latestActivityActivity = getLatestActivityActivity(user.getId());
            boolean needsUpdate = latestActivityActivity.isAfter(clientTime.toInstant());

            if (needsUpdate) {
                try {
                    List<ProfileActivityDTO> activities = ActivityService.getProfileActivities(user.getId(), user.getId());
                    byte[] activitiesData = objectMapper.writeValueAsBytes(activities);

                    if (activitiesData.length < 100_000) {
                        return new CacheValidationResponseDTO(true, activitiesData);
                    }
                } catch (Exception e) {
                    logger.error("Failed to serialize profile activities data", e);
                }
                return new CacheValidationResponseDTO(true, null);
            }
            return new CacheValidationResponseDTO(false, null);
        } catch (Exception e) {
            logger.error("Error validating profile activities cache for user {}: {}", user.getId(), e.getMessage());
            return new CacheValidationResponseDTO(true, null);
        }
    }

    private CacheValidationResponseDTO validateRecentlySpawnedCache(User user, String clientTimestamp) {
        try {
            // Parse the client timestamp
            ZonedDateTime clientTime = ZonedDateTime.parse(clientTimestamp, DateTimeFormatter.ISO_DATE_TIME);

            Instant latestFriendActivity = getLatestFriendActivity(user.getId());

            boolean needsUpdate = latestFriendActivity.isAfter(clientTime.toInstant());

            if (needsUpdate) {
                try {
                    List<RecentlySpawnedUserDTO> recentlySpawnedUsers = userService.getRecentlySpawnedWithUsers(user.getId());
                    byte[] recentlySpawnedData = objectMapper.writeValueAsBytes(recentlySpawnedUsers);
                    if (recentlySpawnedData.length < 100_000) {
                        return new CacheValidationResponseDTO(true, recentlySpawnedData);
                    }
                } catch (Exception e) {
                    logger.error("Failed to serialize recently spawned data", e);
                }
                return new CacheValidationResponseDTO(true, null);
            }
            return new CacheValidationResponseDTO(false, null);
        } catch (Exception e) {
            logger.error("Error validating recently-spawned cache for user {}: {}", user.getId(), e.getMessage());
            return new CacheValidationResponseDTO(true, null);
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
        } catch (Exception e) {
            logger.error("Error getting latest activity activity for user {}: {}", userId, e.getMessage(), e);
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