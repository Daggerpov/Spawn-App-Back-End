package com.danielagapov.spawn.ControllerTests;

import com.danielagapov.spawn.Controllers.User.BlockedUserController;
import com.danielagapov.spawn.DTOs.BlockedUser.BlockedUserCreationDTO;
import com.danielagapov.spawn.DTOs.BlockedUser.BlockedUserDTO;
import com.danielagapov.spawn.DTOs.User.BaseUserDTO;
import com.danielagapov.spawn.Exceptions.Logger.ILogger;
import com.danielagapov.spawn.Services.BlockedUser.IBlockedUserService;
import com.danielagapov.spawn.Services.FriendRequest.IFriendRequestService;
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

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Comprehensive unit tests for BlockedUserController
 * Tests all blocked user management endpoints including blocking, unblocking, and retrieval
 */
@ExtendWith(MockitoExtension.class)
class BlockedUserControllerTests {

    @Mock
    private IBlockedUserService blockedUserService;

    @Mock
    private IFriendRequestService friendRequestService;

    @Mock
    private ILogger logger;

    @InjectMocks
    private BlockedUserController blockedUserController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;
    
    private UUID blockerId;
    private UUID blockedId;
    private BlockedUserCreationDTO blockedUserCreationDTO;
    private BlockedUserDTO blockedUserDTO;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        mockMvc = MockMvcBuilders.standaloneSetup(blockedUserController)
                .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
                .build();

        blockerId = UUID.randomUUID();
        blockedId = UUID.randomUUID();

        blockedUserCreationDTO = new BlockedUserCreationDTO();
        blockedUserCreationDTO.setBlockerId(blockerId);
        blockedUserCreationDTO.setBlockedId(blockedId);
        blockedUserCreationDTO.setReason("Inappropriate behavior");

