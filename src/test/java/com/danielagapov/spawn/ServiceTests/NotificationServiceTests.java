package com.danielagapov.spawn.ServiceTests;

import com.danielagapov.spawn.notification.api.dto.DeviceTokenDTO;
import com.danielagapov.spawn.notification.api.dto.NotificationPreferencesDTO;
import com.danielagapov.spawn.notification.internal.domain.DeviceToken;
import com.danielagapov.spawn.notification.internal.domain.NotificationPreferences;
import com.danielagapov.spawn.notification.internal.repositories.IDeviceTokenRepository;
import com.danielagapov.spawn.notification.internal.repositories.INotificationPreferencesRepository;
import com.danielagapov.spawn.notification.internal.services.FCMService;
import com.danielagapov.spawn.notification.internal.services.NotificationService;
import com.danielagapov.spawn.shared.exceptions.Logger.ILogger;
import com.danielagapov.spawn.shared.util.DeviceType;
import com.danielagapov.spawn.user.internal.domain.User;
import com.danielagapov.spawn.user.internal.services.IUserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for NotificationService
 * Tests notification preferences and device token management with edge cases
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Notification Service Tests")
class NotificationServiceTests {

    @Mock
    private IDeviceTokenRepository deviceTokenRepository;

    @Mock
    private INotificationPreferencesRepository notificationPreferencesRepository;

    @Mock
    private IUserService userService;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private ILogger logger;

    @Mock
    private FCMService fcmService;

    private NotificationService notificationService;

    private UUID userId;
    private User testUser;
    private String deviceToken;
    private DeviceTokenDTO deviceTokenDTO;
    private DeviceToken deviceTokenEntity;
    private NotificationPreferencesDTO preferencesDTO;
    private NotificationPreferences preferencesEntity;

    @BeforeEach
    void setUp() {
        // Create the service with all required dependencies
        notificationService = new NotificationService(
            deviceTokenRepository,
            notificationPreferencesRepository,
            userService,
            eventPublisher,
            logger,
            fcmService
        );

        userId = UUID.randomUUID();
        deviceToken = "test-device-token-12345";

        // Setup test user
        testUser = new User();
        testUser.setId(userId);
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setName("Test User");

        // Setup DeviceTokenDTO with correct constructor order: (token, deviceType, userId)
        deviceTokenDTO = new DeviceTokenDTO(deviceToken, DeviceType.IOS, userId);

        // Setup DeviceToken entity
        deviceTokenEntity = new DeviceToken();
        deviceTokenEntity.setToken(deviceToken);
        deviceTokenEntity.setUser(testUser);
        deviceTokenEntity.setDeviceType(DeviceType.IOS);

        // Setup NotificationPreferencesDTO with correct constructor order:
        // (friendRequestsEnabled, activityInvitesEnabled, activityUpdatesEnabled, chatMessagesEnabled, userId)
        preferencesDTO = new NotificationPreferencesDTO(
            true, true, true, true, userId
        );

        // Setup NotificationPreferences entity
        preferencesEntity = new NotificationPreferences();
        preferencesEntity.setUser(testUser);
        preferencesEntity.setFriendRequestsEnabled(true);
        preferencesEntity.setActivityInvitesEnabled(true);
        preferencesEntity.setActivityUpdatesEnabled(true);
        preferencesEntity.setChatMessagesEnabled(true);

        // Setup default mocks for logger
        lenient().doNothing().when(logger).info(anyString());
        lenient().doNothing().when(logger).warn(anyString());
        lenient().doNothing().when(logger).error(anyString());
    }

    @Nested
    @DisplayName("Register Device Token Tests")
    class RegisterDeviceTokenTests {

        @Test
        @DisplayName("Should save new device token when valid token provided")
        void shouldSaveToken_WhenValidToken() throws Exception {
            // Given
            when(userService.getUserEntityById(userId)).thenReturn(testUser);
            when(deviceTokenRepository.findByToken(deviceToken)).thenReturn(List.of());
            when(deviceTokenRepository.save(any(DeviceToken.class))).thenReturn(deviceTokenEntity);

            // When & Then
            assertThatCode(() -> notificationService.registerDeviceToken(deviceTokenDTO))
                .doesNotThrowAnyException();

            verify(deviceTokenRepository, times(1)).save(any(DeviceToken.class));
        }

