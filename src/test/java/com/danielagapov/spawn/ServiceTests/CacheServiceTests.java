package com.danielagapov.spawn.ServiceTests;

import com.danielagapov.spawn.activity.api.dto.ActivityTypeDTO;
import com.danielagapov.spawn.activity.internal.services.IInternalActivityService;
import com.danielagapov.spawn.activity.internal.services.IActivityTypeService;
import com.danielagapov.spawn.analytics.internal.services.CacheService;
import com.danielagapov.spawn.analytics.internal.services.CacheType;
import com.danielagapov.spawn.shared.config.CacheValidationResponseDTO;
import com.danielagapov.spawn.shared.util.UserStatus;
import com.danielagapov.spawn.social.internal.services.IFriendRequestService;
import com.danielagapov.spawn.user.internal.domain.User;
import com.danielagapov.spawn.user.internal.repositories.IUserRepository;
import com.danielagapov.spawn.user.internal.services.IUserInterestService;
import com.danielagapov.spawn.user.internal.services.IUserService;
import com.danielagapov.spawn.user.internal.services.IUserSocialMediaService;
import com.danielagapov.spawn.user.internal.services.IUserStatsService;
import com.danielagapov.spawn.user.api.dto.UserSocialMediaDTO;
import com.danielagapov.spawn.user.api.dto.UserStatsDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for CacheService - Performance critical component.
 * Tests mobile cache validation functionality.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Cache Service Tests")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Order(3)
class CacheServiceTests {

    @Mock
    private IUserRepository userRepository;

    @Mock
    private IUserService userService;

    @Mock
    private IInternalActivityService activityService;

    @Mock
    private IActivityTypeService activityTypeService;

    @Mock
    private IFriendRequestService friendRequestService;

    @Mock
    private IUserStatsService userStatsService;

    @Mock
    private IUserInterestService userInterestService;

    @Mock
    private IUserSocialMediaService userSocialMediaService;

    @Mock
    private CacheManager cacheManager;

    @Mock
    private Cache cache;

    private CacheService cacheService;
    private ObjectMapper objectMapper;
    private User testUser;
    private UUID testUserId;

    @BeforeEach
    void setup() {
        objectMapper = new ObjectMapper();
        cacheService = new CacheService(
            userRepository,
            userService,
            activityService,
            activityTypeService,
            friendRequestService,
            objectMapper,
            userStatsService,
            userInterestService,
            userSocialMediaService,
            cacheManager
        );

        testUserId = UUID.randomUUID();
        testUser = createTestUser(testUserId);
    }

    private User createTestUser(UUID id) {
        User user = new User();
        user.setId(id);
        user.setUsername("testuser");
        user.setEmail("test@example.com");
        user.setName("Test User");
        user.setStatus(UserStatus.ACTIVE);
        user.setLastUpdated(Instant.now().minusSeconds(3600)); // 1 hour ago
        user.setDateCreated(new Date());
        return user;
    }

    private String getTimestamp(Instant instant) {
        return ZonedDateTime.ofInstant(instant, java.time.ZoneOffset.UTC)
            .format(DateTimeFormatter.ISO_DATE_TIME);
    }

    @Nested
    @DisplayName("Basic Cache Validation Tests")
    class BasicValidationTests {

        @Test
        @DisplayName("Should return empty response for non-existent user")
        void shouldReturnEmptyResponseForNonExistentUser() {
            // Given
            when(userRepository.findById(testUserId)).thenReturn(Optional.empty());
            Map<String, String> timestamps = new HashMap<>();
            timestamps.put(CacheType.FRIENDS.getKey(), getTimestamp(Instant.now()));

            // When
            Map<String, CacheValidationResponseDTO> response = cacheService.validateCache(testUserId, timestamps);

            // Then
            assertThat(response).isEmpty();
        }

        @Test
        @DisplayName("Should return all caches needing refresh when timestamps are null")
        void shouldReturnAllCachesNeedingRefreshForNullTimestamps() {
            // Given
            when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));

