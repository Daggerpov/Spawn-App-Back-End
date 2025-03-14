package com.danielagapov.spawn.ServiceTests;

import com.danielagapov.spawn.DTOs.User.UserDTO;
import com.danielagapov.spawn.Exceptions.Base.BaseNotFoundException;
import com.danielagapov.spawn.Exceptions.Base.BaseSaveException;
import com.danielagapov.spawn.Exceptions.Base.BasesNotFoundException;
import com.danielagapov.spawn.Exceptions.Logger.ILogger;
import com.danielagapov.spawn.Mappers.UserMapper;
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

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
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
    void replaceUser_ShouldUpdateUser_WhenUserExists() {
        UUID userId = UUID.randomUUID();
        UserDTO newUserDTO = new UserDTO(userId, List.of(), "john_doe_updated", "profile.jpg", "John", "Doe", "A bio updated", List.of(), "john.doe@example.com");
        User existingUser = new User(userId, "john_doe", "profile.jpg", "John", "Doe", "A bio", "john.doe@example.com");

        when(userRepository.findById(userId)).thenReturn(Optional.of(existingUser));
        when(userRepository.save(any(User.class))).thenReturn(existingUser);

        UserDTO result = userService.replaceUser(newUserDTO, userId);

        assertEquals("john_doe_updated", result.getUsername());
        verify(userRepository, times(1)).findById(userId);
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    void replaceUser_ShouldCreateUser_WhenUserDoesNotExist() {
        UUID userId = UUID.randomUUID();
        UserDTO newUserDTO = new UserDTO(userId, List.of(), "john_doe", "profile.jpg", "John", "Doe", "A bio", List.of(), "john.doe@example.com");
        User newUser = UserMapper.toEntity(newUserDTO);

        when(userRepository.findById(userId)).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenReturn(newUser);

        UserDTO result = userService.replaceUser(newUserDTO, userId);

        assertEquals("john_doe", result.getUsername());
        verify(userRepository, times(1)).findById(userId);
        verify(userRepository, times(1)).save(any(User.class));
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
    void replaceUser_ShouldLogException_WhenUnexpectedErrorOccurs() {
        UUID userId = UUID.randomUUID();
        UserDTO newUserDTO = new UserDTO(userId, List.of(), "john_doe", "profile.jpg", "John", "Doe", "A bio", List.of(), "john.doe@example.com");

        when(userRepository.findById(userId)).thenThrow(new RuntimeException("Unexpected error"));

        RuntimeException exception = assertThrows(RuntimeException.class, () -> userService.replaceUser(newUserDTO, userId));

        assertTrue(exception.getMessage().contains("Unexpected error"));
        verify(logger, times(1)).error("Unexpected error");
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
    void replaceUser_ShouldThrowException_WhenDatabaseErrorOccurs() {
        UUID userId = UUID.randomUUID();
        UserDTO newUserDTO = new UserDTO(userId, List.of(), "john_doe", "profile.jpg", "John", "Doe", "A bio", List.of(), "john.doe@example.com");
        User existingUser = new User(userId, "john_doe", "profile.jpg", "John", "Doe", "A bio", "john.doe@example.com");

        when(userRepository.findById(userId)).thenReturn(Optional.of(existingUser));
        when(userRepository.save(any(User.class))).thenThrow(new DataAccessException("Database error") {
        });

        DataAccessException exception = assertThrows(DataAccessException.class, () -> userService.replaceUser(newUserDTO, userId));

        assertTrue(exception.getMessage().contains("Database error"));
        verify(logger, atLeastOnce()).error("Database error");
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
            .thenReturn(Optional.of(List.of(createEveryoneTag(user1Id))));
        when(userFriendTagRepository.findFriendIdsByTagId(any()))
            .thenReturn(List.of(mutualFriend1Id, mutualFriend2Id, uniqueFriend1Id))
            .thenReturn(List.of(mutualFriend1Id, mutualFriend2Id, uniqueFriend2Id));

        // User2's friends: mutualFriend1, mutualFriend2, uniqueFriend2
        when(friendTagRepository.findByOwnerId(user2Id))
            .thenReturn(Optional.of(List.of(createEveryoneTag(user2Id))));

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
            .thenReturn(Optional.of(List.of(createEveryoneTag(user1Id))));
        when(userFriendTagRepository.findFriendIdsByTagId(any()))
            .thenReturn(List.of(friend1Id))
            .thenReturn(List.of(friend2Id));

        // User2's friends: friend2
        when(friendTagRepository.findByOwnerId(user2Id))
            .thenReturn(Optional.of(List.of(createEveryoneTag(user2Id))));

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
            .thenReturn(Optional.of(List.of(createEveryoneTag(user1Id))));
        when(userFriendTagRepository.findFriendIdsByTagId(any()))
            .thenReturn(List.of(friendId))
            .thenReturn(List.of()); // User2 has no friends

        // User2 has no friends (empty everyone tag)
        when(friendTagRepository.findByOwnerId(user2Id))
            .thenReturn(Optional.of(List.of(createEveryoneTag(user2Id))));

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
            .thenReturn(Optional.of(List.of(createEveryoneTag(UUID.randomUUID()))));
        when(userFriendTagRepository.findFriendIdsByTagId(any()))
            .thenReturn(List.of());

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
            .thenReturn(Optional.of(List.of()));

        int result = userService.getMutualFriendCount(user1Id, user2Id);

        assertEquals(0, result);
        verify(friendTagRepository, times(2)).findByOwnerId(any());
        verify(userFriendTagRepository, never()).findFriendIdsByTagId(any());
    }

    // Helper method to create an "Everyone" tag
    private FriendTag createEveryoneTag(UUID ownerId) {
        return new FriendTag(UUID.randomUUID(), "Everyone", "#000000", ownerId, true);
    }
}