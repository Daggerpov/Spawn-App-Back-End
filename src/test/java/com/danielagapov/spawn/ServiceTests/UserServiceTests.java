package com.danielagapov.spawn.ServiceTests;

import com.danielagapov.spawn.DTOs.User.BaseUserDTO;
import com.danielagapov.spawn.DTOs.User.RecentlySpawnedUserDTO;
import com.danielagapov.spawn.DTOs.User.UserDTO;
import com.danielagapov.spawn.DTOs.User.UserUpdateDTO;
import com.danielagapov.spawn.DTOs.UserIdActivityTimeDTO;
import com.danielagapov.spawn.Enums.ParticipationStatus;
import com.danielagapov.spawn.Enums.UserStatus;
import com.danielagapov.spawn.Exceptions.Base.BaseNotFoundException;
import com.danielagapov.spawn.Exceptions.Base.BaseSaveException;
import com.danielagapov.spawn.Exceptions.Base.BasesNotFoundException;
import com.danielagapov.spawn.Exceptions.Logger.ILogger;
import com.danielagapov.spawn.Models.FriendTag;
import com.danielagapov.spawn.Models.User.User;
import com.danielagapov.spawn.Repositories.IActivityUserRepository;
import com.danielagapov.spawn.Repositories.IFriendTagRepository;
import com.danielagapov.spawn.Repositories.IUserFriendTagRepository;
import com.danielagapov.spawn.Repositories.User.IUserRepository;
import com.danielagapov.spawn.Services.BlockedUser.IBlockedUserService;
import com.danielagapov.spawn.Services.FriendRequest.IFriendRequestService;
import com.danielagapov.spawn.Services.FriendTag.IFriendTagService;
import com.danielagapov.spawn.Services.S3.IS3Service;
import com.danielagapov.spawn.Services.User.UserService;
import com.danielagapov.spawn.Services.UserSearch.IUserSearchService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.springframework.dao.DataAccessException;

import java.time.OffsetDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@Order(1)
@Execution(ExecutionMode.CONCURRENT)
public class UserServiceTests {

    @Mock
    private IUserRepository userRepository;

    @Mock
    private IUserFriendTagRepository userFriendTagRepository;

    @Mock
    private IFriendTagRepository friendTagRepository;

    @Mock
    private ILogger logger;
    @Mock
    private IFriendTagService friendTagService;
    @Mock
    private IFriendRequestService friendRequestService;

    @Mock
    private IBlockedUserService blockedUserService;

    @Mock
    private IS3Service s3Service;

    @Mock
    private IUserSearchService userSearchService;

    @Mock
    private IActivityUserRepository activityUserRepository;

    @Spy
    @InjectMocks
    private UserService userService;

    @BeforeEach
    void setup() throws Exception {
        MockitoAnnotations.openMocks(this); // Initialize mocks
        
        // Set the adminUsername field using reflection to avoid null pointer in tests
        java.lang.reflect.Field adminUsernameField = UserService.class.getDeclaredField("adminUsername");
        adminUsernameField.setAccessible(true);
        adminUsernameField.set(userService, "admin");
    }

    @Test
    void getAllUsers_ShouldReturnList_WhenUsersExist() {
        User user = new User(UUID.randomUUID(), "john_doe", "profile.jpg", "John Doe", "A bio", "john.doe@example.com");
        user.setStatus(UserStatus.ACTIVE);

        when(userRepository.findAllUsersByStatus(UserStatus.ACTIVE)).thenReturn(List.of(user));

        List<UserDTO> result = userService.getAllUsers();

        assertFalse(result.isEmpty());
        assertEquals("john_doe", result.get(0).getUsername());
        verify(userRepository, times(1)).findAll();
    }

    @Test
    void getUserById_ShouldReturnUser_WhenUserExists() {
        UUID userId = UUID.randomUUID();
        User user = new User(userId, "john_doe", "profile.jpg", "John Doe", "A bio", "john.doe@example.com");

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(friendTagService.getFriendTagsByOwnerId(userId)).thenReturn(List.of());

        UserDTO result = userService.getUserById(userId);

        assertEquals("john_doe", result.getUsername());
        verify(userRepository, times(1)).findById(userId);
    }

