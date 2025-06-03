package com.danielagapov.spawn.ControllerTests;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.util.UUID;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("Cache Controller Integration Tests")
public class CacheControllerIntegrationTest extends BaseIntegrationTest {

    private static final String CACHE_BASE_URL = "/api/v1/cache";
    private UUID testUserId = UUID.randomUUID();

    @Override
    protected void setupTestData() {
        // Setup test cache data for testing
    }

    @Test
    @DisplayName("POST /api/v1/cache/validate/{userId} - Should validate cache successfully")
    void testValidateCache_Success() throws Exception {
        String cacheValidationJson = "{"
                + "\"cacheCategories\":{"
                + "\"activities\":\"2024-01-01T10:00:00Z\","
                + "\"users\":\"2024-01-01T11:00:00Z\","
                + "\"friendRequests\":\"2024-01-01T12:00:00Z\""
                + "}"
                + "}";

        mockMvc.perform(MockMvcRequestBuilders.post(CACHE_BASE_URL + "/validate/" + testUserId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(cacheValidationJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.validationResults").exists());
    }

    @Test
    @DisplayName("POST /api/v1/cache/validate/{userId} - Should handle empty cache categories")
    void testValidateCache_EmptyCategories() throws Exception {
        String emptyCacheJson = "{\"cacheCategories\":{}}";

        mockMvc.perform(MockMvcRequestBuilders.post(CACHE_BASE_URL + "/validate/" + testUserId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(emptyCacheJson))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("POST /api/v1/cache/validate/{userId} - Should return not found for non-existent user")
    void testValidateCache_UserNotFound() throws Exception {
        UUID nonExistentUserId = UUID.randomUUID();
        String cacheValidationJson = "{"
                + "\"cacheCategories\":{"
                + "\"activities\":\"2024-01-01T10:00:00Z\""
                + "}"
                + "}";

        mockMvc.perform(MockMvcRequestBuilders.post(CACHE_BASE_URL + "/validate/" + nonExistentUserId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(cacheValidationJson))
                .andExpect(status().isNotFound());
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
                + "\"cacheCategories\":{"
                + "\"activities\":\"invalid-timestamp\","
                + "\"users\":\"2024-01-01T11:00:00Z\""
                + "}"
                + "}";

        mockMvc.perform(MockMvcRequestBuilders.post(CACHE_BASE_URL + "/validate/" + testUserId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(malformedJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/v1/cache/validate/{userId} - Should handle null userId")
    void testValidateCache_NullUserId() throws Exception {
        String cacheValidationJson = "{"
                + "\"cacheCategories\":{"
                + "\"activities\":\"2024-01-01T10:00:00Z\""
                + "}"
                + "}";

        mockMvc.perform(MockMvcRequestBuilders.post(CACHE_BASE_URL + "/validate/null")
                .contentType(MediaType.APPLICATION_JSON)
                .content(cacheValidationJson))
                .andExpect(status().isBadRequest());
    }
} 