package com.danielagapov.spawn.ServiceTests;

import com.danielagapov.spawn.DTOs.FriendTag.FriendTagDTO;
import com.danielagapov.spawn.Exceptions.Base.BaseNotFoundException;
import com.danielagapov.spawn.Exceptions.Base.BaseSaveException;
import com.danielagapov.spawn.Exceptions.Base.BasesNotFoundException;
import com.danielagapov.spawn.Exceptions.Logger.ILogger;
import com.danielagapov.spawn.Mappers.FriendTagMapper;
import com.danielagapov.spawn.Models.FriendTag;
import com.danielagapov.spawn.Models.User.User;
import com.danielagapov.spawn.Models.UserFriendTag;
import com.danielagapov.spawn.Repositories.IFriendTagRepository;
import com.danielagapov.spawn.Repositories.IUserFriendTagRepository;
import com.danielagapov.spawn.Repositories.User.IUserRepository;
import com.danielagapov.spawn.Services.FriendTag.FriendTagService;
import com.danielagapov.spawn.Services.User.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.dao.DataAccessException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class FriendTagServiceTests {

    @Mock
    private IFriendTagRepository friendTagRepository;

    @Mock
    private IUserRepository userRepository;

    @Mock
    private IUserFriendTagRepository userFriendTagRepository;

    @Mock
    private UserService userService;

    @Mock
    private ILogger logger;

    @Mock
    private CacheManager cacheManager;

    @Mock
    private Cache friendTagsByOwnerIdCache;

    @Mock
    private Cache filteredFeedEventsCache;

    @Spy
    @InjectMocks
    private FriendTagService friendTagService;


    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);
        when(cacheManager.getCache("friendTagsByOwnerId")).thenReturn(friendTagsByOwnerIdCache);
        when(cacheManager.getCache("filteredFeedEvents")).thenReturn(filteredFeedEventsCache);
    }

    @Test
    void getAllFriendTags_ShouldThrowException_WhenDatabaseErrorOccurs() {
        when(friendTagRepository.findAll()).thenThrow(new DataAccessException("Database error") {
        });

        BasesNotFoundException exception = assertThrows(BasesNotFoundException.class,
                () -> friendTagService.getAllFriendTags());

        assertEquals("FriendTag's not found.", exception.getMessage());
        verify(friendTagRepository, times(1)).findAll();
    }

    @Test
    void getFriendTagById_ShouldReturnFriendTag_WhenFriendTagExists() {
        UUID friendTagId = UUID.randomUUID();
        FriendTag friendTag = new FriendTag(friendTagId, "Test Tag", "#FFFFFF", UUID.randomUUID(), false, null);

        when(friendTagRepository.findById(friendTagId)).thenReturn(Optional.of(friendTag));
        when(userService.getFriendUserIdsByFriendTagId(friendTagId)).thenReturn(List.of());

        FriendTagDTO result = friendTagService.getFriendTagById(friendTagId);

        assertEquals("Test Tag", result.getDisplayName());
        verify(friendTagRepository, times(1)).findById(friendTagId);
    }

    @Test
    void getFriendTagById_ShouldThrowException_WhenFriendTagNotFound() {
        UUID friendTagId = UUID.randomUUID();
        when(friendTagRepository.findById(friendTagId)).thenReturn(Optional.empty());

        BaseNotFoundException exception = assertThrows(BaseNotFoundException.class,
                () -> friendTagService.getFriendTagById(friendTagId));

        assertEquals("FriendTag entity not found with ID: " + friendTagId, exception.getMessage());
        verify(friendTagRepository, times(1)).findById(friendTagId);
    }

    @Test
    void saveFriendTag_ShouldSaveFriendTag_WhenValidData() {
        UUID ownerId = UUID.randomUUID();
        FriendTagDTO friendTagDTO = new FriendTagDTO(UUID.randomUUID(), "Test Tag", "#FFFFFF", ownerId, List.of(), false);
        FriendTag friendTag = FriendTagMapper.toEntity(friendTagDTO);

        when(friendTagRepository.save(any(FriendTag.class))).thenReturn(friendTag);

        assertDoesNotThrow(() -> friendTagService.saveFriendTag(friendTagDTO));

        verify(friendTagRepository, times(1)).save(any(FriendTag.class));
    }

    @Test
    void saveFriendTag_ShouldThrowException_WhenDatabaseErrorOccurs() {
        UUID ownerId = UUID.randomUUID();
        FriendTagDTO friendTagDTO = new FriendTagDTO(UUID.randomUUID(), "Test Tag", "#FFFFFF", ownerId, List.of(), false);

        when(friendTagRepository.save(any(FriendTag.class))).thenThrow(new DataAccessException("Database error") {
        });

        BaseSaveException exception = assertThrows(BaseSaveException.class,
                () -> friendTagService.saveFriendTag(friendTagDTO));

        assertTrue(exception.getMessage().contains("Failed to save friendTag"));
        verify(friendTagRepository, times(1)).save(any(FriendTag.class));
    }

    @Test
    void replaceFriendTag_ShouldUpdateFriendTag_WhenFriendTagExists() {
        UUID friendTagId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        FriendTagDTO newFriendTagDTO = new FriendTagDTO(friendTagId, "Updated Tag", "#000000", ownerId, List.of(), false);
        FriendTag existingFriendTag = new FriendTag(friendTagId, "Test Tag", "#FFFFFF", ownerId, false, null);

        when(friendTagRepository.findById(friendTagId)).thenReturn(Optional.of(existingFriendTag));
        when(friendTagRepository.save(any(FriendTag.class))).thenReturn(existingFriendTag);

        FriendTagDTO result = friendTagService.replaceFriendTag(newFriendTagDTO, friendTagId);

        assertEquals("Updated Tag", result.getDisplayName());
        verify(friendTagRepository, times(1)).findById(friendTagId);
        verify(friendTagRepository, times(1)).save(any(FriendTag.class));
    }

    @Test
    void replaceFriendTag_ShouldCreateFriendTag_WhenFriendTagDoesNotExist() {
        UUID friendTagId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        FriendTagDTO newFriendTagDTO = new FriendTagDTO(friendTagId, "Test Tag", "#FFFFFF", ownerId, List.of(), false);
        FriendTag newFriendTag = FriendTagMapper.toEntity(newFriendTagDTO);

        when(friendTagRepository.findById(friendTagId)).thenReturn(Optional.empty());
        when(friendTagRepository.save(any(FriendTag.class))).thenReturn(newFriendTag);

        FriendTagDTO result = friendTagService.replaceFriendTag(newFriendTagDTO, friendTagId);

        assertEquals("Test Tag", result.getDisplayName());
        verify(friendTagRepository, times(1)).findById(friendTagId);
        verify(friendTagRepository, times(1)).save(any(FriendTag.class));
    }

    @Test
    void deleteFriendTagById_ShouldDeleteFriendTag_WhenFriendTagExists() {
        UUID friendTagId = UUID.randomUUID();

        // Create a FriendTag that is NOT an "Everyone" tag
        FriendTag friendTag = new FriendTag(friendTagId, "Test Tag", "#FFFFFF", UUID.randomUUID(), false, null);

        when(friendTagRepository.existsById(friendTagId)).thenReturn(true);
        when(friendTagRepository.findById(friendTagId)).thenReturn(Optional.of(friendTag)); // Mocking retrieval
        when(userFriendTagRepository.findAllById(List.of(friendTagId))).thenReturn(List.of()); // No associated UserFriendTags

        boolean result = friendTagService.deleteFriendTagById(friendTagId);

        assertTrue(result);
        verify(friendTagRepository, times(1)).deleteById(friendTagId);
    }


    @Test
    void deleteFriendTagById_ShouldThrowException_WhenFriendTagDoesNotExist() {
        UUID friendTagId = UUID.randomUUID();

        when(friendTagRepository.existsById(friendTagId)).thenReturn(false);

        BaseNotFoundException exception = assertThrows(BaseNotFoundException.class,
                () -> friendTagService.deleteFriendTagById(friendTagId));

        assertEquals("FriendTag entity not found with ID: " + friendTagId, exception.getMessage());
        verify(friendTagRepository, never()).deleteById(friendTagId);
    }

    @Test
    void saveUserToFriendTag_ShouldThrowException_WhenFriendTagNotFound() {
        UUID friendTagId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        when(friendTagRepository.existsById(friendTagId)).thenReturn(false);

        BaseNotFoundException exception = assertThrows(BaseNotFoundException.class,
                () -> friendTagService.saveUserToFriendTag(friendTagId, userId));

        assertEquals("FriendTag entity not found with ID: " + friendTagId, exception.getMessage());
        verify(userFriendTagRepository, never()).save(any(UserFriendTag.class));
    }

    @Test
    void saveUserToFriendTag_ShouldThrowException_WhenUserNotFound() {
        UUID friendTagId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        when(friendTagRepository.existsById(friendTagId)).thenReturn(true);
        when(userRepository.existsById(userId)).thenReturn(false);

        BaseNotFoundException exception = assertThrows(BaseNotFoundException.class,
                () -> friendTagService.saveUserToFriendTag(friendTagId, userId));

        assertEquals("User entity not found with ID: " + userId, exception.getMessage());
        verify(userFriendTagRepository, never()).save(any(UserFriendTag.class));
    }

    @Test
    void saveUserToFriendTag_ShouldThrowException_WhenDatabaseErrorOccurs() {
        UUID friendTagId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        FriendTag friendTag = new FriendTag(friendTagId, "Test Tag", "#FFFFFF", UUID.randomUUID(), false, null);
        User user = new User(userId, "john_doe", "profile.jpg", "John", "Doe", "A bio", "john.doe@example.com");

        when(friendTagRepository.existsById(friendTagId)).thenReturn(true);
        when(userRepository.existsById(userId)).thenReturn(true);
        when(friendTagRepository.findById(friendTagId)).thenReturn(Optional.of(friendTag));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        doThrow(new DataAccessException("Database error") {
        }).when(userFriendTagRepository).save(any(UserFriendTag.class));

        assertThrows(BaseSaveException.class, () -> friendTagService.saveUserToFriendTag(friendTagId, userId));

        verify(userFriendTagRepository, times(1)).save(any(UserFriendTag.class));
    }

    @Test
    void saveFriendTag_ShouldThrowException_WhenUnexpectedErrorOccurs() {
        UUID ownerId = UUID.randomUUID();
        FriendTagDTO friendTagDTO = new FriendTagDTO(UUID.randomUUID(), "Test Tag", "#FFFFFF", ownerId, List.of(), false);

        when(friendTagRepository.save(any(FriendTag.class))).thenThrow(new RuntimeException("Unexpected error"));

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> friendTagService.saveFriendTag(friendTagDTO));

        assertEquals("Unexpected error", exception.getMessage());
        verify(logger, times(1)).error("Unexpected error");
    }

    @Test
    void deleteFriendTagById_ShouldDeleteFriendTag_WhenValid() {
        UUID friendTagId = UUID.randomUUID();
        UUID userFriendTagId = UUID.randomUUID();

        // Create a FriendTag that is NOT an "Everyone" tag
        FriendTag friendTag = new FriendTag(friendTagId, "Test Tag", "#FFFFFF", UUID.randomUUID(), false, null);
        UserFriendTag userFriendTag = new UserFriendTag();
        userFriendTag.setId(userFriendTagId);

        when(friendTagRepository.existsById(friendTagId)).thenReturn(true);
        when(friendTagRepository.findById(friendTagId)).thenReturn(Optional.of(friendTag)); // Avoids getFriendTagById()
        when(userFriendTagRepository.findAllById(List.of(friendTagId))).thenReturn(List.of(userFriendTag));

        boolean result = friendTagService.deleteFriendTagById(friendTagId);

        assertTrue(result);
        verify(userFriendTagRepository, times(1)).deleteById(userFriendTagId);
        verify(friendTagRepository, times(1)).deleteById(friendTagId);
    }

    @Test
    void deleteFriendTagById_ShouldDeleteAssociatedUserFriendTags() {
        UUID friendTagId = UUID.randomUUID();
        UUID userFriendTagId = UUID.randomUUID();

        // Create a mock FriendTag that is NOT an "Everyone" tag
        FriendTag friendTag = new FriendTag(friendTagId, "Test Tag", "#FFFFFF", UUID.randomUUID(), false, null);
        UserFriendTag userFriendTag = new UserFriendTag();
        userFriendTag.setId(userFriendTagId);

        when(friendTagRepository.existsById(friendTagId)).thenReturn(true);
        when(friendTagRepository.findById(friendTagId)).thenReturn(Optional.of(friendTag)); // Avoids getFriendTagById()
        when(userFriendTagRepository.findAllById(List.of(friendTagId))).thenReturn(List.of(userFriendTag));

        boolean result = friendTagService.deleteFriendTagById(friendTagId);

        assertTrue(result);
        verify(userFriendTagRepository, times(1)).deleteById(userFriendTagId);
        verify(friendTagRepository, times(1)).deleteById(friendTagId);
    }

    @Test
    void deleteFriendTagById_ShouldThrowException_WhenFriendTagNotFound() {
        UUID friendTagId = UUID.randomUUID();

        when(friendTagRepository.existsById(friendTagId)).thenReturn(false);

        assertThrows(BaseNotFoundException.class, () -> friendTagService.deleteFriendTagById(friendTagId));

        verify(friendTagRepository, never()).deleteById(any());
        verify(userFriendTagRepository, never()).deleteById(any());
    }

    @Test
    void getAllFriendTags_ShouldReturnEmptyList_WhenNoTagsExist() {
        when(friendTagRepository.findAll()).thenReturn(List.of());

        List<FriendTagDTO> result = friendTagService.getAllFriendTags();

        assertTrue(result.isEmpty());
        verify(friendTagRepository, times(1)).findAll();
    }

    @Test
    void getFriendTagsByOwnerId_ShouldReturnEmptyList_WhenOwnerHasNoTags() {
        UUID ownerId = UUID.randomUUID();

        // Properly stub the findByOwnerId to return an empty list
        when(friendTagRepository.findByOwnerId(ownerId)).thenReturn(new ArrayList<>());
        // Mock the maps that are used in the service
        when(userService.getOwnerUserIdsMap()).thenReturn(new HashMap<>());
        when(userService.getFriendUserIdsMap()).thenReturn(new HashMap<>());

        List<FriendTagDTO> result = friendTagService.getFriendTagsByOwnerId(ownerId);

        assertTrue(result.isEmpty());
        verify(friendTagRepository, times(1)).findByOwnerId(ownerId);
        verify(userService, times(1)).getOwnerUserIdsMap();
        verify(userService, times(1)).getFriendUserIdsMap();
    }

    @Test
    void removeUserFromFriendTag_ShouldRemoveUser_WhenFriendTagAndUserExist() {
        UUID friendTagId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        when(friendTagRepository.existsById(friendTagId)).thenReturn(true);
        when(userRepository.existsById(userId)).thenReturn(true);

        assertDoesNotThrow(() -> friendTagService.removeUserFromFriendTag(friendTagId, userId));

        verify(userFriendTagRepository, times(1)).deleteByFriendTagIdAndFriendId(friendTagId, userId);
    }

    @Test
    void removeUserFromFriendTag_ShouldThrowException_WhenFriendTagNotFound() {
        UUID friendTagId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        when(friendTagRepository.existsById(friendTagId)).thenReturn(false);

        BaseNotFoundException exception = assertThrows(BaseNotFoundException.class,
                () -> friendTagService.removeUserFromFriendTag(friendTagId, userId));

        assertEquals("FriendTag entity not found with ID: " + friendTagId, exception.getMessage());
        verify(userFriendTagRepository, never()).deleteByFriendTagIdAndFriendId(any(), any());
    }

    @Test
    void removeUserFromFriendTag_ShouldThrowException_WhenUserNotFound() {
        UUID friendTagId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        when(friendTagRepository.existsById(friendTagId)).thenReturn(true);
        when(userRepository.existsById(userId)).thenReturn(false);

        BaseNotFoundException exception = assertThrows(BaseNotFoundException.class,
                () -> friendTagService.removeUserFromFriendTag(friendTagId, userId));

        assertEquals("User entity not found with ID: " + userId, exception.getMessage());
        verify(userFriendTagRepository, never()).deleteByFriendTagIdAndFriendId(any(), any());
    }

    @Test
    void removeUserFromFriendTag_ShouldThrowException_WhenDatabaseErrorOccurs() {
        UUID friendTagId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        when(friendTagRepository.existsById(friendTagId)).thenReturn(true);
        when(userRepository.existsById(userId)).thenReturn(true);
        doThrow(new DataAccessException("Database error") {
        }).when(userFriendTagRepository).deleteByFriendTagIdAndFriendId(friendTagId, userId);

        BaseSaveException exception = assertThrows(BaseSaveException.class,
                () -> friendTagService.removeUserFromFriendTag(friendTagId, userId));

        assertTrue(exception.getMessage().contains("Failed to remove UserFriendTag (friend from friend tag)"));
        verify(userFriendTagRepository, times(1)).deleteByFriendTagIdAndFriendId(friendTagId, userId);
    }
}
