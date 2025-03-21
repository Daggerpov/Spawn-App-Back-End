package com.danielagapov.spawn.ServiceTests;

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
import com.danielagapov.spawn.Services.FriendTag.IFriendTagService;
import com.danielagapov.spawn.Services.User.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.dao.DataAccessException;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.anyString;

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
        when(userRepository.save(any(User.class))).thenThrow(new DataAccessException("Database error") {});
        
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

        List<UserDTO> result = userService.getFriendsByFriendTagId(tagId);

        assertTrue(result.isEmpty());
        verify(userFriendTagRepository, times(1)).findFriendIdsByTagId(tagId);
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

    // Helper method to create an "Everyone" tag
    private FriendTag createEveryoneTag(UUID ownerId) {
        return new FriendTag(UUID.randomUUID(), "Everyone", "#000000", ownerId, true);
    }
}