//package com.danielagapov.spawn;
//
//import com.danielagapov.spawn.DTOs.FriendTagDTO;
//import com.danielagapov.spawn.DTOs.UserDTO;
//import com.danielagapov.spawn.Exceptions.Base.BaseNotFoundException;
//import com.danielagapov.spawn.Exceptions.Base.BaseSaveException;
//import com.danielagapov.spawn.Exceptions.Base.BasesNotFoundException;
//import com.danielagapov.spawn.Mappers.FriendTagMapper;
//import com.danielagapov.spawn.Models.FriendTag;
//import com.danielagapov.spawn.Models.User;
//import com.danielagapov.spawn.Models.UserFriendTag;
//import com.danielagapov.spawn.Repositories.IFriendTagRepository;
//import com.danielagapov.spawn.Repositories.IUserFriendTagRepository;
//import com.danielagapov.spawn.Repositories.IUserRepository;
//import com.danielagapov.spawn.Services.FriendTag.FriendTagService;
//import com.danielagapov.spawn.Services.User.UserService;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.mockito.InjectMocks;
//import org.mockito.Mock;
//import org.mockito.MockitoAnnotations;
//import org.springframework.dao.DataAccessException;
//
//import java.util.List;
//import java.util.Map;
//import java.util.Optional;
//import java.util.UUID;
//
//import static org.junit.jupiter.api.Assertions.*;
//import static org.mockito.Mockito.*;
//
//public class FriendTagServiceTests {
//
//    @Mock
//    private IFriendTagRepository friendTagRepository;
//
//    @Mock
//    private IUserRepository userRepository;
//
//    @Mock
//    private IUserFriendTagRepository userFriendTagRepository;
//
//    @Mock
//    private UserService userService;
//
//    @InjectMocks
//    private FriendTagService friendTagService;
//
//    @BeforeEach
//    void setup() {
//        MockitoAnnotations.openMocks(this); // Initialize mocks
//    }
//
//    @Test
//    void getAllFriendTags_ShouldReturnList_WhenFriendTagsExist() {
//        FriendTag friendTag = new FriendTag(UUID.randomUUID(), "Test Tag", "#FFFFFF", UUID.randomUUID(), false);
//        UserDTO owner = new UserDTO(UUID.randomUUID(), List.of(), "john_doe", "profile.jpg", "John", "Doe", "A bio", List.of(), "john.doe@example.com");
//
//        when(friendTagRepository.findAll()).thenReturn(List.of(friendTag));
//        when(userService.getOwnerUserIdsMap()).thenReturn(Map.of(friendTag, owner));
//        when(userService.getFriendsMap()).thenReturn(Map.of(friendTag, List.of()));
//
//        List<FriendTagDTO> result = friendTagService.getAllFriendTags();
//
//        assertFalse(result.isEmpty());
//        assertEquals("Test Tag", result.get(0).displayName());
//        verify(friendTagRepository, times(1)).findAll();
//    }
//
//    @Test
//    void getAllFriendTags_ShouldThrowException_WhenDatabaseErrorOccurs() {
//        when(friendTagRepository.findAll()).thenThrow(new DataAccessException("Database error") {});
//
//        BasesNotFoundException exception = assertThrows(BasesNotFoundException.class,
//                () -> friendTagService.getAllFriendTags());
//
//        assertTrue(exception.getMessage().contains("Error fetching friendTags"));
//        verify(friendTagRepository, times(1)).findAll();
//    }
//
//    @Test
//    void getFriendTagById_ShouldReturnFriendTag_WhenFriendTagExists() {
//        UUID friendTagId = UUID.randomUUID();
//        FriendTag friendTag = new FriendTag(friendTagId, "Test Tag", "#FFFFFF", UUID.randomUUID(), false);
//        UserDTO owner = new UserDTO(UUID.randomUUID(), List.of(), "john_doe", "profile.jpg", "John", "Doe", "A bio", List.of(), "john.doe@example.com");
//
//        when(friendTagRepository.findById(friendTagId)).thenReturn(Optional.of(friendTag));
//        when(userService.getUserById(friendTag.getOwnerId())).thenReturn(owner);
//        when(userService.getFriendsByFriendTagId(friendTagId)).thenReturn(List.of());
//
//        FriendTagDTO result = friendTagService.getFriendTagById(friendTagId);
//
//        assertEquals("Test Tag", result.displayName());
//        verify(friendTagRepository, times(1)).findById(friendTagId);
//    }
//
//    @Test
//    void getFriendTagById_ShouldThrowException_WhenFriendTagNotFound() {
//        UUID friendTagId = UUID.randomUUID();
//        when(friendTagRepository.findById(friendTagId)).thenReturn(Optional.empty());
//
//        BaseNotFoundException exception = assertThrows(BaseNotFoundException.class,
//                () -> friendTagService.getFriendTagById(friendTagId));
//
//        assertEquals("Entity not found with ID: " + friendTagId, exception.getMessage());
//        verify(friendTagRepository, times(1)).findById(friendTagId);
//    }
//
//    @Test
//    void saveFriendTag_ShouldSaveFriendTag_WhenValidData() {
//        UUID ownerId = UUID.randomUUID();
//        FriendTagDTO friendTagDTO = new FriendTagDTO(UUID.randomUUID(), "Test Tag", "#FFFFFF", new UserDTO(ownerId, List.of(), "john_doe", "profile.jpg", "John", "Doe", "A bio", List.of(), "john.doe@example.com"), List.of(), false);
//        FriendTag friendTag = FriendTagMapper.toEntity(friendTagDTO);
//
//        when(friendTagRepository.save(any(FriendTag.class))).thenReturn(friendTag);
//        when(userService.getUserById(ownerId)).thenReturn(friendTagDTO.owner());
//
//        assertDoesNotThrow(() -> friendTagService.saveFriendTag(friendTagDTO));
//
//        verify(friendTagRepository, times(1)).save(any(FriendTag.class));
//    }
//
//    @Test
//    void saveFriendTag_ShouldThrowException_WhenDatabaseErrorOccurs() {
//        UUID ownerId = UUID.randomUUID();
//        FriendTagDTO friendTagDTO = new FriendTagDTO(UUID.randomUUID(), "Test Tag", "#FFFFFF", new UserDTO(ownerId, List.of(), "john_doe", "profile.jpg", "John", "Doe", "A bio", List.of(), "john.doe@example.com"), List.of(), true);
//
//        when(friendTagRepository.save(any(FriendTag.class))).thenThrow(new DataAccessException("Database error") {});
//
//        BaseSaveException exception = assertThrows(BaseSaveException.class,
//                () -> friendTagService.saveFriendTag(friendTagDTO));
//
//        assertTrue(exception.getMessage().contains("Failed to save friendTag"));
//        verify(friendTagRepository, times(1)).save(any(FriendTag.class));
//    }
//
//    @Test
//    void replaceFriendTag_ShouldUpdateFriendTag_WhenFriendTagExists() {
//        UUID friendTagId = UUID.randomUUID();
//        UUID ownerId = UUID.randomUUID();
//        FriendTagDTO newFriendTagDTO = new FriendTagDTO(friendTagId, "Updated Tag", "#000000", new UserDTO(ownerId, List.of(), "john_doe", "profile.jpg", "John", "Doe", "A bio", List.of(), "john.doe@example.com"), List.of(), false);
//        FriendTag existingFriendTag = new FriendTag(friendTagId, "Test Tag", "#FFFFFF", ownerId, false);
//
//        when(friendTagRepository.findById(friendTagId)).thenReturn(Optional.of(existingFriendTag));
//        when(friendTagRepository.save(any(FriendTag.class))).thenReturn(existingFriendTag);
//        when(userService.getUserById(ownerId)).thenReturn(newFriendTagDTO.owner());
//
//        FriendTagDTO result = friendTagService.replaceFriendTag(newFriendTagDTO, friendTagId);
//
//        assertEquals("Updated Tag", result.displayName());
//        verify(friendTagRepository, times(1)).findById(friendTagId);
//        verify(friendTagRepository, times(1)).save(any(FriendTag.class));
//    }
//
//    @Test
//    void replaceFriendTag_ShouldCreateFriendTag_WhenFriendTagDoesNotExist() {
//        UUID friendTagId = UUID.randomUUID();
//        UUID ownerId = UUID.randomUUID();
//        FriendTagDTO newFriendTagDTO = new FriendTagDTO(friendTagId, "Test Tag", "#FFFFFF", new UserDTO(ownerId, List.of(), "john_doe", "profile.jpg", "John", "Doe", "A bio", List.of(), "john.doe@example.com"), List.of(), false);
//        FriendTag newFriendTag = FriendTagMapper.toEntity(newFriendTagDTO);
//
//        when(friendTagRepository.findById(friendTagId)).thenReturn(Optional.empty());
//        when(friendTagRepository.save(any(FriendTag.class))).thenReturn(newFriendTag);
//        when(userService.getUserById(ownerId)).thenReturn(newFriendTagDTO.owner());
//
//        FriendTagDTO result = friendTagService.replaceFriendTag(newFriendTagDTO, friendTagId);
//
//        assertEquals("Test Tag", result.displayName());
//        verify(friendTagRepository, times(1)).findById(friendTagId);
//        verify(friendTagRepository, times(1)).save(any(FriendTag.class));
//    }
//
//    @Test
//    void deleteFriendTagById_ShouldDeleteFriendTag_WhenFriendTagExists() {
//        UUID friendTagId = UUID.randomUUID();
//
//        when(friendTagRepository.existsById(friendTagId)).thenReturn(true);
//
//        assertDoesNotThrow(() -> friendTagService.deleteFriendTagById(friendTagId));
//
//        verify(friendTagRepository, times(1)).deleteById(friendTagId);
//    }
//
//    @Test
//    void deleteFriendTagById_ShouldThrowException_WhenFriendTagNotFound() {
//        UUID friendTagId = UUID.randomUUID();
//
//        when(friendTagRepository.existsById(friendTagId)).thenReturn(false);
//
//        BaseNotFoundException exception = assertThrows(BaseNotFoundException.class,
//                () -> friendTagService.deleteFriendTagById(friendTagId));
//
//        assertEquals("Entity not found with ID: " + friendTagId, exception.getMessage());
//        verify(friendTagRepository, never()).deleteById(friendTagId);
//    }
//
//    @Test
//    void deleteFriendTagById_ShouldReturnFalse_WhenDatabaseErrorOccurs() {
//        UUID friendTagId = UUID.randomUUID();
//
//        when(friendTagRepository.existsById(friendTagId)).thenReturn(true);
//        doThrow(new DataAccessException("Database error") {}).when(friendTagRepository).deleteById(friendTagId);
//
//        boolean result = friendTagService.deleteFriendTagById(friendTagId);
//
//        assertFalse(result);
//        verify(friendTagRepository, times(1)).deleteById(friendTagId);
//    }
//
//    @Test
//    void saveUserToFriendTag_ShouldSaveUser_WhenValidData() {
//        UUID friendTagId = UUID.randomUUID();
//        UUID userId = UUID.randomUUID();
//        FriendTag friendTag = new FriendTag(friendTagId, "Test Tag", "#FFFFFF", UUID.randomUUID(), false);
//        User user = new User(userId, "john_doe", "profile.jpg", "John", "Doe", "A bio", "john.doe@example.com");
//
//        when(friendTagRepository.existsById(friendTagId)).thenReturn(true);
//        when(userRepository.existsById(userId)).thenReturn(true);
//        when(friendTagRepository.findById(friendTagId)).thenReturn(Optional.of(friendTag));
//        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
//
//        assertDoesNotThrow(() -> friendTagService.saveUserToFriendTag(friendTagId, userId));
//
//        verify(userFriendTagRepository, times(1)).save(any(UserFriendTag.class));
//    }
//
//    @Test
//    void saveUserToFriendTag_ShouldThrowException_WhenFriendTagNotFound() {
//        UUID friendTagId = UUID.randomUUID();
//        UUID userId = UUID.randomUUID();
//
//        when(friendTagRepository.existsById(friendTagId)).thenReturn(false);
//
//        BaseNotFoundException exception = assertThrows(BaseNotFoundException.class,
//                () -> friendTagService.saveUserToFriendTag(friendTagId, userId));
//
//        assertEquals("Entity not found with ID: " + friendTagId, exception.getMessage());
//        verify(userFriendTagRepository, never()).save(any(UserFriendTag.class));
//    }
//
//    @Test
//    void saveUserToFriendTag_ShouldThrowException_WhenUserNotFound() {
//        UUID friendTagId = UUID.randomUUID();
//        UUID userId = UUID.randomUUID();
//        FriendTag friendTag = new FriendTag(friendTagId, "Test Tag", "#FFFFFF", UUID.randomUUID(), false);
//
//        when(friendTagRepository.existsById(friendTagId)).thenReturn(true);
//        when(userRepository.existsById(userId)).thenReturn(false);
//        when(friendTagRepository.findById(friendTagId)).thenReturn(Optional.of(friendTag));
//
//        BaseNotFoundException exception = assertThrows(BaseNotFoundException.class,
//                () -> friendTagService.saveUserToFriendTag(friendTagId, userId));
//
//        assertEquals("Entity not found with ID: " + userId, exception.getMessage());
//        verify(userFriendTagRepository, never()).save(any(UserFriendTag.class));
//    }
//
//    @Test
//    void saveUserToFriendTag_ShouldThrowException_WhenDatabaseErrorOccurs() {
//        UUID friendTagId = UUID.randomUUID();
//        UUID userId = UUID.randomUUID();
//        FriendTag friendTag = new FriendTag(friendTagId, "Test Tag", "#FFFFFF", UUID.randomUUID(), false);
//        User user = new User(userId, "john_doe", "profile.jpg", "John", "Doe", "A bio", "john.doe@example.com");
//
//        when(friendTagRepository.existsById(friendTagId)).thenReturn(true);
//        when(userRepository.existsById(userId)).thenReturn(true);
//        when(friendTagRepository.findById(friendTagId)).thenReturn(Optional.of(friendTag));
//        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
//        doThrow(new DataAccessException("Database error") {}).when(userFriendTagRepository).save(any(UserFriendTag.class));
//
//        BaseSaveException exception = assertThrows(BaseSaveException.class,
//                () -> friendTagService.saveUserToFriendTag(friendTagId, userId));
//
//        assertTrue(exception.getMessage().contains("Failed to save new UserFriendTag"));
//        verify(userFriendTagRepository, times(1)).save(any(UserFriendTag.class));
//    }
//}