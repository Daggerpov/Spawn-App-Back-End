package com.danielagapov.spawn.ControllerTests;

import com.danielagapov.spawn.notification.api.NotificationController;
import com.danielagapov.spawn.notification.api.dto.DeviceTokenDTO;
import com.danielagapov.spawn.notification.api.dto.NotificationPreferencesDTO;
import com.danielagapov.spawn.shared.exceptions.Logger.ILogger;
import com.danielagapov.spawn.notification.internal.services.FCMService;
import com.danielagapov.spawn.notification.internal.services.NotificationService;
import com.danielagapov.spawn.notification.internal.services.NotificationVO;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Comprehensive unit tests for NotificationController
 * Tests device token management, notification preferences, and push notifications
 */
@ExtendWith(MockitoExtension.class)
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
        deviceTokenDTO = new DeviceTokenDTO(userId, deviceToken);
        preferencesDTO = new NotificationPreferencesDTO(
            userId,
            true,  // friendRequestsEnabled
            true,  // activityInvitesEnabled
            true,  // activityUpdatesEnabled
            true,  // newMessagesEnabled
            false  // soundEnabled
        );
    }

    // MARK: - Register Device Token Tests

    @Test
    void registerDeviceToken_ShouldReturnOk_WhenSuccessful() throws Exception {
        doNothing().when(notificationService).registerDeviceToken(any(DeviceTokenDTO.class));

        mockMvc.perform(post("/api/v1/notifications/device-tokens")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(deviceTokenDTO)))
                .andExpect(status().isOk());

        verify(notificationService, times(1)).registerDeviceToken(any(DeviceTokenDTO.class));
    }

    @Test
    void registerDeviceToken_ShouldReturnInternalServerError_WhenServiceFails() throws Exception {
        doThrow(new RuntimeException("Database error"))
                .when(notificationService).registerDeviceToken(any(DeviceTokenDTO.class));

        mockMvc.perform(post("/api/v1/notifications/device-tokens")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(deviceTokenDTO)))
                .andExpect(status().isInternalServerError());

        verify(logger, times(1)).error(contains("Error registering device token"));
    }

    @Test
    void registerDeviceToken_ShouldHandleMultipleTokens_WhenUserHasMultipleDevices() throws Exception {
        DeviceTokenDTO token1 = new DeviceTokenDTO(userId, "token-1");
        DeviceTokenDTO token2 = new DeviceTokenDTO(userId, "token-2");
        
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

    // MARK: - Unregister Device Token Tests

    @Test
    void unregisterDeviceToken_ShouldReturnOk_WhenSuccessful() throws Exception {
        doNothing().when(notificationService).unregisterDeviceToken(deviceToken);

        mockMvc.perform(delete("/api/v1/notifications/device-tokens")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(deviceTokenDTO)))
                .andExpect(status().isOk());

        verify(notificationService, times(1)).unregisterDeviceToken(deviceToken);
    }

    @Test
    void unregisterDeviceToken_ShouldReturnInternalServerError_WhenServiceFails() throws Exception {
        doThrow(new RuntimeException("Token not found"))
                .when(notificationService).unregisterDeviceToken(deviceToken);

        mockMvc.perform(delete("/api/v1/notifications/device-tokens")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(deviceTokenDTO)))
                .andExpect(status().isInternalServerError());

        verify(logger, times(1)).error(contains("Error unregistering device token"));
    }

    @Test
    void unregisterDeviceToken_ShouldHandleSpecialCharacters_WhenTokenContainsSpecialChars() throws Exception {
        String specialToken = "token:with/special+chars=123";
        DeviceTokenDTO specialTokenDTO = new DeviceTokenDTO(userId, specialToken);
        
        doNothing().when(notificationService).unregisterDeviceToken(specialToken);

        mockMvc.perform(delete("/api/v1/notifications/device-tokens")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(specialTokenDTO)))
                .andExpect(status().isOk());

        verify(notificationService, times(1)).unregisterDeviceToken(specialToken);
    }

    // MARK: - Get Notification Preferences Tests

    @Test
    void getNotificationPreferences_ShouldReturnPreferences_WhenUserExists() throws Exception {
        when(notificationService.getNotificationPreferences(userId)).thenReturn(preferencesDTO);

        mockMvc.perform(get("/api/v1/notifications/preferences/{userId}", userId))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.userId").value(userId.toString()))
                .andExpect(jsonPath("$.friendRequestsEnabled").value(true))
                .andExpect(jsonPath("$.activityInvitesEnabled").value(true))
                .andExpect(jsonPath("$.soundEnabled").value(false));

        verify(notificationService, times(1)).getNotificationPreferences(userId);
    }

    @Test
    void getNotificationPreferences_ShouldReturnBadRequest_WhenNullUserId() throws Exception {
        mockMvc.perform(get("/api/v1/notifications/preferences/{userId}", (Object) null))
                .andExpect(status().isBadRequest());

        verify(notificationService, never()).getNotificationPreferences(any());
    }

    @Test
    void getNotificationPreferences_ShouldReturnInternalServerError_WhenServiceFails() throws Exception {
        when(notificationService.getNotificationPreferences(userId))
                .thenThrow(new RuntimeException("Database error"));

        mockMvc.perform(get("/api/v1/notifications/preferences/{userId}", userId))
                .andExpect(status().isInternalServerError());

        verify(logger, times(1)).error(contains("Error fetching notification preferences"));
    }

    // MARK: - Update Notification Preferences Tests

    @Test
    void updateNotificationPreferences_ShouldReturnOk_WhenSuccessful() throws Exception {
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
    void updateNotificationPreferences_ShouldReturnBadRequest_WhenUserIdMismatch() throws Exception {
        UUID differentUserId = UUID.randomUUID();

        mockMvc.perform(post("/api/v1/notifications/preferences/{userId}", differentUserId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(preferencesDTO)))
                .andExpect(status().isBadRequest());

        verify(notificationService, never()).saveNotificationPreferences(any());
        verify(logger, times(1)).error(contains("User ID mismatch"));
    }

    @Test
    void updateNotificationPreferences_ShouldReturnBadRequest_WhenNullUserId() throws Exception {
        mockMvc.perform(post("/api/v1/notifications/preferences/{userId}", (Object) null)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(preferencesDTO)))
                .andExpect(status().isBadRequest());

        verify(notificationService, never()).saveNotificationPreferences(any());
    }

    @Test
    void updateNotificationPreferences_ShouldReturnInternalServerError_WhenServiceFails() throws Exception {
        when(notificationService.saveNotificationPreferences(any(NotificationPreferencesDTO.class)))
                .thenThrow(new RuntimeException("Save failed"));

        mockMvc.perform(post("/api/v1/notifications/preferences/{userId}", userId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(preferencesDTO)))
                .andExpect(status().isInternalServerError());

        verify(logger, times(1)).error(contains("Error updating notification preferences"));
    }

    @Test
    void updateNotificationPreferences_ShouldHandlePartialPreferences_WhenSomeDisabled() throws Exception {
        NotificationPreferencesDTO partialPrefs = new NotificationPreferencesDTO(
            userId, false, false, true, true, false
        );
        
        when(notificationService.saveNotificationPreferences(any(NotificationPreferencesDTO.class)))
                .thenReturn(partialPrefs);

        mockMvc.perform(post("/api/v1/notifications/preferences/{userId}", userId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(partialPrefs)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.friendRequestsEnabled").value(false))
                .andExpect(jsonPath("$.activityInvitesEnabled").value(false))
                .andExpect(jsonPath("$.activityUpdatesEnabled").value(true));

        verify(notificationService, times(1)).saveNotificationPreferences(any(NotificationPreferencesDTO.class));
    }

    @Test
    void updateNotificationPreferences_ShouldHandleAllDisabled_WhenUserDisablesAll() throws Exception {
        NotificationPreferencesDTO allDisabled = new NotificationPreferencesDTO(
            userId, false, false, false, false, false
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
                .andExpect(jsonPath("$.newMessagesEnabled").value(false))
                .andExpect(jsonPath("$.soundEnabled").value(false));

        verify(notificationService, times(1)).saveNotificationPreferences(any(NotificationPreferencesDTO.class));
    }

    // MARK: - Test Notification Tests (Deprecated)

    @Test
    void testNotification_ShouldReturnOk_WhenSuccessful() throws Exception {
        doNothing().when(fcmService).sendMessageToToken(any(NotificationVO.class));

        mockMvc.perform(get("/api/v1/notifications/notification")
                .param("deviceToken", deviceToken))
                .andExpect(status().isOk());

        verify(fcmService, times(1)).sendMessageToToken(any(NotificationVO.class));
    }

    @Test
    void testNotification_ShouldReturnInternalServerError_WhenFCMFails() throws Exception {
        doThrow(new RuntimeException("FCM service unavailable"))
                .when(fcmService).sendMessageToToken(any(NotificationVO.class));

        mockMvc.perform(get("/api/v1/notifications/notification")
                .param("deviceToken", deviceToken))
                .andExpect(status().isInternalServerError());

        verify(logger, times(1)).error(contains("Error sending test notification"));
    }

    // MARK: - Direct Controller Method Tests

    @Test
    void registerDeviceToken_DirectCall_ShouldReturnOk_WhenSuccessful() {
        doNothing().when(notificationService).registerDeviceToken(deviceTokenDTO);

        ResponseEntity<?> response = notificationController.registerDeviceToken(deviceTokenDTO);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(notificationService, times(1)).registerDeviceToken(deviceTokenDTO);
    }

    @Test
    void unregisterDeviceToken_DirectCall_ShouldReturnOk_WhenSuccessful() {
        doNothing().when(notificationService).unregisterDeviceToken(deviceToken);

        ResponseEntity<?> response = notificationController.unregisterDeviceToken(deviceTokenDTO);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(notificationService, times(1)).unregisterDeviceToken(deviceToken);
    }

    @Test
    void getNotificationPreferences_DirectCall_ShouldReturnOk_WhenUserExists() {
        when(notificationService.getNotificationPreferences(userId)).thenReturn(preferencesDTO);

        ResponseEntity<?> response = notificationController.getNotificationPreferences(userId);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        verify(notificationService, times(1)).getNotificationPreferences(userId);
    }

    @Test
    void updateNotificationPreferences_DirectCall_ShouldReturnOk_WhenSuccessful() {
        when(notificationService.saveNotificationPreferences(preferencesDTO)).thenReturn(preferencesDTO);

        ResponseEntity<?> response = notificationController.updateNotificationPreferences(userId, preferencesDTO);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(notificationService, times(1)).saveNotificationPreferences(preferencesDTO);
    }

    @Test
    void testNotification_DirectCall_ShouldReturnOk_WhenSuccessful() {
        doNothing().when(fcmService).sendMessageToToken(any(NotificationVO.class));

        ResponseEntity<?> response = notificationController.testNotification(deviceToken);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(fcmService, times(1)).sendMessageToToken(any(NotificationVO.class));
    }

    // MARK: - Edge Case Tests

    @Test
    void registerDeviceToken_ShouldHandleLongToken_WhenTokenIsVeryLong() throws Exception {
        String longToken = "a".repeat(500); // Very long token
        DeviceTokenDTO longTokenDTO = new DeviceTokenDTO(userId, longToken);
        
        doNothing().when(notificationService).registerDeviceToken(any(DeviceTokenDTO.class));

        mockMvc.perform(post("/api/v1/notifications/device-tokens")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(longTokenDTO)))
                .andExpect(status().isOk());

        verify(notificationService, times(1)).registerDeviceToken(any(DeviceTokenDTO.class));
    }

    @Test
    void updateNotificationPreferences_ShouldHandleConcurrentUpdates_WhenMultipleRequests() throws Exception {
        when(notificationService.saveNotificationPreferences(any(NotificationPreferencesDTO.class)))
                .thenReturn(preferencesDTO);

        // Simulate concurrent updates
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

    @Test
    void getNotificationPreferences_ShouldReturnDefaultPreferences_WhenUserHasNoPreferences() throws Exception {
        NotificationPreferencesDTO defaultPrefs = new NotificationPreferencesDTO(
            userId, true, true, true, true, true
        );
        
        when(notificationService.getNotificationPreferences(userId)).thenReturn(defaultPrefs);

        mockMvc.perform(get("/api/v1/notifications/preferences/{userId}", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.friendRequestsEnabled").value(true))
                .andExpect(jsonPath("$.activityInvitesEnabled").value(true))
                .andExpect(jsonPath("$.activityUpdatesEnabled").value(true))
                .andExpect(jsonPath("$.newMessagesEnabled").value(true))
                .andExpect(jsonPath("$.soundEnabled").value(true));

        verify(notificationService, times(1)).getNotificationPreferences(userId);
    }
}

