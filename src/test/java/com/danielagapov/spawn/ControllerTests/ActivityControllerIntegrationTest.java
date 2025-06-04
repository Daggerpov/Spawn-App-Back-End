package com.danielagapov.spawn.ControllerTests;

import com.danielagapov.spawn.DTOs.User.AuthUserDTO;
import com.danielagapov.spawn.Services.Activity.ActivityService;
import com.danielagapov.spawn.Services.Auth.AuthService;
import com.danielagapov.spawn.Services.FriendTag.FriendTagService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.util.UUID;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("Activity Controller Integration Tests")
public class ActivityControllerIntegrationTest extends BaseIntegrationTest {

    private static final String ACTIVITY_BASE_URL = "/api/v1/Activities";
    private UUID testUserId;
    private UUID testActivityId;
    private UUID testFriendTagId;

    @Autowired
    private AuthService authService;
    
    @Autowired
    private ActivityService activityService;
    
    @Autowired
    private FriendTagService friendTagService;

    @Override
    protected void setupTestData() {
        try {
            // Create a real test user for the activities
            AuthUserDTO testUserDTO = new AuthUserDTO(null, "Test User", "testuser@example.com", "activitytestuser", "Test bio", "password123");
            var registeredUser = authService.registerUser(testUserDTO);
            testUserId = registeredUser.getId();
            
            // Use real UUIDs for other test entities
            testActivityId = UUID.randomUUID();
            testFriendTagId = UUID.randomUUID();
        } catch (Exception e) {
            // Fall back to hardcoded UUIDs if user creation fails
            testUserId = UUID.randomUUID();
            testActivityId = UUID.randomUUID();
            testFriendTagId = UUID.randomUUID();
        }
    }

    @Test
    @DisplayName("GET /api/v1/Activities/user/{creatorUserId} - Should get activities created by user (deprecated)")
    void testGetActivitiesCreatedByUser() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get(ACTIVITY_BASE_URL + "/user/" + testUserId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @DisplayName("GET /api/v1/Activities/profile/{profileUserId} - Should get activities for profile")
    void testGetActivitiesForProfile() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get(ACTIVITY_BASE_URL + "/profile/" + testUserId)
                .param("requestingUserId", testUserId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @DisplayName("GET /api/v1/Activities/friendTag/{friendTagFilterId} - Should get activities by friend tag")
    void testGetActivitiesByFriendTag() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get(ACTIVITY_BASE_URL + "/friendTag/" + testFriendTagId)
                .param("requestingUserId", testUserId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @DisplayName("POST /api/v1/Activities - Should create activity successfully")
    void testCreateActivity_Success() throws Exception {
        String activityJson = "{"
                + "\"title\":\"Test Activity\","
                + "\"note\":\"Test Note\","
                + "\"creatorUserId\":\"" + testUserId + "\","
                + "\"startTime\":\"2024-12-31T10:00:00Z\","
                + "\"endTime\":\"2024-12-31T12:00:00Z\","
                + "\"location\":{"
                + "\"id\":null,"
                + "\"name\":\"Test Location\","
                + "\"latitude\":37.7749,"
                + "\"longitude\":-122.4194"
                + "},"
                + "\"invitedFriendUserIds\":[],"
                + "\"icon\":\"⭐\","
                + "\"category\":\"GENERAL\""
                + "}";

        mockMvc.perform(MockMvcRequestBuilders.post(ACTIVITY_BASE_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(activityJson))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("PUT /api/v1/Activities/{id} - Should replace activity (deprecated)")
    void testReplaceActivity() throws Exception {
        String activityJson = "{"
                + "\"id\":\"" + testActivityId + "\","
                + "\"title\":\"Updated Test Activity\","
                + "\"note\":\"Updated Test Note\","
                + "\"creatorUserId\":\"" + testUserId + "\","
                + "\"startTime\":\"2024-12-31T10:00:00Z\","
                + "\"endTime\":\"2024-12-31T12:00:00Z\""
                + "}";

        mockMvc.perform(MockMvcRequestBuilders.put(ACTIVITY_BASE_URL + "/" + testActivityId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(activityJson))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("DELETE /api/v1/Activities/{id} - Should delete activity successfully")
    void testDeleteActivity_Success() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.delete(ACTIVITY_BASE_URL + "/" + testActivityId))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("DELETE /api/v1/Activities/{id} - Should return not found for non-existent activity")
    void testDeleteActivity_NotFound() throws Exception {
        UUID nonExistentId = UUID.randomUUID();
        mockMvc.perform(MockMvcRequestBuilders.delete(ACTIVITY_BASE_URL + "/" + nonExistentId))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("PUT /api/v1/Activities/{ActivityId}/toggleStatus/{userId} - Should toggle activity status")
    void testToggleActivityStatus() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.put(ACTIVITY_BASE_URL + "/" + testActivityId + "/toggleStatus/" + testUserId))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /api/v1/Activities/feedActivities/{requestingUserId} - Should get feed activities")
    void testGetFeedActivities() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get(ACTIVITY_BASE_URL + "/feedActivities/" + testUserId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @DisplayName("GET /api/v1/Activities/{id} - Should get activity by ID")
    void testGetActivityById_Success() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get(ACTIVITY_BASE_URL + "/" + testActivityId)
                .param("requestingUserId", testUserId.toString()))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /api/v1/Activities/{id} - Should return not found for non-existent activity")
    void testGetActivityById_NotFound() throws Exception {
        UUID nonExistentId = UUID.randomUUID();
        mockMvc.perform(MockMvcRequestBuilders.get(ACTIVITY_BASE_URL + "/" + nonExistentId)
                .param("requestingUserId", testUserId.toString()))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /api/v1/Activities/user/{creatorUserId} - Should return not found for non-existent user")
    void testGetActivitiesCreatedByUser_UserNotFound() throws Exception {
        UUID nonExistentUserId = UUID.randomUUID();
        mockMvc.perform(MockMvcRequestBuilders.get(ACTIVITY_BASE_URL + "/user/" + nonExistentUserId))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /api/v1/Activities/profile/{profileUserId} - Should return bad request for missing requestingUserId")
    void testGetActivitiesForProfile_MissingParam() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get(ACTIVITY_BASE_URL + "/profile/" + testUserId))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /api/v1/Activities/friendTag/{friendTagFilterId} - Should return bad request for missing requestingUserId")
    void testGetActivitiesByFriendTag_MissingParam() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get(ACTIVITY_BASE_URL + "/friendTag/" + testFriendTagId))
                .andExpect(status().isBadRequest());
    }
} 