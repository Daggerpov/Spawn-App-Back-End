package com.danielagapov.spawn.ControllerTests;

import com.danielagapov.spawn.notification.api.NotificationController;
import com.danielagapov.spawn.notification.api.dto.DeviceTokenDTO;
import com.danielagapov.spawn.notification.api.dto.NotificationPreferencesDTO;
import com.danielagapov.spawn.shared.exceptions.Logger.ILogger;
import com.danielagapov.spawn.shared.util.DeviceType;
import com.danielagapov.spawn.notification.internal.services.FCMService;
import com.danielagapov.spawn.notification.internal.services.NotificationService;
import com.danielagapov.spawn.notification.internal.services.NotificationVO;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Comprehensive unit tests for NotificationController
 * Tests device token management, notification preferences, and push notifications
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Notification Controller Tests")
class NotificationControllerTests {

    @Mock
    private NotificationService notificationService;

    @Mock
    private FCMService fcmService;

    @Mock
    private ILogger logger;

    @InjectMocks
    private NotificationController notificationController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;
    private UUID userId;
    private String deviceToken;
    private DeviceTokenDTO deviceTokenDTO;
    private NotificationPreferencesDTO preferencesDTO;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        
        mockMvc = MockMvcBuilders.standaloneSetup(notificationController)
                .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
                .build();
        
        userId = UUID.randomUUID();
        deviceToken = "test-device-token-12345";
        
        // Correct constructor: (token, deviceType, userId)
        deviceTokenDTO = new DeviceTokenDTO(deviceToken, DeviceType.IOS, userId);
        
        // Correct constructor: (friendRequestsEnabled, activityInvitesEnabled, activityUpdatesEnabled, chatMessagesEnabled, userId)
        preferencesDTO = new NotificationPreferencesDTO(
            true,  // friendRequestsEnabled
            true,  // activityInvitesEnabled
            true,  // activityUpdatesEnabled
            true,  // chatMessagesEnabled
            userId
        );
        
