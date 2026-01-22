package com.danielagapov.spawn.ControllerTests;

import com.danielagapov.spawn.social.api.FriendRequestController;
import com.danielagapov.spawn.social.api.dto.CreateFriendRequestDTO;
import com.danielagapov.spawn.social.api.dto.FetchFriendRequestDTO;
import com.danielagapov.spawn.social.api.dto.FetchSentFriendRequestDTO;
import com.danielagapov.spawn.user.api.dto.BaseUserDTO;
import com.danielagapov.spawn.shared.util.EntityType;
import com.danielagapov.spawn.shared.util.FriendRequestAction;
import com.danielagapov.spawn.shared.exceptions.Base.BaseNotFoundException;
import com.danielagapov.spawn.shared.exceptions.Base.BasesNotFoundException;
import com.danielagapov.spawn.shared.exceptions.Logger.ILogger;
import com.danielagapov.spawn.social.internal.services.IBlockedUserService;
import com.danielagapov.spawn.social.internal.services.IFriendRequestService;
import com.fasterxml.jackson.databind.ObjectMapper;
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

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Comprehensive unit tests for FriendRequestController
 * Tests all friend request API endpoints including creation, acceptance, rejection, and retrieval
 */
@ExtendWith(MockitoExtension.class)
class FriendRequestControllerTests {

    @Mock
    private IFriendRequestService friendRequestService;

    @Mock
    private IBlockedUserService blockedUserService;

    @Mock
    private ILogger logger;

    @InjectMocks
    private FriendRequestController friendRequestController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;
    
    private UUID senderId;
    private UUID receiverId;
    private UUID friendRequestId;
    private CreateFriendRequestDTO createFriendRequestDTO;
    private FetchFriendRequestDTO fetchFriendRequestDTO;
    private FetchSentFriendRequestDTO fetchSentFriendRequestDTO;
    private BaseUserDTO senderUserDTO;
    private BaseUserDTO receiverUserDTO;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        mockMvc = MockMvcBuilders.standaloneSetup(friendRequestController)
                .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
                .build();

        senderId = UUID.randomUUID();
        receiverId = UUID.randomUUID();
        friendRequestId = UUID.randomUUID();

        senderUserDTO = new BaseUserDTO(
            senderId, "Sender User", "sender@example.com", "sender_user", "Sender bio", "sender_pic.jpg"
        );
        
        receiverUserDTO = new BaseUserDTO(
            receiverId, "Receiver User", "receiver@example.com", "receiver_user", "Receiver bio", "receiver_pic.jpg"
        );

