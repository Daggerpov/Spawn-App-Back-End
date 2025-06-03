package com.danielagapov.spawn.ControllerTests;

import com.danielagapov.spawn.DTOs.DeviceTokenDTO;
import com.danielagapov.spawn.DTOs.Notification.NotificationPreferencesDTO;
import com.danielagapov.spawn.DTOs.User.AuthUserDTO;
import com.danielagapov.spawn.Services.Auth.AuthService;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("Notification Controller Integration Tests")
@TestInstance(TestInstance.Lifecycle.PER_CLASS) // Allow @BeforeAll on non-static methods
public class NotificationControllerIntegrationTest extends BaseIntegrationTest {

    private static final String NOTIFICATION_BASE_URL = "/api/v1/notifications";
    private UUID testUserId;

    @Autowired
    private AuthService authService;

    @Override
    protected void setupTestData() {
        // Empty implementation - we use @BeforeAll instead
    }

    @BeforeAll
    @Transactional
    @Rollback(false) // Ensure the user persists across tests
    void setupTestUser() {
        try {
            // Create a single test user for all tests in this class
            AuthUserDTO testUserDTO = new AuthUserDTO(null, "Notification Test User", "notificationtest@example.com", "notificationtestuser", "Test bio", "password123");
            var registeredUser = authService.registerUser(testUserDTO);
            testUserId = registeredUser.getId();
            
            // Log the user creation for debugging
            System.out.println("Created shared test user with ID: " + testUserId);
        } catch (Exception e) {
            // Don't fall back to random UUID - if user creation fails, the test should fail
            throw new RuntimeException("Failed to create test user for notification tests: " + e.getMessage(), e);
        }
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
                + "\"friendRequestsEnabled\":true,"
                + "\"ActivityInvitesEnabled\":false,"
                + "\"ActivityUpdatesEnabled\":true,"
                + "\"chatMessagesEnabled\":true"
                + "}";

        mockMvc.perform(MockMvcRequestBuilders.post(NOTIFICATION_BASE_URL + "/preferences/" + testUserId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(preferencesJson))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /api/v1/notifications/notification - Should handle test notification (may fail without Firebase)")
    void testSendTestNotification() throws Exception {
        // Firebase may not be configured in tests, so we expect SERVICE_UNAVAILABLE when Firebase is not initialized
        mockMvc.perform(MockMvcRequestBuilders.get(NOTIFICATION_BASE_URL + "/notification")
                .param("deviceToken", "test-device-token-for-notification"))
                .andExpect(status().isServiceUnavailable()); // Expect 503 when Firebase is not configured
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

    @Test
    @DisplayName("POST /api/v1/notifications/device-tokens/register - Should return not found for non-existent user")
    void testRegisterDeviceToken_UserNotFound() throws Exception {
        UUID nonExistentUserId = UUID.randomUUID();
        String deviceTokenJson = "{"
                + "\"token\":\"test-device-token\","
                + "\"userId\":\"" + nonExistentUserId + "\","
                + "\"platform\":\"ios\""
                + "}";

        mockMvc.perform(MockMvcRequestBuilders.post(NOTIFICATION_BASE_URL + "/device-tokens/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(deviceTokenJson))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("POST /api/v1/notifications/preferences/{userId} - Should return not found for non-existent user")
    void testUpdateNotificationPreferences_UserNotFound() throws Exception {
        UUID nonExistentUserId = UUID.randomUUID();
        String preferencesJson = "{"
                + "\"friendRequestsEnabled\":true,"
                + "\"ActivityInvitesEnabled\":false,"
                + "\"ActivityUpdatesEnabled\":true,"
                + "\"chatMessagesEnabled\":true"
                + "}";

        mockMvc.perform(MockMvcRequestBuilders.post(NOTIFICATION_BASE_URL + "/preferences/" + nonExistentUserId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(preferencesJson))
                .andExpect(status().isNotFound());
    }
} 