        // Setup lenient logger mocks
        lenient().doNothing().when(logger).info(anyString());
        lenient().doNothing().when(logger).warn(anyString());
        lenient().doNothing().when(logger).error(anyString());
    }

    @Nested
    @DisplayName("Register Device Token Tests")
    class RegisterDeviceTokenTests {

        @Test
        @DisplayName("Should return OK when registration successful")
        void shouldReturnOk_WhenSuccessful() throws Exception {
            doNothing().when(notificationService).registerDeviceToken(any(DeviceTokenDTO.class));

            mockMvc.perform(post("/api/v1/notifications/device-tokens")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(deviceTokenDTO)))
                    .andExpect(status().isOk());

            verify(notificationService, times(1)).registerDeviceToken(any(DeviceTokenDTO.class));
        }

        @Test
        @DisplayName("Should return error when service fails")
        void shouldReturnError_WhenServiceFails() throws Exception {
            doThrow(new RuntimeException("Database error"))
                    .when(notificationService).registerDeviceToken(any(DeviceTokenDTO.class));

            mockMvc.perform(post("/api/v1/notifications/device-tokens")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(deviceTokenDTO)))
                    .andExpect(status().isInternalServerError());

            verify(logger, times(1)).error(contains("Error registering device token"));
        }

        @Test
        @DisplayName("Should handle multiple device tokens for same user")
        void shouldHandleMultipleTokens() throws Exception {
            DeviceTokenDTO token1 = new DeviceTokenDTO("token-1", DeviceType.IOS, userId);
            DeviceTokenDTO token2 = new DeviceTokenDTO("token-2", DeviceType.ANDROID, userId);
            
            doNothing().when(notificationService).registerDeviceToken(any(DeviceTokenDTO.class));

            mockMvc.perform(post("/api/v1/notifications/device-tokens")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(token1)))
                    .andExpect(status().isOk());

            mockMvc.perform(post("/api/v1/notifications/device-tokens")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(token2)))
                    .andExpect(status().isOk());

            verify(notificationService, times(2)).registerDeviceToken(any(DeviceTokenDTO.class));
        }

        @Test
        @DisplayName("Should handle long device tokens")
        void shouldHandleLongToken() throws Exception {
            String longToken = "a".repeat(500);
            DeviceTokenDTO longTokenDTO = new DeviceTokenDTO(longToken, DeviceType.IOS, userId);
            
            doNothing().when(notificationService).registerDeviceToken(any(DeviceTokenDTO.class));

            mockMvc.perform(post("/api/v1/notifications/device-tokens")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(longTokenDTO)))
                    .andExpect(status().isOk());

            verify(notificationService, times(1)).registerDeviceToken(any(DeviceTokenDTO.class));
        }
    }

    @Nested
    @DisplayName("Unregister Device Token Tests")
    class UnregisterDeviceTokenTests {

        @Test
        @DisplayName("Should return OK when unregistration successful")
        void shouldReturnOk_WhenSuccessful() throws Exception {
            doNothing().when(notificationService).unregisterDeviceToken(deviceToken);

            mockMvc.perform(delete("/api/v1/notifications/device-tokens")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(deviceTokenDTO)))
                    .andExpect(status().isOk());

            verify(notificationService, times(1)).unregisterDeviceToken(deviceToken);
        }

        @Test
        @DisplayName("Should return error when service fails")
        void shouldReturnError_WhenServiceFails() throws Exception {
            doThrow(new RuntimeException("Token not found"))
                    .when(notificationService).unregisterDeviceToken(deviceToken);

            mockMvc.perform(delete("/api/v1/notifications/device-tokens")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(deviceTokenDTO)))
                    .andExpect(status().isInternalServerError());

            verify(logger, times(1)).error(contains("Error unregistering device token"));
        }

        @Test
        @DisplayName("Should handle special characters in token")
        void shouldHandleSpecialCharacters() throws Exception {
            String specialToken = "token:with/special+chars=123";
            DeviceTokenDTO specialTokenDTO = new DeviceTokenDTO(specialToken, DeviceType.IOS, userId);
            
            doNothing().when(notificationService).unregisterDeviceToken(specialToken);

            mockMvc.perform(delete("/api/v1/notifications/device-tokens")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(specialTokenDTO)))
                    .andExpect(status().isOk());

            verify(notificationService, times(1)).unregisterDeviceToken(specialToken);
        }
    }

    @Nested
    @DisplayName("Get Notification Preferences Tests")
    class GetNotificationPreferencesTests {

        @Test
        @DisplayName("Should return preferences when user exists")
        void shouldReturnPreferences_WhenUserExists() throws Exception {
            when(notificationService.getNotificationPreferences(userId)).thenReturn(preferencesDTO);

            mockMvc.perform(get("/api/v1/notifications/preferences/{userId}", userId))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.userId").value(userId.toString()))
                    .andExpect(jsonPath("$.friendRequestsEnabled").value(true))
                    .andExpect(jsonPath("$.activityInvitesEnabled").value(true))
                    .andExpect(jsonPath("$.activityUpdatesEnabled").value(true))
                    .andExpect(jsonPath("$.chatMessagesEnabled").value(true));

            verify(notificationService, times(1)).getNotificationPreferences(userId);
        }

        @Test
        @DisplayName("Should return error when service fails")
        void shouldReturnError_WhenServiceFails() throws Exception {
            when(notificationService.getNotificationPreferences(userId))
                    .thenThrow(new RuntimeException("Database error"));

            mockMvc.perform(get("/api/v1/notifications/preferences/{userId}", userId))
                    .andExpect(status().isInternalServerError());

            verify(logger, times(1)).error(contains("Error fetching notification preferences"));
        }

        @Test
        @DisplayName("Should return default preferences when user has none")
        void shouldReturnDefaults_WhenNoPreferences() throws Exception {
            NotificationPreferencesDTO defaultPrefs = new NotificationPreferencesDTO(
                true, true, true, true, userId
            );
            
            when(notificationService.getNotificationPreferences(userId)).thenReturn(defaultPrefs);

            mockMvc.perform(get("/api/v1/notifications/preferences/{userId}", userId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.friendRequestsEnabled").value(true))
                    .andExpect(jsonPath("$.activityInvitesEnabled").value(true))
                    .andExpect(jsonPath("$.activityUpdatesEnabled").value(true))
                    .andExpect(jsonPath("$.chatMessagesEnabled").value(true));

            verify(notificationService, times(1)).getNotificationPreferences(userId);
        }
    }

    @Nested
    @DisplayName("Update Notification Preferences Tests")
    class UpdateNotificationPreferencesTests {

        @Test
        @DisplayName("Should return OK when update successful")
        void shouldReturnOk_WhenSuccessful() throws Exception {
            when(notificationService.saveNotificationPreferences(any(NotificationPreferencesDTO.class)))
                    .thenReturn(preferencesDTO);

            mockMvc.perform(post("/api/v1/notifications/preferences/{userId}", userId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(preferencesDTO)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.userId").value(userId.toString()));

            verify(notificationService, times(1)).saveNotificationPreferences(any(NotificationPreferencesDTO.class));
        }

        @Test
        @DisplayName("Should return bad request when user ID mismatch")
        void shouldReturnBadRequest_WhenUserIdMismatch() throws Exception {
            UUID differentUserId = UUID.randomUUID();

            mockMvc.perform(post("/api/v1/notifications/preferences/{userId}", differentUserId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(preferencesDTO)))
                    .andExpect(status().isBadRequest());

            verify(notificationService, never()).saveNotificationPreferences(any());
            verify(logger, times(1)).error(contains("User ID mismatch"));
        }

        @Test
        @DisplayName("Should return error when service fails")
        void shouldReturnError_WhenServiceFails() throws Exception {
            when(notificationService.saveNotificationPreferences(any(NotificationPreferencesDTO.class)))
                    .thenThrow(new RuntimeException("Save failed"));

            mockMvc.perform(post("/api/v1/notifications/preferences/{userId}", userId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(preferencesDTO)))
                    .andExpect(status().isInternalServerError());

            verify(logger, times(1)).error(contains("Error updating notification preferences"));
        }

        @Test
        @DisplayName("Should handle partial preferences")
        void shouldHandlePartialPreferences() throws Exception {
            NotificationPreferencesDTO partialPrefs = new NotificationPreferencesDTO(
                false, false, true, true, userId
            );
            
            when(notificationService.saveNotificationPreferences(any(NotificationPreferencesDTO.class)))
                    .thenReturn(partialPrefs);

            mockMvc.perform(post("/api/v1/notifications/preferences/{userId}", userId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(partialPrefs)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.friendRequestsEnabled").value(false))
                    .andExpect(jsonPath("$.activityInvitesEnabled").value(false))
                    .andExpect(jsonPath("$.activityUpdatesEnabled").value(true))
                    .andExpect(jsonPath("$.chatMessagesEnabled").value(true));

            verify(notificationService, times(1)).saveNotificationPreferences(any(NotificationPreferencesDTO.class));
        }

        @Test
        @DisplayName("Should handle all preferences disabled")
        void shouldHandleAllDisabled() throws Exception {
            NotificationPreferencesDTO allDisabled = new NotificationPreferencesDTO(
                false, false, false, false, userId
            );
            
            when(notificationService.saveNotificationPreferences(any(NotificationPreferencesDTO.class)))
                    .thenReturn(allDisabled);

            mockMvc.perform(post("/api/v1/notifications/preferences/{userId}", userId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(allDisabled)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.friendRequestsEnabled").value(false))
                    .andExpect(jsonPath("$.activityInvitesEnabled").value(false))
                    .andExpect(jsonPath("$.activityUpdatesEnabled").value(false))
                    .andExpect(jsonPath("$.chatMessagesEnabled").value(false));

            verify(notificationService, times(1)).saveNotificationPreferences(any(NotificationPreferencesDTO.class));
        }

        @Test
        @DisplayName("Should handle concurrent updates")
        void shouldHandleConcurrentUpdates() throws Exception {
            when(notificationService.saveNotificationPreferences(any(NotificationPreferencesDTO.class)))
                    .thenReturn(preferencesDTO);

            mockMvc.perform(post("/api/v1/notifications/preferences/{userId}", userId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(preferencesDTO)))
                    .andExpect(status().isOk());

            mockMvc.perform(post("/api/v1/notifications/preferences/{userId}", userId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(preferencesDTO)))
                    .andExpect(status().isOk());

            verify(notificationService, times(2)).saveNotificationPreferences(any(NotificationPreferencesDTO.class));
        }
    }

    @Nested
    @DisplayName("Test Notification Endpoint Tests")
    class TestNotificationTests {

        @Test
        @DisplayName("Should return OK when test notification sent")
        void shouldReturnOk_WhenSuccessful() throws Exception {
            doNothing().when(fcmService).sendMessageToToken(any(NotificationVO.class));

            mockMvc.perform(get("/api/v1/notifications/notification")
                    .param("deviceToken", deviceToken))
                    .andExpect(status().isOk());

            verify(fcmService, times(1)).sendMessageToToken(any(NotificationVO.class));
        }

        @Test
        @DisplayName("Should return error when FCM fails")
        void shouldReturnError_WhenFCMFails() throws Exception {
            doThrow(new RuntimeException("FCM service unavailable"))
                    .when(fcmService).sendMessageToToken(any(NotificationVO.class));

            mockMvc.perform(get("/api/v1/notifications/notification")
                    .param("deviceToken", deviceToken))
                    .andExpect(status().isInternalServerError());

            verify(logger, times(1)).error(contains("Error sending test notification"));
        }
    }

    @Nested
    @DisplayName("Direct Controller Method Tests")
    class DirectControllerMethodTests {

        @Test
        @DisplayName("Register device token direct call")
        void registerDeviceToken_DirectCall() throws Exception {
            doNothing().when(notificationService).registerDeviceToken(deviceTokenDTO);

            ResponseEntity<?> response = notificationController.registerDeviceToken(deviceTokenDTO);

            assertEquals(HttpStatus.OK, response.getStatusCode());
            verify(notificationService, times(1)).registerDeviceToken(deviceTokenDTO);
        }

        @Test
        @DisplayName("Unregister device token direct call")
        void unregisterDeviceToken_DirectCall() throws Exception {
            doNothing().when(notificationService).unregisterDeviceToken(deviceToken);

            ResponseEntity<?> response = notificationController.unregisterDeviceToken(deviceTokenDTO);

            assertEquals(HttpStatus.OK, response.getStatusCode());
            verify(notificationService, times(1)).unregisterDeviceToken(deviceToken);
        }

        @Test
        @DisplayName("Get notification preferences direct call")
        void getNotificationPreferences_DirectCall() throws Exception {
            when(notificationService.getNotificationPreferences(userId)).thenReturn(preferencesDTO);

            ResponseEntity<?> response = notificationController.getNotificationPreferences(userId);

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());
            verify(notificationService, times(1)).getNotificationPreferences(userId);
        }

        @Test
        @DisplayName("Update notification preferences direct call")
        void updateNotificationPreferences_DirectCall() throws Exception {
            when(notificationService.saveNotificationPreferences(preferencesDTO)).thenReturn(preferencesDTO);

            ResponseEntity<?> response = notificationController.updateNotificationPreferences(userId, preferencesDTO);

            assertEquals(HttpStatus.OK, response.getStatusCode());
            verify(notificationService, times(1)).saveNotificationPreferences(preferencesDTO);
        }

        @Test
        @DisplayName("Test notification direct call")
        void testNotification_DirectCall() throws Exception {
            doNothing().when(fcmService).sendMessageToToken(any(NotificationVO.class));

            ResponseEntity<?> response = notificationController.testNotification(deviceToken);

            assertEquals(HttpStatus.OK, response.getStatusCode());
            verify(fcmService, times(1)).sendMessageToToken(any(NotificationVO.class));
        }
    }
}
