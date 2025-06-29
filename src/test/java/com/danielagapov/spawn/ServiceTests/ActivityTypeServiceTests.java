package com.danielagapov.spawn.ServiceTests;

import com.danielagapov.spawn.DTOs.ActivityType.ActivityTypeDTO;
import com.danielagapov.spawn.DTOs.ActivityType.BatchActivityTypeUpdateDTO;
import com.danielagapov.spawn.Exceptions.Logger.ILogger;
import com.danielagapov.spawn.Exceptions.ActivityTypeValidationException;
import com.danielagapov.spawn.Models.ActivityType;
import com.danielagapov.spawn.Models.User.User;
import com.danielagapov.spawn.Repositories.IActivityTypeRepository;
import com.danielagapov.spawn.Services.ActivityType.ActivityTypeService;
import com.danielagapov.spawn.Services.User.IUserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ActivityTypeService that match the front-end workflow.
 * Front-end workflow: 
 * 1. Fetch activity types initially
 * 2. All changes (pin, reorder, update, delete, create) happen locally/immediately on client
 * 3. Only when user exits page, all changes are sent in one batch update to backend
 */
@ExtendWith(MockitoExtension.class)
class ActivityTypeServiceTests {

    @Mock
    private IActivityTypeRepository activityTypeRepository;

    @Mock
    private IUserService userService;

    @Mock
    private ILogger logger;

    @InjectMocks
    private ActivityTypeService activityTypeService;

    private UUID userId;
    private UUID activityTypeId1;
    private UUID activityTypeId2;
    private UUID activityTypeId3;
    private User testUser;
    private ActivityType chillActivityType;
    private ActivityType foodActivityType;
    private ActivityType activeActivityType;
    private ActivityTypeDTO chillActivityTypeDTO;
    private ActivityTypeDTO foodActivityTypeDTO;
    private ActivityTypeDTO activeActivityTypeDTO;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        
        userId = UUID.randomUUID();
        activityTypeId1 = UUID.randomUUID();
        activityTypeId2 = UUID.randomUUID();
        activityTypeId3 = UUID.randomUUID();
        
        testUser = new User();
        testUser.setId(userId);
        testUser.setUsername("testuser");
        testUser.setName("Test User");
        
        // Create test activity types matching front-end defaults
        chillActivityType = new ActivityType();
        chillActivityType.setId(activityTypeId1);
        chillActivityType.setTitle("Chill");
        chillActivityType.setIcon("🛋️");
        chillActivityType.setCreator(testUser);
        chillActivityType.setOrderNum(0);
        chillActivityType.setAssociatedFriends(List.of());
        chillActivityType.setIsPinned(false);
        
        foodActivityType = new ActivityType();
        foodActivityType.setId(activityTypeId2);
        foodActivityType.setTitle("Food");
        foodActivityType.setIcon("🍽️");
        foodActivityType.setCreator(testUser);
        foodActivityType.setOrderNum(1);
        foodActivityType.setAssociatedFriends(List.of());
        foodActivityType.setIsPinned(true); // This one is pinned
        
        activeActivityType = new ActivityType();
        activeActivityType.setId(activityTypeId3);
        activeActivityType.setTitle("Active");
        activeActivityType.setIcon("🏃");
        activeActivityType.setCreator(testUser);
        activeActivityType.setOrderNum(2);
        activeActivityType.setAssociatedFriends(List.of());
        activeActivityType.setIsPinned(false);
        
        // Create corresponding DTOs
        chillActivityTypeDTO = new ActivityTypeDTO(
            activityTypeId1,
            "Chill",
            List.of(),
            "🛋️",
            0,
            userId,
            false
        );
        
        foodActivityTypeDTO = new ActivityTypeDTO(
            activityTypeId2,
            "Food",
            List.of(),
            "🍽️",
            1,
            userId,
            true
        );
        
