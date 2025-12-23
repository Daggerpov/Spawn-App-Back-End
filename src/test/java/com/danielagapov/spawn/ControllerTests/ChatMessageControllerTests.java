package com.danielagapov.spawn.ControllerTests;

import com.danielagapov.spawn.chat.api.ChatMessageController;
import com.danielagapov.spawn.chat.api.dto.*;
import com.danielagapov.spawn.user.api.dto.BaseUserDTO;
import com.danielagapov.spawn.shared.util.EntityType;
import com.danielagapov.spawn.shared.exceptions.Base.BaseNotFoundException;
import com.danielagapov.spawn.shared.exceptions.Logger.ILogger;
import com.danielagapov.spawn.chat.internal.services.IChatMessageService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
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

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Comprehensive unit tests for ChatMessageController
 * Tests chat message creation, deletion, and likes functionality
 */
@ExtendWith(MockitoExtension.class)
class ChatMessageControllerTests {

    @Mock
    private IChatMessageService chatMessageService;

    @Mock
    private ILogger logger;

    @InjectMocks
    private ChatMessageController chatMessageController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;
    private UUID chatMessageId;
    private UUID userId;
    private UUID activityId;
    private CreateChatMessageDTO createChatMessageDTO;
    private FullActivityChatMessageDTO fullActivityChatMessageDTO;
    private ChatMessageLikesDTO chatMessageLikesDTO;
    private BaseUserDTO baseUserDTO;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        
        mockMvc = MockMvcBuilders.standaloneSetup(chatMessageController)
                .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
                .build();
        
        chatMessageId = UUID.randomUUID();
        userId = UUID.randomUUID();
        activityId = UUID.randomUUID();
        
        createChatMessageDTO = new CreateChatMessageDTO(
            "Test message content",
            activityId,
            userId
        );
        
        // BaseUserDTO constructor: (id, name, email, username, bio, profilePicture)
        baseUserDTO = new BaseUserDTO(userId, "Test User", "test@example.com", "testuser", "Test bio", "pic.jpg");
        
        // FullActivityChatMessageDTO constructor: (id, content, timestamp, senderUser, activityId, likedByUsers)
        fullActivityChatMessageDTO = new FullActivityChatMessageDTO(
            chatMessageId,
            "Test message content",
            Instant.now(),
            baseUserDTO,
            activityId,
            List.of()
        );
        
