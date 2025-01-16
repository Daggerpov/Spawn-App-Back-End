package com.danielagapov.spawn;

import com.danielagapov.spawn.DTOs.FriendTagDTO;
import com.danielagapov.spawn.DTOs.UserDTO;
import com.danielagapov.spawn.Enums.EntityType;
import com.danielagapov.spawn.Exceptions.Base.BaseNotFoundException;
import com.danielagapov.spawn.Exceptions.Base.BaseSaveException;
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
    void getAllUsers_ShouldThrowException_WhenDatabaseErrorOccurs() {
        when(userRepository.findAll()).thenThrow(new DataAccessException("Database error") {});

        BaseNotFoundException exception = assertThrows(BaseNotFoundException.class,
                () -> userService.getAllUsers());

        assertTrue(exception.getMessage().contains("Error fetching users"));
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
    void getUserById_ShouldThrowException_WhenUserNotFound() {
        UUID userId = UUID.randomUUID();
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        BaseNotFoundException exception = assertThrows(BaseNotFoundException.class,
                () -> userService.getUserById(userId));

        assertEquals("Entity not found with ID: " + userId, exception.getMessage());
        verify(userRepository, times(1)).findById(userId);
    }

    @Test
    void saveUser_ShouldSaveUser_WhenValidData() {
        UserDTO userDTO = new UserDTO(UUID.randomUUID(), List.of(), "john_doe", "profile.jpg", "John", "Doe", "A bio", List.of(), "john.doe@example.com");
        User user = UserMapper.toEntity(userDTO);

        when(userRepository.save(any(User.class))).thenReturn(user);

        assertDoesNotThrow(() -> userService.saveUser(userDTO));

        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    void saveUser_ShouldThrowException_WhenDatabaseErrorOccurs() {
        UserDTO userDTO = new UserDTO(UUID.randomUUID(), List.of(), "john_doe", "profile.jpg", "John", "Doe", "A bio", List.of(), "john.doe@example.com");

        when(userRepository.save(any(User.class))).thenThrow(new DataAccessException("Database error") {});

        BaseSaveException exception = assertThrows(BaseSaveException.class,
                () -> userService.saveUser(userDTO));

        assertTrue(exception.getMessage().contains("Failed to save user"));
        verify(userRepository, times(1)).save(any(User.class));
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

        when(userRepository.existsById(userId)).thenReturn(true);

        assertDoesNotThrow(() -> userService.deleteUserById(userId));

        verify(userRepository, times(1)).deleteById(userId);
    }

    @Test
    void deleteUserById_ShouldThrowException_WhenUserNotFound() {
        UUID userId = UUID.randomUUID();

        when(userRepository.existsById(userId)).thenReturn(false);

        BaseNotFoundException exception = assertThrows(BaseNotFoundException.class,
                () -> userService.deleteUserById(userId));

        assertEquals("Entity not found with ID: " + userId, exception.getMessage());
        verify(userRepository, never()).deleteById(userId);
    }

    @Test
    void deleteUserById_ShouldReturnFalse_WhenDatabaseErrorOccurs() {
        UUID userId = UUID.randomUUID();

        when(userRepository.existsById(userId)).thenReturn(true);
        doThrow(new DataAccessException("Database error") {}).when(userRepository).deleteById(userId);

        boolean result = userService.deleteUserById(userId);

        assertFalse(result);
        verify(userRepository, times(1)).deleteById(userId);
    }
}