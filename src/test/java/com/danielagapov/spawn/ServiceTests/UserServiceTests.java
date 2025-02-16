package com.danielagapov.spawn.ServiceTests;

import com.danielagapov.spawn.DTOs.UserDTO;
import com.danielagapov.spawn.Exceptions.Base.BaseNotFoundException;
import com.danielagapov.spawn.Exceptions.Base.BaseSaveException;
import com.danielagapov.spawn.Exceptions.Base.BasesNotFoundException;
import com.danielagapov.spawn.Exceptions.Logger.ILogger;
import com.danielagapov.spawn.Mappers.UserMapper;
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
        assertEquals("john_doe", result.get(0).username());
        verify(userRepository, times(1)).findAll();
    }

    @Test
    void getUserById_ShouldReturnUser_WhenUserExists() {
        UUID userId = UUID.randomUUID();
        User user = new User(userId, "john_doe", "profile.jpg", "John", "Doe", "A bio", "john.doe@example.com");

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(friendTagService.getFriendTagsByOwnerId(userId)).thenReturn(List.of());

        UserDTO result = userService.getUserById(userId);

        assertEquals("john_doe", result.username());
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
        verify(logger, times(1)).log("Database error");  // Verify that logging happened
    }


    @Test
    void replaceUser_ShouldUpdateUser_WhenUserExists() {
        UUID userId = UUID.randomUUID();
        UserDTO newUserDTO = new UserDTO(userId, List.of(), "john_doe_updated", "profile.jpg", "John", "Doe", "A bio updated", List.of(), "john.doe@example.com");
        User existingUser = new User(userId, "john_doe", "profile.jpg", "John", "Doe", "A bio", "john.doe@example.com");

        when(userRepository.findById(userId)).thenReturn(Optional.of(existingUser));
        when(userRepository.save(any(User.class))).thenReturn(existingUser);

        UserDTO result = userService.replaceUser(newUserDTO, userId);

        assertEquals("john_doe_updated", result.username());
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

        assertEquals("john_doe", result.username());
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
        verify(logger, times(1)).log("Database error");  // Ensure logger is called
    }

    @Test
    void getAllUsers_ShouldThrowException_WhenDatabaseErrorOccurs() {
        when(userRepository.findAll()).thenThrow(new DataAccessException("Database error") {
        });

        BasesNotFoundException exception = assertThrows(BasesNotFoundException.class, () -> userService.getAllUsers());

        assertTrue(exception.getMessage().contains("User"));
        verify(logger, atLeastOnce()).log("Database error");
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
        verify(logger, times(1)).log("Unexpected error");
    }

    @Test
    void replaceUser_ShouldLogException_WhenUnexpectedErrorOccurs() {
        UUID userId = UUID.randomUUID();
        UserDTO newUserDTO = new UserDTO(userId, List.of(), "john_doe", "profile.jpg", "John", "Doe", "A bio", List.of(), "john.doe@example.com");

        when(userRepository.findById(userId)).thenThrow(new RuntimeException("Unexpected error"));

        RuntimeException exception = assertThrows(RuntimeException.class, () -> userService.replaceUser(newUserDTO, userId));

        assertTrue(exception.getMessage().contains("Unexpected error"));
        verify(logger, times(1)).log("Unexpected error");
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
        verify(logger, times(1)).log("Unexpected error");
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
        verify(logger, atLeastOnce()).log("Database error");
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
}