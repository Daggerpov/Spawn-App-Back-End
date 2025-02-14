package com.danielagapov.spawn;

import com.danielagapov.spawn.DTOs.*;
import com.danielagapov.spawn.Exceptions.Base.BaseSaveException;
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
    void deleteUserById_ShouldReturnFalse_WhenDatabaseErrorOccurs() {
        UUID userId = UUID.randomUUID();

        when(userRepository.existsById(userId)).thenReturn(true);
        doThrow(new DataAccessException("Database error") {}).when(userRepository).deleteById(userId);

        boolean result = userService.deleteUserById(userId);

        assertFalse(result);
        verify(userRepository, times(1)).deleteById(userId);
    }

    @Test
    void getRecommendedFriendsBySearch_ShouldWorkProperly() {
        UUID userId = UUID.randomUUID();
        UUID userId2 = UUID.randomUUID();
        UUID ftId = UUID.randomUUID();
        User user1 = new User(userId, "john_doe", "profile.jpg", "John", "Doe", "A bio", "john.doe@example.com");
        FullUserDTO user1fdto = new FullUserDTO(userId, List.of(), "john_doe", "profile.jpg", "John", "Doe", "A bio", List.of(), "john.doe@example.com");
        UserDTO user1dto = new UserDTO(userId,
                List.of(userId2),
                user1.getUsername(),
                user1.getProfilePictureUrlString(),
                user1.getFirstName(),
                user1.getLastName(),
                user1.getBio(),
                List.of(ftId),
                user1.getEmail());

        User user2 = new User(userId2, "jane_doe", "profile.jpg", "Jane", "Doe", "A bio 2", "jane.doe@example.com");
        FullUserDTO user2dto = new FullUserDTO(userId, List.of(user1fdto),
                user2.getUsername(),
                user2.getProfilePictureUrlString(),
                user2.getFirstName(),
                user2.getLastName(),
                user2.getBio(), List.of(), "jane.doe@example.com");
        when(userRepository.findByFirstName("John")).thenReturn(List.of(user1));
        when(userRepository.findByLastName("Doe")).thenReturn(List.of(user1, user2));

        FullFriendTagDTO ft1 = new FullFriendTagDTO(ftId, "everyone", "ffffff", List.of(), true);

        RecommendedFriendUserDTO rfu1 = new RecommendedFriendUserDTO(userId, List.of(), "john_doe", "profile.jpg", "John", "Doe", "A bio", List.of())
    }

}