        @Test
        @DisplayName("Should not save duplicate device token")
        void shouldNotSaveDuplicate_WhenTokenExists() throws Exception {
            // Given
            when(userService.getUserEntityById(userId)).thenReturn(testUser);
            when(deviceTokenRepository.findByToken(deviceToken)).thenReturn(List.of(deviceTokenEntity));

            // When
            notificationService.registerDeviceToken(deviceTokenDTO);

            // Then - save should not be called since token exists
            verify(deviceTokenRepository, never()).save(any(DeviceToken.class));
        }

        @Test
        @DisplayName("Should handle long device token")
        void shouldHandleLongToken() throws Exception {
            // Given
            String longToken = "a".repeat(500);
            DeviceTokenDTO longTokenDTO = new DeviceTokenDTO(longToken, DeviceType.IOS, userId);
            
            when(userService.getUserEntityById(userId)).thenReturn(testUser);
            when(deviceTokenRepository.findByToken(longToken)).thenReturn(List.of());
            when(deviceTokenRepository.save(any(DeviceToken.class))).thenReturn(deviceTokenEntity);

            // When & Then
            assertThatCode(() -> notificationService.registerDeviceToken(longTokenDTO))
                .doesNotThrowAnyException();

            verify(deviceTokenRepository, times(1)).save(any(DeviceToken.class));
        }

        @Test
        @DisplayName("Should handle special characters in token")
        void shouldHandleSpecialCharacters() throws Exception {
            // Given
            String specialToken = "token:with/special+chars=123";
            DeviceTokenDTO specialTokenDTO = new DeviceTokenDTO(specialToken, DeviceType.ANDROID, userId);
            
            when(userService.getUserEntityById(userId)).thenReturn(testUser);
            when(deviceTokenRepository.findByToken(specialToken)).thenReturn(List.of());
            when(deviceTokenRepository.save(any(DeviceToken.class))).thenReturn(deviceTokenEntity);

            // When & Then
            assertThatCode(() -> notificationService.registerDeviceToken(specialTokenDTO))
                .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Should propagate exception when user not found")
        void shouldThrowException_WhenUserNotFound() {
            // Given
            when(userService.getUserEntityById(userId)).thenThrow(new RuntimeException("User not found"));

            // When & Then
            assertThatThrownBy(() -> notificationService.registerDeviceToken(deviceTokenDTO))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("User not found");
        }
    }

    @Nested
    @DisplayName("Unregister Device Token Tests")
    class UnregisterDeviceTokenTests {

        @Test
        @DisplayName("Should delete token when token exists")
        void shouldDeleteToken_WhenTokenExists() throws Exception {
            // Given
            when(deviceTokenRepository.existsByToken(deviceToken)).thenReturn(true);
            doNothing().when(deviceTokenRepository).deleteByToken(deviceToken);

            // When & Then
            assertThatCode(() -> notificationService.unregisterDeviceToken(deviceToken))
                .doesNotThrowAnyException();

            verify(deviceTokenRepository, times(1)).deleteByToken(deviceToken);
        }

        @Test
        @DisplayName("Should do nothing when token not found")
        void shouldDoNothing_WhenTokenNotFound() throws Exception {
            // Given
            when(deviceTokenRepository.existsByToken(deviceToken)).thenReturn(false);

            // When
            notificationService.unregisterDeviceToken(deviceToken);

            // Then
            verify(deviceTokenRepository, never()).deleteByToken(anyString());
            verify(logger).warn(contains("non-existent"));
        }

