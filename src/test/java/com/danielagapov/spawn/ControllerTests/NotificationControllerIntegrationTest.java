package com.danielagapov.spawn.ControllerTests;

import com.danielagapov.spawn.DTOs.DeviceTokenDTO;
import com.danielagapov.spawn.DTOs.Notification.NotificationPreferencesDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.util.UUID;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("Notification Controller Integration Tests")
public class NotificationControllerIntegrationTest extends BaseIntegrationTest {

    private static final String NOTIFICATION_BASE_URL = "/api/v1/notifications";
    private UUID testUserId = UUID.randomUUID();

    @Override
    protected void setupTestData() {
        // Setup test notifications and users for testing
    }

    @Test
    @DisplayName("POST /api/v1/notifications/device-tokens/register - Should register device token")
    void testRegisterDeviceToken() throws Exception {
        String deviceTokenJson = "{"
                + "\"token\":\"test-device-token\","
                + "\"userId\":\"" + testUserId + "\","
                + "\"platform\":\"ios\""
                + "}";

        mockMvc.perform(MockMvcRequestBuilders.post(NOTIFICATION_BASE_URL + "/device-tokens/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(deviceTokenJson))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("DELETE /api/v1/notifications/device-tokens/unregister - Should unregister device token")
    void testUnregisterDeviceToken() throws Exception {
        String deviceTokenJson = "{"
                + "\"token\":\"test-device-token\","
                + "\"userId\":\"" + testUserId + "\","
                + "\"platform\":\"ios\""
                + "}";

        mockMvc.perform(MockMvcRequestBuilders.delete(NOTIFICATION_BASE_URL + "/device-tokens/unregister")
                .contentType(MediaType.APPLICATION_JSON)
                .content(deviceTokenJson))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /api/v1/notifications/preferences/{userId} - Should get notification preferences")
    void testGetNotificationPreferences() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get(NOTIFICATION_BASE_URL + "/preferences/" + testUserId))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("POST /api/v1/notifications/preferences/{userId} - Should update notification preferences")
    void testUpdateNotificationPreferences() throws Exception {
        String preferencesJson = "{"
                + "\"pushNotificationsEnabled\":true,"
                + "\"emailNotificationsEnabled\":false,"
                + "\"friendRequestNotifications\":true,"
                + "\"activityInviteNotifications\":true"
                + "}";

        mockMvc.perform(MockMvcRequestBuilders.post(NOTIFICATION_BASE_URL + "/preferences/" + testUserId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(preferencesJson))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /api/v1/notifications/notification - Should get notifications")
    void testGetNotifications() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get(NOTIFICATION_BASE_URL + "/notification"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("POST /api/v1/notifications/device-tokens/register - Should handle invalid device token data")
    void testRegisterDeviceToken_InvalidData() throws Exception {
        String invalidJson = "{}";

        mockMvc.perform(MockMvcRequestBuilders.post(NOTIFICATION_BASE_URL + "/device-tokens/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidJson))
                .andExpect(status().isInternalServerError());
    }

    @Test
    @DisplayName("GET /api/v1/notifications/preferences/{userId} - Should return not found for non-existent user")
    void testGetNotificationPreferences_UserNotFound() throws Exception {
        UUID nonExistentId = UUID.randomUUID();
        mockMvc.perform(MockMvcRequestBuilders.get(NOTIFICATION_BASE_URL + "/preferences/" + nonExistentId))
                .andExpect(status().isNotFound());
    }
} 