    @Test
    void saveUser_ShouldThrowException_WhenDatabaseErrorOccurs() {
        UserDTO userDTO = new UserDTO(UUID.randomUUID(), new ArrayList<>(), "john_doe", "profile.jpg", "John Doe", "A bio", new ArrayList<>(), "john.doe@example.com");
        when(userRepository.save(any(User.class))).thenThrow(new DataAccessException("Database error") {
        });

        assertThrows(BaseSaveException.class, () -> userService.saveUser(userDTO));
        verify(userRepository, times(1)).save(any(User.class));
        verify(logger, times(1)).error("Failed to save user: Database error");
    }

    @Test
    void updateUser_ShouldUpdateAllFields_WhenUserExists() {
        // Arrange
        UUID userId = UUID.randomUUID();
        User existingUser = new User(userId, "old_username", "profile.jpg", "Old Name", "Old bio", "user@example.com");
        User updatedUser = new User(userId, "new_username", "profile.jpg", "New Name", "New bio", "user@example.com");

        when(userRepository.findById(userId)).thenReturn(Optional.of(existingUser));
        when(userRepository.save(any(User.class))).thenReturn(updatedUser);
        UserUpdateDTO updateDTO = new UserUpdateDTO("New bio", "new_username", "New Name");

        // Act
        var result = userService.updateUser(userId, updateDTO);

        // Assert
        assertEquals("new_username", result.getUsername());
        assertEquals("New Name", result.getName());
        assertEquals("New bio", result.getBio());

        verify(userRepository, times(1)).findById(userId);
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    void updateUser_ShouldHandleNullValues_WhenUpdatingPartially() {
        // Arrange
        UUID userId = UUID.randomUUID();
        User existingUser = new User(userId, "old_username", "profile.jpg", "Old Name", "Old bio", "user@example.com");
        User updatedUser = new User(userId, "old_username", "profile.jpg", "New Name", "New bio", "user@example.com");

        when(userRepository.findById(userId)).thenReturn(Optional.of(existingUser));
        when(userRepository.save(any(User.class))).thenReturn(updatedUser);

        UserUpdateDTO updateDTO = new UserUpdateDTO("New bio", null, "New Name");

        // Act
        var result = userService.updateUser(userId, updateDTO);
        // Assert
        assertEquals("old_username", result.getUsername());  // unchanged
        assertEquals("New Name", result.getName());          // changed
        assertEquals("New bio", result.getBio());            // changed

        verify(userRepository, times(1)).findById(userId);
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    void updateUser_ShouldThrowException_WhenUserDoesNotExist() {
        // Arrange
        UUID userId = UUID.randomUUID();
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // Act & Assert
        BaseNotFoundException exception = assertThrows(BaseNotFoundException.class,
                () -> userService.updateUser(userId, new UserUpdateDTO("New bio", "new_username", "New Name")));

        assertTrue(exception.getMessage().contains("User"));
        verify(userRepository, times(1)).findById(userId);
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void updateUser_ShouldLogAndThrowException_WhenDatabaseErrorOccurs() {
        // Arrange
        UUID userId = UUID.randomUUID();
        User existingUser = new User(userId, "username", "profile.jpg", "Full Name", "Bio", "user@example.com");

        when(userRepository.findById(userId)).thenReturn(Optional.of(existingUser));
        when(userRepository.save(any(User.class))).thenThrow(new DataAccessException("Database error") {
        });

        // Act & Assert
        Exception exception = assertThrows(DataAccessException.class,
                () -> userService.updateUser(userId, new UserUpdateDTO("New bio", "new_username", "New Name")));

        assertTrue(exception.getMessage().contains("Database error"));
        verify(userRepository, times(1)).findById(userId);
        verify(userRepository, times(1)).save(any(User.class));
        verify(logger, times(1)).error(anyString());
    }

    @Test
    void deleteUserById_ShouldDeleteUser_WhenUserExists() {
        UUID userId = UUID.randomUUID();
        User user = new User(userId, "JohnDoe123", "profile.jpg", "John Doe", null, "johndoe@anon.com");

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        doNothing().when(s3Service).deleteObjectByURL(anyString());
        doNothing().when(userRepository).deleteById(userId);

        userService.deleteUserById(userId);

        verify(userRepository, times(1)).deleteById(userId);
        verify(s3Service, times(1)).deleteObjectByURL("profile.jpg");
    }

    @Test
    void deleteUserById_ShouldLogException_WhenUnexpectedErrorOccurs() {
        UUID userId = UUID.randomUUID();
        User user = new User(userId, "JohnDoe123", "profile.jpg", "John Doe", null, "johndoe@anon.com");

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        doNothing().when(s3Service).deleteObjectByURL(anyString());
        doThrow(new RuntimeException("Unexpected error")).when(userRepository).deleteById(userId);

        Exception exception = assertThrows(RuntimeException.class, () -> userService.deleteUserById(userId));

        assertEquals("Unexpected error", exception.getMessage());
        verify(logger, times(1)).error(anyString());
    }

    @Test
    void deleteUserById_ShouldThrowException_WhenUserDoesNotExist() {
        UUID userId = UUID.randomUUID();
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        BaseNotFoundException exception = assertThrows(BaseNotFoundException.class, () -> userService.deleteUserById(userId));

        assertTrue(exception.getMessage().contains("User"));
        verify(userRepository, times(1)).findById(userId);
        verify(userRepository, never()).deleteById(userId);
    }

    @Test
    void deleteUserById_ShouldNotCallDelete_WhenUserDoesNotExist() {
        UUID userId = UUID.randomUUID();
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        BaseNotFoundException exception = assertThrows(BaseNotFoundException.class, () -> userService.deleteUserById(userId));

        assertTrue(exception.getMessage().contains("User"));
        verify(userRepository, never()).deleteById(userId);
    }

    @Test
    void getAllUsers_ShouldThrowException_WhenDatabaseErrorOccurs() {
        when(userRepository.findAllUsersByStatus(UserStatus.ACTIVE)).thenThrow(new DataAccessException("Database error") {
        });

        BasesNotFoundException exception = assertThrows(BasesNotFoundException.class, () -> userService.getAllUsers());

        assertTrue(exception.getMessage().contains("User"));
        verify(logger, atLeastOnce()).error("Database error");
    }

    @Test
    void getUserById_ShouldThrowException_WhenUserDoesNotExist() {
        UUID userId = UUID.randomUUID();

        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        BaseNotFoundException exception = assertThrows(BaseNotFoundException.class, () -> userService.getUserById(userId));

        assertTrue(exception.getMessage().contains("User"));
        verify(userRepository, times(1)).findById(userId);
    }

    @Test
    void saveUser_ShouldLogException_WhenUnexpectedErrorOccurs() {
        UserDTO userDTO = new UserDTO(UUID.randomUUID(), List.of(), "john_doe", "profile.jpg", "John Doe", "A bio", List.of(), "john.doe@example.com");
        when(userRepository.save(any(User.class))).thenThrow(new RuntimeException("Unexpected error"));

        RuntimeException exception = assertThrows(RuntimeException.class, () -> userService.saveUser(userDTO));

        assertTrue(exception.getMessage().contains("Unexpected error"));
        verify(logger, times(1)).error("Error saving user: Unexpected error");
    }

    @Test
    void updateUser_ShouldLogAndThrowException_WhenUnexpectedErrorOccurs() {
        // Arrange
        UUID userId = UUID.randomUUID();
        when(userRepository.findById(userId)).thenThrow(new RuntimeException("Unexpected error"));

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> userService.updateUser(userId, new UserUpdateDTO("New bio", "new_username", "New Name")));

        assertTrue(exception.getMessage().contains("Unexpected error"));
        verify(logger, times(1)).error(anyString());
    }

    @Test
    void getAllUsers_ShouldReturnEmptyList_WhenNoUsersExist() {
        when(userRepository.findAllUsersByStatus(UserStatus.ACTIVE)).thenReturn(List.of());

        List<UserDTO> result = userService.getAllUsers();

        assertTrue(result.isEmpty());
        verify(userRepository, times(1)).findAll();
    }

    @Test
    void getMutualFriendCount_ShouldReturnCorrectCount_WhenUsersHaveMutualFriends() {
        // Setup
        UUID user1Id = UUID.randomUUID();
        UUID user2Id = UUID.randomUUID();
        UUID mutualFriend1Id = UUID.randomUUID();
        UUID mutualFriend2Id = UUID.randomUUID();
        UUID uniqueFriend1Id = UUID.randomUUID();
        UUID uniqueFriend2Id = UUID.randomUUID();

        // User1's friends: mutualFriend1, mutualFriend2, uniqueFriend1
        when(userFriendTagRepository.findFriendIdsByUserId(any()))
                .thenReturn(List.of(mutualFriend1Id, mutualFriend2Id, uniqueFriend1Id))
                .thenReturn(List.of(mutualFriend1Id, mutualFriend2Id, uniqueFriend2Id));

        // User2's friends: mutualFriend1, mutualFriend2, uniqueFriend2

        int result = userService.getMutualFriendCount(user1Id, user2Id);

        assertEquals(2, result);
        verify(userFriendTagRepository, times(2)).findFriendIdsByUserId(any());
    }

    @Test
    void getMutualFriendCount_ShouldReturnZero_WhenNoMutualFriends() {
        UUID user1Id = UUID.randomUUID();
        UUID user2Id = UUID.randomUUID();
        UUID friend1Id = UUID.randomUUID();
        UUID friend2Id = UUID.randomUUID();

        // User1's friends: friend1
        when(friendTagRepository.findByOwnerId(user1Id))
                .thenReturn(List.of(createEveryoneTag(user1Id)));
        when(userFriendTagRepository.findFriendIdsByTagId(any()))
                .thenReturn(List.of(friend1Id))
                .thenReturn(List.of(friend2Id));

        // User2's friends: friend2
        when(friendTagRepository.findByOwnerId(user2Id))
                .thenReturn(List.of(createEveryoneTag(user2Id)));

        int result = userService.getMutualFriendCount(user1Id, user2Id);

        assertEquals(0, result);
        verify(userFriendTagRepository, times(2)).findFriendIdsByUserId(any());
    }

    @Test
    void getMutualFriendCount_ShouldReturnZero_WhenOneUserHasNoFriends() {
        UUID user1Id = UUID.randomUUID();
        UUID user2Id = UUID.randomUUID();
        UUID friendId = UUID.randomUUID();

        // User1 has one friend
        when(friendTagRepository.findByOwnerId(user1Id))
                .thenReturn(List.of(createEveryoneTag(user1Id)));
        when(userFriendTagRepository.findFriendIdsByTagId(any()))
                .thenReturn(List.of(friendId))
                .thenReturn(Collections.emptyList()); // User2 has no friends

        // User2 has no friends (empty everyone tag)
        when(friendTagRepository.findByOwnerId(user2Id))
                .thenReturn(List.of(createEveryoneTag(user2Id)));

        int result = userService.getMutualFriendCount(user1Id, user2Id);

        assertEquals(0, result);
        verify(userFriendTagRepository, times(2)).findFriendIdsByUserId(any());
    }

    @Test
    void getMutualFriendCount_ShouldReturnZero_WhenBothUsersHaveNoFriends() {
        UUID user1Id = UUID.randomUUID();
        UUID user2Id = UUID.randomUUID();

        // Both users have empty everyone tags
        when(friendTagRepository.findByOwnerId(any()))
                .thenReturn(List.of(createEveryoneTag(UUID.randomUUID())));
        when(userFriendTagRepository.findFriendIdsByTagId(any()))
                .thenReturn(Collections.emptyList());

        int result = userService.getMutualFriendCount(user1Id, user2Id);

        assertEquals(0, result);
        verify(userFriendTagRepository, times(2)).findFriendIdsByUserId(any());
    }

    @Test
    void getMutualFriendCount_ShouldReturnZero_WhenNoEveryoneTag() {
        UUID user1Id = UUID.randomUUID();
        UUID user2Id = UUID.randomUUID();

        // Users have no everyone tag
        when(friendTagRepository.findByOwnerId(any()))
                .thenReturn(List.of());

        int result = userService.getMutualFriendCount(user1Id, user2Id);

        assertEquals(0, result);
        verify(userFriendTagRepository, times(2)).findFriendIdsByUserId(any());
    }

    @Test
    void getMutualFriendCount_ShouldHandleEmptyOptionals() {
        UUID user1Id = UUID.randomUUID();
        UUID user2Id = UUID.randomUUID();

        when(friendTagRepository.findByOwnerId(any()))
                .thenReturn(List.of());

        int result = userService.getMutualFriendCount(user1Id, user2Id);

        assertEquals(0, result);
        verify(userFriendTagRepository, times(2)).findFriendIdsByUserId(any());
    }

    @Test
    void getFriendUserIdsByFriendTagId_ShouldReturnEmptyList_WhenNoFriendsFound() {
        UUID tagId = UUID.randomUUID();
        when(userFriendTagRepository.findFriendIdsByTagId(tagId))
                .thenReturn(List.of());

        List<UUID> result = userService.getFriendUserIdsByFriendTagId(tagId);

        assertTrue(result.isEmpty());
        verify(userFriendTagRepository, times(1)).findFriendIdsByTagId(tagId);
    }

    @Test
    void getFriendsByFriendTagId_ShouldReturnEmptyList_WhenNoFriendsFound() {
        UUID tagId = UUID.randomUUID();
        when(userFriendTagRepository.findFriendIdsByTagId(tagId))
                .thenReturn(List.of());

        List<BaseUserDTO> result = userService.getFriendsByFriendTagId(tagId);

        assertTrue(result.isEmpty());
        verify(userFriendTagRepository, times(1)).findFriendIdsByTagId(tagId);
    }

    @Test
    void saveFriendToUser_ShouldSkip_WhenNoEveryoneTagFound() {
        UUID userId = UUID.randomUUID();
        UUID friendId = UUID.randomUUID();

        when(friendTagRepository.findByOwnerIdAndIsEveryoneTrue(any()))
                .thenReturn(Optional.empty());

        userService.saveFriendToUser(userId, friendId);

        verify(friendTagService, never()).saveUserToFriendTag(any(), any());
    }

    @Test
    void getFriendUserIdsByUserId_ShouldReturnEmptyList_WhenNoEveryoneTagFound() {
        UUID userId = UUID.randomUUID();

        when(friendTagRepository.findByOwnerId(userId))
                .thenReturn(List.of(new FriendTag(UUID.randomUUID(), "Test", "#000000", userId, false, null)));

        List<UUID> result = userService.getFriendUserIdsByUserId(userId);

        assertTrue(result.isEmpty());
        verify(userFriendTagRepository, times(1)).findFriendIdsByUserId(any());
    }

    @Test
    void testGetRecentlySpawnedWithUsers() {
        // Setup mock data for the repository methods
        UUID requestingUserId = UUID.randomUUID();
        List<UUID> pastActivityIds = Arrays.asList(UUID.randomUUID(), UUID.randomUUID());
        UUID friendId1 = UUID.randomUUID();
        UUID friendId2 = UUID.randomUUID();
        UUID nonFriendId = UUID.randomUUID();
        UUID blockedId = UUID.randomUUID();
        UserIdActivityTimeDTO friendIdActivityTime1 = new UserIdActivityTimeDTO(friendId1, OffsetDateTime.now());
        UserIdActivityTimeDTO friendIdActivityTime2 = new UserIdActivityTimeDTO(friendId2, OffsetDateTime.now());
        UserIdActivityTimeDTO nonfriendIdActivityTime = new UserIdActivityTimeDTO(nonFriendId, OffsetDateTime.now());
        UserIdActivityTimeDTO blockedIdActivityTime = new UserIdActivityTimeDTO(blockedId, OffsetDateTime.now());
        List<UserIdActivityTimeDTO> pastActivityParticipants = Arrays.asList(friendIdActivityTime1, friendIdActivityTime2, nonfriendIdActivityTime, blockedIdActivityTime);
        List<UUID> friendIds = Arrays.asList(friendId1, friendId2);

        // Mock the repository methods
        when(activityUserRepository.findPastActivityIdsForUser(eq(requestingUserId), eq(ParticipationStatus.participating), any()))
                .thenReturn(pastActivityIds);
        when(activityUserRepository.findOtherUserIdsByActivityIds(eq(pastActivityIds), eq(requestingUserId), eq(ParticipationStatus.participating)))
                .thenReturn(pastActivityParticipants);
        when(userSearchService.getExcludedUserIds(requestingUserId)).thenReturn(Set.of(blockedId, requestingUserId, friendId1, friendId2));

        // Mock the getBaseUserById method (if necessary, return a mocked BaseUserDTO)
        BaseUserDTO mockBaseUserDTO = new BaseUserDTO();
        mockBaseUserDTO.setId(UUID.randomUUID());
        doReturn(mockBaseUserDTO).when(userService).getBaseUserById(nonFriendId);

        // Call the method
        List<RecentlySpawnedUserDTO> result = userService.getRecentlySpawnedWithUsers(requestingUserId);

        // Verify repository method interactions
        verify(activityUserRepository, times(1)).findPastActivityIdsForUser(eq(requestingUserId), eq(ParticipationStatus.participating), any());
        verify(activityUserRepository, times(1)).findOtherUserIdsByActivityIds(eq(pastActivityIds), eq(requestingUserId), eq(ParticipationStatus.participating));
        verify(userService, times(1)).getBaseUserById(any(UUID.class));

        // Assert the results
        assertNotNull(result);
        assertEquals(1, result.size());
        assertTrue(result.stream().allMatch(recentUser -> !friendIds.contains(recentUser.getUser().getId())));
    }

    @Test
    void testGetRecentlySpawnedWithUsers_NoParticipants() {
        // Setup mock data for no participants
        UUID requestingUserId = UUID.randomUUID();
        List<UUID> pastActivityIds = Collections.emptyList();
        List<UserIdActivityTimeDTO> pastActivityParticipantIds = Collections.emptyList();
        List<UUID> friendIds = new ArrayList<>();

        // Mock the repository methods
        when(activityUserRepository.findPastActivityIdsForUser(eq(requestingUserId), eq(ParticipationStatus.participating), any()))
                .thenReturn(pastActivityIds);
        when(activityUserRepository.findOtherUserIdsByActivityIds(eq(pastActivityIds), eq(requestingUserId), eq(ParticipationStatus.participating)))
                .thenReturn(pastActivityParticipantIds);
        when(userService.getFriendUserIdsByUserId(eq(requestingUserId)))
                .thenReturn(friendIds);

        // Call the method
        List<RecentlySpawnedUserDTO> result = userService.getRecentlySpawnedWithUsers(requestingUserId);

        // Assert the results (should be an empty list)
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void testGetRecentlySpawnedWithUsers_ExceptionHandling() {
        // Simulate an exception being thrown in the repository method
        UUID requestingUserId = UUID.randomUUID();
        when(activityUserRepository.findPastActivityIdsForUser(eq(requestingUserId), eq(ParticipationStatus.participating), any()))
                .thenThrow(new RuntimeException("Database error"));

        // Call the method and assert it handles the exception
        try {
            userService.getRecentlySpawnedWithUsers(requestingUserId);
            fail("Expected an exception to be thrown");
        } catch (RuntimeException e) {
            assertEquals("Database error", e.getMessage());
        }

        // Verify the repository interaction
        verify(activityUserRepository, times(1)).findPastActivityIdsForUser(eq(requestingUserId), eq(ParticipationStatus.participating), any());
    }

    // Helper method to create an "Everyone" tag
    private FriendTag createEveryoneTag(UUID ownerId) {
        return new FriendTag(UUID.randomUUID(), "Everyone", "#000000", ownerId, true, null);
    }


}