package com.danielagapov.spawn.ControllerTests;

import com.danielagapov.spawn.DTOs.Activity.ActivityCreationDTO;
import com.danielagapov.spawn.DTOs.Activity.LocationDTO;
import com.danielagapov.spawn.DTOs.ChatMessage.CreateChatMessageDTO;
import com.danielagapov.spawn.DTOs.User.AuthUserDTO;
import com.danielagapov.spawn.Enums.ActivityCategory;
import com.danielagapov.spawn.Services.Activity.IActivityService;
import com.danielagapov.spawn.Services.Auth.IAuthService;
import com.danielagapov.spawn.Services.ChatMessage.IChatMessageService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("Chat Message Controller Integration Tests")
public class ChatMessageControllerIntegrationTest extends BaseIntegrationTest {

    private static final String CHAT_MESSAGE_BASE_URL = "/api/v1/chatMessages";
    private UUID testUserId;
    private UUID testActivityId;
    private UUID testChatMessageId;

    @Autowired
    private IAuthService authService;
    
    @Autowired
    private IActivityService activityService;
    
    @Autowired
    private IChatMessageService chatMessageService;

    @Override
    protected void setupTestData() {
        // Create test user via auth service
        AuthUserDTO authUserDTO = new AuthUserDTO(
            null,
            "Test User",
            "testuser@example.com",
            "testuser",
            "Test bio",
            "password123"
        );
        testUserId = authService.registerUser(authUserDTO).getId();

        // Create test location
        LocationDTO locationDTO = new LocationDTO(
            UUID.randomUUID(),
            "Test Location", 
            37.7749, // latitude
            -122.4194 // longitude
        );

        // Create test activity
        ActivityCreationDTO activityCreationDTO = new ActivityCreationDTO(
            UUID.randomUUID(),
            "Test Activity",
            OffsetDateTime.now().plusDays(1),
            OffsetDateTime.now().plusDays(1).plusHours(2),
            locationDTO,
            "Test note",
            "🎯",
            ActivityCategory.ACTIVE,
            testUserId,
            List.of(), // invitedFriendUserIds
            Instant.now()
        );
        testActivityId = activityService.createActivity(activityCreationDTO).getId();

        // Create test chat message
        CreateChatMessageDTO createChatMessageDTO = new CreateChatMessageDTO(
            "Test chat message content",
            testUserId,
            testActivityId
        );
        testChatMessageId = chatMessageService.createChatMessage(createChatMessageDTO).getId();
    }

    @Test
    @DisplayName("POST /api/v1/chatMessages - Should create chat message successfully")
    void testCreateChatMessage_Success() throws Exception {
        String chatMessageJson = "{"
                + "\"content\":\"Test message content\","
                + "\"senderUserId\":\"" + testUserId + "\","
                + "\"activityId\":\"" + testActivityId + "\""
                + "}";

        mockMvc.perform(MockMvcRequestBuilders.post(CHAT_MESSAGE_BASE_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(chatMessageJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.content").value("Test message content"))
                .andExpect(jsonPath("$.senderUserId").value(testUserId.toString()))
                .andExpect(jsonPath("$.activityId").value(testActivityId.toString()));
    }

    @Test
    @DisplayName("DELETE /api/v1/chatMessages/{id} - Should delete chat message successfully (deprecated)")
    void testDeleteChatMessage_Success() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.delete(CHAT_MESSAGE_BASE_URL + "/" + testChatMessageId))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("DELETE /api/v1/chatMessages/{id} - Should return not found for non-existent message")
    void testDeleteChatMessage_NotFound() throws Exception {
        UUID nonExistentId = UUID.randomUUID();
        mockMvc.perform(MockMvcRequestBuilders.delete(CHAT_MESSAGE_BASE_URL + "/" + nonExistentId))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("POST /api/v1/chatMessages/{chatMessageId}/likes/{userId} - Should like chat message (deprecated)")
    void testLikeChatMessage() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post(CHAT_MESSAGE_BASE_URL + "/" + testChatMessageId + "/likes/" + testUserId))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.chatMessageId").value(testChatMessageId.toString()))
                .andExpect(jsonPath("$.userId").value(testUserId.toString()));
    }

    @Test
    @DisplayName("GET /api/v1/chatMessages/{chatMessageId}/likes - Should get chat message likes (deprecated)")
    void testGetChatMessageLikes() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get(CHAT_MESSAGE_BASE_URL + "/" + testChatMessageId + "/likes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @DisplayName("GET /api/v1/chatMessages/{chatMessageId}/likes - Should return not found for non-existent message")
    void testGetChatMessageLikes_MessageNotFound() throws Exception {
        UUID nonExistentId = UUID.randomUUID();
        mockMvc.perform(MockMvcRequestBuilders.get(CHAT_MESSAGE_BASE_URL + "/" + nonExistentId + "/likes"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("DELETE /api/v1/chatMessages/{chatMessageId}/likes/{userId} - Should unlike chat message (deprecated)")
    void testUnlikeChatMessage() throws Exception {
        // First create a like, then delete it
        mockMvc.perform(MockMvcRequestBuilders.post(CHAT_MESSAGE_BASE_URL + "/" + testChatMessageId + "/likes/" + testUserId))
                .andExpect(status().isCreated());
        
        mockMvc.perform(MockMvcRequestBuilders.delete(CHAT_MESSAGE_BASE_URL + "/" + testChatMessageId + "/likes/" + testUserId))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("POST /api/v1/chatMessages - Should handle invalid chat message data")
    void testCreateChatMessage_InvalidData() throws Exception {
        String invalidJson = "{}";

        mockMvc.perform(MockMvcRequestBuilders.post(CHAT_MESSAGE_BASE_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidJson))
                .andExpect(status().isInternalServerError());
    }

    @Test
    @DisplayName("DELETE /api/v1/chatMessages/{id} - Should return bad request for null ID")
    void testDeleteChatMessage_NullId() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.delete(CHAT_MESSAGE_BASE_URL + "/null"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /api/v1/chatMessages/{chatMessageId}/likes - Should return bad request for null ID")
    void testGetChatMessageLikes_NullId() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get(CHAT_MESSAGE_BASE_URL + "/null/likes"))
                .andExpect(status().isBadRequest());
    }
} 