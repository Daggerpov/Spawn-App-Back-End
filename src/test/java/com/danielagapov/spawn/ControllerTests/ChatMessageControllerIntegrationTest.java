package com.danielagapov.spawn.ControllerTests;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.util.UUID;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("Chat Message Controller Integration Tests")
public class ChatMessageControllerIntegrationTest extends BaseIntegrationTest {

    private static final String CHAT_MESSAGE_BASE_URL = "/api/v1/chatMessages";
    private UUID testUserId = UUID.randomUUID();
    private UUID testActivityId = UUID.randomUUID();
    private UUID testChatMessageId = UUID.randomUUID();

    @Override
    protected void setupTestData() {
        // Setup test chat messages and users for testing
    }

    @Test
    @DisplayName("POST /api/v1/chatMessages - Should create chat message successfully")
    void testCreateChatMessage_Success() throws Exception {
        String chatMessageJson = "{"
                + "\"content\":\"Test message content\","
                + "\"fromUserId\":\"" + testUserId + "\","
                + "\"activityId\":\"" + testActivityId + "\""
                + "}";

        mockMvc.perform(MockMvcRequestBuilders.post(CHAT_MESSAGE_BASE_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(chatMessageJson))
                .andExpect(status().isCreated());
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
                .andExpect(status().isOk());
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
        mockMvc.perform(MockMvcRequestBuilders.delete(CHAT_MESSAGE_BASE_URL + "/" + testChatMessageId + "/likes/" + testUserId))
                .andExpect(status().isOk());
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