        @Test
        @DisplayName("Should handle null token")
        void shouldHandleNullToken() throws Exception {
            // Given
            when(deviceTokenRepository.existsByToken(null)).thenReturn(false);

            // When & Then
            assertThatCode(() -> notificationService.unregisterDeviceToken(null))
                .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Should handle empty token")
        void shouldHandleEmptyToken() throws Exception {
            // Given
            when(deviceTokenRepository.existsByToken("")).thenReturn(false);

            // When & Then
            assertThatCode(() -> notificationService.unregisterDeviceToken(""))
                .doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("Get Notification Preferences Tests")
    class GetNotificationPreferencesTests {

        @Test
        @DisplayName("Should return preferences when preferences exist")
        void shouldReturnPreferences_WhenPreferencesExist() throws Exception {
            // Given
            when(userService.getUserEntityById(userId)).thenReturn(testUser);
            when(notificationPreferencesRepository.findByUser(testUser))
                .thenReturn(Optional.of(preferencesEntity));

            // When
            NotificationPreferencesDTO result = notificationService.getNotificationPreferences(userId);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getUserId()).isEqualTo(userId);
            assertThat(result.isFriendRequestsEnabled()).isTrue();
            assertThat(result.isActivityInvitesEnabled()).isTrue();
            assertThat(result.isActivityUpdatesEnabled()).isTrue();
            assertThat(result.isChatMessagesEnabled()).isTrue();
        }

        @Test
        @DisplayName("Should create and return defaults when preferences not found")
        void shouldReturnDefaults_WhenPreferencesNotFound() throws Exception {
            // Given
            when(userService.getUserEntityById(userId)).thenReturn(testUser);
            when(notificationPreferencesRepository.findByUser(testUser))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(preferencesEntity)); // Second call for save
            when(notificationPreferencesRepository.save(any(NotificationPreferences.class)))
                .thenReturn(preferencesEntity);

            // When
            NotificationPreferencesDTO result = notificationService.getNotificationPreferences(userId);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getUserId()).isEqualTo(userId);
            // Default values should all be true
            assertThat(result.isFriendRequestsEnabled()).isTrue();
            assertThat(result.isActivityInvitesEnabled()).isTrue();
            assertThat(result.isActivityUpdatesEnabled()).isTrue();
            assertThat(result.isChatMessagesEnabled()).isTrue();
        }
    }

    @Nested
    @DisplayName("Save Notification Preferences Tests")
    class SaveNotificationPreferencesTests {

        @Test
        @DisplayName("Should save preferences when valid preferences provided")
        void shouldSavePreferences_WhenValidPreferences() throws Exception {
            // Given
            when(userService.getUserEntityById(userId)).thenReturn(testUser);
            when(notificationPreferencesRepository.findByUser(testUser))
                .thenReturn(Optional.empty());
            when(notificationPreferencesRepository.save(any(NotificationPreferences.class)))
                .thenReturn(preferencesEntity);

            // When
            NotificationPreferencesDTO result = notificationService.saveNotificationPreferences(preferencesDTO);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getUserId()).isEqualTo(userId);
            verify(notificationPreferencesRepository, times(1)).save(any(NotificationPreferences.class));
        }

        @Test
        @DisplayName("Should handle all preferences disabled")
        void shouldHandleAllDisabled() throws Exception {
            // Given
            NotificationPreferencesDTO allDisabledDTO = new NotificationPreferencesDTO(
                false, false, false, false, userId
            );
            
            NotificationPreferences allDisabledEntity = new NotificationPreferences();
            allDisabledEntity.setUser(testUser);
            allDisabledEntity.setFriendRequestsEnabled(false);
            allDisabledEntity.setActivityInvitesEnabled(false);
            allDisabledEntity.setActivityUpdatesEnabled(false);
            allDisabledEntity.setChatMessagesEnabled(false);

            when(userService.getUserEntityById(userId)).thenReturn(testUser);
            when(notificationPreferencesRepository.findByUser(testUser))
                .thenReturn(Optional.empty());
            when(notificationPreferencesRepository.save(any(NotificationPreferences.class)))
                .thenReturn(allDisabledEntity);

            // When
            NotificationPreferencesDTO result = notificationService.saveNotificationPreferences(allDisabledDTO);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.isFriendRequestsEnabled()).isFalse();
            assertThat(result.isActivityInvitesEnabled()).isFalse();
            assertThat(result.isActivityUpdatesEnabled()).isFalse();
            assertThat(result.isChatMessagesEnabled()).isFalse();
        }

