package com.danielagapov.spawn.ServiceTests;

import com.danielagapov.spawn.DTOs.ActivityType.ActivityTypeDTO;
import com.danielagapov.spawn.DTOs.ActivityType.BatchActivityTypeUpdateDTO;
import com.danielagapov.spawn.Enums.EntityType;
import com.danielagapov.spawn.Exceptions.Base.BaseNotFoundException;
import com.danielagapov.spawn.Exceptions.Logger.ILogger;
import com.danielagapov.spawn.Mappers.ActivityTypeMapper;
import com.danielagapov.spawn.Models.ActivityType;
import com.danielagapov.spawn.Models.User.User;
import com.danielagapov.spawn.Models.UserActivityTypePin;
import com.danielagapov.spawn.Repositories.IActivityTypeRepository;
import com.danielagapov.spawn.Repositories.IUserActivityTypePinRepository;
import com.danielagapov.spawn.Services.ActivityType.ActivityTypeService;
import com.danielagapov.spawn.Services.User.IUserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.dao.DataAccessException;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class ActivityTypeServiceTests {

    @Mock
    private IActivityTypeRepository activityTypeRepository;

    @Mock
    private IUserActivityTypePinRepository userActivityTypePinRepository;

    @Mock
    private IUserService userService;

    @Mock
    private ILogger logger;

    @InjectMocks
    private ActivityTypeService activityTypeService;

    private UUID userId;
    private UUID activityTypeId;
    private User testUser;
    private ActivityType testActivityType;
    private ActivityTypeDTO testActivityTypeDTO;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);
        
        userId = UUID.randomUUID();
        activityTypeId = UUID.randomUUID();
        
        testUser = new User();
        testUser.setId(userId);
        testUser.setUsername("testuser");
        testUser.setName("Test User");
        
        testActivityType = new ActivityType();
        testActivityType.setId(activityTypeId);
        testActivityType.setTitle("Test Activity Type");
        testActivityType.setIcon("🎯");
        testActivityType.setCreator(testUser);
        testActivityType.setOrderNum(1);
        
        testActivityTypeDTO = new ActivityTypeDTO(
            activityTypeId,
            "Test Activity Type",
            List.of(),
            "🎯",
            1
        );
    }

    // Tests for getPinnedActivityTypeIds
    @Test
    void getPinnedActivityTypeIds_ShouldReturnPinnedIds_WhenUserHasPinnedActivityTypes() {
        UUID pinnedId1 = UUID.randomUUID();
        UUID pinnedId2 = UUID.randomUUID();
        List<UUID> expectedPinnedIds = Arrays.asList(pinnedId1, pinnedId2);

        when(userActivityTypePinRepository.findPinnedActivityTypeIdsByUserId(userId))
            .thenReturn(expectedPinnedIds);

        List<UUID> result = activityTypeService.getPinnedActivityTypeIds(userId);

        assertEquals(expectedPinnedIds, result);
        assertEquals(2, result.size());
        assertTrue(result.contains(pinnedId1));
        assertTrue(result.contains(pinnedId2));
        verify(userActivityTypePinRepository, times(1)).findPinnedActivityTypeIdsByUserId(userId);
    }

    @Test
    void getPinnedActivityTypeIds_ShouldReturnEmptyList_WhenUserHasNoPinnedActivityTypes() {
        when(userActivityTypePinRepository.findPinnedActivityTypeIdsByUserId(userId))
            .thenReturn(List.of());

        List<UUID> result = activityTypeService.getPinnedActivityTypeIds(userId);

        assertTrue(result.isEmpty());
        verify(userActivityTypePinRepository, times(1)).findPinnedActivityTypeIdsByUserId(userId);
    }

    @Test
    void getPinnedActivityTypeIds_ShouldThrowException_WhenDatabaseErrorOccurs() {
        when(userActivityTypePinRepository.findPinnedActivityTypeIdsByUserId(userId))
            .thenThrow(new DataAccessException("Database error") {});

        RuntimeException exception = assertThrows(RuntimeException.class,
            () -> activityTypeService.getPinnedActivityTypeIds(userId));

        assertEquals("Failed to fetch pinned activity types", exception.getMessage());
        verify(userActivityTypePinRepository, times(1)).findPinnedActivityTypeIdsByUserId(userId);
    }

    // Tests for toggleActivityTypePin
    @Test
    void toggleActivityTypePin_ShouldPinActivityType_WhenNotCurrentlyPinnedAndNotOwnedByUser() {
        // Create a different user as the creator of the activity type
        UUID otherUserId = UUID.randomUUID();
        User otherUser = new User();
        otherUser.setId(otherUserId);
        testActivityType.setCreator(otherUser); // Activity type is owned by another user
        
        when(activityTypeRepository.findById(activityTypeId)).thenReturn(Optional.of(testActivityType));
        when(userService.getUserEntityById(userId)).thenReturn(testUser);
        when(userActivityTypePinRepository.existsByUserIdAndActivityTypeId(userId, activityTypeId))
            .thenReturn(false);

        assertDoesNotThrow(() -> activityTypeService.toggleActivityTypePin(userId, activityTypeId, true));

        verify(activityTypeRepository, times(1)).findById(activityTypeId);
        verify(userService, times(1)).getUserEntityById(userId);
        verify(userActivityTypePinRepository, times(1)).existsByUserIdAndActivityTypeId(userId, activityTypeId);
        verify(userActivityTypePinRepository, times(1)).save(any(UserActivityTypePin.class));
    }

    @Test
    void toggleActivityTypePin_ShouldUnpinActivityType_WhenCurrentlyPinned() {
        // Create a different user as the creator 
        UUID otherUserId = UUID.randomUUID();
        User otherUser = new User();
        otherUser.setId(otherUserId);
        testActivityType.setCreator(otherUser);
        
        when(activityTypeRepository.findById(activityTypeId)).thenReturn(Optional.of(testActivityType));
        when(userService.getUserEntityById(userId)).thenReturn(testUser);
        when(userActivityTypePinRepository.existsByUserIdAndActivityTypeId(userId, activityTypeId))
            .thenReturn(true);

        assertDoesNotThrow(() -> activityTypeService.toggleActivityTypePin(userId, activityTypeId, false));

        verify(activityTypeRepository, times(1)).findById(activityTypeId);
        verify(userService, times(1)).getUserEntityById(userId);
        verify(userActivityTypePinRepository, times(1)).existsByUserIdAndActivityTypeId(userId, activityTypeId);
        verify(userActivityTypePinRepository, times(1)).deleteByUserIdAndActivityTypeId(userId, activityTypeId);
    }

    @Test
    void toggleActivityTypePin_ShouldDoNothing_WhenAlreadyInDesiredState() {
        when(activityTypeRepository.findById(activityTypeId)).thenReturn(Optional.of(testActivityType));
        when(userService.getUserEntityById(userId)).thenReturn(testUser);
        when(userActivityTypePinRepository.existsByUserIdAndActivityTypeId(userId, activityTypeId))
            .thenReturn(true);

        assertDoesNotThrow(() -> activityTypeService.toggleActivityTypePin(userId, activityTypeId, true));

        verify(activityTypeRepository, times(1)).findById(activityTypeId);
        verify(userService, times(1)).getUserEntityById(userId);
        verify(userActivityTypePinRepository, times(1)).existsByUserIdAndActivityTypeId(userId, activityTypeId);
        verify(userActivityTypePinRepository, never()).save(any(UserActivityTypePin.class));
        verify(userActivityTypePinRepository, never()).deleteByUserIdAndActivityTypeId(any(UUID.class), any(UUID.class));
    }

    @Test
    void toggleActivityTypePin_ShouldThrowException_WhenActivityTypeNotFound() {
        when(activityTypeRepository.findById(activityTypeId)).thenReturn(Optional.empty());

        BaseNotFoundException exception = assertThrows(BaseNotFoundException.class,
            () -> activityTypeService.toggleActivityTypePin(userId, activityTypeId, true));

        assertEquals("ActivityType entity not found with ID: " + activityTypeId, exception.getMessage());
        verify(activityTypeRepository, times(1)).findById(activityTypeId);
        verify(userService, never()).getUserEntityById(any(UUID.class));
    }

    @Test
    void toggleActivityTypePin_ShouldThrowException_WhenUserTriesToPinOwnActivityType() {
        // Set up so user tries to pin their own activity type
        testActivityType.setCreator(testUser); // Same user owns the activity type
        
        when(activityTypeRepository.findById(activityTypeId)).thenReturn(Optional.of(testActivityType));
        when(userService.getUserEntityById(userId)).thenReturn(testUser);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> activityTypeService.toggleActivityTypePin(userId, activityTypeId, true));

        assertEquals("Cannot pin your own activity type - you already own it", exception.getMessage());
        verify(activityTypeRepository, times(1)).findById(activityTypeId);
        verify(userService, times(1)).getUserEntityById(userId);
        verify(userActivityTypePinRepository, never()).save(any(UserActivityTypePin.class));
    }

    @Test
    void toggleActivityTypePin_ShouldThrowException_WhenUserNotFound() {
        // Create a different user as the creator 
        UUID otherUserId = UUID.randomUUID();
        User otherUser = new User();
        otherUser.setId(otherUserId);
        testActivityType.setCreator(otherUser);
        
        when(activityTypeRepository.findById(activityTypeId)).thenReturn(Optional.of(testActivityType));
        when(userService.getUserEntityById(userId))
            .thenThrow(new BaseNotFoundException(EntityType.User, userId));

        BaseNotFoundException exception = assertThrows(BaseNotFoundException.class,
            () -> activityTypeService.toggleActivityTypePin(userId, activityTypeId, true));

        assertEquals("User entity not found with ID: " + userId, exception.getMessage());
        verify(activityTypeRepository, times(1)).findById(activityTypeId);
        verify(userService, times(1)).getUserEntityById(userId);
    }

    @Test
    void toggleActivityTypePin_ShouldThrowException_WhenDatabaseErrorOccurs() {
        when(activityTypeRepository.findById(activityTypeId)).thenReturn(Optional.of(testActivityType));
        when(userService.getUserEntityById(userId)).thenReturn(testUser);
        when(userActivityTypePinRepository.existsByUserIdAndActivityTypeId(userId, activityTypeId))
            .thenReturn(false);
        when(userActivityTypePinRepository.save(any(UserActivityTypePin.class)))
            .thenThrow(new DataAccessException("Database error") {});

        RuntimeException exception = assertThrows(RuntimeException.class,
            () -> activityTypeService.toggleActivityTypePin(userId, activityTypeId, true));

        assertEquals("Failed to toggle activity type pin status", exception.getMessage());
        verify(userActivityTypePinRepository, times(1)).save(any(UserActivityTypePin.class));
    }

    // Tests for createActivityType
    @Test
    void createActivityType_ShouldCreateActivityType_WhenValidData() {
        when(userService.getUserEntityById(userId)).thenReturn(testUser);
        when(activityTypeRepository.findMaxOrderNumberByCreatorId(userId)).thenReturn(2);
        when(activityTypeRepository.save(any(ActivityType.class))).thenReturn(testActivityType);

        ActivityTypeDTO result = activityTypeService.createActivityType(userId, testActivityTypeDTO);

        assertNotNull(result);
        assertEquals("Test Activity Type", result.getTitle());
        assertEquals("🎯", result.getIcon());
        verify(userService, times(1)).getUserEntityById(userId);
        verify(activityTypeRepository, times(1)).findMaxOrderNumberByCreatorId(userId);
        verify(activityTypeRepository, times(1)).save(any(ActivityType.class));
    }

    @Test
    void createActivityType_ShouldThrowException_WhenUserNotFound() {
        when(userService.getUserEntityById(userId))
            .thenThrow(new BaseNotFoundException(EntityType.User, userId));

        RuntimeException exception = assertThrows(RuntimeException.class,
            () -> activityTypeService.createActivityType(userId, testActivityTypeDTO));

        assertEquals("Failed to create activity type", exception.getMessage());
        verify(userService, times(1)).getUserEntityById(userId);
        verify(activityTypeRepository, never()).save(any(ActivityType.class));
    }

    @Test
    void createActivityType_ShouldThrowException_WhenDatabaseErrorOccurs() {
        when(userService.getUserEntityById(userId)).thenReturn(testUser);
        when(activityTypeRepository.findMaxOrderNumberByCreatorId(userId)).thenReturn(2);
        when(activityTypeRepository.save(any(ActivityType.class)))
            .thenThrow(new DataAccessException("Database error") {});

        RuntimeException exception = assertThrows(RuntimeException.class,
            () -> activityTypeService.createActivityType(userId, testActivityTypeDTO));

        assertEquals("Failed to create activity type", exception.getMessage());
        verify(activityTypeRepository, times(1)).save(any(ActivityType.class));
    }

    // Tests for existing methods to ensure they still work
    @Test
    void getActivityTypesByUserId_ShouldReturnOwnedActivityTypes_WhenUserExists() {
        List<ActivityType> activityTypes = Arrays.asList(testActivityType);
        when(activityTypeRepository.findActivityTypesByCreatorId(userId)).thenReturn(activityTypes);

        List<ActivityTypeDTO> result = activityTypeService.getActivityTypesByUserId(userId);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Test Activity Type", result.get(0).getTitle());
        verify(activityTypeRepository, times(1)).findActivityTypesByCreatorId(userId);
    }

    @Test
    void getAllAvailableActivityTypesForUser_ShouldReturnOwnedAndPinnedActivityTypes() {
        // Create owned activity type
        ActivityType ownedActivityType = new ActivityType();
        ownedActivityType.setId(UUID.randomUUID());
        ownedActivityType.setTitle("My Activity Type");
        ownedActivityType.setCreator(testUser);
        
        // Create pinned activity type (owned by another user)
        UUID otherUserId = UUID.randomUUID();
        User otherUser = new User();
        otherUser.setId(otherUserId);
        ActivityType pinnedActivityType = new ActivityType();
        pinnedActivityType.setId(UUID.randomUUID());
        pinnedActivityType.setTitle("Pinned Activity Type");
        pinnedActivityType.setCreator(otherUser);
        
        when(activityTypeRepository.findActivityTypesByCreatorId(userId))
            .thenReturn(Arrays.asList(ownedActivityType));
        when(userActivityTypePinRepository.findPinnedActivityTypeIdsByUserId(userId))
            .thenReturn(Arrays.asList(pinnedActivityType.getId()));
        when(activityTypeRepository.findAllById(Arrays.asList(pinnedActivityType.getId())))
            .thenReturn(Arrays.asList(pinnedActivityType));

        List<ActivityTypeDTO> result = activityTypeService.getAllAvailableActivityTypesForUser(userId);

        assertNotNull(result);
        assertEquals(2, result.size());
        // Should contain both owned and pinned activity types
        assertTrue(result.stream().anyMatch(dto -> dto.getTitle().equals("My Activity Type")));
        assertTrue(result.stream().anyMatch(dto -> dto.getTitle().equals("Pinned Activity Type")));
        
        verify(activityTypeRepository, times(1)).findActivityTypesByCreatorId(userId);
        verify(userActivityTypePinRepository, times(1)).findPinnedActivityTypeIdsByUserId(userId);
        verify(activityTypeRepository, times(1)).findAllById(any(List.class));
    }

    @Test
    void getAllAvailableActivityTypesForUser_ShouldReturnOnlyOwnedActivityTypes_WhenNoPinnedTypes() {
        ActivityType ownedActivityType = new ActivityType();
        ownedActivityType.setId(UUID.randomUUID());
        ownedActivityType.setTitle("My Activity Type");
        ownedActivityType.setCreator(testUser);
        
        when(activityTypeRepository.findActivityTypesByCreatorId(userId))
            .thenReturn(Arrays.asList(ownedActivityType));
        when(userActivityTypePinRepository.findPinnedActivityTypeIdsByUserId(userId))
            .thenReturn(List.of()); // No pinned types
        when(activityTypeRepository.findAllById(List.of()))
            .thenReturn(List.of());

        List<ActivityTypeDTO> result = activityTypeService.getAllAvailableActivityTypesForUser(userId);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("My Activity Type", result.get(0).getTitle());
        
        verify(activityTypeRepository, times(1)).findActivityTypesByCreatorId(userId);
        verify(userActivityTypePinRepository, times(1)).findPinnedActivityTypeIdsByUserId(userId);
    }

    @Test
    void deleteActivityType_ShouldDeleteActivityType_WhenActivityTypeExists() {
        when(activityTypeRepository.existsById(activityTypeId)).thenReturn(true);

        assertDoesNotThrow(() -> activityTypeService.deleteActivityType(activityTypeId));

        verify(activityTypeRepository, times(1)).existsById(activityTypeId);
        verify(activityTypeRepository, times(1)).deleteById(activityTypeId);
    }

    @Test
    void deleteActivityType_ShouldThrowException_WhenActivityTypeNotFound() {
        when(activityTypeRepository.existsById(activityTypeId)).thenReturn(false);

        BaseNotFoundException exception = assertThrows(BaseNotFoundException.class,
            () -> activityTypeService.deleteActivityType(activityTypeId));

        assertEquals("ActivityType entity not found with ID: " + activityTypeId, exception.getMessage());
        verify(activityTypeRepository, times(1)).existsById(activityTypeId);
        verify(activityTypeRepository, never()).deleteById(any(UUID.class));
    }

    @Test
    void updateActivityTypes_ShouldUpdateSuccessfully_WhenValidBatchData() {
        BatchActivityTypeUpdateDTO batchDTO = new BatchActivityTypeUpdateDTO(
            Arrays.asList(testActivityTypeDTO),
            Arrays.asList(UUID.randomUUID())
        );
        
        when(userService.getUserEntityById(userId)).thenReturn(testUser);
        when(activityTypeRepository.saveAll(any(List.class))).thenReturn(Arrays.asList(testActivityType));

        BatchActivityTypeUpdateDTO result = activityTypeService.updateActivityTypes(userId, batchDTO);

        assertNotNull(result);
        assertEquals(batchDTO, result);
        verify(userService, times(1)).getUserEntityById(userId);
        verify(activityTypeRepository, times(1)).deleteAllById(batchDTO.getDeletedActivityTypeIds());
        verify(activityTypeRepository, times(1)).saveAll(any(List.class));
    }

    @Test
    void initializeDefaultActivityTypesForUser_ShouldCreateDefaultActivityTypes() {
        when(activityTypeRepository.save(any(ActivityType.class))).thenReturn(testActivityType);

        assertDoesNotThrow(() -> activityTypeService.initializeDefaultActivityTypesForUser(testUser));

        verify(activityTypeRepository, times(4)).save(any(ActivityType.class));
    }

    @Test
    void setOrderNumber_ShouldSetCorrectOrderNumber_WhenMaxOrderExists() {
        when(activityTypeRepository.findMaxOrderNumberByCreatorId(userId)).thenReturn(5);

        activityTypeService.setOrderNumber(testActivityType);

        assertEquals(6, testActivityType.getOrderNum());
        verify(activityTypeRepository, times(1)).findMaxOrderNumberByCreatorId(userId);
    }

    @Test
    void setOrderNumber_ShouldSetZeroOrderNumber_WhenNoMaxOrderExists() {
        when(activityTypeRepository.findMaxOrderNumberByCreatorId(userId)).thenReturn(null);

        activityTypeService.setOrderNumber(testActivityType);

        assertEquals(0, testActivityType.getOrderNum());
        verify(activityTypeRepository, times(1)).findMaxOrderNumberByCreatorId(userId);
    }
} 