            // When
            Map<String, CacheValidationResponseDTO> response = cacheService.validateCache(testUserId, null);

            // Then
            assertThat(response).isNotEmpty();
            assertThat(response).containsKey(CacheType.FRIENDS.getKey());
            assertThat(response).containsKey(CacheType.EVENTS.getKey());
            assertThat(response).containsKey(CacheType.ACTIVITY_TYPES.getKey());
            
            // All should need refresh
            response.values().forEach(dto -> 
                assertThat(dto.isInvalidate()).isTrue()
            );
        }

        @Test
        @DisplayName("Should only validate requested cache types")
        void shouldOnlyValidateRequestedCacheTypes() {
            // Given
            when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));
            
            Map<String, String> timestamps = new HashMap<>();
            timestamps.put(CacheType.FRIENDS.getKey(), getTimestamp(Instant.now().minusSeconds(7200)));
            // Only request friends cache
            
            setupFriendsValidation();

            // When
            Map<String, CacheValidationResponseDTO> response = cacheService.validateCache(testUserId, timestamps);

            // Then
            assertThat(response).hasSize(1);
            assertThat(response).containsKey(CacheType.FRIENDS.getKey());
            assertThat(response).doesNotContainKey(CacheType.EVENTS.getKey());
        }
    }

    @Nested
    @DisplayName("Friends Cache Validation Tests")
    class FriendsCacheValidationTests {

        @Test
        @DisplayName("Should invalidate friends cache when stale")
        void shouldInvalidateFriendsCacheWhenStale() {
            // Given
            when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));
            
            // Client timestamp is 2 hours ago
            String clientTimestamp = getTimestamp(Instant.now().minusSeconds(7200));
            Map<String, String> timestamps = new HashMap<>();
            timestamps.put(CacheType.FRIENDS.getKey(), clientTimestamp);

            // Server has newer data (friend request was made 1 hour ago)
            when(friendRequestService.getLatestFriendRequestTimestamp(testUserId))
                .thenReturn(Instant.now().minusSeconds(3600));
            when(userRepository.findLatestFriendProfileUpdate(testUserId))
                .thenReturn(Instant.now().minusSeconds(3700));
            when(userService.getFullFriendUsersByUserId(testUserId))
                .thenReturn(List.of());

            // When
            Map<String, CacheValidationResponseDTO> response = cacheService.validateCache(testUserId, timestamps);

            // Then
            assertThat(response.get(CacheType.FRIENDS.getKey()).isInvalidate()).isTrue();
        }

        @Test
        @DisplayName("Should not invalidate friends cache when fresh")
        void shouldNotInvalidateFriendsCacheWhenFresh() {
            // Given
            when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));
            
            // Client timestamp is recent (10 seconds ago)
            String clientTimestamp = getTimestamp(Instant.now().minusSeconds(10));
            Map<String, String> timestamps = new HashMap<>();
            timestamps.put(CacheType.FRIENDS.getKey(), clientTimestamp);

            // Server data is older (1 hour ago)
            when(friendRequestService.getLatestFriendRequestTimestamp(testUserId))
                .thenReturn(Instant.now().minusSeconds(3600));
            when(userRepository.findLatestFriendProfileUpdate(testUserId))
                .thenReturn(Instant.now().minusSeconds(3700));

            // When
            Map<String, CacheValidationResponseDTO> response = cacheService.validateCache(testUserId, timestamps);

            // Then
            assertThat(response.get(CacheType.FRIENDS.getKey()).isInvalidate()).isFalse();
        }
    }

    @Nested
    @DisplayName("Events Cache Validation Tests")
    class EventsCacheValidationTests {

        @Test
        @DisplayName("Should invalidate events cache when activity is newer")
        void shouldInvalidateEventsCacheWhenActivityNewer() {
            // Given
            when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));
            
            String clientTimestamp = getTimestamp(Instant.now().minusSeconds(7200));
            Map<String, String> timestamps = new HashMap<>();
            timestamps.put(CacheType.EVENTS.getKey(), clientTimestamp);

            // Server has newer activity
            when(activityService.getLatestCreatedActivityTimestamp(testUserId))
                .thenReturn(Instant.now().minusSeconds(1800));
            when(activityService.getLatestInvitedActivityTimestamp(testUserId))
                .thenReturn(null);
            when(activityService.getLatestUpdatedActivityTimestamp(testUserId))
                .thenReturn(null);
            when(activityService.getFeedActivities(testUserId))
                .thenReturn(List.of());

            // When
            Map<String, CacheValidationResponseDTO> response = cacheService.validateCache(testUserId, timestamps);

            // Then
            assertThat(response.get(CacheType.EVENTS.getKey()).isInvalidate()).isTrue();
        }
    }

    @Nested
    @DisplayName("Activity Types Cache Validation Tests")
    class ActivityTypesCacheValidationTests {

        @Test
        @DisplayName("Should handle empty activity types list")
        void shouldHandleEmptyActivityTypesList() {
            // Given
            when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));
            
            String clientTimestamp = getTimestamp(Instant.now().minusSeconds(100));
            Map<String, String> timestamps = new HashMap<>();
            timestamps.put(CacheType.ACTIVITY_TYPES.getKey(), clientTimestamp);

            when(activityTypeService.getActivityTypesByUserId(testUserId))
                .thenReturn(List.of());

            // When
            Map<String, CacheValidationResponseDTO> response = cacheService.validateCache(testUserId, timestamps);

            // Then
            // Empty activity types should not invalidate (using epoch as timestamp)
            assertThat(response.get(CacheType.ACTIVITY_TYPES.getKey()).isInvalidate()).isFalse();
        }

        @Test
        @DisplayName("Should invalidate activity types cache periodically")
        void shouldInvalidateActivityTypesCachePeriodically() {
            // Given
            when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));
            
            // Client timestamp is 2 hours ago (older than the 1 hour refresh period)
            String clientTimestamp = getTimestamp(Instant.now().minusSeconds(7200));
            Map<String, String> timestamps = new HashMap<>();
            timestamps.put(CacheType.ACTIVITY_TYPES.getKey(), clientTimestamp);

            ActivityTypeDTO activityType = new ActivityTypeDTO();
            activityType.setId(UUID.randomUUID());
            when(activityTypeService.getActivityTypesByUserId(testUserId))
                .thenReturn(List.of(activityType));

            // When
            Map<String, CacheValidationResponseDTO> response = cacheService.validateCache(testUserId, timestamps);

            // Then
            assertThat(response.get(CacheType.ACTIVITY_TYPES.getKey()).isInvalidate()).isTrue();
        }
    }

    @Nested
    @DisplayName("Profile Picture Cache Validation Tests")
    class ProfilePictureCacheValidationTests {

        @Test
        @DisplayName("Should invalidate profile picture cache when user updated")
        void shouldInvalidateProfilePictureWhenUserUpdated() {
            // Given
            testUser.setLastUpdated(Instant.now().minusSeconds(100)); // Recent update
            when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));
            
            // Client cached 2 hours ago
            String clientTimestamp = getTimestamp(Instant.now().minusSeconds(7200));
            Map<String, String> timestamps = new HashMap<>();
            timestamps.put(CacheType.PROFILE_PICTURE.getKey(), clientTimestamp);

            // When
            Map<String, CacheValidationResponseDTO> response = cacheService.validateCache(testUserId, timestamps);

            // Then
            assertThat(response.get(CacheType.PROFILE_PICTURE.getKey()).isInvalidate()).isTrue();
        }

        @Test
        @DisplayName("Should not invalidate profile picture cache when fresh")
        void shouldNotInvalidateProfilePictureWhenFresh() {
            // Given
            testUser.setLastUpdated(Instant.now().minusSeconds(7200)); // Updated 2 hours ago
            when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));
            
            // Client cached 1 hour ago (after update)
            String clientTimestamp = getTimestamp(Instant.now().minusSeconds(3600));
            Map<String, String> timestamps = new HashMap<>();
            timestamps.put(CacheType.PROFILE_PICTURE.getKey(), clientTimestamp);

            // When
            Map<String, CacheValidationResponseDTO> response = cacheService.validateCache(testUserId, timestamps);

            // Then
            assertThat(response.get(CacheType.PROFILE_PICTURE.getKey()).isInvalidate()).isFalse();
        }
    }

    @Nested
    @DisplayName("Friend Requests Cache Validation Tests")
    class FriendRequestsCacheValidationTests {

        @Test
        @DisplayName("Should handle null friend request timestamp")
        void shouldHandleNullFriendRequestTimestamp() {
            // Given
            when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));
            
            String clientTimestamp = getTimestamp(Instant.now().minusSeconds(3600));
            Map<String, String> timestamps = new HashMap<>();
            timestamps.put(CacheType.FRIEND_REQUESTS.getKey(), clientTimestamp);

            // No friend requests exist
            when(friendRequestService.getLatestFriendRequestTimestamp(testUserId))
                .thenReturn(null);

            // When
            Map<String, CacheValidationResponseDTO> response = cacheService.validateCache(testUserId, timestamps);

            // Then
            // Null server timestamp means no data, so cache is valid
            assertThat(response.get(CacheType.FRIEND_REQUESTS.getKey()).isInvalidate()).isFalse();
        }

        @Test
        @DisplayName("Should invalidate friend requests cache when new requests exist")
        void shouldInvalidateWhenNewRequestsExist() {
            // Given
            when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));
            
            String clientTimestamp = getTimestamp(Instant.now().minusSeconds(7200));
            Map<String, String> timestamps = new HashMap<>();
            timestamps.put(CacheType.FRIEND_REQUESTS.getKey(), clientTimestamp);

            // New friend request 1 hour ago
            when(friendRequestService.getLatestFriendRequestTimestamp(testUserId))
                .thenReturn(Instant.now().minusSeconds(3600));
            when(friendRequestService.getIncomingFetchFriendRequestsByUserId(testUserId))
                .thenReturn(List.of());

            // When
            Map<String, CacheValidationResponseDTO> response = cacheService.validateCache(testUserId, timestamps);

            // Then
            assertThat(response.get(CacheType.FRIEND_REQUESTS.getKey()).isInvalidate()).isTrue();
        }
    }

    @Nested
    @DisplayName("Profile Stats Cache Validation Tests")
    class ProfileStatsCacheValidationTests {

        @Test
        @DisplayName("Should validate profile stats cache based on user lastUpdated")
        void shouldValidateProfileStatsCacheBasedOnLastUpdated() {
            // Given
            testUser.setLastUpdated(Instant.now().minusSeconds(100));
            when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));
            
            String clientTimestamp = getTimestamp(Instant.now().minusSeconds(7200));
            Map<String, String> timestamps = new HashMap<>();
            timestamps.put(CacheType.PROFILE_STATS.getKey(), clientTimestamp);

            when(userStatsService.getUserStats(testUserId)).thenReturn(new UserStatsDTO());

            // When
            Map<String, CacheValidationResponseDTO> response = cacheService.validateCache(testUserId, timestamps);

            // Then
            assertThat(response.get(CacheType.PROFILE_STATS.getKey()).isInvalidate()).isTrue();
        }
    }

    @Nested
    @DisplayName("Multiple Cache Types Tests")
    class MultipleCacheTypesTests {

        @Test
        @DisplayName("Should validate multiple cache types in single request")
        void shouldValidateMultipleCacheTypes() {
            // Given
            when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));
            
            Map<String, String> timestamps = new HashMap<>();
            timestamps.put(CacheType.FRIENDS.getKey(), getTimestamp(Instant.now().minusSeconds(7200)));
            timestamps.put(CacheType.PROFILE_PICTURE.getKey(), getTimestamp(Instant.now().minusSeconds(7200)));
            timestamps.put(CacheType.PROFILE_STATS.getKey(), getTimestamp(Instant.now().minusSeconds(7200)));

            setupFriendsValidation();
            when(userStatsService.getUserStats(testUserId)).thenReturn(new UserStatsDTO());

            // When
            Map<String, CacheValidationResponseDTO> response = cacheService.validateCache(testUserId, timestamps);

            // Then
            assertThat(response).hasSize(3);
            assertThat(response).containsKeys(
                CacheType.FRIENDS.getKey(),
                CacheType.PROFILE_PICTURE.getKey(),
                CacheType.PROFILE_STATS.getKey()
            );
        }

        @Test
        @DisplayName("Should handle mixed cache validation results")
        void shouldHandleMixedCacheValidationResults() {
            // Given
            testUser.setLastUpdated(Instant.now().minusSeconds(7200)); // Updated 2 hours ago
            when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));
            
            Map<String, String> timestamps = new HashMap<>();
            // Profile picture cached 1 hour ago (should be valid - cached after update)
            timestamps.put(CacheType.PROFILE_PICTURE.getKey(), getTimestamp(Instant.now().minusSeconds(3600)));
            // Friends cached 3 hours ago (should be invalid if newer activity exists)
            timestamps.put(CacheType.FRIENDS.getKey(), getTimestamp(Instant.now().minusSeconds(10800)));

            // Friends have activity 2 hours ago
            when(friendRequestService.getLatestFriendRequestTimestamp(testUserId))
                .thenReturn(Instant.now().minusSeconds(7200));
            when(userRepository.findLatestFriendProfileUpdate(testUserId))
                .thenReturn(Instant.now().minusSeconds(7300));
            when(userService.getFullFriendUsersByUserId(testUserId))
                .thenReturn(List.of());

            // When
            Map<String, CacheValidationResponseDTO> response = cacheService.validateCache(testUserId, timestamps);

            // Then
            assertThat(response.get(CacheType.PROFILE_PICTURE.getKey()).isInvalidate()).isFalse();
            assertThat(response.get(CacheType.FRIENDS.getKey()).isInvalidate()).isTrue();
        }
    }

    @Nested
    @DisplayName("Error Handling Tests")
    class ErrorHandlingTests {

        @Test
        @DisplayName("Should handle exception during friends validation")
        void shouldHandleExceptionDuringFriendsValidation() {
            // Given
            when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));
            
            Map<String, String> timestamps = new HashMap<>();
            timestamps.put(CacheType.FRIENDS.getKey(), getTimestamp(Instant.now().minusSeconds(3600)));

            // Simulate exception
            when(friendRequestService.getLatestFriendRequestTimestamp(testUserId))
                .thenThrow(new RuntimeException("Database error"));

            // When
            Map<String, CacheValidationResponseDTO> response = cacheService.validateCache(testUserId, timestamps);

            // Then - Should return invalidate=true on error to force refresh
            assertThat(response.get(CacheType.FRIENDS.getKey()).isInvalidate()).isTrue();
        }

        @Test
        @DisplayName("Should handle invalid timestamp format")
        void shouldHandleInvalidTimestampFormat() {
            // Given
            when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));
            
            Map<String, String> timestamps = new HashMap<>();
            timestamps.put(CacheType.FRIENDS.getKey(), "not-a-valid-timestamp");

            // When
            Map<String, CacheValidationResponseDTO> response = cacheService.validateCache(testUserId, timestamps);

            // Then - Should return invalidate=true on parse error
            assertThat(response.get(CacheType.FRIENDS.getKey()).isInvalidate()).isTrue();
        }
    }

    @Nested
    @DisplayName("Cache Type Enum Tests")
    class CacheTypeEnumTests {

        @Test
        @DisplayName("Should have correct keys for all cache types")
        void shouldHaveCorrectKeysForAllCacheTypes() {
            assertThat(CacheType.FRIENDS.getKey()).isEqualTo("friends");
            assertThat(CacheType.EVENTS.getKey()).isEqualTo("events");
            assertThat(CacheType.ACTIVITY_TYPES.getKey()).isEqualTo("activityTypes");
            assertThat(CacheType.PROFILE_PICTURE.getKey()).isEqualTo("profilePicture");
            assertThat(CacheType.OTHER_PROFILES.getKey()).isEqualTo("otherProfiles");
            assertThat(CacheType.RECOMMENDED_FRIENDS.getKey()).isEqualTo("recommendedFriends");
            assertThat(CacheType.FRIEND_REQUESTS.getKey()).isEqualTo("friendRequests");
            assertThat(CacheType.SENT_FRIEND_REQUESTS.getKey()).isEqualTo("sentFriendRequests");
            assertThat(CacheType.RECENTLY_SPAWNED.getKey()).isEqualTo("recentlySpawned");
            assertThat(CacheType.PROFILE_STATS.getKey()).isEqualTo("profileStats");
            assertThat(CacheType.PROFILE_INTERESTS.getKey()).isEqualTo("profileInterests");
            assertThat(CacheType.PROFILE_SOCIAL_MEDIA.getKey()).isEqualTo("profileSocialMedia");
            assertThat(CacheType.PROFILE_EVENTS.getKey()).isEqualTo("profileEvents");
        }

        @Test
        @DisplayName("Should have display names for all cache types")
        void shouldHaveDisplayNamesForAllCacheTypes() {
            for (CacheType type : CacheType.values()) {
                assertThat(type.getDisplayName()).isNotEmpty();
            }
        }
    }

    @Nested
    @DisplayName("All Cache Types Validation Tests")
    class AllCacheTypesValidationTests {

        @Test
        @DisplayName("Should handle other profiles cache validation")
        void shouldHandleOtherProfilesCacheValidation() {
            // Given
            when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));
            
            Map<String, String> timestamps = new HashMap<>();
            timestamps.put(CacheType.OTHER_PROFILES.getKey(), getTimestamp(Instant.now().minusSeconds(7200)));

            lenient().when(friendRequestService.getLatestFriendRequestTimestamp(testUserId))
                .thenReturn(Instant.now().minusSeconds(3600));
            lenient().when(userRepository.findLatestFriendProfileUpdate(testUserId))
                .thenReturn(Instant.now().minusSeconds(3700));
            lenient().when(userService.getFriendUserIdsByUserId(testUserId))
                .thenReturn(List.of());

            // When
            Map<String, CacheValidationResponseDTO> response = cacheService.validateCache(testUserId, timestamps);

            // Then
            assertThat(response).containsKey(CacheType.OTHER_PROFILES.getKey());
        }

        @Test
        @DisplayName("Should handle recommended friends cache validation")
        void shouldHandleRecommendedFriendsCacheValidation() {
            // Given
            when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));
            
            Map<String, String> timestamps = new HashMap<>();
            timestamps.put(CacheType.RECOMMENDED_FRIENDS.getKey(), getTimestamp(Instant.now().minusSeconds(7200)));

            when(friendRequestService.getLatestFriendRequestTimestamp(testUserId))
                .thenReturn(Instant.now().minusSeconds(3600));
            when(userRepository.findLatestFriendProfileUpdate(testUserId))
                .thenReturn(Instant.now().minusSeconds(3700));
            when(userService.getLimitedRecommendedFriendsForUserId(testUserId))
                .thenReturn(List.of());

            // When
            Map<String, CacheValidationResponseDTO> response = cacheService.validateCache(testUserId, timestamps);

            // Then
            assertThat(response).containsKey(CacheType.RECOMMENDED_FRIENDS.getKey());
        }

        @Test
        @DisplayName("Should handle sent friend requests cache validation")
        void shouldHandleSentFriendRequestsCacheValidation() {
            // Given
            when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));
            
            Map<String, String> timestamps = new HashMap<>();
            timestamps.put(CacheType.SENT_FRIEND_REQUESTS.getKey(), getTimestamp(Instant.now().minusSeconds(7200)));

            when(friendRequestService.getLatestFriendRequestTimestamp(testUserId))
                .thenReturn(Instant.now().minusSeconds(3600));
            when(friendRequestService.getSentFetchFriendRequestsByUserId(testUserId))
                .thenReturn(List.of());

            // When
            Map<String, CacheValidationResponseDTO> response = cacheService.validateCache(testUserId, timestamps);

            // Then
            assertThat(response).containsKey(CacheType.SENT_FRIEND_REQUESTS.getKey());
        }

        @Test
        @DisplayName("Should handle profile interests cache validation")
        void shouldHandleProfileInterestsCacheValidation() {
            // Given
            when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));
            
            Map<String, String> timestamps = new HashMap<>();
            timestamps.put(CacheType.PROFILE_INTERESTS.getKey(), getTimestamp(Instant.now().minusSeconds(7200)));

            when(userInterestService.getUserInterests(testUserId)).thenReturn(List.of());

            // When
            Map<String, CacheValidationResponseDTO> response = cacheService.validateCache(testUserId, timestamps);

            // Then
            assertThat(response).containsKey(CacheType.PROFILE_INTERESTS.getKey());
        }

        @Test
        @DisplayName("Should handle profile social media cache validation")
        void shouldHandleProfileSocialMediaCacheValidation() {
            // Given
            when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));
            
            Map<String, String> timestamps = new HashMap<>();
            timestamps.put(CacheType.PROFILE_SOCIAL_MEDIA.getKey(), getTimestamp(Instant.now().minusSeconds(7200)));

            when(userSocialMediaService.getUserSocialMedia(testUserId)).thenReturn(new UserSocialMediaDTO());

            // When
            Map<String, CacheValidationResponseDTO> response = cacheService.validateCache(testUserId, timestamps);

            // Then
            assertThat(response).containsKey(CacheType.PROFILE_SOCIAL_MEDIA.getKey());
        }

        @Test
        @DisplayName("Should handle profile events cache validation")
        void shouldHandleProfileEventsCacheValidation() {
            // Given
            when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));
            
            Map<String, String> timestamps = new HashMap<>();
            timestamps.put(CacheType.PROFILE_EVENTS.getKey(), getTimestamp(Instant.now().minusSeconds(7200)));

            when(activityService.getLatestCreatedActivityTimestamp(testUserId))
                .thenReturn(Instant.now().minusSeconds(3600));
            when(activityService.getLatestInvitedActivityTimestamp(testUserId))
                .thenReturn(null);
            when(activityService.getLatestUpdatedActivityTimestamp(testUserId))
                .thenReturn(null);
            when(activityService.getProfileActivities(testUserId, testUserId))
                .thenReturn(List.of());

            // When
            Map<String, CacheValidationResponseDTO> response = cacheService.validateCache(testUserId, timestamps);

            // Then
            assertThat(response).containsKey(CacheType.PROFILE_EVENTS.getKey());
        }

        @Test
        @DisplayName("Should handle recently spawned cache validation")
        void shouldHandleRecentlySpawnedCacheValidation() {
            // Given
            when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));
            
            Map<String, String> timestamps = new HashMap<>();
            timestamps.put(CacheType.RECENTLY_SPAWNED.getKey(), getTimestamp(Instant.now().minusSeconds(7200)));

            when(friendRequestService.getLatestFriendRequestTimestamp(testUserId))
                .thenReturn(Instant.now().minusSeconds(3600));
            when(userRepository.findLatestFriendProfileUpdate(testUserId))
                .thenReturn(Instant.now().minusSeconds(3700));
            when(userService.getRecentlySpawnedWithUsers(testUserId))
                .thenReturn(List.of());

            // When
            Map<String, CacheValidationResponseDTO> response = cacheService.validateCache(testUserId, timestamps);

            // Then
            assertThat(response).containsKey(CacheType.RECENTLY_SPAWNED.getKey());
        }
    }

    // Helper methods
    private void setupFriendsValidation() {
        when(friendRequestService.getLatestFriendRequestTimestamp(testUserId))
            .thenReturn(Instant.now().minusSeconds(3600));
        when(userRepository.findLatestFriendProfileUpdate(testUserId))
            .thenReturn(Instant.now().minusSeconds(3700));
        when(userService.getFullFriendUsersByUserId(testUserId))
            .thenReturn(List.of());
    }
}

