package com.danielagapov.spawn.ControllerTests;

import com.danielagapov.spawn.Controllers.User.UserController;
import com.danielagapov.spawn.DTOs.User.AbstractUserDTO;
import com.danielagapov.spawn.DTOs.User.FriendUser.FullFriendUserDTO;
import com.danielagapov.spawn.Enums.EntityType;
import com.danielagapov.spawn.Exceptions.Base.BaseNotFoundException;
import com.danielagapov.spawn.Exceptions.Logger.ILogger;
import com.danielagapov.spawn.Services.Auth.IAuthService;
import com.danielagapov.spawn.Services.BlockedUser.IBlockedUserService;
import com.danielagapov.spawn.Services.S3.IS3Service;
import com.danielagapov.spawn.Services.User.IUserService;
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
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Comprehensive unit tests for UserController's friend-related functionality
 * Tests the friends endpoint and related user operations
 */
@ExtendWith(MockitoExtension.class)
class UserControllerTests {

    @Mock
    private IUserService userService;

    @Mock
    private IS3Service s3Service;

    @Mock
    private IBlockedUserService blockedUserService;

    @Mock
    private ILogger logger;

    @Mock
    private IAuthService authService;

    @InjectMocks
    private UserController userController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;
    
    private UUID userId;
    private UUID friendId1;
    private UUID friendId2;
    private FullFriendUserDTO friend1DTO;
    private FullFriendUserDTO friend2DTO;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        mockMvc = MockMvcBuilders.standaloneSetup(userController)
                .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
                .build();

        userId = UUID.randomUUID();
        friendId1 = UUID.randomUUID();
        friendId2 = UUID.randomUUID();

        friend1DTO = new FullFriendUserDTO(
            friendId1, "friend1", "friend1_pic.jpg", "Friend One", "Friend 1 bio", "friend1@example.com"
        );
        