        @Test
        @DisplayName("Should handle partial preferences")
        void shouldHandlePartialPreferences() throws Exception {
            // Given
            NotificationPreferencesDTO partialDTO = new NotificationPreferencesDTO(
                false, false, true, true, userId
            );
            
            NotificationPreferences partialEntity = new NotificationPreferences();
            partialEntity.setUser(testUser);
            partialEntity.setFriendRequestsEnabled(false);
            partialEntity.setActivityInvitesEnabled(false);
            partialEntity.setActivityUpdatesEnabled(true);
            partialEntity.setChatMessagesEnabled(true);

            when(userService.getUserEntityById(userId)).thenReturn(testUser);
            when(notificationPreferencesRepository.findByUser(testUser))
                .thenReturn(Optional.empty());
            when(notificationPreferencesRepository.save(any(NotificationPreferences.class)))
                .thenReturn(partialEntity);

            // When
            NotificationPreferencesDTO result = notificationService.saveNotificationPreferences(partialDTO);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.isFriendRequestsEnabled()).isFalse();
            assertThat(result.isActivityInvitesEnabled()).isFalse();
            assertThat(result.isActivityUpdatesEnabled()).isTrue();
            assertThat(result.isChatMessagesEnabled()).isTrue();
        }

        @Test
        @DisplayName("Should update existing preferences")
        void shouldUpdateExisting_WhenPreferencesExist() throws Exception {
            // Given
            when(userService.getUserEntityById(userId)).thenReturn(testUser);
            when(notificationPreferencesRepository.findByUser(testUser))
                .thenReturn(Optional.of(preferencesEntity));
            when(notificationPreferencesRepository.save(any(NotificationPreferences.class)))
                .thenReturn(preferencesEntity);

            // When
            NotificationPreferencesDTO result = notificationService.saveNotificationPreferences(preferencesDTO);

            // Then
            assertThat(result).isNotNull();
            verify(notificationPreferencesRepository, times(1)).save(any(NotificationPreferences.class));
        }
    }

    @Nested
    @DisplayName("Edge Case Tests")
    class EdgeCaseTests {

        @Test
        @DisplayName("Should handle multiple tokens for same user")
        void shouldHandleMultipleTokens() throws Exception {
            // Given
            String token1 = "token-1";
            String token2 = "token-2";
            DeviceTokenDTO dto1 = new DeviceTokenDTO(token1, DeviceType.IOS, userId);
            DeviceTokenDTO dto2 = new DeviceTokenDTO(token2, DeviceType.ANDROID, userId);

            when(userService.getUserEntityById(userId)).thenReturn(testUser);
            when(deviceTokenRepository.findByToken(token1)).thenReturn(List.of());
            when(deviceTokenRepository.findByToken(token2)).thenReturn(List.of());
            when(deviceTokenRepository.save(any(DeviceToken.class))).thenReturn(deviceTokenEntity);

            // When & Then
            assertThatCode(() -> {
                notificationService.registerDeviceToken(dto1);
                notificationService.registerDeviceToken(dto2);
            }).doesNotThrowAnyException();

            verify(deviceTokenRepository, times(2)).save(any(DeviceToken.class));
        }

        @Test
        @DisplayName("Should handle concurrent preference updates")
        void shouldHandleConcurrentUpdates() throws Exception {
            // Given
            when(userService.getUserEntityById(userId)).thenReturn(testUser);
            when(notificationPreferencesRepository.findByUser(testUser))
                .thenReturn(Optional.of(preferencesEntity));
            when(notificationPreferencesRepository.save(any(NotificationPreferences.class)))
                .thenReturn(preferencesEntity);

            // When
            NotificationPreferencesDTO result1 = notificationService.saveNotificationPreferences(preferencesDTO);
            NotificationPreferencesDTO result2 = notificationService.saveNotificationPreferences(preferencesDTO);

            // Then
            assertThat(result1).isNotNull();
            assertThat(result2).isNotNull();
            verify(notificationPreferencesRepository, times(2)).save(any(NotificationPreferences.class));
        }
    }
}