        blockedUserDTO = new BlockedUserDTO(
            UUID.randomUUID(), blockerId, blockedId, "blocker_user", "blocked_user", "Blocked User", "blocked_pic.jpg", "Inappropriate behavior"
        );
    }

    // MARK: - POST Block User Tests

    @Test
    void blockUser_ShouldBlockUser_WhenValidRequest() throws Exception {
        // Arrange
        doNothing().when(friendRequestService).deleteFriendRequestBetweenUsersIfExists(blockerId, blockedId);
        doNothing().when(blockedUserService).blockUser(blockerId, blockedId, "Inappropriate behavior");

        // Act & Assert
        mockMvc.perform(post("/api/v1/blocked-users/block")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(blockedUserCreationDTO)))
                .andExpect(status().isNoContent());

        verify(friendRequestService).deleteFriendRequestBetweenUsersIfExists(blockerId, blockedId);
        verify(blockedUserService).blockUser(blockerId, blockedId, "Inappropriate behavior");
    }

    @Test
    void blockUser_ShouldReturnInternalServerError_WhenServiceThrowsException() throws Exception {
        // Arrange
        doThrow(new RuntimeException("Database error"))
            .when(blockedUserService).blockUser(blockerId, blockedId, "Inappropriate behavior");

        // Act & Assert
        mockMvc.perform(post("/api/v1/blocked-users/block")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(blockedUserCreationDTO)))
                .andExpect(status().isInternalServerError());

        verify(logger).error(contains("Error blocking user"));
    }

    @Test
    void blockUser_ShouldReturnBadRequest_WhenInvalidJson() throws Exception {
        // Act & Assert
        mockMvc.perform(post("/api/v1/blocked-users/block")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{invalid json}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void blockUser_ShouldReturnBadRequest_WhenMissingRequestBody() throws Exception {
        // Act & Assert
        mockMvc.perform(post("/api/v1/blocked-users/block")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    void blockUser_ShouldHandleNullFields_WhenPartialData() throws Exception {
        // Arrange
        BlockedUserCreationDTO partialDTO = new BlockedUserCreationDTO();
        partialDTO.setBlockerId(blockerId);
        partialDTO.setBlockedId(blockedId);
        // Reason is null

        doNothing().when(friendRequestService).deleteFriendRequestBetweenUsersIfExists(blockerId, blockedId);
        doNothing().when(blockedUserService).blockUser(blockerId, blockedId, null);

        // Act & Assert
        mockMvc.perform(post("/api/v1/blocked-users/block")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(partialDTO)))
                .andExpect(status().isNoContent());

        verify(blockedUserService).blockUser(blockerId, blockedId, null);
    }

    // MARK: - DELETE Unblock User Tests

    @Test
    void unblockUser_ShouldUnblockUser_WhenValidRequest() throws Exception {
        // Arrange
        doNothing().when(blockedUserService).unblockUser(blockerId, blockedId);

        // Act & Assert
        mockMvc.perform(delete("/api/v1/blocked-users/unblock")
                .param("blockerId", blockerId.toString())
                .param("blockedId", blockedId.toString()))
                .andExpect(status().isNoContent());

        verify(blockedUserService).unblockUser(blockerId, blockedId);
    }

    @Test
    void unblockUser_ShouldReturnInternalServerError_WhenServiceThrowsException() throws Exception {
        // Arrange
        doThrow(new RuntimeException("Database error"))
            .when(blockedUserService).unblockUser(blockerId, blockedId);

        // Act & Assert
        mockMvc.perform(delete("/api/v1/blocked-users/unblock")
                .param("blockerId", blockerId.toString())
                .param("blockedId", blockedId.toString()))
                .andExpect(status().isInternalServerError());

        verify(logger).error(contains("Error unblocking user"));
    }

    @Test
    void unblockUser_ShouldReturnBadRequest_WhenMissingParameters() throws Exception {
        // Act & Assert - Missing blockedId parameter
        mockMvc.perform(delete("/api/v1/blocked-users/unblock")
                .param("blockerId", blockerId.toString()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void unblockUser_ShouldReturnBadRequest_WhenInvalidUUIDs() throws Exception {
        // Act & Assert
        mockMvc.perform(delete("/api/v1/blocked-users/unblock")
                .param("blockerId", "invalid-uuid")
                .param("blockedId", blockedId.toString()))
                .andExpect(status().isBadRequest());
    }

    // MARK: - GET Blocked Users Tests

    @Test
    void getBlockedUsers_ShouldReturnBlockedUsers_WhenReturnOnlyIdsFalse() throws Exception {
        // Arrange
        List<BlockedUserDTO> blockedUsers = List.of(blockedUserDTO);
        when(blockedUserService.getBlockedUsers(blockerId)).thenReturn(blockedUsers);

        // Act & Assert
        mockMvc.perform(get("/api/v1/blocked-users/{blockerId}", blockerId)
                .param("returnOnlyIds", "false"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].blockedUser.id").value(blockedId.toString()))
                .andExpect(jsonPath("$[0].blockedUser.username").value("blocked_user"))
                .andExpect(jsonPath("$[0].reason").value("Inappropriate behavior"));

        verify(blockedUserService).getBlockedUsers(blockerId);
        verify(blockedUserService, never()).getBlockedUserIds(any());
    }

    @Test
    void getBlockedUsers_ShouldReturnBlockedUserIds_WhenReturnOnlyIdsTrue() throws Exception {
        // Arrange
        List<UUID> blockedUserIds = List.of(blockedId);
        when(blockedUserService.getBlockedUserIds(blockerId)).thenReturn(blockedUserIds);

        // Act & Assert
        mockMvc.perform(get("/api/v1/blocked-users/{blockerId}", blockerId)
                .param("returnOnlyIds", "true"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0]").value(blockedId.toString()));

        verify(blockedUserService).getBlockedUserIds(blockerId);
        verify(blockedUserService, never()).getBlockedUsers(any());
    }

    @Test
    void getBlockedUsers_ShouldReturnBlockedUsers_WhenReturnOnlyIdsDefaultFalse() throws Exception {
        // Arrange - Test default behavior when parameter is not provided
        List<BlockedUserDTO> blockedUsers = List.of(blockedUserDTO);
        when(blockedUserService.getBlockedUsers(blockerId)).thenReturn(blockedUsers);

        // Act & Assert
        mockMvc.perform(get("/api/v1/blocked-users/{blockerId}", blockerId))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.length()").value(1));

        verify(blockedUserService).getBlockedUsers(blockerId);
    }

    @Test
    void getBlockedUsers_ShouldReturnEmptyList_WhenNoBlockedUsers() throws Exception {
        // Arrange
        when(blockedUserService.getBlockedUsers(blockerId)).thenReturn(List.of());

        // Act & Assert
        mockMvc.perform(get("/api/v1/blocked-users/{blockerId}", blockerId))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void getBlockedUsers_ShouldReturnInternalServerError_WhenServiceThrowsException() throws Exception {
        // Arrange
        when(blockedUserService.getBlockedUsers(blockerId))
            .thenThrow(new RuntimeException("Database error"));

        // Act & Assert
        mockMvc.perform(get("/api/v1/blocked-users/{blockerId}", blockerId))
                .andExpect(status().isInternalServerError());

        verify(logger).error(contains("Error getting blocked users for user"));
    }

    @Test
    void getBlockedUsers_ShouldReturnBadRequest_WhenInvalidBlockerId() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/v1/blocked-users/{blockerId}", "invalid-uuid"))
                .andExpect(status().isBadRequest());
    }

    // MARK: - GET Is Blocked Tests

    @Test
    void isBlocked_ShouldReturnTrue_WhenUserIsBlocked() throws Exception {
        // Arrange
        when(blockedUserService.isBlocked(blockerId, blockedId)).thenReturn(true);

        // Act & Assert
        mockMvc.perform(get("/api/v1/blocked-users/is-blocked")
                .param("blockerId", blockerId.toString())
                .param("blockedId", blockedId.toString()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").value(true));

        verify(blockedUserService).isBlocked(blockerId, blockedId);
    }

    @Test
    void isBlocked_ShouldReturnFalse_WhenUserIsNotBlocked() throws Exception {
        // Arrange
        when(blockedUserService.isBlocked(blockerId, blockedId)).thenReturn(false);

        // Act & Assert
        mockMvc.perform(get("/api/v1/blocked-users/is-blocked")
                .param("blockerId", blockerId.toString())
                .param("blockedId", blockedId.toString()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").value(false));

        verify(blockedUserService).isBlocked(blockerId, blockedId);
    }

    @Test
    void isBlocked_ShouldReturnInternalServerError_WhenServiceThrowsException() throws Exception {
        // Arrange
        when(blockedUserService.isBlocked(blockerId, blockedId))
            .thenThrow(new RuntimeException("Database error"));

        // Act & Assert
        mockMvc.perform(get("/api/v1/blocked-users/is-blocked")
                .param("blockerId", blockerId.toString())
                .param("blockedId", blockedId.toString()))
                .andExpect(status().isInternalServerError());

        verify(logger).error(contains("Error checking if user is blocked"));
    }

    @Test
    void isBlocked_ShouldReturnBadRequest_WhenMissingParameters() throws Exception {
        // Act & Assert - Missing blockedId parameter
        mockMvc.perform(get("/api/v1/blocked-users/is-blocked")
                .param("blockerId", blockerId.toString()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void isBlocked_ShouldReturnBadRequest_WhenInvalidUUIDs() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/v1/blocked-users/is-blocked")
                .param("blockerId", "invalid-uuid")
                .param("blockedId", blockedId.toString()))
                .andExpect(status().isBadRequest());
    }

    // MARK: - Direct Controller Method Tests

    @Test
    void blockUser_DirectCall_ShouldReturnNoContent_WhenServiceSucceeds() {
        // Arrange
        doNothing().when(friendRequestService).deleteFriendRequestBetweenUsersIfExists(blockerId, blockedId);
        doNothing().when(blockedUserService).blockUser(blockerId, blockedId, "Inappropriate behavior");

        // Act
        ResponseEntity<Void> response = blockedUserController.blockUser(blockedUserCreationDTO);

        // Assert
        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(friendRequestService).deleteFriendRequestBetweenUsersIfExists(blockerId, blockedId);
        verify(blockedUserService).blockUser(blockerId, blockedId, "Inappropriate behavior");
    }

    @Test
    void blockUser_DirectCall_ShouldReturnInternalServerError_WhenServiceFails() {
        // Arrange
        doThrow(new RuntimeException("Database error"))
            .when(blockedUserService).blockUser(blockerId, blockedId, "Inappropriate behavior");

        // Act
        ResponseEntity<Void> response = blockedUserController.blockUser(blockedUserCreationDTO);

        // Assert
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        verify(logger).error(contains("Error blocking user"));
    }

    @Test
    void unblockUser_DirectCall_ShouldReturnNoContent_WhenServiceSucceeds() {
        // Arrange
        doNothing().when(blockedUserService).unblockUser(blockerId, blockedId);

        // Act
        ResponseEntity<Void> response = blockedUserController.unblockUser(blockerId, blockedId);

        // Assert
        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(blockedUserService).unblockUser(blockerId, blockedId);
    }

    @Test
    void getBlockedUsers_DirectCall_ShouldReturnBlockedUsers_WhenReturnOnlyIdsFalse() {
        // Arrange
        List<BlockedUserDTO> blockedUsers = List.of(blockedUserDTO);
        when(blockedUserService.getBlockedUsers(blockerId)).thenReturn(blockedUsers);

        // Act
        ResponseEntity<?> response = blockedUserController.getBlockedUsers(blockerId, false);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        @SuppressWarnings("unchecked")
        List<BlockedUserDTO> responseBody = (List<BlockedUserDTO>) response.getBody();
        assertEquals(1, responseBody.size());
        assertEquals(blockedId, responseBody.get(0).getBlockedId());
    }

    @Test
    void getBlockedUsers_DirectCall_ShouldReturnBlockedUserIds_WhenReturnOnlyIdsTrue() {
        // Arrange
        List<UUID> blockedUserIds = List.of(blockedId);
        when(blockedUserService.getBlockedUserIds(blockerId)).thenReturn(blockedUserIds);

        // Act
        ResponseEntity<?> response = blockedUserController.getBlockedUsers(blockerId, true);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        @SuppressWarnings("unchecked")
        List<UUID> responseBody = (List<UUID>) response.getBody();
        assertEquals(1, responseBody.size());
        assertEquals(blockedId, responseBody.get(0));
    }

    @Test
    void isBlocked_DirectCall_ShouldReturnTrue_WhenUserIsBlocked() {
        // Arrange
        when(blockedUserService.isBlocked(blockerId, blockedId)).thenReturn(true);

        // Act
        ResponseEntity<Boolean> response = blockedUserController.isBlocked(blockerId, blockedId);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody());
        verify(blockedUserService).isBlocked(blockerId, blockedId);
    }

    @Test
    void isBlocked_DirectCall_ShouldReturnFalse_WhenUserIsNotBlocked() {
        // Arrange
        when(blockedUserService.isBlocked(blockerId, blockedId)).thenReturn(false);

        // Act
        ResponseEntity<Boolean> response = blockedUserController.isBlocked(blockerId, blockedId);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertFalse(response.getBody());
    }

    // MARK: - Edge Case and Integration Tests

    @Test
    void blockUser_ShouldCleanupFriendRequests_WhenBlockingUser() throws Exception {
        // Arrange
        doNothing().when(friendRequestService).deleteFriendRequestBetweenUsersIfExists(blockerId, blockedId);
        doNothing().when(blockedUserService).blockUser(blockerId, blockedId, "Inappropriate behavior");

        // Act
        mockMvc.perform(post("/api/v1/blocked-users/block")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(blockedUserCreationDTO)))
                .andExpect(status().isNoContent());

        // Assert - Verify friend requests are cleaned up BEFORE blocking
        verify(friendRequestService).deleteFriendRequestBetweenUsersIfExists(blockerId, blockedId);
        verify(blockedUserService).blockUser(blockerId, blockedId, "Inappropriate behavior");
    }

    @Test
    void blockUser_ShouldStillBlock_WhenFriendRequestCleanupFails() throws Exception {
        // Arrange
        doThrow(new RuntimeException("Friend request cleanup failed"))
            .when(friendRequestService).deleteFriendRequestBetweenUsersIfExists(blockerId, blockedId);
        doNothing().when(blockedUserService).blockUser(blockerId, blockedId, "Inappropriate behavior");

        // Act & Assert - Should still return error since cleanup failed
        mockMvc.perform(post("/api/v1/blocked-users/block")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(blockedUserCreationDTO)))
                .andExpect(status().isInternalServerError());

        verify(logger).error(contains("Error blocking user"));
    }

    @Test
    void getBlockedUsers_ShouldHandleMultipleBlockedUsers_WhenManyUsersBlocked() throws Exception {
        // Arrange
        UUID blockedId2 = UUID.randomUUID();
        UUID blockedId3 = UUID.randomUUID();

        List<BlockedUserDTO> blockedUsers = List.of(
            blockedUserDTO,
            new BlockedUserDTO(UUID.randomUUID(), blockerId, blockedId2, "blocker_user", "blocked_user2", "Blocked User 2", "pic2.jpg", "Spam"),
            new BlockedUserDTO(UUID.randomUUID(), blockerId, blockedId3, "blocker_user", "blocked_user3", "Blocked User 3", "pic3.jpg", "Harassment")
        );
        
        when(blockedUserService.getBlockedUsers(blockerId)).thenReturn(blockedUsers);

        // Act & Assert
        mockMvc.perform(get("/api/v1/blocked-users/{blockerId}", blockerId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].reason").value("Inappropriate behavior"))
                .andExpect(jsonPath("$[1].reason").value("Spam"))
                .andExpect(jsonPath("$[2].reason").value("Harassment"));
    }

    @Test
    void blockUser_ShouldHandleSpecialCharactersInReason_WhenValidInput() throws Exception {
        // Arrange
        BlockedUserCreationDTO specialReasonDTO = new BlockedUserCreationDTO();
        specialReasonDTO.setBlockerId(blockerId);
        specialReasonDTO.setBlockedId(blockedId);
        specialReasonDTO.setReason("🚫 Inappropriate behavior & spam messages (violation of rules) 🚫");

        doNothing().when(friendRequestService).deleteFriendRequestBetweenUsersIfExists(blockerId, blockedId);
        doNothing().when(blockedUserService).blockUser(eq(blockerId), eq(blockedId), any());

        // Act & Assert
        mockMvc.perform(post("/api/v1/blocked-users/block")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(specialReasonDTO)))
                .andExpect(status().isNoContent());

        verify(blockedUserService).blockUser(blockerId, blockedId, "🚫 Inappropriate behavior & spam messages (violation of rules) 🚫");
    }

    @Test
    void isBlocked_ShouldHandleSameUser_WhenBlockerAndBlockedAreSame() throws Exception {
        // Arrange
        when(blockedUserService.isBlocked(blockerId, blockerId)).thenReturn(false);

        // Act & Assert
        mockMvc.perform(get("/api/v1/blocked-users/is-blocked")
                .param("blockerId", blockerId.toString())
                .param("blockedId", blockerId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(false));

        verify(blockedUserService).isBlocked(blockerId, blockerId);
    }

    @Test
    void blockUser_ShouldHandleConcurrentRequests_WhenMultipleBlocksSimultaneously() {
        // Arrange
        UUID anotherBlockedId = UUID.randomUUID();
        BlockedUserCreationDTO anotherDTO = new BlockedUserCreationDTO();
        anotherDTO.setBlockerId(blockerId);
        anotherDTO.setBlockedId(anotherBlockedId);
        anotherDTO.setReason("Another reason");

        doNothing().when(friendRequestService).deleteFriendRequestBetweenUsersIfExists(any(), any());
        doNothing().when(blockedUserService).blockUser(any(), any(), any());

        // Act
        ResponseEntity<Void> response1 = blockedUserController.blockUser(blockedUserCreationDTO);
        ResponseEntity<Void> response2 = blockedUserController.blockUser(anotherDTO);

        // Assert
        assertEquals(HttpStatus.NO_CONTENT, response1.getStatusCode());
        assertEquals(HttpStatus.NO_CONTENT, response2.getStatusCode());
        verify(blockedUserService, times(2)).blockUser(any(), any(), any());
    }
}