        createFriendRequestDTO = new CreateFriendRequestDTO(friendRequestId, senderId, receiverId);
        fetchFriendRequestDTO = new FetchFriendRequestDTO(friendRequestId, senderUserDTO, 5);
        fetchSentFriendRequestDTO = new FetchSentFriendRequestDTO(friendRequestId, receiverUserDTO);
    }

    // MARK: - GET Incoming Friend Requests Tests

    @Test
    void getIncomingFriendRequestsByUserId_ShouldReturnRequests_WhenRequestsExist() throws Exception {
        // Arrange
        List<FetchFriendRequestDTO> incomingRequests = List.of(fetchFriendRequestDTO);
        List<FetchFriendRequestDTO> filteredRequests = List.of(fetchFriendRequestDTO);
        
        when(friendRequestService.getIncomingFetchFriendRequestsByUserId(receiverId))
            .thenReturn(incomingRequests);
        when(blockedUserService.filterOutBlockedUsers(incomingRequests, receiverId))
            .thenReturn(filteredRequests);

        // Act & Assert
        mockMvc.perform(get("/api/v1/friend-requests/incoming/{userId}", receiverId))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(friendRequestId.toString()))
                .andExpect(jsonPath("$[0].senderUser.id").value(senderId.toString()))
                .andExpect(jsonPath("$[0].senderUser.username").value("sender_user"))
                .andExpect(jsonPath("$[0].mutualFriendCount").value(5));

        verify(friendRequestService).getIncomingFetchFriendRequestsByUserId(receiverId);
        verify(blockedUserService).filterOutBlockedUsers(incomingRequests, receiverId);
    }

    @Test
    void getIncomingFriendRequestsByUserId_ShouldReturnEmptyList_WhenNoRequestsExist() throws Exception {
        // Arrange
        when(friendRequestService.getIncomingFetchFriendRequestsByUserId(receiverId))
            .thenReturn(List.of());
        when(blockedUserService.filterOutBlockedUsers(List.of(), receiverId))
            .thenReturn(List.of());

        // Act & Assert
        mockMvc.perform(get("/api/v1/friend-requests/incoming/{userId}", receiverId))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.length()").value(0));

        verify(friendRequestService).getIncomingFetchFriendRequestsByUserId(receiverId);
    }

    @Test
    void getIncomingFriendRequestsByUserId_ShouldReturnNotFound_WhenUserNotFound() throws Exception {
        // Arrange
        BaseNotFoundException exception = new BaseNotFoundException(EntityType.User);
        when(friendRequestService.getIncomingFetchFriendRequestsByUserId(receiverId))
            .thenThrow(exception);

        // Act & Assert
        mockMvc.perform(get("/api/v1/friend-requests/incoming/{userId}", receiverId))
                .andExpect(status().isNotFound());

        verify(logger).error(contains("User not found for incoming friend requests"));
    }

    @Test
    void getIncomingFriendRequestsByUserId_ShouldReturnEmptyList_WhenFriendRequestsNotFound() throws Exception {
        // Arrange
        BasesNotFoundException exception = new BasesNotFoundException(EntityType.FriendRequest);
        when(friendRequestService.getIncomingFetchFriendRequestsByUserId(receiverId))
            .thenThrow(exception);

        // Act & Assert
        mockMvc.perform(get("/api/v1/friend-requests/incoming/{userId}", receiverId))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void getIncomingFriendRequestsByUserId_ShouldReturnBadRequest_WhenNullUserId() throws Exception {
        // Act & Assert - Spring automatically handles UUID conversion, but we can test invalid format
        mockMvc.perform(get("/api/v1/friend-requests/incoming/{userId}", "invalid-uuid"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getIncomingFriendRequestsByUserId_ShouldReturnInternalServerError_WhenUnexpectedException() throws Exception {
        // Arrange
        when(friendRequestService.getIncomingFetchFriendRequestsByUserId(receiverId))
            .thenThrow(new RuntimeException("Database connection failed"));

        // Act & Assert
        mockMvc.perform(get("/api/v1/friend-requests/incoming/{userId}", receiverId))
                .andExpect(status().isInternalServerError());

        verify(logger).error(contains("Error getting incoming friend requests for user"));
    }

    // MARK: - GET Sent Friend Requests Tests

    @Test
    void getSentFriendRequestsByUserId_ShouldReturnRequests_WhenRequestsExist() throws Exception {
        // Arrange
        List<FetchSentFriendRequestDTO> sentRequests = List.of(fetchSentFriendRequestDTO);
        List<FetchSentFriendRequestDTO> filteredRequests = List.of(fetchSentFriendRequestDTO);
        
        when(friendRequestService.getSentFetchFriendRequestsByUserId(senderId))
            .thenReturn(sentRequests);
        when(blockedUserService.filterOutBlockedUsers(sentRequests, senderId))
            .thenReturn(filteredRequests);

        // Act & Assert
        mockMvc.perform(get("/api/v1/friend-requests/sent/{userId}", senderId))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(friendRequestId.toString()));

        verify(friendRequestService).getSentFetchFriendRequestsByUserId(senderId);
        verify(blockedUserService).filterOutBlockedUsers(sentRequests, senderId);
    }

    @Test
    void getSentFriendRequestsByUserId_ShouldReturnEmptyList_WhenNoRequestsExist() throws Exception {
        // Arrange
        when(friendRequestService.getSentFetchFriendRequestsByUserId(senderId))
            .thenReturn(List.of());
        when(blockedUserService.filterOutBlockedUsers(List.of(), senderId))
            .thenReturn(List.of());

        // Act & Assert
        mockMvc.perform(get("/api/v1/friend-requests/sent/{userId}", senderId))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void getSentFriendRequestsByUserId_ShouldReturnNotFound_WhenUserNotFound() throws Exception {
        // Arrange
        BaseNotFoundException exception = new BaseNotFoundException(EntityType.User);
        when(friendRequestService.getSentFetchFriendRequestsByUserId(senderId))
            .thenThrow(exception);

        // Act & Assert
        mockMvc.perform(get("/api/v1/friend-requests/sent/{userId}", senderId))
                .andExpect(status().isNotFound());

        verify(logger).error(contains("User not found for sent friend requests"));
    }

    // MARK: - POST Create Friend Request Tests

    @Test
    void createFriendRequest_ShouldReturnCreatedRequest_WhenValidRequest() throws Exception {
        // Arrange
        when(friendRequestService.saveFriendRequest(any(CreateFriendRequestDTO.class)))
            .thenReturn(createFriendRequestDTO);

        // Act & Assert
        mockMvc.perform(post("/api/v1/friend-requests")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createFriendRequestDTO)))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(friendRequestId.toString()))
                .andExpect(jsonPath("$.senderUserId").value(senderId.toString()))
                .andExpect(jsonPath("$.receiverUserId").value(receiverId.toString()));

        verify(friendRequestService).saveFriendRequest(any(CreateFriendRequestDTO.class));
    }

    @Test
    void createFriendRequest_ShouldReturnInternalServerError_WhenServiceThrowsException() throws Exception {
        // Arrange
        when(friendRequestService.saveFriendRequest(any(CreateFriendRequestDTO.class)))
            .thenThrow(new RuntimeException("Database error"));

        // Act & Assert
        mockMvc.perform(post("/api/v1/friend-requests")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createFriendRequestDTO)))
                .andExpect(status().isInternalServerError());

        verify(logger).error(contains("Error creating friend request"));
    }

    @Test
    void createFriendRequest_ShouldReturnBadRequest_WhenInvalidJson() throws Exception {
        // Act & Assert
        mockMvc.perform(post("/api/v1/friend-requests")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{invalid json}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createFriendRequest_ShouldReturnBadRequest_WhenMissingRequestBody() throws Exception {
        // Act & Assert
        mockMvc.perform(post("/api/v1/friend-requests")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    // MARK: - PUT Friend Request Action Tests (Accept/Reject)

    @Test
    void friendRequestAction_ShouldAcceptRequest_WhenAcceptActionProvided() throws Exception {
        // Arrange
        doNothing().when(friendRequestService).acceptFriendRequest(friendRequestId);

        // Act & Assert
        mockMvc.perform(put("/api/v1/friend-requests/{friendRequestId}", friendRequestId)
                .param("friendRequestAction", "accept"))
                .andExpect(status().isNoContent());

        verify(friendRequestService).acceptFriendRequest(friendRequestId);
        verify(logger).info(contains("Processing friend request action: accept"));
        verify(logger).info(contains("Successfully accepted friend request"));
    }

    @Test
    void friendRequestAction_ShouldRejectRequest_WhenRejectActionProvided() throws Exception {
        // Arrange
        doNothing().when(friendRequestService).deleteFriendRequest(friendRequestId);

        // Act & Assert
        mockMvc.perform(put("/api/v1/friend-requests/{friendRequestId}", friendRequestId)
                .param("friendRequestAction", "reject"))
                .andExpect(status().isNoContent());

        verify(friendRequestService).deleteFriendRequest(friendRequestId);
        verify(logger).info(contains("Processing friend request action: reject"));
        verify(logger).info(contains("Successfully rejected friend request"));
    }

    @Test
    void friendRequestAction_ShouldReturnBadRequest_WhenInvalidAction() throws Exception {
        // Act & Assert
        mockMvc.perform(put("/api/v1/friend-requests/{friendRequestId}", friendRequestId)
                .param("friendRequestAction", "invalid_action"))
                .andExpect(status().isBadRequest());

        // Spring handles invalid enum conversion and returns 400 before controller logic
        verifyNoInteractions(friendRequestService);
    }

    @Test
    void friendRequestAction_ShouldReturnBadRequest_WhenMissingAction() throws Exception {
        // Act & Assert
        mockMvc.perform(put("/api/v1/friend-requests/{friendRequestId}", friendRequestId))
                .andExpect(status().isBadRequest());
    }

    @Test
    void friendRequestAction_ShouldReturnNoContent_WhenFriendRequestNotFound() throws Exception {
        // Arrange - Controller handles this gracefully to avoid client-side errors
        BaseNotFoundException exception = new BaseNotFoundException(EntityType.FriendRequest);
        doThrow(exception).when(friendRequestService).acceptFriendRequest(friendRequestId);

        // Act & Assert
        mockMvc.perform(put("/api/v1/friend-requests/{friendRequestId}", friendRequestId)
                .param("friendRequestAction", "accept"))
                .andExpect(status().isNoContent());

        verify(logger).warn(contains("Friend request not found (may have been already processed)"));
    }

    @Test
    void friendRequestAction_ShouldReturnInternalServerError_WhenUnexpectedException() throws Exception {
        // Arrange
        doThrow(new RuntimeException("Database error"))
            .when(friendRequestService).acceptFriendRequest(friendRequestId);

        // Act & Assert
        mockMvc.perform(put("/api/v1/friend-requests/{friendRequestId}", friendRequestId)
                .param("friendRequestAction", "accept"))
                .andExpect(status().isInternalServerError());

        verify(logger).error(contains("Error processing friend request action"));
    }

    @Test
    void friendRequestAction_ShouldReturnBadRequest_WhenInvalidFriendRequestId() throws Exception {
        // Act & Assert
        mockMvc.perform(put("/api/v1/friend-requests/{friendRequestId}", "invalid-uuid")
                .param("friendRequestAction", "accept"))
                .andExpect(status().isBadRequest());
    }

    // MARK: - DELETE Friend Request Tests

    @Test
    void deleteFriendRequest_ShouldDeleteRequest_WhenValidRequest() throws Exception {
        // Arrange
        doNothing().when(friendRequestService).deleteFriendRequest(friendRequestId);

        // Act & Assert
        mockMvc.perform(delete("/api/v1/friend-requests/{friendRequestId}", friendRequestId))
                .andExpect(status().isNoContent());

        verify(friendRequestService).deleteFriendRequest(friendRequestId);
    }

    @Test
    void deleteFriendRequest_ShouldReturnNoContent_WhenFriendRequestNotFound() throws Exception {
        // Arrange - Controller handles this gracefully for idempotent behavior
        BaseNotFoundException exception = new BaseNotFoundException(EntityType.FriendRequest);
        doThrow(exception).when(friendRequestService).deleteFriendRequest(friendRequestId);

        // Act & Assert
        mockMvc.perform(delete("/api/v1/friend-requests/{friendRequestId}", friendRequestId))
                .andExpect(status().isNoContent());

        verify(logger).warn(contains("Friend request not found (may have been already deleted)"));
    }

    @Test
    void deleteFriendRequest_ShouldReturnInternalServerError_WhenUnexpectedException() throws Exception {
        // Arrange
        doThrow(new RuntimeException("Database error"))
            .when(friendRequestService).deleteFriendRequest(friendRequestId);

        // Act & Assert
        mockMvc.perform(delete("/api/v1/friend-requests/{friendRequestId}", friendRequestId))
                .andExpect(status().isInternalServerError());

        verify(logger).error(contains("Error deleting friend request"));
    }

    @Test
    void deleteFriendRequest_ShouldReturnBadRequest_WhenInvalidFriendRequestId() throws Exception {
        // Act & Assert
        mockMvc.perform(delete("/api/v1/friend-requests/{friendRequestId}", "invalid-uuid"))
                .andExpect(status().isBadRequest());
    }

    // MARK: - Direct Controller Method Tests

    @Test
    void getIncomingFriendRequestsByUserId_DirectCall_ShouldReturnRequests_WhenServiceSucceeds() {
        // Arrange
        List<FetchFriendRequestDTO> incomingRequests = List.of(fetchFriendRequestDTO);
        List<FetchFriendRequestDTO> filteredRequests = List.of(fetchFriendRequestDTO);
        
        when(friendRequestService.getIncomingFetchFriendRequestsByUserId(receiverId))
            .thenReturn(incomingRequests);
        when(blockedUserService.filterOutBlockedUsers(incomingRequests, receiverId))
            .thenReturn(filteredRequests);

        // Act
        ResponseEntity<?> response = friendRequestController.getIncomingFriendRequestsByUserId(receiverId);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        @SuppressWarnings("unchecked")
        List<FetchFriendRequestDTO> responseBody = (List<FetchFriendRequestDTO>) response.getBody();
        assertEquals(1, responseBody.size());
        assertEquals(friendRequestId, responseBody.get(0).getId());
    }

    @Test
    void createFriendRequest_DirectCall_ShouldReturnCreatedRequest_WhenServiceSucceeds() {
        // Arrange
        when(friendRequestService.saveFriendRequest(createFriendRequestDTO))
            .thenReturn(createFriendRequestDTO);

        // Act
        ResponseEntity<CreateFriendRequestDTO> response = 
            friendRequestController.createFriendRequest(createFriendRequestDTO);

        // Assert
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(friendRequestId, response.getBody().getId());
        assertEquals(senderId, response.getBody().getSenderUserId());
        assertEquals(receiverId, response.getBody().getReceiverUserId());
    }

    @Test
    void friendRequestAction_DirectCall_ShouldReturnNoContent_WhenAcceptActionSucceeds() {
        // Arrange
        doNothing().when(friendRequestService).acceptFriendRequest(friendRequestId);

        // Act
        ResponseEntity<?> response = friendRequestController
            .friendRequestAction(friendRequestId, FriendRequestAction.accept);

        // Assert
        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(friendRequestService).acceptFriendRequest(friendRequestId);
    }

    @Test
    void friendRequestAction_DirectCall_ShouldReturnNoContent_WhenRejectActionSucceeds() {
        // Arrange
        doNothing().when(friendRequestService).deleteFriendRequest(friendRequestId);

        // Act
        ResponseEntity<?> response = friendRequestController
            .friendRequestAction(friendRequestId, FriendRequestAction.reject);

        // Assert
        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(friendRequestService).deleteFriendRequest(friendRequestId);
    }

    @Test
    void deleteFriendRequest_DirectCall_ShouldReturnNoContent_WhenServiceSucceeds() {
        // Arrange
        doNothing().when(friendRequestService).deleteFriendRequest(friendRequestId);

        // Act
        ResponseEntity<?> response = friendRequestController.deleteFriendRequest(friendRequestId);

        // Assert
        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(friendRequestService).deleteFriendRequest(friendRequestId);
    }

    // MARK: - Edge Case and Integration Tests

    @Test
    void getIncomingFriendRequestsByUserId_ShouldFilterBlockedUsers_WhenBlockedUsersExist() throws Exception {
        // Arrange
        List<FetchFriendRequestDTO> incomingRequests = List.of(fetchFriendRequestDTO);
        List<FetchFriendRequestDTO> filteredRequests = List.of(); // Blocked user filtered out
        
        when(friendRequestService.getIncomingFetchFriendRequestsByUserId(receiverId))
            .thenReturn(incomingRequests);
        when(blockedUserService.filterOutBlockedUsers(incomingRequests, receiverId))
            .thenReturn(filteredRequests);

        // Act & Assert
        mockMvc.perform(get("/api/v1/friend-requests/incoming/{userId}", receiverId))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.length()").value(0));

        verify(blockedUserService).filterOutBlockedUsers(incomingRequests, receiverId);
    }

    @Test
    void friendRequestAction_ShouldHandleMultipleActionsSequentially_WhenConcurrentRequests() {
        // Arrange
        doNothing().when(friendRequestService).acceptFriendRequest(friendRequestId);
        doNothing().when(friendRequestService).deleteFriendRequest(any());

        // Act - Simulate concurrent requests
        ResponseEntity<?> response1 = friendRequestController
            .friendRequestAction(friendRequestId, FriendRequestAction.accept);
        ResponseEntity<?> response2 = friendRequestController
            .friendRequestAction(UUID.randomUUID(), FriendRequestAction.reject);

        // Assert
        assertEquals(HttpStatus.NO_CONTENT, response1.getStatusCode());
        assertEquals(HttpStatus.NO_CONTENT, response2.getStatusCode());
    }

    @Test
    void createFriendRequest_ShouldHandleSpecialCharacters_WhenValidUUIDs() throws Exception {
        // Arrange - Test with different UUID formats
        UUID specialSenderId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        UUID specialReceiverId = UUID.fromString("ffffffff-ffff-ffff-ffff-ffffffffffff");
        CreateFriendRequestDTO specialRequest = new CreateFriendRequestDTO(
            UUID.randomUUID(), specialSenderId, specialReceiverId
        );
        
        when(friendRequestService.saveFriendRequest(any(CreateFriendRequestDTO.class)))
            .thenReturn(specialRequest);

        // Act & Assert
        mockMvc.perform(post("/api/v1/friend-requests")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(specialRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.senderUserId").value(specialSenderId.toString()))
                .andExpect(jsonPath("$.receiverUserId").value(specialReceiverId.toString()));
    }

    @Test
    void friendRequestAction_ShouldLogAppropriateMessages_WhenDifferentActionsPerformed() throws Exception {
        // Test accept action logging
        doNothing().when(friendRequestService).acceptFriendRequest(friendRequestId);
        
        mockMvc.perform(put("/api/v1/friend-requests/{friendRequestId}", friendRequestId)
                .param("friendRequestAction", "accept"))
                .andExpect(status().isNoContent());
        
        verify(logger).info(contains("Processing friend request action: accept"));
        verify(logger).info(contains("Successfully accepted friend request: " + friendRequestId));

        // Test reject action logging
        reset(logger);
        doNothing().when(friendRequestService).deleteFriendRequest(friendRequestId);
        
        mockMvc.perform(put("/api/v1/friend-requests/{friendRequestId}", friendRequestId)
                .param("friendRequestAction", "reject"))
                .andExpect(status().isNoContent());
        
        verify(logger).info(contains("Processing friend request action: reject"));
        verify(logger).info(contains("Successfully rejected friend request: " + friendRequestId));
    }
}