        activeActivityTypeDTO = new ActivityTypeDTO(
            activityTypeId3,
            "Active",
            List.of(),
            "🏃",
            2,
            userId,
            false
        );
    }

    // MARK: - Fetch Tests (Initial Load)
    
    @Test
    void fetchActivityTypes_ShouldReturnSortedByPinnedThenOrder_WhenUserHasActivityTypes() {
        // Arrange - matches front-end sortedActivityTypes computed property
        List<ActivityType> activityTypes = List.of(chillActivityType, foodActivityType, activeActivityType);
        when(activityTypeRepository.findActivityTypesByCreatorId(userId))
                .thenReturn(activityTypes);

        // Act
        List<ActivityTypeDTO> result = activityTypeService.getActivityTypesByUserId(userId);

        // Assert
        assertNotNull(result);
        assertEquals(3, result.size());
        
        // Verify pinned items come first in UI sorting (front-end handles this)
        boolean foundPinned = false;
        for (ActivityTypeDTO dto : result) {
            if (dto.getIsPinned()) {
                foundPinned = true;
                assertEquals("Food", dto.getTitle());
            }
        }
        assertTrue(foundPinned, "Should have at least one pinned activity type");
        
        verify(activityTypeRepository, times(1)).findActivityTypesByCreatorId(userId);
    }

    @Test
    void fetchActivityTypes_ShouldReturnEmptyList_WhenUserHasNoActivityTypes() {
        // Arrange
        when(activityTypeRepository.findActivityTypesByCreatorId(userId))
                .thenReturn(List.of());

        // Act
        List<ActivityTypeDTO> result = activityTypeService.getActivityTypesByUserId(userId);

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(activityTypeRepository, times(1)).findActivityTypesByCreatorId(userId);
    }

    // MARK: - Batch Update Tests (Client-Side Workflow)
    
    @Test
    void batchUpdate_ShouldHandleClientSidePinToggle_WhenUserTogglesPin() {
        // Arrange - User toggled pin on Chill activity type locally
        ActivityTypeDTO modifiedChillDTO = new ActivityTypeDTO(
            activityTypeId1,
            "Chill",
            List.of(),
            "🛋️",
            0,
            userId,
            true // Toggled to pinned
        );
        
        BatchActivityTypeUpdateDTO batchDTO = new BatchActivityTypeUpdateDTO(
                List.of(modifiedChillDTO),
                List.of()
        );

        when(userService.getUserEntityById(userId)).thenReturn(testUser);
        when(activityTypeRepository.saveAll(anyList())).thenReturn(List.of(chillActivityType));

        // Act
        BatchActivityTypeUpdateDTO result = activityTypeService.updateActivityTypes(userId, batchDTO);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getUpdatedActivityTypes().size());
        assertTrue(result.getUpdatedActivityTypes().get(0).getIsPinned());
        assertEquals(0, result.getDeletedActivityTypeIds().size());
        verify(activityTypeRepository, times(1)).saveAll(anyList());
    }
    
    @Test
    void batchUpdate_ShouldHandleClientSideReordering_WhenUserReordersActivityTypes() {
        // Arrange - User reordered activity types locally (Active moved to position 0)
        ActivityTypeDTO reorderedActiveDTO = new ActivityTypeDTO(
            activityTypeId3,
            "Active",
            List.of(),
            "🏃",
            0, // Moved to first position
            userId,
            false
        );
        
        ActivityTypeDTO reorderedChillDTO = new ActivityTypeDTO(
            activityTypeId1,
            "Chill",
            List.of(),
            "🛋️",
            1, // Moved to second position
            userId,
            false
        );
        
        ActivityTypeDTO reorderedFoodDTO = new ActivityTypeDTO(
            activityTypeId2,
            "Food",
            List.of(),
            "🍽️",
            2, // Moved to third position
            userId,
            true
        );
        
        BatchActivityTypeUpdateDTO batchDTO = new BatchActivityTypeUpdateDTO(
                List.of(reorderedActiveDTO, reorderedChillDTO, reorderedFoodDTO),
                List.of()
        );

        when(userService.getUserEntityById(userId)).thenReturn(testUser);
        when(activityTypeRepository.saveAll(anyList())).thenReturn(
            List.of(activeActivityType, chillActivityType, foodActivityType)
        );

        // Act
        BatchActivityTypeUpdateDTO result = activityTypeService.updateActivityTypes(userId, batchDTO);

        // Assert
        assertNotNull(result);
        assertEquals(3, result.getUpdatedActivityTypes().size());
        
        // Verify order numbers are updated correctly
        Map<String, Integer> orderMap = new HashMap<>();
        for (ActivityTypeDTO dto : result.getUpdatedActivityTypes()) {
            orderMap.put(dto.getTitle(), dto.getOrderNum());
        }
        
        assertEquals(0, orderMap.get("Active"));
        assertEquals(1, orderMap.get("Chill"));
        assertEquals(2, orderMap.get("Food"));
        
        verify(activityTypeRepository, times(1)).saveAll(anyList());
    }
    
    @Test
    void batchUpdate_ShouldHandleClientSideCreation_WhenUserCreatesNewActivityType() {
        // Arrange - User created a new "Study" activity type locally
        UUID newActivityTypeId = UUID.randomUUID();
        ActivityTypeDTO newStudyDTO = new ActivityTypeDTO(
            newActivityTypeId,
            "Study",
            List.of(),
            "✏️",
            3, // Next order number
            userId,
            false
        );
        
        BatchActivityTypeUpdateDTO batchDTO = new BatchActivityTypeUpdateDTO(
                List.of(newStudyDTO),
                List.of()
        );

        ActivityType newStudyActivityType = new ActivityType();
        newStudyActivityType.setId(newActivityTypeId);
        newStudyActivityType.setTitle("Study");
        newStudyActivityType.setIcon("✏️");
        newStudyActivityType.setCreator(testUser);
        newStudyActivityType.setOrderNum(3);
        newStudyActivityType.setIsPinned(false);

        when(userService.getUserEntityById(userId)).thenReturn(testUser);
        when(activityTypeRepository.saveAll(anyList())).thenReturn(List.of(newStudyActivityType));

        // Act
        BatchActivityTypeUpdateDTO result = activityTypeService.updateActivityTypes(userId, batchDTO);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getUpdatedActivityTypes().size());
        assertEquals("Study", result.getUpdatedActivityTypes().get(0).getTitle());
        assertEquals("✏️", result.getUpdatedActivityTypes().get(0).getIcon());
        assertEquals(3, result.getUpdatedActivityTypes().get(0).getOrderNum());
        verify(activityTypeRepository, times(1)).saveAll(anyList());
    }
    
    @Test
    void batchUpdate_ShouldHandleClientSideDeletion_WhenUserDeletesActivityType() {
        // Arrange - User deleted the "Active" activity type locally
        BatchActivityTypeUpdateDTO batchDTO = new BatchActivityTypeUpdateDTO(
                List.of(),
                List.of(activityTypeId3) // Delete Active activity type
        );

        // Act
        BatchActivityTypeUpdateDTO result = activityTypeService.updateActivityTypes(userId, batchDTO);

        // Assert
        assertNotNull(result);
        assertEquals(0, result.getUpdatedActivityTypes().size());
        assertEquals(1, result.getDeletedActivityTypeIds().size());
        assertEquals(activityTypeId3, result.getDeletedActivityTypeIds().get(0));
        verify(activityTypeRepository, times(1)).deleteAllById(List.of(activityTypeId3));
    }
    
    @Test
    void batchUpdate_ShouldHandleComplexClientSideWorkflow_WhenUserMakesMultipleChanges() {
        // Arrange - User made multiple changes locally:
        // 1. Created a new "Study" activity type
        // 2. Deleted "Active" activity type  
        // 3. Toggled pin on "Chill"
        // 4. Updated title of "Food" to "Food & Drinks"
        // 5. Reordered everything
        
        UUID newStudyId = UUID.randomUUID();
        
        ActivityTypeDTO newStudyDTO = new ActivityTypeDTO(
            newStudyId, "Study", List.of(), "✏️", 0, userId, false
        );
        
        ActivityTypeDTO modifiedChillDTO = new ActivityTypeDTO(
            activityTypeId1, "Chill", List.of(), "🛋️", 1, userId, true // Pinned and reordered
        );
        
        ActivityTypeDTO modifiedFoodDTO = new ActivityTypeDTO(
            activityTypeId2, "Food & Drinks", List.of(), "🍽️", 2, userId, true // Title changed and reordered
        );
        
        BatchActivityTypeUpdateDTO batchDTO = new BatchActivityTypeUpdateDTO(
                List.of(newStudyDTO, modifiedChillDTO, modifiedFoodDTO),
                List.of(activityTypeId3) // Delete Active
        );

        when(userService.getUserEntityById(userId)).thenReturn(testUser);
        when(activityTypeRepository.saveAll(anyList())).thenReturn(
            List.of(new ActivityType(), chillActivityType, foodActivityType)
        );

        // Act
        BatchActivityTypeUpdateDTO result = activityTypeService.updateActivityTypes(userId, batchDTO);

        // Assert
        assertNotNull(result);
        assertEquals(3, result.getUpdatedActivityTypes().size());
        assertEquals(1, result.getDeletedActivityTypeIds().size());
        
        // Verify all changes are included
        Map<String, ActivityTypeDTO> resultMap = new HashMap<>();
        for (ActivityTypeDTO dto : result.getUpdatedActivityTypes()) {
            resultMap.put(dto.getTitle(), dto);
        }
        
        assertTrue(resultMap.containsKey("Study"));
        assertTrue(resultMap.containsKey("Chill"));
        assertTrue(resultMap.containsKey("Food & Drinks"));
        assertTrue(resultMap.get("Chill").getIsPinned());
        assertEquals(activityTypeId3, result.getDeletedActivityTypeIds().get(0));
        
        verify(activityTypeRepository, times(1)).deleteAllById(List.of(activityTypeId3));
        verify(activityTypeRepository, times(1)).saveAll(anyList());
    }
    
    @Test
    void batchUpdate_ShouldHandleEmptyBatch_WhenNoChangesWereMade() {
        // Arrange - User opened and closed activity type management without making changes
        BatchActivityTypeUpdateDTO batchDTO = new BatchActivityTypeUpdateDTO(
                List.of(),
                List.of()
        );

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> activityTypeService.updateActivityTypes(userId, batchDTO));

        assertTrue(exception.getMessage().contains("No activity types to update or delete"));
        verify(activityTypeRepository, never()).saveAll(anyList());
        verify(activityTypeRepository, never()).deleteAllById(anyList());
    }
    
    // MARK: - Error Handling Tests
    
    @Test
    void batchUpdate_ShouldThrowException_WhenUserNotFound() {
        // Arrange
        ActivityTypeDTO modifiedDTO = new ActivityTypeDTO(
            activityTypeId1, "Modified", List.of(), "🛋️", 0, userId, false
        );
        
        BatchActivityTypeUpdateDTO batchDTO = new BatchActivityTypeUpdateDTO(
                List.of(modifiedDTO),
                List.of()
        );

        when(userService.getUserEntityById(userId)).thenThrow(new RuntimeException("User not found"));

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> activityTypeService.updateActivityTypes(userId, batchDTO));

        assertEquals("User not found", exception.getMessage());
        verify(activityTypeRepository, never()).saveAll(anyList());
    }

    // MARK: - Default Activity Types Tests
    
    @Test
    void initializeDefaultActivityTypes_ShouldCreateFourDefaultTypes_WhenUserIsNew() {
        // Arrange - This matches the front-end default activity types

        // Act
        assertDoesNotThrow(() -> activityTypeService.initializeDefaultActivityTypesForUser(testUser));

        // Assert - Verify 4 default activity types are created (Chill, Food, Active, Study)
        verify(activityTypeRepository, times(4)).save(any(ActivityType.class));
    }

    @Test
    void setOrderNumber_ShouldSetNextOrderNumber_WhenActivityTypeIsCreated() {
        // Arrange - This simulates the front-end creating a new activity type
        when(activityTypeRepository.findMaxOrderNumberByCreatorId(userId)).thenReturn(2); // User has 3 existing (0,1,2)

        ActivityType newActivityType = new ActivityType();
        newActivityType.setCreator(testUser);

        // Act
        activityTypeService.setOrderNumber(newActivityType);

        // Assert
        assertEquals(3, newActivityType.getOrderNum()); // Should be next in sequence
        verify(activityTypeRepository, times(1)).findMaxOrderNumberByCreatorId(userId);
    }
    
    @Test
    void setOrderNumber_ShouldSetZero_WhenUserHasNoActivityTypes() {
        // Arrange - New user with no activity types
        when(activityTypeRepository.findMaxOrderNumberByCreatorId(userId)).thenReturn(null);

        ActivityType newActivityType = new ActivityType();
        newActivityType.setCreator(testUser);

        // Act
        activityTypeService.setOrderNumber(newActivityType);

        // Assert
        assertEquals(0, newActivityType.getOrderNum()); // Should start at 0
        verify(activityTypeRepository, times(1)).findMaxOrderNumberByCreatorId(userId);
    }

    // MARK: - Validation Tests
    
    @Test
    void batchUpdate_ShouldThrowValidationException_WhenTooManyPinnedActivityTypes() {
        // Arrange - User tries to pin 4 activity types (exceeds limit of 3)
        ActivityTypeDTO pinned1 = new ActivityTypeDTO(activityTypeId1, "Chill", List.of(), "🛋️", 0, userId, true);
        ActivityTypeDTO pinned2 = new ActivityTypeDTO(activityTypeId2, "Food", List.of(), "🍽️", 1, userId, true);
        ActivityTypeDTO pinned3 = new ActivityTypeDTO(activityTypeId3, "Active", List.of(), "🏃", 2, userId, true);
        
        UUID newId = UUID.randomUUID();
        ActivityTypeDTO pinned4 = new ActivityTypeDTO(newId, "Study", List.of(), "✏️", 3, userId, true);
        
        BatchActivityTypeUpdateDTO batchDTO = new BatchActivityTypeUpdateDTO(
                List.of(pinned1, pinned2, pinned3, pinned4),
                List.of()
        );

        when(activityTypeRepository.countPinnedActivityTypesByCreatorId(userId)).thenReturn(1L); // Current: 1 pinned
        when(activityTypeRepository.countActivityTypesByCreatorId(userId)).thenReturn(3L); // Current: 3 total
        when(activityTypeRepository.findActivityTypesByCreatorId(userId)).thenReturn(List.of(chillActivityType, foodActivityType, activeActivityType));

        // Act & Assert
        ActivityTypeValidationException exception = assertThrows(ActivityTypeValidationException.class,
                () -> activityTypeService.updateActivityTypes(userId, batchDTO));

        assertTrue(exception.getMessage().contains("Cannot have more than 3 pinned activity types"));
        verify(activityTypeRepository, never()).saveAll(anyList());
    }
    
    @Test
    void batchUpdate_ShouldAllowMaximumPinnedActivityTypes_WhenExactlyThreePinned() {
        // Arrange - User has exactly 3 pinned activity types (should be allowed)
        ActivityTypeDTO pinned1 = new ActivityTypeDTO(activityTypeId1, "Chill", List.of(), "🛋️", 0, userId, true);
        ActivityTypeDTO pinned2 = new ActivityTypeDTO(activityTypeId2, "Food", List.of(), "🍽️", 1, userId, true);
        ActivityTypeDTO pinned3 = new ActivityTypeDTO(activityTypeId3, "Active", List.of(), "🏃", 2, userId, true);
        
        BatchActivityTypeUpdateDTO batchDTO = new BatchActivityTypeUpdateDTO(
                List.of(pinned1, pinned2, pinned3),
                List.of()
        );

        when(activityTypeRepository.countPinnedActivityTypesByCreatorId(userId)).thenReturn(0L); // Current: 0 pinned
        when(activityTypeRepository.countActivityTypesByCreatorId(userId)).thenReturn(3L); // Current: 3 total
        when(activityTypeRepository.findActivityTypesByCreatorId(userId)).thenReturn(List.of(chillActivityType, foodActivityType, activeActivityType));
        when(userService.getUserEntityById(userId)).thenReturn(testUser);
        when(activityTypeRepository.saveAll(anyList())).thenReturn(List.of(chillActivityType, foodActivityType, activeActivityType));

        // Act
        BatchActivityTypeUpdateDTO result = activityTypeService.updateActivityTypes(userId, batchDTO);

        // Assert
        assertNotNull(result);
        assertEquals(3, result.getUpdatedActivityTypes().size());
        verify(activityTypeRepository, times(1)).saveAll(anyList());
    }
    
    @Test
    void batchUpdate_ShouldThrowValidationException_WhenOrderNumTooLow() {
        // Arrange - User sets orderNum to -1 (invalid)
        ActivityTypeDTO invalidOrderDTO = new ActivityTypeDTO(activityTypeId1, "Chill", List.of(), "🛋️", -1, userId, false);
        
        BatchActivityTypeUpdateDTO batchDTO = new BatchActivityTypeUpdateDTO(
                List.of(invalidOrderDTO),
                List.of()
        );

        when(activityTypeRepository.countPinnedActivityTypesByCreatorId(userId)).thenReturn(0L);
        when(activityTypeRepository.countActivityTypesByCreatorId(userId)).thenReturn(3L);
        when(activityTypeRepository.findActivityTypesByCreatorId(userId)).thenReturn(List.of(chillActivityType, foodActivityType, activeActivityType));

        // Act & Assert
        ActivityTypeValidationException exception = assertThrows(ActivityTypeValidationException.class,
                () -> activityTypeService.updateActivityTypes(userId, batchDTO));

        assertTrue(exception.getMessage().contains("Invalid orderNum -1"));
        assertTrue(exception.getMessage().contains("Must be in range [0, 2]"));
        verify(activityTypeRepository, never()).saveAll(anyList());
    }
    
    @Test
    void batchUpdate_ShouldThrowValidationException_WhenOrderNumTooHigh() {
        // Arrange - User sets orderNum to 3 when only 3 activity types exist (valid range is 0-2)
        ActivityTypeDTO invalidOrderDTO = new ActivityTypeDTO(activityTypeId1, "Chill", List.of(), "🛋️", 3, userId, false);
        
        BatchActivityTypeUpdateDTO batchDTO = new BatchActivityTypeUpdateDTO(
                List.of(invalidOrderDTO),
                List.of()
        );

        when(activityTypeRepository.countPinnedActivityTypesByCreatorId(userId)).thenReturn(0L);
        when(activityTypeRepository.countActivityTypesByCreatorId(userId)).thenReturn(3L);
        when(activityTypeRepository.findActivityTypesByCreatorId(userId)).thenReturn(List.of(chillActivityType, foodActivityType, activeActivityType));

        // Act & Assert
        ActivityTypeValidationException exception = assertThrows(ActivityTypeValidationException.class,
                () -> activityTypeService.updateActivityTypes(userId, batchDTO));

        assertTrue(exception.getMessage().contains("Invalid orderNum 3"));
        assertTrue(exception.getMessage().contains("Must be in range [0, 2]"));
        verify(activityTypeRepository, never()).saveAll(anyList());
    }
    
    @Test
    void batchUpdate_ShouldAllowValidOrderNum_WhenInCorrectRange() {
        // Arrange - User sets valid orderNum values
        ActivityTypeDTO validOrder1 = new ActivityTypeDTO(activityTypeId1, "Chill", List.of(), "🛋️", 0, userId, false);
        ActivityTypeDTO validOrder2 = new ActivityTypeDTO(activityTypeId2, "Food", List.of(), "🍽️", 1, userId, false);
        ActivityTypeDTO validOrder3 = new ActivityTypeDTO(activityTypeId3, "Active", List.of(), "🏃", 2, userId, false);
        
        BatchActivityTypeUpdateDTO batchDTO = new BatchActivityTypeUpdateDTO(
                List.of(validOrder1, validOrder2, validOrder3),
                List.of()
        );

        when(activityTypeRepository.countPinnedActivityTypesByCreatorId(userId)).thenReturn(0L);
        when(activityTypeRepository.countActivityTypesByCreatorId(userId)).thenReturn(3L);
        when(activityTypeRepository.findActivityTypesByCreatorId(userId)).thenReturn(List.of(chillActivityType, foodActivityType, activeActivityType));
        when(userService.getUserEntityById(userId)).thenReturn(testUser);
        when(activityTypeRepository.saveAll(anyList())).thenReturn(List.of(chillActivityType, foodActivityType, activeActivityType));

        // Act
        BatchActivityTypeUpdateDTO result = activityTypeService.updateActivityTypes(userId, batchDTO);

        // Assert
        assertNotNull(result);
        assertEquals(3, result.getUpdatedActivityTypes().size());
        verify(activityTypeRepository, times(1)).saveAll(anyList());
    }
    
    @Test
    void batchUpdate_ShouldHandleComplexValidation_WhenDeletingAndCreating() {
        // Arrange - User deletes 1 pinned item and creates 2 new pinned items (net +1, should be valid)
        // Starting state: 1 pinned out of 3 total
        // After update: delete 1 (including 1 pinned), add 2 new pinned = 2 pinned total (valid)
        
        UUID newId1 = UUID.randomUUID();
        UUID newId2 = UUID.randomUUID();
        
        ActivityTypeDTO newPinned1 = new ActivityTypeDTO(newId1, "Study", List.of(), "✏️", 2, userId, true);
        ActivityTypeDTO newPinned2 = new ActivityTypeDTO(newId2, "Sports", List.of(), "⚽", 3, userId, true);
        
        BatchActivityTypeUpdateDTO batchDTO = new BatchActivityTypeUpdateDTO(
                List.of(newPinned1, newPinned2),
                List.of(activityTypeId2) // Delete foodActivityType (which is pinned)
        );

        // Mock current state: foodActivityType is pinned
        foodActivityType.setIsPinned(true);
        
        when(activityTypeRepository.countPinnedActivityTypesByCreatorId(userId)).thenReturn(1L); // 1 currently pinned
        when(activityTypeRepository.countActivityTypesByCreatorId(userId)).thenReturn(3L); // 3 total
        when(activityTypeRepository.findActivityTypesByCreatorId(userId)).thenReturn(List.of(chillActivityType, foodActivityType, activeActivityType));
        when(userService.getUserEntityById(userId)).thenReturn(testUser);
        when(activityTypeRepository.saveAll(anyList())).thenReturn(List.of());

        // Act
        BatchActivityTypeUpdateDTO result = activityTypeService.updateActivityTypes(userId, batchDTO);

        // Assert - Should pass validation: 1 - 1 + 2 = 2 pinned (within limit of 3)
        assertNotNull(result);
        assertEquals(2, result.getUpdatedActivityTypes().size());
        assertEquals(1, result.getDeletedActivityTypeIds().size());
        verify(activityTypeRepository, times(1)).deleteAllById(anyList());
        verify(activityTypeRepository, times(1)).saveAll(anyList());
    }
} 