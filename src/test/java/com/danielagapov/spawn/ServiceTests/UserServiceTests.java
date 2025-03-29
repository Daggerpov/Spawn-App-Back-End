package com.danielagapov.spawn.ServiceTests;

import com.danielagapov.spawn.DTOs.FriendRequest.FetchFriendRequestDTO;
import com.danielagapov.spawn.DTOs.FriendTag.FriendTagDTO;
import com.danielagapov.spawn.DTOs.User.AbstractUserDTO;
import com.danielagapov.spawn.DTOs.User.BaseUserDTO;
import com.danielagapov.spawn.DTOs.User.FriendUser.FullFriendUserDTO;
import com.danielagapov.spawn.DTOs.User.FriendUser.RecommendedFriendUserDTO;
import com.danielagapov.spawn.DTOs.User.UserDTO;
import com.danielagapov.spawn.Exceptions.Base.BaseNotFoundException;
import com.danielagapov.spawn.Exceptions.Base.BaseSaveException;
import com.danielagapov.spawn.Exceptions.Base.BasesNotFoundException;
import com.danielagapov.spawn.Exceptions.Logger.ILogger;
import com.danielagapov.spawn.Models.FriendTag;
import com.danielagapov.spawn.Models.User;
import com.danielagapov.spawn.Repositories.IFriendTagRepository;
import com.danielagapov.spawn.Repositories.IUserFriendTagRepository;
import com.danielagapov.spawn.Repositories.IUserRepository;
import com.danielagapov.spawn.Services.FriendRequest.IFriendRequestService;
import com.danielagapov.spawn.Services.FriendTag.IFriendTagService;
import com.danielagapov.spawn.Services.User.UserService;
import com.danielagapov.spawn.Util.SearchedUserResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.dao.DataAccessException;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

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

    @InjectMocks
    private UserService userService;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this); // Initialize mocks
    }

    @Test
    void getAllUsers_ShouldReturnList_WhenUsersExist() {
        User user = new User(UUID.randomUUID(), "john_doe", "profile.jpg", "John", "Doe", "A bio", "john.doe@example.com");

        when(userRepository.findAll()).thenReturn(List.of(user));

        List<UserDTO> result = userService.getAllUsers();

        assertFalse(result.isEmpty());
        assertEquals("john_doe", result.get(0).getUsername());
        verify(userRepository, times(1)).findAll();
    }

    @Test
    void getUserById_ShouldReturnUser_WhenUserExists() {
        UUID userId = UUID.randomUUID();
        User user = new User(userId, "john_doe", "profile.jpg", "John", "Doe", "A bio", "john.doe@example.com");

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(friendTagService.getFriendTagsByOwnerId(userId)).thenReturn(List.of());

        UserDTO result = userService.getUserById(userId);

        assertEquals("john_doe", result.getUsername());
        verify(userRepository, times(1)).findById(userId);
    }

    @Test
    void saveUser_ShouldThrowException_WhenDatabaseErrorOccurs() {
        // Arrange
        UserDTO userDTO = new UserDTO(UUID.randomUUID(), List.of(), "john_doe", "profile.jpg", "John", "Doe", "A bio", List.of(), "john.doe@example.com");
        when(userRepository.save(any(User.class))).thenThrow(new DataAccessException("Database error") {
        });

        // Act & Assert
        BaseSaveException exception = assertThrows(BaseSaveException.class,
                () -> userService.saveUser(userDTO));

        assertTrue(exception.getMessage().contains("Failed to save user"));
        verify(userRepository, times(1)).save(any(User.class));
        verify(logger, times(1)).error("Database error");  // Verify that logging happened
    }

    @Test
    void updateUser_ShouldUpdateAllFields_WhenUserExists() {
        // Arrange
        UUID userId = UUID.randomUUID();
        User existingUser = new User(userId, "old_username", "profile.jpg", "OldFirst", "OldLast", "Old bio", "user@example.com");
        User updatedUser = new User(userId, "new_username", "profile.jpg", "NewFirst", "NewLast", "New bio", "user@example.com");

        when(userRepository.findById(userId)).thenReturn(Optional.of(existingUser));
        when(userRepository.save(any(User.class))).thenReturn(updatedUser);

        // Act
        var result = userService.updateUser(userId, "New bio", "new_username", "NewFirst", "NewLast");

        // Assert
        assertEquals("new_username", result.getUsername());
        assertEquals("NewFirst", result.getFirstName());
        assertEquals("NewLast", result.getLastName());
        assertEquals("New bio", result.getBio());

        verify(userRepository, times(1)).findById(userId);
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    void updateUser_ShouldHandleNullValues_WhenUpdatingPartially() {
        // Arrange
        UUID userId = UUID.randomUUID();
        User existingUser = new User(userId, "old_username", "profile.jpg", "OldFirst", "OldLast", "Old bio", "user@example.com");
        User updatedUser = new User(userId, "old_username", "profile.jpg", "OldFirst", "NewLast", "New bio", "user@example.com");

        when(userRepository.findById(userId)).thenReturn(Optional.of(existingUser));
        when(userRepository.save(any(User.class))).thenReturn(updatedUser);

        // Act - only updating bio and lastName, keeping other fields the same
        var result = userService.updateUser(userId, "New bio", null, null, "NewLast");

        // Assert
        assertEquals("old_username", result.getUsername());  // unchanged
        assertEquals("OldFirst", result.getFirstName());     // unchanged
        assertEquals("NewLast", result.getLastName());       // changed
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
                () -> userService.updateUser(userId, "New bio", "new_username", "NewFirst", "NewLast"));

        assertTrue(exception.getMessage().contains("User"));
        verify(userRepository, times(1)).findById(userId);
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void updateUser_ShouldLogAndThrowException_WhenDatabaseErrorOccurs() {
        // Arrange
        UUID userId = UUID.randomUUID();
        User existingUser = new User(userId, "username", "profile.jpg", "First", "Last", "Bio", "user@example.com");

        when(userRepository.findById(userId)).thenReturn(Optional.of(existingUser));
        when(userRepository.save(any(User.class))).thenThrow(new DataAccessException("Database error") {
        });

        // Act & Assert
        Exception exception = assertThrows(DataAccessException.class,
                () -> userService.updateUser(userId, "New bio", "new_username", "NewFirst", "NewLast"));

        assertTrue(exception.getMessage().contains("Database error"));
        verify(userRepository, times(1)).findById(userId);
        verify(userRepository, times(1)).save(any(User.class));
        verify(logger, times(1)).error(anyString());
    }

    @Test
    void deleteUserById_ShouldDeleteUser_WhenUserExists() {
        UUID userId = UUID.randomUUID();

        when(userRepository.findById(userId)).
                thenReturn(
                        Optional.of(
                                new User(userId, "JohnDoe123", null, "John", "Doe", null, "johndoe@anon.com")));

        assertDoesNotThrow(() -> userService.deleteUserById(userId));

        verify(userRepository, times(1)).deleteById(userId);
    }

    @Test
    void deleteUserById_ShouldReturnFalse_WhenDatabaseErrorOccurs() {
        UUID userId = UUID.randomUUID();
        when(userRepository.findById(userId)).
                thenReturn(
                        Optional.of(
                                new User(userId, "JohnDoe123", null, "John", "Doe", null, "johndoe@anon.com")));
        doThrow(new DataAccessException("Database error") {
        }).when(userRepository).deleteById(userId);

        boolean result = userService.deleteUserById(userId);

        assertFalse(result);
        verify(userRepository, times(1)).deleteById(userId);
        verify(logger, times(1)).error("Database error");  // Ensure logger is called
    }

    @Test
    void getAllUsers_ShouldThrowException_WhenDatabaseErrorOccurs() {
        when(userRepository.findAll()).thenThrow(new DataAccessException("Database error") {
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
    void deleteUserById_ShouldThrowException_WhenUserDoesNotExist() {
        UUID userId = UUID.randomUUID();
        when(userRepository.findById(userId)).
                thenReturn(Optional.empty());

        BaseNotFoundException exception = assertThrows(BaseNotFoundException.class, () -> userService.deleteUserById(userId));

        assertTrue(exception.getMessage().contains("User"));
        verify(userRepository, times(1)).findById(userId);
    }

    @Test
    void saveUser_ShouldLogException_WhenUnexpectedErrorOccurs() {
        UserDTO userDTO = new UserDTO(UUID.randomUUID(), List.of(), "john_doe", "profile.jpg", "John", "Doe", "A bio", List.of(), "john.doe@example.com");
        when(userRepository.save(any(User.class))).thenThrow(new RuntimeException("Unexpected error"));

        RuntimeException exception = assertThrows(RuntimeException.class, () -> userService.saveUser(userDTO));

        assertTrue(exception.getMessage().contains("Unexpected error"));
        verify(logger, times(1)).error("Unexpected error");
    }

    @Test
    void updateUser_ShouldLogAndThrowException_WhenUnexpectedErrorOccurs() {
        // Arrange
        UUID userId = UUID.randomUUID();
        when(userRepository.findById(userId)).thenThrow(new RuntimeException("Unexpected error"));

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> userService.updateUser(userId, "New bio", "new_username", "NewFirst", "NewLast"));

        assertTrue(exception.getMessage().contains("Unexpected error"));
        verify(logger, times(1)).error(anyString());
    }

    @Test
    void deleteUserById_ShouldLogException_WhenUnexpectedErrorOccurs() {
        UUID userId = UUID.randomUUID();
        when(userRepository.findById(userId)).
                thenReturn(
                        Optional.of(
                                new User(userId, "JohnDoe123", null, "John", "Doe", null, "johndoe@anon.com")));
        doThrow(new RuntimeException("Unexpected error")).when(userRepository).deleteById(userId);

        boolean result = userService.deleteUserById(userId);

        assertFalse(result);
        verify(logger, times(1)).error("Unexpected error");
    }

    @Test
    void deleteUserById_ShouldNotCallDelete_WhenUserDoesNotExist() {
        UUID userId = UUID.randomUUID();

        when(userRepository.existsById(userId)).thenReturn(false);

        assertThrows(BaseNotFoundException.class, () -> userService.deleteUserById(userId));
        verify(userRepository, never()).deleteById(userId);
    }

    @Test
    void getAllUsers_ShouldReturnEmptyList_WhenNoUsersExist() {
        when(userRepository.findAll()).thenReturn(List.of());

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
        when(friendTagRepository.findByOwnerId(user1Id))
                .thenReturn(List.of(createEveryoneTag(user1Id)));
        when(userFriendTagRepository.findFriendIdsByTagId(any()))
                .thenReturn(List.of(mutualFriend1Id, mutualFriend2Id, uniqueFriend1Id))
                .thenReturn(List.of(mutualFriend1Id, mutualFriend2Id, uniqueFriend2Id));

        // User2's friends: mutualFriend1, mutualFriend2, uniqueFriend2
        when(friendTagRepository.findByOwnerId(user2Id))
                .thenReturn(List.of(createEveryoneTag(user2Id)));

        int result = userService.getMutualFriendCount(user1Id, user2Id);

        assertEquals(2, result);
        verify(friendTagRepository, times(2)).findByOwnerId(any());
        verify(userFriendTagRepository, times(2)).findFriendIdsByTagId(any());
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
        verify(friendTagRepository, times(2)).findByOwnerId(any());
        verify(userFriendTagRepository, times(2)).findFriendIdsByTagId(any());
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
        verify(friendTagRepository, times(2)).findByOwnerId(any());
        verify(userFriendTagRepository, times(2)).findFriendIdsByTagId(any());
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
        verify(friendTagRepository, times(2)).findByOwnerId(any());
        verify(userFriendTagRepository, times(2)).findFriendIdsByTagId(any());
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
        verify(friendTagRepository, times(2)).findByOwnerId(any());
        verify(userFriendTagRepository, never()).findFriendIdsByTagId(any());
    }

    @Test
    void getMutualFriendCount_ShouldHandleEmptyOptionals() {
        UUID user1Id = UUID.randomUUID();
        UUID user2Id = UUID.randomUUID();

        when(friendTagRepository.findByOwnerId(any()))
                .thenReturn(List.of());

        int result = userService.getMutualFriendCount(user1Id, user2Id);

        assertEquals(0, result);
        verify(friendTagRepository, times(2)).findByOwnerId(any());
        verify(userFriendTagRepository, never()).findFriendIdsByTagId(any());
    }

    @Test
    void saveFriendToUser_ShouldSkip_WhenNoEveryoneTagFound() {
        UUID userId = UUID.randomUUID();
        UUID friendId = UUID.randomUUID();

        when(friendTagRepository.findEveryoneTagByOwnerId(any()))
                .thenReturn(Optional.empty());

        userService.saveFriendToUser(userId, friendId);

        verify(friendTagService, never()).saveUserToFriendTag(any(), any());
    }

    @Test
    void getFriendUserIdsByUserId_ShouldReturnEmptyList_WhenNoEveryoneTagFound() {
        UUID userId = UUID.randomUUID();

        when(friendTagRepository.findByOwnerId(userId))
                .thenReturn(List.of(new FriendTag(UUID.randomUUID(), "Test", "#000000", userId, false)));

        List<UUID> result = userService.getFriendUserIdsByUserId(userId);

        assertTrue(result.isEmpty());
        verify(friendTagRepository, times(1)).findByOwnerId(userId);
        verify(userFriendTagRepository, never()).findFriendIdsByTagId(any());
    }

    @Test
    void getRecommendedFriendsBySearch_ShouldReturnFilteredRecommendations_WhenSearchQueryIsProvided() {
        // Arrange
        UUID userId = UUID.randomUUID();
        RecommendedFriendUserDTO friend1 = new RecommendedFriendUserDTO(UUID.randomUUID(), "Alice", "Smith", "alice@example.com", "alice", "Bio", "profile.jpg", 1);
        RecommendedFriendUserDTO friend2 = new RecommendedFriendUserDTO(UUID.randomUUID(), "Bob", "Johnson", "bob@example.com", "bob", "Bio", "profile.jpg", 1);
        RecommendedFriendUserDTO friend3 = new RecommendedFriendUserDTO(UUID.randomUUID(), "Charlie", "Brown", "charlie@example.com", "charlie", "Bio", "profile.jpg", 1);

        // Mock all required friend request service methods
        when(friendRequestService.getIncomingFetchFriendRequestsByUserId(userId)).thenReturn(List.of());
        when(friendRequestService.getIncomingCreateFriendRequestsByUserId(userId)).thenReturn(List.of());
        when(friendRequestService.getSentFriendRequestsByUserId(userId)).thenReturn(List.of());

        // Use spy to isolate the test from internal implementations
        UserService spyUserService = spy(userService);
        doReturn(List.of(friend1, friend2, friend3)).when(spyUserService).getRecommendedMutuals(userId);
        doReturn(List.of()).when(spyUserService).getFullFriendUsersByUserId(userId);

        // Act
        SearchedUserResult result = spyUserService.getRecommendedFriendsBySearch(userId, "Alice");

        // Assert
        assertEquals(1, result.getSecond().size()); // Only Alice should be returned
        assertEquals(friend1, result.getSecond().get(0));
    }

    @Test
    void getRecommendedFriendsBySearch_ShouldReturnAllRecommendations_WhenSearchQueryIsEmpty() {
        // Arrange
        UUID userId = UUID.randomUUID();
        RecommendedFriendUserDTO friend1 = new RecommendedFriendUserDTO(UUID.randomUUID(), "Alice", "Smith", "alice@example.com", "alice", "Bio", "profile.jpg", 1);
        RecommendedFriendUserDTO friend2 = new RecommendedFriendUserDTO(UUID.randomUUID(), "Bob", "Johnson", "bob@example.com", "bob", "Bio", "profile.jpg", 1);

        // Mock the friend request service methods
        when(friendRequestService.getIncomingFetchFriendRequestsByUserId(userId)).thenReturn(List.of());
        when(friendRequestService.getIncomingCreateFriendRequestsByUserId(userId)).thenReturn(List.of());
        when(friendRequestService.getSentFriendRequestsByUserId(userId)).thenReturn(List.of());

        // Use spy to isolate the test from internal implementations
        UserService spyUserService = spy(userService);
        doReturn(List.of(friend1, friend2)).when(spyUserService).getLimitedRecommendedFriendsForUserId(userId);
        doReturn(List.of()).when(spyUserService).getFullFriendUsersByUserId(userId);

        // Act
        SearchedUserResult result = spyUserService.getRecommendedFriendsBySearch(userId, "");

        // Assert
        assertEquals(2, result.getSecond().size()); // Both friends should be returned
        assertTrue(result.getSecond().contains(friend1));
        assertTrue(result.getSecond().contains(friend2));
    }

    @Test
    void getRecommendedFriendsBySearch_ShouldReturnEmpty_WhenNoRecommendationsMatch() {
        // Arrange
        UUID userId = UUID.randomUUID();
        RecommendedFriendUserDTO friend1 = new RecommendedFriendUserDTO(UUID.randomUUID(), "Alice", "Smith", "alice@example.com", "alice", "Bio", "profile.jpg", 1);
        RecommendedFriendUserDTO friend2 = new RecommendedFriendUserDTO(UUID.randomUUID(), "Bob", "Johnson", "bob@example.com", "bob", "Bio", "profile.jpg", 1);

        // Mock the friend request service methods
        when(friendRequestService.getIncomingFetchFriendRequestsByUserId(userId)).thenReturn(List.of());
        when(friendRequestService.getIncomingCreateFriendRequestsByUserId(userId)).thenReturn(List.of());
        when(friendRequestService.getSentFriendRequestsByUserId(userId)).thenReturn(List.of());

        // Use spy to isolate the test from internal implementations
        UserService spyUserService = spy(userService);
        doReturn(List.of(friend1, friend2)).when(spyUserService).getRecommendedMutuals(userId);
        doReturn(List.of()).when(spyUserService).getFullFriendUsersByUserId(userId);

        // Act
        SearchedUserResult result = spyUserService.getRecommendedFriendsBySearch(userId, "Charlie");

        // Assert
        assertEquals(0, result.getSecond().size()); // No recommendations should match
    }

    @Test
    void getRecommendedFriendsBySearch_ShouldWorkWithQueryFullRecommendationsAndFriends() {
        UserService spyUserService = spy(userService);
        UUID user1Id = UUID.randomUUID();
        UUID user2Id = UUID.randomUUID();
        UUID user3Id = UUID.randomUUID();
        UUID user4Id = UUID.randomUUID();
        UUID user5Id = UUID.randomUUID();
        RecommendedFriendUserDTO user2Full = new RecommendedFriendUserDTO(user2Id, "Jane", "Doe", "jane.doe@example.com", "jane_doe", "A bio", "profile.jpg", 1);
        RecommendedFriendUserDTO user3Full = new RecommendedFriendUserDTO(user3Id, "Lorem", "Ipsum", "email@e.com", "person", "A bio", "profile.jpg", 1);
        RecommendedFriendUserDTO user4Full = new RecommendedFriendUserDTO(user4Id, "Lauren", "Ibson", "lauren_ibson@e.ca", "LaurenIbson", "A bio", "profile.jpg", 1);

        UUID ftId = UUID.randomUUID();
        // Very incomplete relationship but it should suffice for a test.
        FriendTagDTO ft = new FriendTagDTO(ftId, "Everyone", "#ffffff", user1Id, List.of(), true);
        FullFriendUserDTO user5Full = new FullFriendUserDTO(user5Id, "thatPerson", "profile.jpg", "That", "Person", "A bio", "thatPerson@email.com", List.of(ft));

        when(friendRequestService.getIncomingFetchFriendRequestsByUserId(user1Id)).thenReturn(List.of());
        when(friendRequestService.getIncomingCreateFriendRequestsByUserId(user1Id)).thenReturn(List.of());
        when(friendRequestService.getSentFriendRequestsByUserId(user1Id)).thenReturn(List.of());
        when(spyUserService.getRecommendedMutuals(user1Id)).thenReturn(List.of(user2Full, user3Full, user4Full));
        when(spyUserService.getFullFriendUsersByUserId(user1Id)).thenReturn(List.of(user5Full));

        SearchedUserResult res = spyUserService.getRecommendedFriendsBySearch(user1Id, "person");
        SearchedUserResult expected = new SearchedUserResult(List.of(), List.of(user3Full), List.of(user5Full));
        assertEquals(expected, res);
    }

    @Test
    void getRecommendedFriendsBySearch_ShouldWorkWithQueryFullRecommendations() {
        UserService spyUserService = spy(userService);
        UUID user1Id = UUID.randomUUID();
        UUID user2Id = UUID.randomUUID();
        UUID user3Id = UUID.randomUUID();
        UUID user4Id = UUID.randomUUID();
        RecommendedFriendUserDTO user2Full = new RecommendedFriendUserDTO(user2Id, "Jane", "Doe", "jane.doe@example.com", "jane_doe", "A bio", "profile.jpg", 1);
        RecommendedFriendUserDTO user3Full = new RecommendedFriendUserDTO(user3Id, "Lorem", "Ipsum", "email@e.com", "person", "A bio", "profile.jpg", 1);
        RecommendedFriendUserDTO user4Full = new RecommendedFriendUserDTO(user4Id, "Lauren", "Ibson", "lauren_ibson@e.ca", "LaurenIbson", "A bio", "profile.jpg", 1);

        when(friendRequestService.getIncomingFetchFriendRequestsByUserId(user1Id)).thenReturn(List.of());
        when(friendRequestService.getIncomingCreateFriendRequestsByUserId(user1Id)).thenReturn(List.of());
        when(friendRequestService.getSentFriendRequestsByUserId(user1Id)).thenReturn(List.of());
        when(spyUserService.getRecommendedMutuals(user1Id)).thenReturn(List.of(user2Full, user3Full, user4Full));
        when(spyUserService.getFullFriendUsersByUserId(user1Id)).thenReturn(List.of());

        SearchedUserResult res = spyUserService.getRecommendedFriendsBySearch(user1Id, "person");
        assertEquals(new SearchedUserResult(List.of(), List.of(user3Full), List.of()), res);
    }

    @Test
    void isQueryMatch_ShouldMatchPartialFirstName() {
        // Arrange
        UUID userId = UUID.randomUUID();
        AbstractUserDTO user = new BaseUserDTO(userId, "John", "Doe", "john@example.com", "johndoe", "Bio", "profile.jpg");

        // Act & Assert - Using reflection to access private method
        boolean result = (boolean) ReflectionTestUtils.invokeMethod(userService, "isQueryMatch", user, "Jo");
        assertTrue(result);
    }

    @Test
    void isQueryMatch_ShouldMatchPartialLastName() {
        // Arrange
        UUID userId = UUID.randomUUID();
        AbstractUserDTO user = new BaseUserDTO(userId, "John", "Doe", "john@example.com", "johndoe", "Bio", "profile.jpg");

        // Act & Assert - Using reflection to access private method
        boolean result = (boolean) ReflectionTestUtils.invokeMethod(userService, "isQueryMatch", user, "oe");
        assertTrue(result);
    }

    @Test
    void isQueryMatch_ShouldMatchPartialUsername() {
        // Arrange
        UUID userId = UUID.randomUUID();
        AbstractUserDTO user = new BaseUserDTO(userId, "John", "Doe", "john@example.com", "johndoe", "Bio", "profile.jpg");

        // Act & Assert - Using reflection to access private method
        boolean result = (boolean) ReflectionTestUtils.invokeMethod(userService, "isQueryMatch", user, "hnd");
        assertTrue(result);
    }

    @Test
    void isQueryMatch_ShouldBeCaseInsensitive() {
        // Arrange
        UUID userId = UUID.randomUUID();
        AbstractUserDTO user = new BaseUserDTO(userId, "John", "Doe", "john@example.com", "johndoe", "Bio", "profile.jpg");

        // Act & Assert - Using reflection to access private method
        boolean result = (boolean) ReflectionTestUtils.invokeMethod(userService, "isQueryMatch", user, "JOHN");
        assertTrue(result);
    }

    @Test
    void isQueryMatch_ShouldReturnFalseWhenNoMatch() {
        // Arrange
        UUID userId = UUID.randomUUID();
        AbstractUserDTO user = new BaseUserDTO(userId, "John", "Doe", "john@example.com", "johndoe", "Bio", "profile.jpg");

        // Act & Assert - Using reflection to access private method
        boolean result = (boolean) ReflectionTestUtils.invokeMethod(userService, "isQueryMatch", user, "xyz");
        assertFalse(result);
    }

    @Test
    void getRecommendedFriendsBySearch_ShouldHandleEmptySearchQuery() {
        // Arrange
        UUID userId = UUID.randomUUID();
        RecommendedFriendUserDTO friend = new RecommendedFriendUserDTO(UUID.randomUUID(), "Alice", "Smith", "alice@example.com", "alice", "Bio", "profile.jpg", 1);

        when(friendRequestService.getIncomingFetchFriendRequestsByUserId(userId)).thenReturn(List.of());
        when(friendRequestService.getIncomingCreateFriendRequestsByUserId(userId)).thenReturn(List.of());
        when(friendRequestService.getSentFriendRequestsByUserId(userId)).thenReturn(List.of());

        UserService spyUserService = spy(userService);
        doReturn(List.of(friend)).when(spyUserService).getLimitedRecommendedFriendsForUserId(userId);
        doReturn(List.of()).when(spyUserService).getFullFriendUsersByUserId(userId);

        // Act
        SearchedUserResult result = spyUserService.getRecommendedFriendsBySearch(userId, "");

        // Assert
        assertEquals(1, result.getSecond().size());
        assertEquals(friend, result.getSecond().get(0));
    }

    @Test
    void getRecommendedFriendsBySearch_ShouldHandleSpecialCharactersInQuery() {
        // Arrange
        UUID userId = UUID.randomUUID();

        when(friendRequestService.getIncomingFetchFriendRequestsByUserId(userId)).thenReturn(List.of());
        when(friendRequestService.getIncomingCreateFriendRequestsByUserId(userId)).thenReturn(List.of());
        when(friendRequestService.getSentFriendRequestsByUserId(userId)).thenReturn(List.of());

        UserService spyUserService = spy(userService);
        doReturn(List.of()).when(spyUserService).getRecommendedMutuals(userId);
        doReturn(List.of()).when(spyUserService).getFullFriendUsersByUserId(userId);

        // Act & Assert - Should not throw exceptions for unusual search terms
        assertDoesNotThrow(() -> spyUserService.getRecommendedFriendsBySearch(userId, "%^&*"));
    }

    @Test
    void getRecommendedFriendsBySearch_ShouldFilterIncomingFriendRequests() {
        // Arrange
        UUID userId = UUID.randomUUID();
        UUID requesterId = UUID.randomUUID();

        BaseUserDTO requesterInfo = new BaseUserDTO(requesterId, "David", "Search", "dsearch@example.com", "davidsearch", "Bio", "profile.jpg");
        FetchFriendRequestDTO friendRequest = mock(FetchFriendRequestDTO.class);
        when(friendRequest.getSenderUser()).thenReturn(requesterInfo);

        // Mock services
        when(friendRequestService.getIncomingFetchFriendRequestsByUserId(userId)).thenReturn(List.of(friendRequest));
        when(friendRequestService.getIncomingCreateFriendRequestsByUserId(userId)).thenReturn(List.of());
        when(friendRequestService.getSentFriendRequestsByUserId(userId)).thenReturn(List.of());

        UserService spyUserService = spy(userService);
        doReturn(List.of()).when(spyUserService).getRecommendedMutuals(userId);
        doReturn(List.of()).when(spyUserService).getFullFriendUsersByUserId(userId);

        // Act
        SearchedUserResult result = spyUserService.getRecommendedFriendsBySearch(userId, "search");

        // Assert
        assertEquals(1, result.getFirst().size());
        assertEquals(friendRequest, result.getFirst().get(0));
    }

    // Helper method to create an "Everyone" tag
    private FriendTag createEveryoneTag(UUID ownerId) {
        return new FriendTag(UUID.randomUUID(), "Everyone", "#000000", ownerId, true);
    }
}