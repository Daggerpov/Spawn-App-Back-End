package com.danielagapov.spawn.ControllerTests;

import com.danielagapov.spawn.DTOs.FriendRequest.CreateFriendRequestDTO;
import com.danielagapov.spawn.Enums.FriendRequestAction;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.util.UUID;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("Friend Request Controller Integration Tests")
public class FriendRequestControllerIntegrationTest extends BaseIntegrationTest {

    private static final String FRIEND_REQUEST_BASE_URL = "/api/v1/friend-requests";
    private UUID testUserId = UUID.randomUUID();
    private UUID testFriendRequestId = UUID.randomUUID();

    @Override
    protected void setupTestData() {
        // Setup test friend requests and users for testing
    }

    @Test
    @DisplayName("GET /api/v1/friend-requests/incoming/{userId} - Should get incoming friend requests")
    void testGetIncomingFriendRequests() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get(FRIEND_REQUEST_BASE_URL + "/incoming/" + testUserId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @DisplayName("GET /api/v1/friend-requests/incoming/{userId} - Should return not found for non-existent user")
    void testGetIncomingFriendRequests_UserNotFound() throws Exception {
        UUID nonExistentId = UUID.randomUUID();
        mockMvc.perform(MockMvcRequestBuilders.get(FRIEND_REQUEST_BASE_URL + "/incoming/" + nonExistentId))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("POST /api/v1/friend-requests - Should create friend request successfully")
    void testCreateFriendRequest_Success() throws Exception {
        String friendRequestJson = "{"
                + "\"fromUserId\":\"" + testUserId + "\","
                + "\"toUserId\":\"" + UUID.randomUUID() + "\""
                + "}";

        mockMvc.perform(MockMvcRequestBuilders.post(FRIEND_REQUEST_BASE_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(friendRequestJson))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("PUT /api/v1/friend-requests/{friendRequestId} - Should accept friend request")
    void testAcceptFriendRequest() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.put(FRIEND_REQUEST_BASE_URL + "/" + testFriendRequestId)
                .param("friendRequestAction", "ACCEPT"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("PUT /api/v1/friend-requests/{friendRequestId} - Should reject friend request")
    void testRejectFriendRequest() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.put(FRIEND_REQUEST_BASE_URL + "/" + testFriendRequestId)
                .param("friendRequestAction", "REJECT"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("PUT /api/v1/friend-requests/{friendRequestId} - Should return not found for non-existent request")
    void testFriendRequestAction_NotFound() throws Exception {
        UUID nonExistentId = UUID.randomUUID();
        mockMvc.perform(MockMvcRequestBuilders.put(FRIEND_REQUEST_BASE_URL + "/" + nonExistentId)
                .param("friendRequestAction", "ACCEPT"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /api/v1/friend-requests/incoming/{userId} - Should return bad request for null userId")
    void testGetIncomingFriendRequests_NullUserId() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get(FRIEND_REQUEST_BASE_URL + "/incoming/null"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/v1/friend-requests - Should handle invalid friend request data")
    void testCreateFriendRequest_InvalidData() throws Exception {
        String invalidJson = "{}";

        mockMvc.perform(MockMvcRequestBuilders.post(FRIEND_REQUEST_BASE_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidJson))
                .andExpect(status().isInternalServerError());
    }
} 