        // ChatMessageLikesDTO constructor: (chatMessageId, userId)
        chatMessageLikesDTO = new ChatMessageLikesDTO(
            chatMessageId,
            userId
        );
    }

    // MARK: - Create Chat Message Tests

    @Test
    void createChatMessage_ShouldReturnCreated_WhenValidMessage() throws Exception {
        when(chatMessageService.createChatMessage(any(CreateChatMessageDTO.class)))
                .thenReturn(fullActivityChatMessageDTO);

        mockMvc.perform(post("/api/v1/chat-messages")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createChatMessageDTO)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(chatMessageId.toString()))
                .andExpect(jsonPath("$.content").value("Test message content"))
                .andExpect(jsonPath("$.activityId").value(activityId.toString()));

        verify(chatMessageService, times(1)).createChatMessage(any(CreateChatMessageDTO.class));
    }

    @Test
    void createChatMessage_ShouldReturnInternalServerError_WhenServiceFails() throws Exception {
        when(chatMessageService.createChatMessage(any(CreateChatMessageDTO.class)))
                .thenThrow(new RuntimeException("Database error"));

        mockMvc.perform(post("/api/v1/chat-messages")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createChatMessageDTO)))
                .andExpect(status().isInternalServerError());

        verify(logger, times(1)).error(contains("Error creating chat message"));
    }

    @Test
    void createChatMessage_ShouldHandleEmptyContent_WhenContentIsEmpty() throws Exception {
        CreateChatMessageDTO emptyContentDTO = new CreateChatMessageDTO("", activityId, userId);
        
        when(chatMessageService.createChatMessage(any(CreateChatMessageDTO.class)))
                .thenReturn(fullActivityChatMessageDTO);

        mockMvc.perform(post("/api/v1/chat-messages")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(emptyContentDTO)))
                .andExpect(status().isCreated());

        verify(chatMessageService, times(1)).createChatMessage(any(CreateChatMessageDTO.class));
    }

    @Test
    void createChatMessage_ShouldHandleLongContent_WhenContentIsVeryLong() throws Exception {
        String longContent = "a".repeat(1000);
        CreateChatMessageDTO longContentDTO = new CreateChatMessageDTO(longContent, activityId, userId);
        FullActivityChatMessageDTO longMessageResponse = new FullActivityChatMessageDTO(
            chatMessageId, longContent, Instant.now(), baseUserDTO, activityId, List.of()
        );
        
        when(chatMessageService.createChatMessage(any(CreateChatMessageDTO.class)))
                .thenReturn(longMessageResponse);

        mockMvc.perform(post("/api/v1/chat-messages")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(longContentDTO)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.content").value(longContent));

        verify(chatMessageService, times(1)).createChatMessage(any(CreateChatMessageDTO.class));
    }

    // MARK: - Delete Chat Message Tests

    @Test
    void deleteChatMessage_ShouldReturnNoContent_WhenSuccessful() throws Exception {
        when(chatMessageService.deleteChatMessageById(chatMessageId)).thenReturn(true);

        mockMvc.perform(delete("/api/v1/chat-messages/{id}", chatMessageId))
                .andExpect(status().isNoContent());

        verify(chatMessageService, times(1)).deleteChatMessageById(chatMessageId);
    }

    @Test
    void deleteChatMessage_ShouldReturnBadRequest_WhenNullId() throws Exception {
        mockMvc.perform(delete("/api/v1/chat-messages/{id}", (Object) null))
                .andExpect(status().isBadRequest());

        verify(chatMessageService, never()).deleteChatMessageById(any());
        verify(logger, times(1)).error(contains("chat message ID is null"));
    }

    @Test
    void deleteChatMessage_ShouldReturnNotFound_WhenMessageNotFound() throws Exception {
        when(chatMessageService.deleteChatMessageById(chatMessageId))
                .thenThrow(new BaseNotFoundException(EntityType.ChatMessage, chatMessageId));

        mockMvc.perform(delete("/api/v1/chat-messages/{id}", chatMessageId))
                .andExpect(status().isNotFound());

        verify(logger, times(1)).error(contains("Chat message not found for deletion"));
    }

    @Test
    void deleteChatMessage_ShouldReturnInternalServerError_WhenDeletionFails() throws Exception {
        when(chatMessageService.deleteChatMessageById(chatMessageId)).thenReturn(false);

        mockMvc.perform(delete("/api/v1/chat-messages/{id}", chatMessageId))
                .andExpect(status().isInternalServerError());

        verify(logger, times(1)).error(contains("Failed to delete chat message"));
    }

    @Test
    void deleteChatMessage_ShouldReturnInternalServerError_WhenExceptionThrown() throws Exception {
        when(chatMessageService.deleteChatMessageById(chatMessageId))
                .thenThrow(new RuntimeException("Unexpected error"));

        mockMvc.perform(delete("/api/v1/chat-messages/{id}", chatMessageId))
                .andExpect(status().isInternalServerError());

        verify(logger, times(1)).error(contains("Error deleting chat message"));
    }

    // MARK: - Create Chat Message Like Tests

    @Test
    void createChatMessageLike_ShouldReturnCreated_WhenSuccessful() throws Exception {
        when(chatMessageService.createChatMessageLike(chatMessageId, userId))
                .thenReturn(chatMessageLikesDTO);

        mockMvc.perform(post("/api/v1/chat-messages/{chatMessageId}/likes/{userId}", chatMessageId, userId))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.chatMessageId").value(chatMessageId.toString()))
                .andExpect(jsonPath("$.userId").value(userId.toString()));

        verify(chatMessageService, times(1)).createChatMessageLike(chatMessageId, userId);
    }

    @Test
    void createChatMessageLike_ShouldReturnBadRequest_WhenNullParameters() throws Exception {
        mockMvc.perform(post("/api/v1/chat-messages/{chatMessageId}/likes/{userId}", chatMessageId, (Object) null))
                .andExpect(status().isBadRequest());

        verify(chatMessageService, never()).createChatMessageLike(any(), any());
        verify(logger, times(1)).error(contains("chatMessageId or userId is null"));
    }

    @Test
    void createChatMessageLike_ShouldReturnNotFound_WhenMessageNotFound() throws Exception {
        when(chatMessageService.createChatMessageLike(chatMessageId, userId))
                .thenThrow(new BaseNotFoundException(EntityType.ChatMessage, chatMessageId));

        mockMvc.perform(post("/api/v1/chat-messages/{chatMessageId}/likes/{userId}", chatMessageId, userId))
                .andExpect(status().isNotFound());

        verify(logger, times(1)).error(contains("Chat message or user not found"));
    }

    @Test
    void createChatMessageLike_ShouldReturnNotFound_WhenUserNotFound() throws Exception {
        when(chatMessageService.createChatMessageLike(chatMessageId, userId))
                .thenThrow(new BaseNotFoundException(EntityType.User, userId));

        mockMvc.perform(post("/api/v1/chat-messages/{chatMessageId}/likes/{userId}", chatMessageId, userId))
                .andExpect(status().isNotFound());

        verify(logger, times(1)).error(contains("Chat message or user not found"));
    }

    @Test
    void createChatMessageLike_ShouldReturnInternalServerError_WhenServiceFails() throws Exception {
        when(chatMessageService.createChatMessageLike(chatMessageId, userId))
                .thenThrow(new RuntimeException("Database error"));

        mockMvc.perform(post("/api/v1/chat-messages/{chatMessageId}/likes/{userId}", chatMessageId, userId))
                .andExpect(status().isInternalServerError());

        verify(logger, times(1)).error(contains("Error creating chat message like"));
    }

    // MARK: - Get Chat Message Likes Tests

    @Test
    void getChatMessageLikes_ShouldReturnLikes_WhenLikesExist() throws Exception {
        List<BaseUserDTO> likes = List.of(baseUserDTO);
        when(chatMessageService.getChatMessageLikes(chatMessageId)).thenReturn(likes);

        mockMvc.perform(get("/api/v1/chat-messages/{chatMessageId}/likes", chatMessageId))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(userId.toString()));

        verify(chatMessageService, times(1)).getChatMessageLikes(chatMessageId);
    }

    @Test
    void getChatMessageLikes_ShouldReturnEmptyList_WhenNoLikes() throws Exception {
        when(chatMessageService.getChatMessageLikes(chatMessageId)).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/chat-messages/{chatMessageId}/likes", chatMessageId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        verify(chatMessageService, times(1)).getChatMessageLikes(chatMessageId);
    }

    @Test
    void getChatMessageLikes_ShouldReturnBadRequest_WhenNullId() throws Exception {
        mockMvc.perform(get("/api/v1/chat-messages/{chatMessageId}/likes", (Object) null))
                .andExpect(status().isBadRequest());

        verify(chatMessageService, never()).getChatMessageLikes(any());
        verify(logger, times(1)).error(contains("chatMessageId is null"));
    }

    @Test
    void getChatMessageLikes_ShouldReturnNotFound_WhenMessageNotFound() throws Exception {
        when(chatMessageService.getChatMessageLikes(chatMessageId))
                .thenThrow(new BaseNotFoundException(EntityType.ChatMessage, chatMessageId));

        mockMvc.perform(get("/api/v1/chat-messages/{chatMessageId}/likes", chatMessageId))
                .andExpect(status().isNotFound());

        verify(logger, times(1)).error(contains("Chat message not found for likes retrieval"));
    }

    @Test
    void getChatMessageLikes_ShouldReturnInternalServerError_WhenServiceFails() throws Exception {
        when(chatMessageService.getChatMessageLikes(chatMessageId))
                .thenThrow(new RuntimeException("Database error"));

        mockMvc.perform(get("/api/v1/chat-messages/{chatMessageId}/likes", chatMessageId))
                .andExpect(status().isInternalServerError());

        verify(logger, times(1)).error(contains("Error getting chat message likes"));
    }

    // MARK: - Delete Chat Message Like Tests

    @Test
    void deleteChatMessageLike_ShouldReturnNoContent_WhenSuccessful() throws Exception {
        doNothing().when(chatMessageService).deleteChatMessageLike(chatMessageId, userId);

        mockMvc.perform(delete("/api/v1/chat-messages/{chatMessageId}/likes/{userId}", chatMessageId, userId))
                .andExpect(status().isNoContent());

        verify(chatMessageService, times(1)).deleteChatMessageLike(chatMessageId, userId);
    }

    @Test
    void deleteChatMessageLike_ShouldReturnBadRequest_WhenNullParameters() throws Exception {
        mockMvc.perform(delete("/api/v1/chat-messages/{chatMessageId}/likes/{userId}", chatMessageId, (Object) null))
                .andExpect(status().isBadRequest());

        verify(chatMessageService, never()).deleteChatMessageLike(any(), any());
        verify(logger, times(1)).error(contains("chatMessageId or userId is null"));
    }

    @Test
    void deleteChatMessageLike_ShouldReturnNotFound_WhenLikeNotFound() throws Exception {
        doThrow(new BaseNotFoundException(EntityType.ChatMessageLike, chatMessageId))
                .when(chatMessageService).deleteChatMessageLike(chatMessageId, userId);

        mockMvc.perform(delete("/api/v1/chat-messages/{chatMessageId}/likes/{userId}", chatMessageId, userId))
                .andExpect(status().isNotFound());

        verify(logger, times(1)).error(contains("Chat message like not found for deletion"));
    }

    @Test
    void deleteChatMessageLike_ShouldReturnInternalServerError_WhenServiceFails() throws Exception {
        doThrow(new RuntimeException("Database error"))
                .when(chatMessageService).deleteChatMessageLike(chatMessageId, userId);

        mockMvc.perform(delete("/api/v1/chat-messages/{chatMessageId}/likes/{userId}", chatMessageId, userId))
                .andExpect(status().isInternalServerError());

        verify(logger, times(1)).error(contains("Error deleting chat message like"));
    }

    // MARK: - Direct Controller Method Tests

    @Test
    void createChatMessage_DirectCall_ShouldReturnCreated_WhenSuccessful() {
        when(chatMessageService.createChatMessage(any(CreateChatMessageDTO.class)))
                .thenReturn(fullActivityChatMessageDTO);

        ResponseEntity<FullActivityChatMessageDTO> response = 
            chatMessageController.createChatMessage(createChatMessageDTO);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(chatMessageId, response.getBody().getId());
        verify(chatMessageService, times(1)).createChatMessage(any(CreateChatMessageDTO.class));
    }

    @Test
    void deleteChatMessage_DirectCall_ShouldReturnNoContent_WhenSuccessful() {
        when(chatMessageService.deleteChatMessageById(chatMessageId)).thenReturn(true);

        ResponseEntity<Void> response = chatMessageController.deleteChatMessage(chatMessageId);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(chatMessageService, times(1)).deleteChatMessageById(chatMessageId);
    }

    @Test
    void createChatMessageLike_DirectCall_ShouldReturnCreated_WhenSuccessful() {
        when(chatMessageService.createChatMessageLike(chatMessageId, userId))
                .thenReturn(chatMessageLikesDTO);

        ResponseEntity<ChatMessageLikesDTO> response = 
            chatMessageController.createChatMessageLike(chatMessageId, userId);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        verify(chatMessageService, times(1)).createChatMessageLike(chatMessageId, userId);
    }

    @Test
    void getChatMessageLikes_DirectCall_ShouldReturnOk_WhenLikesExist() {
        List<BaseUserDTO> likes = List.of(baseUserDTO);
        when(chatMessageService.getChatMessageLikes(chatMessageId)).thenReturn(likes);

        ResponseEntity<List<BaseUserDTO>> response = 
            chatMessageController.getChatMessageLikes(chatMessageId);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().size());
        verify(chatMessageService, times(1)).getChatMessageLikes(chatMessageId);
    }

    @Test
    void deleteChatMessageLike_DirectCall_ShouldReturnNoContent_WhenSuccessful() {
        doNothing().when(chatMessageService).deleteChatMessageLike(chatMessageId, userId);

        ResponseEntity<?> response = chatMessageController.deleteChatMessageLike(chatMessageId, userId);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(chatMessageService, times(1)).deleteChatMessageLike(chatMessageId, userId);
    }

    // MARK: - Edge Case Tests

    @Test
    void createChatMessage_ShouldHandleSpecialCharacters_WhenContentHasSpecialChars() throws Exception {
        String specialContent = "Test 🎉 message with émojis & spëcial çhars!";
        CreateChatMessageDTO specialDTO = new CreateChatMessageDTO(specialContent, activityId, userId);
        FullActivityChatMessageDTO specialResponse = new FullActivityChatMessageDTO(
            chatMessageId, specialContent, Instant.now(), baseUserDTO, activityId, List.of()
        );
        
        when(chatMessageService.createChatMessage(any(CreateChatMessageDTO.class)))
                .thenReturn(specialResponse);

        mockMvc.perform(post("/api/v1/chat-messages")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(specialDTO)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.content").value(specialContent));

        verify(chatMessageService, times(1)).createChatMessage(any(CreateChatMessageDTO.class));
    }

    @Test
    void getChatMessageLikes_ShouldHandleMultipleLikes_WhenManyUsersLiked() throws Exception {
        List<BaseUserDTO> manyLikes = List.of(
            new BaseUserDTO(UUID.randomUUID(), "User 1", "user1@test.com", "user1", "Bio 1", "pic1.jpg"),
            new BaseUserDTO(UUID.randomUUID(), "User 2", "user2@test.com", "user2", "Bio 2", "pic2.jpg"),
            new BaseUserDTO(UUID.randomUUID(), "User 3", "user3@test.com", "user3", "Bio 3", "pic3.jpg")
        );
        
        when(chatMessageService.getChatMessageLikes(chatMessageId)).thenReturn(manyLikes);

        mockMvc.perform(get("/api/v1/chat-messages/{chatMessageId}/likes", chatMessageId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3));

        verify(chatMessageService, times(1)).getChatMessageLikes(chatMessageId);
    }

    @Test
    void createChatMessageLike_ShouldHandleDuplicateLike_WhenUserAlreadyLiked() throws Exception {
        when(chatMessageService.createChatMessageLike(chatMessageId, userId))
                .thenThrow(new RuntimeException("Duplicate like"));

        mockMvc.perform(post("/api/v1/chat-messages/{chatMessageId}/likes/{userId}", chatMessageId, userId))
                .andExpect(status().isInternalServerError());

        verify(logger, times(1)).error(contains("Error creating chat message like"));
    }

    @Test
    void createChatMessage_ShouldHandleConcurrentMessages_WhenMultipleUsersPost() throws Exception {
        when(chatMessageService.createChatMessage(any(CreateChatMessageDTO.class)))
                .thenReturn(fullActivityChatMessageDTO);

        // Simulate concurrent message creation
        mockMvc.perform(post("/api/v1/chat-messages")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createChatMessageDTO)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/chat-messages")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createChatMessageDTO)))
                .andExpect(status().isCreated());

        verify(chatMessageService, times(2)).createChatMessage(any(CreateChatMessageDTO.class));
    }
}

