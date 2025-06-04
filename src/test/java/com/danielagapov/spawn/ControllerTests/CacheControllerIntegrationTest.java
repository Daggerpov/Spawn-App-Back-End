package com.danielagapov.spawn.ControllerTests;

import com.danielagapov.spawn.DTOs.User.AuthUserDTO;
import com.danielagapov.spawn.Services.Auth.AuthService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.util.UUID;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("Cache Controller Integration Tests")
public class CacheControllerIntegrationTest extends BaseIntegrationTest {

    private static final String CACHE_BASE_URL = "/api/v1/cache";
    private UUID testUserId;

    @Autowired
    private AuthService authService;

    @Override
    protected void setupTestData() {
        try {
            // Create a real test user for cache validation
            AuthUserDTO testUserDTO = new AuthUserDTO(null, "Cache Test User", "cachetest@example.com", "cachetestuser", "Test bio", "password123");
            var registeredUser = authService.registerUser(testUserDTO);
            testUserId = registeredUser.getId();
        } catch (Exception e) {
            // Fall back to random UUID if user creation fails
            testUserId = UUID.randomUUID();
        }
    }

    @Test
    @DisplayName("POST /api/v1/cache/validate/{userId} - Should validate cache successfully")
    void testValidateCache_Success() throws Exception {
        String cacheValidationJson = "{"
                + "\"timestamps\":{"
                + "\"friends\":\"2024-01-01T10:00:00Z\","
                + "\"events\":\"2024-01-01T11:00:00Z\""
                + "}"
                + "}";

        mockMvc.perform(MockMvcRequestBuilders.post(CACHE_BASE_URL + "/validate/" + testUserId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(cacheValidationJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isMap());
    }

    @Test
    @DisplayName("POST /api/v1/cache/validate/{userId} - Should handle empty cache categories")
    void testValidateCache_EmptyCategories() throws Exception {
        String emptyCacheJson = "{\"timestamps\":{}}";

        mockMvc.perform(MockMvcRequestBuilders.post(CACHE_BASE_URL + "/validate/" + testUserId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(emptyCacheJson))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("POST /api/v1/cache/validate/{userId} - Should return empty response for non-existent user")
    void testValidateCache_UserNotFound() throws Exception {
        UUID nonExistentUserId = UUID.randomUUID();
        String cacheValidationJson = "{"
                + "\"timestamps\":{"
                + "\"friends\":\"2024-01-01T10:00:00Z\""
                + "}"
                + "}";

        mockMvc.perform(MockMvcRequestBuilders.post(CACHE_BASE_URL + "/validate/" + nonExistentUserId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(cacheValidationJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    @DisplayName("POST /api/v1/cache/validate/{userId} - Should handle invalid JSON")
    void testValidateCache_InvalidJson() throws Exception {
        String invalidJson = "{invalid json}";

        mockMvc.perform(MockMvcRequestBuilders.post(CACHE_BASE_URL + "/validate/" + testUserId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/v1/cache/validate/{userId} - Should handle missing request body")
    void testValidateCache_MissingBody() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post(CACHE_BASE_URL + "/validate/" + testUserId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/v1/cache/validate/{userId} - Should handle malformed timestamps")
    void testValidateCache_MalformedTimestamps() throws Exception {
        String malformedJson = "{"
                + "\"timestamps\":{"
                + "\"friends\":\"invalid-timestamp\","
                + "\"events\":\"2024-01-01T11:00:00Z\""
                + "}"
                + "}";

        mockMvc.perform(MockMvcRequestBuilders.post(CACHE_BASE_URL + "/validate/" + testUserId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(malformedJson))
                .andExpect(status().isOk()); // Service handles invalid timestamps gracefully
    }

    @Test
    @DisplayName("POST /api/v1/cache/validate/{userId} - Should handle null userId")
    void testValidateCache_NullUserId() throws Exception {
        String cacheValidationJson = "{"
                + "\"timestamps\":{"
                + "\"friends\":\"2024-01-01T10:00:00Z\""
                + "}"
                + "}";

        mockMvc.perform(MockMvcRequestBuilders.post(CACHE_BASE_URL + "/validate/null")
                .contentType(MediaType.APPLICATION_JSON)
                .content(cacheValidationJson))
                .andExpect(status().isBadRequest());
    }
} 