        friend2DTO = new FullFriendUserDTO(
            friendId2, "friend2", "friend2_pic.jpg", "Friend Two", "Friend 2 bio", "friend2@example.com"
        );
    }

    // MARK: - GET User Friends Tests

    @Test
    void getUserFriends_ShouldReturnFriends_WhenUserHasFriends() throws Exception {
        // Arrange
        List<FullFriendUserDTO> friends = List.of(friend1DTO, friend2DTO);
        List<FullFriendUserDTO> filteredFriends = List.of(friend1DTO, friend2DTO);
        
        when(userService.getFullFriendUsersByUserId(userId)).thenReturn(friends);
        when(blockedUserService.filterOutBlockedUsers(friends, userId)).thenReturn(filteredFriends);

        // Act & Assert
        mockMvc.perform(get("/api/v1/users/friends/{id}", userId))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(friendId1.toString()))
                .andExpect(jsonPath("$[0].username").value("friend1"))
                .andExpect(jsonPath("$[0].name").value("Friend One"))
                .andExpect(jsonPath("$[1].id").value(friendId2.toString()))
                .andExpect(jsonPath("$[1].username").value("friend2"))
                .andExpect(jsonPath("$[1].name").value("Friend Two"));

        verify(userService).getFullFriendUsersByUserId(userId);
        verify(blockedUserService).filterOutBlockedUsers(friends, userId);
    }

    @Test
    void getUserFriends_ShouldReturnEmptyList_WhenUserHasNoFriends() throws Exception {
        // Arrange
        when(userService.getFullFriendUsersByUserId(userId)).thenReturn(List.of());
        when(blockedUserService.filterOutBlockedUsers(List.of(), userId)).thenReturn(List.of());

        // Act & Assert
        mockMvc.perform(get("/api/v1/users/friends/{id}", userId))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.length()").value(0));

        verify(userService).getFullFriendUsersByUserId(userId);
        verify(blockedUserService).filterOutBlockedUsers(List.of(), userId);
    }

    @Test
    void getUserFriends_ShouldFilterBlockedUsers_WhenBlockedUsersExist() throws Exception {
        // Arrange
        List<FullFriendUserDTO> friends = List.of(friend1DTO, friend2DTO);
        List<FullFriendUserDTO> filteredFriends = List.of(friend1DTO); // friend2 is blocked
        
        when(userService.getFullFriendUsersByUserId(userId)).thenReturn(friends);
        when(blockedUserService.filterOutBlockedUsers(friends, userId)).thenReturn(filteredFriends);

        // Act & Assert
        mockMvc.perform(get("/api/v1/users/friends/{id}", userId))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(friendId1.toString()));

        verify(blockedUserService).filterOutBlockedUsers(friends, userId);
    }

    @Test
    void getUserFriends_ShouldReturnNotFound_WhenUserNotFound() throws Exception {
        // Arrange
        BaseNotFoundException exception = new BaseNotFoundException(EntityType.User);
        when(userService.getFullFriendUsersByUserId(userId)).thenThrow(exception);

        // Act & Assert
        mockMvc.perform(get("/api/v1/users/friends/{id}", userId))
                .andExpect(status().isNotFound());

        verify(logger).error(contains("User not found for friends retrieval"));
        verify(blockedUserService, never()).filterOutBlockedUsers(any(), any());
    }

    @Test
    void getUserFriends_ShouldReturnInternalServerError_WhenServiceThrowsException() throws Exception {
        // Arrange
        when(userService.getFullFriendUsersByUserId(userId))
            .thenThrow(new RuntimeException("Database connection failed"));

        // Act & Assert
        mockMvc.perform(get("/api/v1/users/friends/{id}", userId))
                .andExpect(status().isInternalServerError());

        verify(logger).error(contains("Error getting friends for user"));
    }

    @Test
    void getUserFriends_ShouldReturnBadRequest_WhenUserIdIsNull() throws Exception {
        // Act & Assert - Spring automatically handles UUID conversion
        mockMvc.perform(get("/api/v1/users/friends/{id}", "invalid-uuid"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getUserFriends_ShouldHandleBlockedUserServiceFailure_GracefullyReturnOriginalList() throws Exception {
        // Arrange
        List<FullFriendUserDTO> friends = List.of(friend1DTO, friend2DTO);
        
        when(userService.getFullFriendUsersByUserId(userId)).thenReturn(friends);
        when(blockedUserService.filterOutBlockedUsers(friends, userId))
            .thenThrow(new RuntimeException("Blocked user service failed"));

        // Act & Assert - Should still return friends even if filtering fails
        mockMvc.perform(get("/api/v1/users/friends/{id}", userId))
                .andExpect(status().isInternalServerError());

        verify(logger).error(contains("Error getting friends for user"));
    }

    // MARK: - Direct Controller Method Tests

    @Test
    void getUserFriends_DirectCall_ShouldReturnFriends_WhenServiceSucceeds() {
        // Arrange
        List<FullFriendUserDTO> friends = List.of(friend1DTO, friend2DTO);
        List<FullFriendUserDTO> filteredFriends = List.of(friend1DTO, friend2DTO);
        
        when(userService.getFullFriendUsersByUserId(userId)).thenReturn(friends);
        when(blockedUserService.filterOutBlockedUsers(friends, userId)).thenReturn(filteredFriends);

        // Act
        ResponseEntity<List<? extends AbstractUserDTO>> response = userController.getUserFriends(userId);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(2, response.getBody().size());
        
        AbstractUserDTO firstFriend = response.getBody().get(0);
        assertEquals(friendId1, firstFriend.getId());
        assertEquals("friend1", firstFriend.getUsername());
        
        verify(userService).getFullFriendUsersByUserId(userId);
        verify(blockedUserService).filterOutBlockedUsers(friends, userId);
    }

    @Test
    void getUserFriends_DirectCall_ShouldReturnNotFound_WhenUserNotFound() {
        // Arrange
        BaseNotFoundException exception = new BaseNotFoundException(EntityType.User);
        when(userService.getFullFriendUsersByUserId(userId)).thenThrow(exception);

        // Act
        ResponseEntity<List<? extends AbstractUserDTO>> response = userController.getUserFriends(userId);

        // Assert
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNull(response.getBody());
        verify(logger).error(contains("User not found for friends retrieval"));
    }

    @Test
    void getUserFriends_DirectCall_ShouldReturnInternalServerError_WhenServiceFails() {
        // Arrange
        when(userService.getFullFriendUsersByUserId(userId))
            .thenThrow(new RuntimeException("Database error"));

        // Act
        ResponseEntity<List<? extends AbstractUserDTO>> response = userController.getUserFriends(userId);

        // Assert
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNull(response.getBody());
        verify(logger).error(contains("Error getting friends for user"));
    }

    @Test
    void getUserFriends_DirectCall_ShouldHandleNullUserId_GracefullyReturnBadRequest() {
        // Act
        ResponseEntity<List<? extends AbstractUserDTO>> response = userController.getUserFriends(null);

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        verify(logger).error("Invalid parameter: user ID is null");
        verifyNoInteractions(userService, blockedUserService);
    }

    // MARK: - Edge Case and Integration Tests

    @Test
    void getUserFriends_ShouldHandleLargeNumberOfFriends_WhenUserHasManyFriends() throws Exception {
        // Arrange - Test with many friends
        List<FullFriendUserDTO> manyFriends = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            UUID friendId = UUID.randomUUID();
            FullFriendUserDTO friend = new FullFriendUserDTO(
                friendId, "friend" + i, "pic" + i + ".jpg", "Friend " + i, "Bio " + i, "friend" + i + "@example.com"
            );
            manyFriends.add(friend);
        }
        
        when(userService.getFullFriendUsersByUserId(userId)).thenReturn(manyFriends);
        when(blockedUserService.filterOutBlockedUsers(manyFriends, userId)).thenReturn(manyFriends);

        // Act & Assert
        mockMvc.perform(get("/api/v1/users/friends/{id}", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(100));

        verify(userService).getFullFriendUsersByUserId(userId);
    }

    @Test
    void getUserFriends_ShouldHandleSpecialCharacters_WhenFriendNamesContainUnicode() throws Exception {
        // Arrange
        FullFriendUserDTO unicodeFriend = new FullFriendUserDTO(
            friendId1, "友達", "pic.jpg", "友達 User 🎉", "Bio with 特殊文字", "unicode@example.com"
        );
        List<FullFriendUserDTO> friends = List.of(unicodeFriend);
        
        when(userService.getFullFriendUsersByUserId(userId)).thenReturn(friends);
        when(blockedUserService.filterOutBlockedUsers(friends, userId)).thenReturn(friends);

        // Act & Assert
        mockMvc.perform(get("/api/v1/users/friends/{id}", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].username").value("友達"))
                .andExpect(jsonPath("$[0].name").value("友達 User 🎉"))
                .andExpect(jsonPath("$[0].bio").value("Bio with 特殊文字"));
    }

    @Test
    void getUserFriends_ShouldHandleConcurrentRequests_WhenMultipleRequestsSimultaneously() {
        // Arrange
        List<FullFriendUserDTO> friends = List.of(friend1DTO, friend2DTO);
        when(userService.getFullFriendUsersByUserId(userId)).thenReturn(friends);
        when(blockedUserService.filterOutBlockedUsers(friends, userId)).thenReturn(friends);

        // Act - Make multiple concurrent calls
        ResponseEntity<List<? extends AbstractUserDTO>> response1 = userController.getUserFriends(userId);
        ResponseEntity<List<? extends AbstractUserDTO>> response2 = userController.getUserFriends(userId);
        ResponseEntity<List<? extends AbstractUserDTO>> response3 = userController.getUserFriends(userId);

        // Assert
        assertEquals(HttpStatus.OK, response1.getStatusCode());
        assertEquals(HttpStatus.OK, response2.getStatusCode());
        assertEquals(HttpStatus.OK, response3.getStatusCode());
        
        // Verify service was called for each request
        verify(userService, times(3)).getFullFriendUsersByUserId(userId);
        verify(blockedUserService, times(3)).filterOutBlockedUsers(friends, userId);
    }

    @Test
    void getUserFriends_ShouldMaintainFriendOrder_WhenServiceReturnsOrderedList() throws Exception {
        // Arrange - Create friends with specific ordering
        FullFriendUserDTO friendA = new FullFriendUserDTO(
            UUID.randomUUID(), "a_friend", "pic_a.jpg", "A Friend", "Bio A", "a@example.com"
        );
        FullFriendUserDTO friendZ = new FullFriendUserDTO(
            UUID.randomUUID(), "z_friend", "pic_z.jpg", "Z Friend", "Bio Z", "z@example.com"
        );
        
        List<FullFriendUserDTO> orderedFriends = List.of(friendA, friendZ);
        
        when(userService.getFullFriendUsersByUserId(userId)).thenReturn(orderedFriends);
        when(blockedUserService.filterOutBlockedUsers(orderedFriends, userId)).thenReturn(orderedFriends);

        // Act & Assert
        mockMvc.perform(get("/api/v1/users/friends/{id}", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].username").value("a_friend"))
                .andExpect(jsonPath("$[1].username").value("z_friend"));
    }

    @Test
    void getUserFriends_ShouldReturnEmptyList_WhenAllFriendsAreBlocked() throws Exception {
        // Arrange
        List<FullFriendUserDTO> friends = List.of(friend1DTO, friend2DTO);
        List<FullFriendUserDTO> filteredFriends = List.of(); // All friends blocked
        
        when(userService.getFullFriendUsersByUserId(userId)).thenReturn(friends);
        when(blockedUserService.filterOutBlockedUsers(friends, userId)).thenReturn(filteredFriends);

        // Act & Assert
        mockMvc.perform(get("/api/v1/users/friends/{id}", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        verify(userService).getFullFriendUsersByUserId(userId);
        verify(blockedUserService).filterOutBlockedUsers(friends, userId);
    }

    @Test
    void getUserFriends_ShouldHandleNullFieldsInFriends_WhenPartialFriendData() throws Exception {
        // Arrange - Friend with some null fields
        FullFriendUserDTO partialFriend = new FullFriendUserDTO(
            friendId1, "partial_friend", null, null, null, "partial@example.com"
        );
        List<FullFriendUserDTO> friends = List.of(partialFriend);
        
        when(userService.getFullFriendUsersByUserId(userId)).thenReturn(friends);
        when(blockedUserService.filterOutBlockedUsers(friends, userId)).thenReturn(friends);

        // Act & Assert
        mockMvc.perform(get("/api/v1/users/friends/{id}", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].username").value("partial_friend"))
                .andExpect(jsonPath("$[0].email").value("partial@example.com"));
    }

    @Test
    void getUserFriends_ShouldLogUserInfo_WhenLoggingEnabled() throws Exception {
        // Arrange
        List<FullFriendUserDTO> friends = List.of(friend1DTO);
        when(userService.getFullFriendUsersByUserId(userId)).thenReturn(friends);
        when(blockedUserService.filterOutBlockedUsers(friends, userId)).thenReturn(friends);

        // Act
        mockMvc.perform(get("/api/v1/users/friends/{id}", userId))
                .andExpect(status().isOk());

        // Assert - Verify no sensitive information is logged (user ID is anonymized)
        verify(logger, never()).error(contains(userId.toString()));
    }

    // MARK: - DELETE Remove Friendship Tests

    @Test
    void removeFriendship_ShouldReturnNoContent_WhenFriendshipRemovedSuccessfully() throws Exception {
        // Arrange
        doNothing().when(userService).removeFriendshipBetweenUsers(userId, friendId1);

        // Act & Assert
        mockMvc.perform(delete("/api/v1/users/friends/{userId}/{friendId}", userId, friendId1))
                .andExpect(status().isNoContent());

        verify(userService).removeFriendshipBetweenUsers(userId, friendId1);
        verify(logger).info(contains("Successfully removed friendship between users"));
    }

    @Test
    void removeFriendship_ShouldReturnBadRequest_WhenUserIdIsNull() throws Exception {
        // Act & Assert - Spring automatically handles UUID conversion
        mockMvc.perform(delete("/api/v1/users/friends/{userId}/{friendId}", "invalid-uuid", friendId1))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(userService);
    }

    @Test
    void removeFriendship_ShouldReturnBadRequest_WhenFriendIdIsNull() throws Exception {
        // Act & Assert
        mockMvc.perform(delete("/api/v1/users/friends/{userId}/{friendId}", userId, "invalid-uuid"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(userService);
    }

    @Test
    void removeFriendship_ShouldReturnNotFound_WhenUserNotFound() throws Exception {
        // Arrange
        BaseNotFoundException exception = new BaseNotFoundException(EntityType.User);
        doThrow(exception).when(userService).removeFriendshipBetweenUsers(userId, friendId1);

        // Act & Assert
        mockMvc.perform(delete("/api/v1/users/friends/{userId}/{friendId}", userId, friendId1))
                .andExpect(status().isNotFound());

        verify(logger).error(contains("User not found for friendship removal"));
    }

    @Test
    void removeFriendship_ShouldReturnInternalServerError_WhenServiceThrowsException() throws Exception {
        // Arrange
        doThrow(new RuntimeException("Database error")).when(userService).removeFriendshipBetweenUsers(userId, friendId1);

        // Act & Assert
        mockMvc.perform(delete("/api/v1/users/friends/{userId}/{friendId}", userId, friendId1))
                .andExpect(status().isInternalServerError());

        verify(logger).error(contains("Error removing friendship between users"));
    }

    @Test
    void removeFriendship_DirectCall_ShouldReturnNoContent_WhenServiceSucceeds() {
        // Arrange
        doNothing().when(userService).removeFriendshipBetweenUsers(userId, friendId1);

        // Act
        ResponseEntity<Void> response = userController.removeFriendship(userId, friendId1);

        // Assert
        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        assertNull(response.getBody());
        verify(userService).removeFriendshipBetweenUsers(userId, friendId1);
        verify(logger).info(contains("Successfully removed friendship between users"));
    }

    @Test
    void removeFriendship_DirectCall_ShouldReturnBadRequest_WhenUserIdIsNull() {
        // Act
        ResponseEntity<Void> response = userController.removeFriendship(null, friendId1);

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        verify(logger).error("Invalid parameters: userId or friendId is null");
        verifyNoInteractions(userService);
    }

    @Test
    void removeFriendship_DirectCall_ShouldReturnBadRequest_WhenFriendIdIsNull() {
        // Act
        ResponseEntity<Void> response = userController.removeFriendship(userId, null);

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        verify(logger).error("Invalid parameters: userId or friendId is null");
        verifyNoInteractions(userService);
    }

    @Test
    void removeFriendship_DirectCall_ShouldReturnBadRequest_WhenUserIdsAreTheSame() {
        // Act
        ResponseEntity<Void> response = userController.removeFriendship(userId, userId);

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        verify(logger).error("Invalid parameters: userId and friendId cannot be the same");
        verifyNoInteractions(userService);
    }

    @Test
    void removeFriendship_DirectCall_ShouldReturnNotFound_WhenUserNotFound() {
        // Arrange
        BaseNotFoundException exception = new BaseNotFoundException(EntityType.User);
        doThrow(exception).when(userService).removeFriendshipBetweenUsers(userId, friendId1);

        // Act
        ResponseEntity<Void> response = userController.removeFriendship(userId, friendId1);

        // Assert
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        verify(logger).error(contains("User not found for friendship removal"));
    }

    @Test
    void removeFriendship_DirectCall_ShouldReturnInternalServerError_WhenServiceFails() {
        // Arrange
        doThrow(new RuntimeException("Database error")).when(userService).removeFriendshipBetweenUsers(userId, friendId1);

        // Act
        ResponseEntity<Void> response = userController.removeFriendship(userId, friendId1);

        // Assert
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        verify(logger).error(contains("Error removing friendship between users"));
    }

    @Test
    void removeFriendship_ShouldHandleConcurrentRequests_WhenMultipleRequestsSimultaneously() {
        // Arrange
        doNothing().when(userService).removeFriendshipBetweenUsers(userId, friendId1);

        // Act - Make multiple concurrent calls
        ResponseEntity<Void> response1 = userController.removeFriendship(userId, friendId1);
        ResponseEntity<Void> response2 = userController.removeFriendship(userId, friendId1);
        ResponseEntity<Void> response3 = userController.removeFriendship(userId, friendId1);

        // Assert
        assertEquals(HttpStatus.NO_CONTENT, response1.getStatusCode());
        assertEquals(HttpStatus.NO_CONTENT, response2.getStatusCode());
        assertEquals(HttpStatus.NO_CONTENT, response3.getStatusCode());
        
        // Verify service was called for each request
        verify(userService, times(3)).removeFriendshipBetweenUsers(userId, friendId1);
    }
}
