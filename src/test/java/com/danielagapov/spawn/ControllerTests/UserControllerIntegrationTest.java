package com.danielagapov.spawn.ControllerTests;

import com.danielagapov.spawn.DTOs.User.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.util.UUID;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("User Controller Integration Tests")
public class UserControllerIntegrationTest extends BaseIntegrationTest {

    private static final String USER_BASE_URL = "/api/v1/users";
    private UUID testUserId = UUID.randomUUID();
    private String testUsername = "testuser";

    @Override
    protected void setupTestData() {
        // Setup test users for testing
    }

    @Test
    @DisplayName("GET /api/v1/users/{id} - Should get user by ID successfully")
    void testGetUser_Success() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get(USER_BASE_URL + "/" + testUserId)
                .header(AUTH_HEADER, BEARER_PREFIX + createMockJwtToken(testUsername)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(testUserId.toString()));
    }

    @Test
    @DisplayName("GET /api/v1/users/{id} - Should return not found for non-existent user")
    void testGetUser_NotFound() throws Exception {
        UUID nonExistentId = UUID.randomUUID();
        mockMvc.perform(MockMvcRequestBuilders.get(USER_BASE_URL + "/" + nonExistentId)
                .header(AUTH_HEADER, BEARER_PREFIX + createMockJwtToken(testUsername)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("DELETE /api/v1/users/{id} - Should delete user successfully")
    void testDeleteUser_Success() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.delete(USER_BASE_URL + "/" + testUserId)
                .header(AUTH_HEADER, BEARER_PREFIX + createMockJwtToken(testUsername)))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("DELETE /api/v1/users/{id} - Should return not found for non-existent user")
    void testDeleteUser_NotFound() throws Exception {
        UUID nonExistentId = UUID.randomUUID();
        mockMvc.perform(MockMvcRequestBuilders.delete(USER_BASE_URL + "/" + nonExistentId)
                .header(AUTH_HEADER, BEARER_PREFIX + createMockJwtToken(testUsername)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /api/v1/users/friends/{id} - Should get user friends")
    void testGetUserFriends() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get(USER_BASE_URL + "/friends/" + testUserId)
                .header(AUTH_HEADER, BEARER_PREFIX + createMockJwtToken(testUsername)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @DisplayName("GET /api/v1/users/recommended-friends/{id} - Should get recommended friends")
    void testGetRecommendedFriends() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get(USER_BASE_URL + "/recommended-friends/" + testUserId)
                .header(AUTH_HEADER, BEARER_PREFIX + createMockJwtToken(testUsername)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @DisplayName("PATCH /api/v1/users/update-pfp/{id} - Should update profile picture")
    void testUpdateProfilePicture() throws Exception {
        byte[] imageData = "test image data".getBytes();

        mockMvc.perform(MockMvcRequestBuilders.patch(USER_BASE_URL + "/update-pfp/" + testUserId)
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .content(imageData)
                .header(AUTH_HEADER, BEARER_PREFIX + createMockJwtToken(testUsername)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /api/v1/users/default-pfp - Should get default profile picture")
    void testGetDefaultProfilePicture() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get(USER_BASE_URL + "/default-pfp")
                .header(AUTH_HEADER, BEARER_PREFIX + createMockJwtToken(testUsername)))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.notNullValue()));
    }

    @Test
    @DisplayName("PATCH /api/v1/users/update/{id} - Should update user successfully")
    void testUpdateUser_Success() throws Exception {
        UserUpdateDTO updateDTO = new UserUpdateDTO("Updated bio", "updateduser", "Updated Name");

        mockMvc.perform(MockMvcRequestBuilders.patch(USER_BASE_URL + "/update/" + testUserId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(updateDTO))
                .header(AUTH_HEADER, BEARER_PREFIX + createMockJwtToken(testUsername)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("updateduser"));
    }

    @Test
    @DisplayName("PATCH /api/v1/users/update/{id} - Should return not found for non-existent user")
    void testUpdateUser_NotFound() throws Exception {
        UUID nonExistentId = UUID.randomUUID();
        UserUpdateDTO updateDTO = new UserUpdateDTO("Updated bio", "updateduser", "Updated Name");

        mockMvc.perform(MockMvcRequestBuilders.patch(USER_BASE_URL + "/update/" + nonExistentId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(updateDTO))
                .header(AUTH_HEADER, BEARER_PREFIX + createMockJwtToken(testUsername)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /api/v1/users/filtered/{requestingUserId} - Should get filtered users")
    void testGetFilteredUsers() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get(USER_BASE_URL + "/filtered/" + testUserId)
                .param("query", "test")
                .header(AUTH_HEADER, BEARER_PREFIX + createMockJwtToken(testUsername)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @DisplayName("GET /api/v1/users/search - Should search users")
    void testSearchUsers() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get(USER_BASE_URL + "/search")
                .param("query", "test")
                .param("requestingUserId", testUserId.toString())
                .header(AUTH_HEADER, BEARER_PREFIX + createMockJwtToken(testUsername)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @DisplayName("GET /api/v1/users/{userId}/recent-users - Should get recent users")
    void testGetRecentUsers() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get(USER_BASE_URL + "/" + testUserId + "/recent-users")
                .header(AUTH_HEADER, BEARER_PREFIX + createMockJwtToken(testUsername)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @DisplayName("GET /api/v1/users/{userId}/is-friend/{potentialFriendId} - Should check if users are friends")
    void testIsFriend() throws Exception {
        UUID friendId = UUID.randomUUID();
        mockMvc.perform(MockMvcRequestBuilders.get(USER_BASE_URL + "/" + testUserId + "/is-friend/" + friendId)
                .header(AUTH_HEADER, BEARER_PREFIX + createMockJwtToken(testUsername)))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.anyOf(
                        org.hamcrest.Matchers.is("true"),
                        org.hamcrest.Matchers.is("false")
                )));
    }

    @Test
    @DisplayName("POST /api/v1/users/s3/test-s3 - Should handle S3 test upload (deprecated)")
    void testS3Upload() throws Exception {
        byte[] testFile = "test file content".getBytes();

        mockMvc.perform(MockMvcRequestBuilders.post(USER_BASE_URL + "/s3/test-s3")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .content(testFile)
                .header(AUTH_HEADER, BEARER_PREFIX + createMockJwtToken(testUsername)))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.notNullValue()));
    }

    @Test
    @DisplayName("GET /api/v1/users/search - Should return bad request for missing parameters")
    void testSearchUsers_MissingParams() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get(USER_BASE_URL + "/search")
                .header(AUTH_HEADER, BEARER_PREFIX + createMockJwtToken(testUsername)))
                .andExpect(status().isBadRequest());
    }
} 