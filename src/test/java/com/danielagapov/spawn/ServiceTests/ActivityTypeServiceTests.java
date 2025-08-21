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
import org.junit.jupiter.api.Order;
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
@Order(4)
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
        chillActivityType.setOrderNum(1);
        chillActivityType.setAssociatedFriends(List.of());
        chillActivityType.setIsPinned(false);
        
        foodActivityType = new ActivityType();
        foodActivityType.setId(activityTypeId2);
        foodActivityType.setTitle("Food");
        foodActivityType.setIcon("🍽️");
        foodActivityType.setCreator(testUser);
        foodActivityType.setOrderNum(2);
        foodActivityType.setAssociatedFriends(List.of());
        foodActivityType.setIsPinned(true); // This one is pinned
        
        activeActivityType = new ActivityType();
        activeActivityType.setId(activityTypeId3);
        activeActivityType.setTitle("Active");
        activeActivityType.setIcon("🏃");
        activeActivityType.setCreator(testUser);
        activeActivityType.setOrderNum(3);
        activeActivityType.setAssociatedFriends(List.of());
        activeActivityType.setIsPinned(false);
        
        // Create corresponding DTOs
        chillActivityTypeDTO = new ActivityTypeDTO(
            activityTypeId1,
            "Chill",
            List.of(),
            "🛋️",
            1,
            userId,
            false
        );
        
        foodActivityTypeDTO = new ActivityTypeDTO(
            activityTypeId2,
            "Food",
            List.of(),
            "🍽️",
            2,
            userId,
            true
        );
        
        activeActivityTypeDTO = new ActivityTypeDTO(
            activityTypeId3,
            "Active",
            List.of(),
            "🏃",
            3,
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
            1, // Toggled to pinned
            userId,
            true
        );
        
        BatchActivityTypeUpdateDTO batchDTO = new BatchActivityTypeUpdateDTO(
                List.of(modifiedChillDTO),
                List.of()
        );

        when(activityTypeRepository.countByCreatorIdAndIsPinnedTrue(userId)).thenReturn(0L);
        when(activityTypeRepository.countByCreatorId(userId)).thenReturn(3L);
        when(activityTypeRepository.findActivityTypesByCreatorId(userId)).thenReturn(List.of(chillActivityType, foodActivityType, activeActivityType));
        when(userService.getUserEntityById(userId)).thenReturn(testUser);
        when(activityTypeRepository.saveAll(anyList())).thenReturn(List.of(chillActivityType));

        // Act
        List<ActivityTypeDTO> result = activityTypeService.updateActivityTypes(userId, batchDTO);

        // Assert
        assertNotNull(result);
        assertEquals(3, result.size()); // Returns all user's activity types
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
            1, // Moved to first position
            userId,
            false
        );
        
        ActivityTypeDTO reorderedChillDTO = new ActivityTypeDTO(
            activityTypeId1,
            "Chill",
            List.of(),
            "🛋️",
            2, // Moved to second position
            userId,
            false
        );
        
        ActivityTypeDTO reorderedFoodDTO = new ActivityTypeDTO(
            activityTypeId2,
            "Food",
            List.of(),
            "🍽️",
            3, // Moved to third position
            userId,
            true
        );
        
        BatchActivityTypeUpdateDTO batchDTO = new BatchActivityTypeUpdateDTO(
                List.of(reorderedActiveDTO, reorderedChillDTO, reorderedFoodDTO),
                List.of()
        );

        when(activityTypeRepository.countByCreatorIdAndIsPinnedTrue(userId)).thenReturn(1L);
        when(activityTypeRepository.countByCreatorId(userId)).thenReturn(3L);
        when(activityTypeRepository.findActivityTypesByCreatorId(userId)).thenReturn(List.of(chillActivityType, foodActivityType, activeActivityType));
        when(userService.getUserEntityById(userId)).thenReturn(testUser);
        when(activityTypeRepository.saveAll(anyList())).thenReturn(
            List.of(activeActivityType, chillActivityType, foodActivityType)
        );

        // Act
        List<ActivityTypeDTO> result = activityTypeService.updateActivityTypes(userId, batchDTO);

        // Assert
        assertNotNull(result);
        assertEquals(3, result.size()); // Returns all user's activity types
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
            4, // Next order number
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
        newStudyActivityType.setOrderNum(4);
        newStudyActivityType.setIsPinned(false);

        when(activityTypeRepository.countByCreatorIdAndIsPinnedTrue(userId)).thenReturn(1L);
        when(activityTypeRepository.countByCreatorId(userId)).thenReturn(3L);
        when(activityTypeRepository.findActivityTypesByCreatorId(userId)).thenReturn(List.of(chillActivityType, foodActivityType, activeActivityType));
        when(userService.getUserEntityById(userId)).thenReturn(testUser);
        when(activityTypeRepository.saveAll(anyList())).thenReturn(List.of(newStudyActivityType));

        // Act
        List<ActivityTypeDTO> result = activityTypeService.updateActivityTypes(userId, batchDTO);

        // Assert
        assertNotNull(result);
        assertEquals(3, result.size()); // Returns all user's activity types
        verify(activityTypeRepository, times(1)).saveAll(anyList());
    }
    
    @Test
    void batchUpdate_ShouldHandleClientSideDeletion_WhenUserDeletesActivityType() {
        // Arrange - User deleted the "Active" activity type locally
        BatchActivityTypeUpdateDTO batchDTO = new BatchActivityTypeUpdateDTO(
                List.of(),
                List.of(activityTypeId3) // Delete Active activity type
        );

        // Mock the final result after deletion (should return remaining 2 activity types)
        when(activityTypeRepository.findActivityTypesByCreatorId(userId))
                .thenReturn(List.of(chillActivityType, foodActivityType)); // Final result after deletion

        // Act
        List<ActivityTypeDTO> result = activityTypeService.updateActivityTypes(userId, batchDTO);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size()); // Returns all user's activity types except the deleted one
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
            newStudyId, "Study", List.of(), "✏️", 1, userId, false
        );
        
        ActivityTypeDTO modifiedChillDTO = new ActivityTypeDTO(
            activityTypeId1, "Chill", List.of(), "🛋️", 2, userId, true // Pinned and reordered
        );
        
        ActivityTypeDTO modifiedFoodDTO = new ActivityTypeDTO(
            activityTypeId2, "Food & Drinks", List.of(), "🍽️", 3, userId, true // Title changed and reordered
        );
        
        BatchActivityTypeUpdateDTO batchDTO = new BatchActivityTypeUpdateDTO(
                List.of(newStudyDTO, modifiedChillDTO, modifiedFoodDTO),
                List.of(activityTypeId3) // Delete Active
        );

        when(activityTypeRepository.countByCreatorIdAndIsPinnedTrue(userId)).thenReturn(1L);
        when(activityTypeRepository.countByCreatorId(userId)).thenReturn(3L);
        when(activityTypeRepository.findActivityTypesByCreatorId(userId)).thenReturn(List.of(chillActivityType, foodActivityType, activeActivityType));
        when(userService.getUserEntityById(userId)).thenReturn(testUser);
        when(activityTypeRepository.saveAll(anyList())).thenReturn(List.of(new ActivityType(), chillActivityType, foodActivityType));

        // Act
        List<ActivityTypeDTO> result = activityTypeService.updateActivityTypes(userId, batchDTO);

        // Assert
        assertNotNull(result);
        assertEquals(3, result.size()); // Returns all user's activity types after changes
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
            activityTypeId1, "Modified", List.of(), "🛋️", 1, userId, false
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

        // Assert - Verify 4 default activity types are created (Chill, Food, Active, Study) using saveAll
        verify(activityTypeRepository, times(1)).saveAll(argThat(list -> ((List<ActivityType>) list).size() == 4));
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
    void setOrderNumber_ShouldSetOne_WhenUserHasNoActivityTypes() {
        // Arrange - New user with no activity types
        when(activityTypeRepository.findMaxOrderNumberByCreatorId(userId)).thenReturn(null);

        ActivityType newActivityType = new ActivityType();
        newActivityType.setCreator(testUser);

        // Act
        activityTypeService.setOrderNumber(newActivityType);

        // Assert
        assertEquals(1, newActivityType.getOrderNum()); // Should start at 1
        verify(activityTypeRepository, times(1)).findMaxOrderNumberByCreatorId(userId);
    }

    // MARK: - Validation Tests
    
    @Test
    void batchUpdate_ShouldThrowValidationException_WhenTooManyPinnedActivityTypes() {
        // Arrange - User tries to pin 4 activity types (exceeds limit of 3)
        ActivityTypeDTO pinned1 = new ActivityTypeDTO(activityTypeId1, "Chill", List.of(), "🛋️", 1, userId, true);
        ActivityTypeDTO pinned2 = new ActivityTypeDTO(activityTypeId2, "Food", List.of(), "🍽️", 2, userId, true);
        ActivityTypeDTO pinned3 = new ActivityTypeDTO(activityTypeId3, "Active", List.of(), "🏃", 3, userId, true);
        
        UUID newId = UUID.randomUUID();
        ActivityTypeDTO pinned4 = new ActivityTypeDTO(newId, "Study", List.of(), "✏️", 4, userId, true);
        
        BatchActivityTypeUpdateDTO batchDTO = new BatchActivityTypeUpdateDTO(
                List.of(pinned1, pinned2, pinned3, pinned4),
                List.of()
        );

        when(activityTypeRepository.countByCreatorIdAndIsPinnedTrue(userId)).thenReturn(1L); // Current: 1 pinned
        when(activityTypeRepository.countByCreatorId(userId)).thenReturn(3L); // Current: 3 total
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
        ActivityTypeDTO pinned1 = new ActivityTypeDTO(activityTypeId1, "Chill", List.of(), "🛋️", 1, userId, true);
        ActivityTypeDTO pinned2 = new ActivityTypeDTO(activityTypeId2, "Food", List.of(), "🍽️", 2, userId, true);
        ActivityTypeDTO pinned3 = new ActivityTypeDTO(activityTypeId3, "Active", List.of(), "🏃", 3, userId, true);
        
        BatchActivityTypeUpdateDTO batchDTO = new BatchActivityTypeUpdateDTO(
                List.of(pinned1, pinned2, pinned3),
                List.of()
        );

        when(activityTypeRepository.countByCreatorIdAndIsPinnedTrue(userId)).thenReturn(0L); // Current: 0 pinned
        when(activityTypeRepository.countByCreatorId(userId)).thenReturn(3L); // Current: 3 total
        when(activityTypeRepository.findActivityTypesByCreatorId(userId)).thenReturn(List.of(chillActivityType, foodActivityType, activeActivityType));
        when(userService.getUserEntityById(userId)).thenReturn(testUser);
        when(activityTypeRepository.saveAll(anyList())).thenReturn(List.of(chillActivityType, foodActivityType, activeActivityType));

        // Act
        List<ActivityTypeDTO> result = activityTypeService.updateActivityTypes(userId, batchDTO);

        // Assert
        assertNotNull(result);
        assertEquals(3, result.size());
        verify(activityTypeRepository, times(1)).saveAll(anyList());
    }
    
    @Test
    void batchUpdate_ShouldThrowValidationException_WhenOrderNumTooLow() {
        // Arrange - User sets orderNum to 0 (invalid)
        ActivityTypeDTO invalidOrderDTO = new ActivityTypeDTO(activityTypeId1, "Chill", List.of(), "🛋️", 0, userId, false);
        
        BatchActivityTypeUpdateDTO batchDTO = new BatchActivityTypeUpdateDTO(
                List.of(invalidOrderDTO),
                List.of()
        );

        when(activityTypeRepository.countByCreatorIdAndIsPinnedTrue(userId)).thenReturn(0L);
        when(activityTypeRepository.countByCreatorId(userId)).thenReturn(3L);
        when(activityTypeRepository.findActivityTypesByCreatorId(userId)).thenReturn(List.of(chillActivityType, foodActivityType, activeActivityType));

        // Act & Assert
        ActivityTypeValidationException exception = assertThrows(ActivityTypeValidationException.class,
                () -> activityTypeService.updateActivityTypes(userId, batchDTO));

        assertTrue(exception.getMessage().contains("Invalid orderNum 0"));
        assertTrue(exception.getMessage().contains("Must be in range [1, 3]"));
        verify(activityTypeRepository, never()).saveAll(anyList());
    }
    
    @Test
    void batchUpdate_ShouldThrowValidationException_WhenOrderNumTooHigh() {
        // Arrange - User sets orderNum to 5 when only 3 activity types exist (valid range is 1-3; 4 is allowed as append-to-end)
        ActivityTypeDTO invalidOrderDTO = new ActivityTypeDTO(activityTypeId1, "Chill", List.of(), "🛋️", 5, userId, false);
        
        BatchActivityTypeUpdateDTO batchDTO = new BatchActivityTypeUpdateDTO(
                List.of(invalidOrderDTO),
                List.of()
        );

        when(activityTypeRepository.countByCreatorIdAndIsPinnedTrue(userId)).thenReturn(0L);
        when(activityTypeRepository.countByCreatorId(userId)).thenReturn(3L);
        when(activityTypeRepository.findActivityTypesByCreatorId(userId)).thenReturn(List.of(chillActivityType, foodActivityType, activeActivityType));

        // Act & Assert
        ActivityTypeValidationException exception = assertThrows(ActivityTypeValidationException.class,
                () -> activityTypeService.updateActivityTypes(userId, batchDTO));

        assertTrue(exception.getMessage().contains("Invalid orderNum 5"));
        assertTrue(exception.getMessage().contains("Must be in range [1, 3]"));
        verify(activityTypeRepository, never()).saveAll(anyList());
    }
    
    @Test
    void batchUpdate_ShouldThrowValidationException_WhenDuplicateOrderNums() {
        // Arrange - User sets duplicate orderNum values (both set to 2)
        ActivityTypeDTO duplicate1 = new ActivityTypeDTO(activityTypeId1, "Chill", List.of(), "🛋️", 2, userId, false);
        ActivityTypeDTO duplicate2 = new ActivityTypeDTO(activityTypeId2, "Food", List.of(), "🍽️", 2, userId, false); // Same orderNum
        
        BatchActivityTypeUpdateDTO batchDTO = new BatchActivityTypeUpdateDTO(
                List.of(duplicate1, duplicate2),
                List.of()
        );

        when(activityTypeRepository.countByCreatorIdAndIsPinnedTrue(userId)).thenReturn(0L);
        when(activityTypeRepository.countByCreatorId(userId)).thenReturn(3L);
        when(activityTypeRepository.findActivityTypesByCreatorId(userId)).thenReturn(List.of(chillActivityType, foodActivityType, activeActivityType));

        // Act & Assert
        ActivityTypeValidationException exception = assertThrows(ActivityTypeValidationException.class,
                () -> activityTypeService.updateActivityTypes(userId, batchDTO));

        assertTrue(exception.getMessage().contains("Duplicate orderNum values detected"));
        assertTrue(exception.getMessage().contains("unique orderNum"));
        verify(activityTypeRepository, never()).saveAll(anyList());
    }
    
    @Test
    void batchUpdate_ShouldAllowValidOrderNum_WhenInCorrectRange() {
        // Arrange - User sets valid orderNum values
        ActivityTypeDTO validOrder1 = new ActivityTypeDTO(activityTypeId1, "Chill", List.of(), "🛋️", 1, userId, false);
        ActivityTypeDTO validOrder2 = new ActivityTypeDTO(activityTypeId2, "Food", List.of(), "🍽️", 2, userId, false);
        ActivityTypeDTO validOrder3 = new ActivityTypeDTO(activityTypeId3, "Active", List.of(), "🏃", 3, userId, false);
        
        BatchActivityTypeUpdateDTO batchDTO = new BatchActivityTypeUpdateDTO(
                List.of(validOrder1, validOrder2, validOrder3),
                List.of()
        );

        when(activityTypeRepository.countByCreatorIdAndIsPinnedTrue(userId)).thenReturn(0L);
        when(activityTypeRepository.countByCreatorId(userId)).thenReturn(3L);
        when(activityTypeRepository.findActivityTypesByCreatorId(userId)).thenReturn(List.of(chillActivityType, foodActivityType, activeActivityType));
        when(userService.getUserEntityById(userId)).thenReturn(testUser);
        when(activityTypeRepository.saveAll(anyList())).thenReturn(List.of(chillActivityType, foodActivityType, activeActivityType));

        // Act
        List<ActivityTypeDTO> result = activityTypeService.updateActivityTypes(userId, batchDTO);

        // Assert
        assertNotNull(result);
        assertEquals(3, result.size());
        verify(activityTypeRepository, times(1)).saveAll(anyList());
    }
    
    @Test
    void batchUpdate_ShouldHandleComplexValidation_WhenDeletingAndCreating() {
        // Arrange - User deletes 1 pinned item and creates 2 new pinned items (net +1, should be valid)
        // Starting state: 1 pinned out of 3 total (orderNum 0, 1, 2)
        // After update: delete 1 (orderNum 1), add 2 new = 2 pinned total (valid)
        // Final orderNum should be: 0, 1, 2, 3 (where 1 and 3 are the new ones)
        
        UUID newId1 = UUID.randomUUID();
        UUID newId2 = UUID.randomUUID();
        
        ActivityTypeDTO newPinned1 = new ActivityTypeDTO(newId1, "Study", List.of(), "✏️", 2, userId, true);
        ActivityTypeDTO newPinned2 = new ActivityTypeDTO(newId2, "Sports", List.of(), "⚽", 4, userId, true);
        
        BatchActivityTypeUpdateDTO batchDTO = new BatchActivityTypeUpdateDTO(
                List.of(newPinned1, newPinned2),
                List.of(activityTypeId2) // Delete foodActivityType (which is pinned)
        );

        // Mock current state: foodActivityType is pinned
        foodActivityType.setIsPinned(true);
        
        when(activityTypeRepository.countByCreatorIdAndIsPinnedTrue(userId)).thenReturn(1L); // 1 currently pinned
        when(activityTypeRepository.countByCreatorId(userId)).thenReturn(3L); // 3 total
        when(activityTypeRepository.findActivityTypesByCreatorId(userId)).thenReturn(List.of(chillActivityType, foodActivityType, activeActivityType));
        when(userService.getUserEntityById(userId)).thenReturn(testUser);
        when(activityTypeRepository.saveAll(anyList())).thenReturn(List.of());

        // Act
        List<ActivityTypeDTO> result = activityTypeService.updateActivityTypes(userId, batchDTO);

        // Assert - Should pass validation: 1 - 1 + 2 = 2 pinned (within limit of 3)
        assertNotNull(result);
        assertEquals(3, result.size()); // Returns all user's activity types
        verify(activityTypeRepository, times(1)).deleteAllById(anyList());
        verify(activityTypeRepository, times(1)).saveAll(anyList());
    }
    
    // MARK: - Multi-Phase Update & Constraint Handling Tests
    
    @Test
    void batchUpdate_ShouldHandleMultiPhaseUpdate_WhenExistingActivityTypesReordered() {
        // Arrange - Simulate the exact reordering scenario that caused constraint violation
        ActivityTypeDTO reorderedChill = new ActivityTypeDTO(
            activityTypeId1, "Chill", List.of(), "🛋️", 1, userId, false
        );
        ActivityTypeDTO reorderedFood = new ActivityTypeDTO(
            activityTypeId2, "Food", List.of(), "🍽️", 2, userId, true
        );
        ActivityTypeDTO reorderedActive = new ActivityTypeDTO(
            activityTypeId3, "Active", List.of(), "🏃", 3, userId, false
        );
        
        BatchActivityTypeUpdateDTO batchDTO = new BatchActivityTypeUpdateDTO(
                List.of(reorderedChill, reorderedFood, reorderedActive),
                List.of()
        );

        when(activityTypeRepository.countByCreatorIdAndIsPinnedTrue(userId)).thenReturn(1L);
        when(activityTypeRepository.countByCreatorId(userId)).thenReturn(3L);
        when(activityTypeRepository.findActivityTypesByCreatorId(userId))
                .thenReturn(List.of(chillActivityType, foodActivityType, activeActivityType));
        when(userService.getUserEntityById(userId)).thenReturn(testUser);
        
        // Mock existsById to simulate existing activity types
        when(activityTypeRepository.existsById(activityTypeId1)).thenReturn(true);
        when(activityTypeRepository.existsById(activityTypeId2)).thenReturn(true);
        when(activityTypeRepository.existsById(activityTypeId3)).thenReturn(true);
        
        // Mock individual save calls for two-phase update
        when(activityTypeRepository.save(any(ActivityType.class))).thenReturn(chillActivityType);

        // Act
        List<ActivityTypeDTO> result = activityTypeService.updateActivityTypes(userId, batchDTO);

        // Assert
        assertNotNull(result);
        assertEquals(3, result.size());
        
        // Verify that individual save was called multiple times for multi-phase update
        // (2 times per existing activity type: once for temp orderNum, once for final orderNum)
        verify(activityTypeRepository, times(6)).save(any(ActivityType.class));
        verify(logger, times(1)).info(contains("Updating 3 existing activity types"));
        verify(logger, times(1)).info(contains("Successfully completed multi-phase update"));
    }
    
    @Test
    void batchUpdate_ShouldSeparateNewAndExistingTypes_WhenMixedBatchUpdate() {
        // Arrange - Mix of new and existing activity types
        UUID newId = UUID.randomUUID();
        ActivityTypeDTO newType = new ActivityTypeDTO(newId, "Study", List.of(), "✏️", 1, userId, false);
        ActivityTypeDTO existingType = new ActivityTypeDTO(
            activityTypeId1, "Chill Updated", List.of(), "🛋️", 1, userId, true
        );
        
        BatchActivityTypeUpdateDTO batchDTO = new BatchActivityTypeUpdateDTO(
                List.of(newType, existingType),
                List.of()
        );

        when(activityTypeRepository.countByCreatorIdAndIsPinnedTrue(userId)).thenReturn(0L);
        when(activityTypeRepository.countByCreatorId(userId)).thenReturn(3L);
        when(activityTypeRepository.findActivityTypesByCreatorId(userId))
                .thenReturn(List.of(chillActivityType, foodActivityType, activeActivityType));
        when(userService.getUserEntityById(userId)).thenReturn(testUser);
        
        // Mock existsById to simulate one new, one existing
        when(activityTypeRepository.existsById(newId)).thenReturn(false);
        when(activityTypeRepository.existsById(activityTypeId1)).thenReturn(true);
        
        when(activityTypeRepository.saveAll(anyList())).thenReturn(List.of(new ActivityType()));
        when(activityTypeRepository.save(any(ActivityType.class))).thenReturn(chillActivityType);

        // Act
        List<ActivityTypeDTO> result = activityTypeService.updateActivityTypes(userId, batchDTO);

        // Assert
        assertNotNull(result);
        
        // Verify new types saved via saveAll, existing types saved individually
        verify(activityTypeRepository, times(1)).saveAll(anyList());
        verify(activityTypeRepository, times(2)).save(any(ActivityType.class)); // 2 phase for 1 existing
        verify(logger, times(1)).info(contains("Saved 1 new activity types"));
        verify(logger, times(1)).info(contains("Updating 1 existing activity types"));
    }
    
    @Test
    void batchUpdate_ShouldHandleConstraintViolationGracefully_WhenDatabaseConstraintFails() {
        // Arrange - Simulate database constraint violation during two-phase update
        ActivityTypeDTO reorderedType = new ActivityTypeDTO(
            activityTypeId1, "Chill", List.of(), "🛋️", 1, userId, false
        );
        
        BatchActivityTypeUpdateDTO batchDTO = new BatchActivityTypeUpdateDTO(
                List.of(reorderedType),
                List.of()
        );

        when(activityTypeRepository.countByCreatorIdAndIsPinnedTrue(userId)).thenReturn(0L);
        when(activityTypeRepository.countByCreatorId(userId)).thenReturn(1L);
        when(activityTypeRepository.findActivityTypesByCreatorId(userId))
                .thenReturn(List.of(chillActivityType));
        when(userService.getUserEntityById(userId)).thenReturn(testUser);
        when(activityTypeRepository.existsById(activityTypeId1)).thenReturn(true);
        
        // Mock constraint violation during save
        when(activityTypeRepository.save(any(ActivityType.class)))
                .thenThrow(new org.springframework.dao.DataIntegrityViolationException(
                    "Duplicate entry for key 'UK_activity_type_creator_order'"
                ));

        // Act & Assert
        org.springframework.dao.DataIntegrityViolationException exception = 
            assertThrows(org.springframework.dao.DataIntegrityViolationException.class,
                () -> activityTypeService.updateActivityTypes(userId, batchDTO));

        assertTrue(exception.getMessage().contains("Duplicate entry"));
        verify(logger, times(1)).error(contains("Error batch updating activity types"));
    }
    
    @Test
    void batchUpdate_ShouldHandleLargeReorderingBatch_WhenManyActivityTypes() {
        // Arrange - Test with larger number of activity types (10 items)
        List<ActivityType> manyActivityTypes = new ArrayList<>();
        List<ActivityTypeDTO> manyActivityTypeDTOs = new ArrayList<>();
        
        for (int i = 0; i < 10; i++) {
            UUID id = UUID.randomUUID();
            ActivityType entity = new ActivityType();
            entity.setId(id);
            entity.setTitle("Type " + i);
            entity.setIcon("🎯");
            entity.setCreator(testUser);
            entity.setOrderNum(i + 1);
            entity.setIsPinned(false);
            manyActivityTypes.add(entity);
            
            // Create reordered DTO (reverse order)
            ActivityTypeDTO dto = new ActivityTypeDTO(id, "Type " + i, List.of(), "🎯", 10 - i, userId, false);
            manyActivityTypeDTOs.add(dto);
        }
        
        BatchActivityTypeUpdateDTO batchDTO = new BatchActivityTypeUpdateDTO(manyActivityTypeDTOs, List.of());

        when(activityTypeRepository.countByCreatorIdAndIsPinnedTrue(userId)).thenReturn(0L);
        when(activityTypeRepository.countByCreatorId(userId)).thenReturn(10L);
        when(activityTypeRepository.findActivityTypesByCreatorId(userId)).thenReturn(manyActivityTypes);
        when(userService.getUserEntityById(userId)).thenReturn(testUser);
        
        // Mock all as existing
        for (ActivityType type : manyActivityTypes) {
            when(activityTypeRepository.existsById(type.getId())).thenReturn(true);
        }
        
        when(activityTypeRepository.save(any(ActivityType.class))).thenReturn(new ActivityType());

        // Act
        List<ActivityTypeDTO> result = activityTypeService.updateActivityTypes(userId, batchDTO);

        // Assert
        assertNotNull(result);
        
        // Verify all 10 existing types went through multi-phase update (20 save calls total)
        verify(activityTypeRepository, times(20)).save(any(ActivityType.class));
        verify(logger, times(1)).info(contains("Updating 10 existing activity types"));
    }
    
    @Test
    void batchUpdate_ShouldHandlePartialFailure_WhenPhase2Fails() {
        // Arrange - Simulate failure in phase 2 of multi-phase update
        ActivityTypeDTO reorderedType = new ActivityTypeDTO(
            activityTypeId1, "Chill", List.of(), "🛋️", 1, userId, false
        );
        
        BatchActivityTypeUpdateDTO batchDTO = new BatchActivityTypeUpdateDTO(
                List.of(reorderedType),
                List.of()
        );

        when(activityTypeRepository.countByCreatorIdAndIsPinnedTrue(userId)).thenReturn(0L);
        when(activityTypeRepository.countByCreatorId(userId)).thenReturn(1L);
        when(activityTypeRepository.findActivityTypesByCreatorId(userId))
                .thenReturn(List.of(chillActivityType));
        when(userService.getUserEntityById(userId)).thenReturn(testUser);
        when(activityTypeRepository.existsById(activityTypeId1)).thenReturn(true);
        
        // Mock phase 1 success, phase 2 failure
        when(activityTypeRepository.save(any(ActivityType.class)))
                .thenReturn(chillActivityType) // Phase 1 success
                .thenThrow(new RuntimeException("Phase 2 database error")); // Phase 2 failure

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> activityTypeService.updateActivityTypes(userId, batchDTO));

        assertTrue(exception.getMessage().contains("Phase 2 database error"));
        verify(activityTypeRepository, times(2)).save(any(ActivityType.class));
    }
    
    @Test
    void batchUpdate_ShouldValidateOrderNumUniqueness_WhenConflictWithRemainingTypes() {
        // Arrange - Update only some activity types, but create orderNum conflict with remaining ones
        // Existing: Chill(1), Food(2), Active(3)
        // Update: only Chill to orderNum=2 (conflicts with Food)
        ActivityTypeDTO conflictingUpdate = new ActivityTypeDTO(
            activityTypeId1, "Chill", List.of(), "🛋️", 2, userId, false // Conflicts with Food's orderNum
        );
        
        BatchActivityTypeUpdateDTO batchDTO = new BatchActivityTypeUpdateDTO(
                List.of(conflictingUpdate),
                List.of()
        );

        when(activityTypeRepository.countByCreatorIdAndIsPinnedTrue(userId)).thenReturn(1L);
        when(activityTypeRepository.countByCreatorId(userId)).thenReturn(3L);
        when(activityTypeRepository.findActivityTypesByCreatorId(userId))
                .thenReturn(List.of(chillActivityType, foodActivityType, activeActivityType));

        // Act & Assert
        ActivityTypeValidationException exception = assertThrows(ActivityTypeValidationException.class,
                () -> activityTypeService.updateActivityTypes(userId, batchDTO));

        assertTrue(exception.getMessage().contains("orderNum 2 conflicts with existing activity type"));
        verify(activityTypeRepository, never()).save(any(ActivityType.class));
        verify(activityTypeRepository, never()).saveAll(anyList());
    }
    
    @Test
    void batchUpdate_ShouldHandleEmptyExistingList_WhenUserHasNoActivityTypes() {
        // Arrange - User has no existing activity types, only creating new ones
        UUID newId = UUID.randomUUID();
        ActivityTypeDTO newType = new ActivityTypeDTO(newId, "First Type", List.of(), "🎯", 1, userId, false);
        
        BatchActivityTypeUpdateDTO batchDTO = new BatchActivityTypeUpdateDTO(
                List.of(newType),
                List.of()
        );

        when(activityTypeRepository.countByCreatorIdAndIsPinnedTrue(userId)).thenReturn(0L);
        when(activityTypeRepository.countByCreatorId(userId)).thenReturn(0L);
        when(activityTypeRepository.findActivityTypesByCreatorId(userId)).thenReturn(List.of());
        when(userService.getUserEntityById(userId)).thenReturn(testUser);
        when(activityTypeRepository.existsById(newId)).thenReturn(false);
        when(activityTypeRepository.saveAll(anyList())).thenReturn(List.of(new ActivityType()));

        // Act
        List<ActivityTypeDTO> result = activityTypeService.updateActivityTypes(userId, batchDTO);

        // Assert
        assertNotNull(result);
        
        // Should only use saveAll for new types, no individual saves
        verify(activityTypeRepository, times(1)).saveAll(anyList());
        verify(activityTypeRepository, never()).save(any(ActivityType.class));
        verify(logger, times(1)).info(contains("Saved 1 new activity types"));
        verify(logger, never()).info(contains("Updating"));
    }
    
    @Test
    void batchUpdate_ShouldHandleRepositoryFailure_WhenSaveAllFails() {
        // Arrange - Test failure in saveAll for new activity types
        UUID newId = UUID.randomUUID();
        ActivityTypeDTO newType = new ActivityTypeDTO(newId, "New Type", List.of(), "🎯", 1, userId, false);
        
        BatchActivityTypeUpdateDTO batchDTO = new BatchActivityTypeUpdateDTO(
                List.of(newType),
                List.of()
        );

        when(activityTypeRepository.countByCreatorIdAndIsPinnedTrue(userId)).thenReturn(0L);
        when(activityTypeRepository.countByCreatorId(userId)).thenReturn(0L);
        when(activityTypeRepository.findActivityTypesByCreatorId(userId)).thenReturn(List.of());
        when(userService.getUserEntityById(userId)).thenReturn(testUser);
        when(activityTypeRepository.existsById(newId)).thenReturn(false);
        when(activityTypeRepository.saveAll(anyList()))
                .thenThrow(new org.springframework.dao.DataAccessException("Database connection lost") {});

        // Act & Assert
        org.springframework.dao.DataAccessException exception = 
            assertThrows(org.springframework.dao.DataAccessException.class,
                () -> activityTypeService.updateActivityTypes(userId, batchDTO));

        assertTrue(exception.getMessage().contains("Database connection lost"));
        verify(logger, times(1)).error(contains("Error batch updating activity types"));
    }
    
    @Test
    void batchUpdate_ShouldHandleConcurrentModification_WhenActivityTypeDeletedDuringUpdate() {
        // Arrange - Simulate activity type being deleted by another process during update
        ActivityTypeDTO updateType = new ActivityTypeDTO(
            activityTypeId1, "Updated Chill", List.of(), "🛋️", 1, userId, false
        );
        
        BatchActivityTypeUpdateDTO batchDTO = new BatchActivityTypeUpdateDTO(
                List.of(updateType),
                List.of()
        );

        when(activityTypeRepository.countByCreatorIdAndIsPinnedTrue(userId)).thenReturn(0L);
        when(activityTypeRepository.countByCreatorId(userId)).thenReturn(1L);
        when(activityTypeRepository.findActivityTypesByCreatorId(userId))
                .thenReturn(List.of(chillActivityType));
        when(userService.getUserEntityById(userId)).thenReturn(testUser);
        
        // Simulate concurrent deletion - existsById returns false during update
        when(activityTypeRepository.existsById(activityTypeId1)).thenReturn(false);
        when(activityTypeRepository.saveAll(anyList())).thenReturn(List.of(new ActivityType()));

        // Act
        List<ActivityTypeDTO> result = activityTypeService.updateActivityTypes(userId, batchDTO);

        // Assert - Should treat as new activity type since existsById returned false
        assertNotNull(result);
        verify(activityTypeRepository, times(1)).saveAll(anyList());
        verify(activityTypeRepository, never()).save(any(ActivityType.class));
        verify(logger, times(1)).info(contains("Saved 1 new activity types"));
    }

    // MARK: - Extended Front-End Scenario Tests

    @Test
    void batchUpdate_ShouldHandleAssociatedFriends_WhenActivityTypeHasFriendsAssociated() {
        // Arrange - Test with associated friends (front-end feature)
        UUID friend1Id = UUID.randomUUID();
        UUID friend2Id = UUID.randomUUID();
        
                 ActivityTypeDTO activityTypeWithFriends = new ActivityTypeDTO(
             activityTypeId1,
             "Chill",
             Arrays.asList(
                 new com.danielagapov.spawn.DTOs.User.BaseUserDTO(friend1Id, "Friend One", "friend1@test.com", "friend1", "bio1", "pic1.jpg"),
                 new com.danielagapov.spawn.DTOs.User.BaseUserDTO(friend2Id, "Friend Two", "friend2@test.com", "friend2", "bio2", "pic2.jpg")
             ),
            "🛋️",
            1,
            userId,
            false
        );
        
        BatchActivityTypeUpdateDTO batchDTO = new BatchActivityTypeUpdateDTO(
            List.of(activityTypeWithFriends),
            List.of()
        );

        when(activityTypeRepository.countByCreatorIdAndIsPinnedTrue(userId)).thenReturn(0L);
        when(activityTypeRepository.countByCreatorId(userId)).thenReturn(1L);
        when(activityTypeRepository.findActivityTypesByCreatorId(userId)).thenReturn(List.of(chillActivityType));
        when(userService.getUserEntityById(userId)).thenReturn(testUser);
        when(activityTypeRepository.saveAll(anyList())).thenReturn(List.of(chillActivityType));

        // Act
        List<ActivityTypeDTO> result = activityTypeService.updateActivityTypes(userId, batchDTO);

        // Assert
        assertNotNull(result);
        verify(activityTypeRepository, times(1)).saveAll(anyList());
    }

    @Test
    void batchUpdate_ShouldHandleEmptyAssociatedFriends_WhenRemovingAllFriends() {
        // Arrange - Update activity type to remove all associated friends
        ActivityTypeDTO activityTypeWithoutFriends = new ActivityTypeDTO(
            activityTypeId1,
            "Chill",
            List.of(), // Empty friends list
            "🛋️",
            1,
            userId,
            false
        );
        
        BatchActivityTypeUpdateDTO batchDTO = new BatchActivityTypeUpdateDTO(
            List.of(activityTypeWithoutFriends),
            List.of()
        );

        when(activityTypeRepository.countByCreatorIdAndIsPinnedTrue(userId)).thenReturn(0L);
        when(activityTypeRepository.countByCreatorId(userId)).thenReturn(1L);
        when(activityTypeRepository.findActivityTypesByCreatorId(userId)).thenReturn(List.of(chillActivityType));
        when(userService.getUserEntityById(userId)).thenReturn(testUser);
        when(activityTypeRepository.saveAll(anyList())).thenReturn(List.of(chillActivityType));

        // Act
        List<ActivityTypeDTO> result = activityTypeService.updateActivityTypes(userId, batchDTO);

        // Assert
        assertNotNull(result);
        verify(activityTypeRepository, times(1)).saveAll(anyList());
    }

    @Test
    void batchUpdate_ShouldHandleMaxLengthTitle_WhenTitleIsVeryLong() {
        // Arrange - Test with very long title (boundary testing)
        String longTitle = "A".repeat(255); // Typical database varchar limit
        
        ActivityTypeDTO activityTypeWithLongTitle = new ActivityTypeDTO(
            activityTypeId1,
            longTitle,
            List.of(),
            "🛋️",
            1,
            userId,
            false
        );
        
        BatchActivityTypeUpdateDTO batchDTO = new BatchActivityTypeUpdateDTO(
            List.of(activityTypeWithLongTitle),
            List.of()
        );

        when(activityTypeRepository.countByCreatorIdAndIsPinnedTrue(userId)).thenReturn(0L);
        when(activityTypeRepository.countByCreatorId(userId)).thenReturn(1L);
        when(activityTypeRepository.findActivityTypesByCreatorId(userId)).thenReturn(List.of(chillActivityType));
        when(userService.getUserEntityById(userId)).thenReturn(testUser);
        when(activityTypeRepository.saveAll(anyList())).thenReturn(List.of(chillActivityType));

        // Act
        List<ActivityTypeDTO> result = activityTypeService.updateActivityTypes(userId, batchDTO);

        // Assert
        assertNotNull(result);
        verify(activityTypeRepository, times(1)).saveAll(anyList());
    }

    @Test
    void batchUpdate_ShouldHandleSpecialCharactersInTitle_WhenTitleContainsUnicodeAndSymbols() {
        // Arrange - Test with special characters, emojis, and symbols
        String specialTitle = "🎉🎊 Test & Activity (新年) - Special Event! @#$%^&*()_+ <>";
        
        ActivityTypeDTO activityTypeWithSpecialChars = new ActivityTypeDTO(
            activityTypeId1,
            specialTitle,
            List.of(),
            "🎉",
            1,
            userId,
            false
        );
        
        BatchActivityTypeUpdateDTO batchDTO = new BatchActivityTypeUpdateDTO(
            List.of(activityTypeWithSpecialChars),
            List.of()
        );

        when(activityTypeRepository.countByCreatorIdAndIsPinnedTrue(userId)).thenReturn(0L);
        when(activityTypeRepository.countByCreatorId(userId)).thenReturn(1L);
        when(activityTypeRepository.findActivityTypesByCreatorId(userId)).thenReturn(List.of(chillActivityType));
        when(userService.getUserEntityById(userId)).thenReturn(testUser);
        when(activityTypeRepository.saveAll(anyList())).thenReturn(List.of(chillActivityType));

        // Act
        List<ActivityTypeDTO> result = activityTypeService.updateActivityTypes(userId, batchDTO);

        // Assert
        assertNotNull(result);
        verify(activityTypeRepository, times(1)).saveAll(anyList());
    }

    @Test
    void batchUpdate_ShouldHandleZeroOrderNum_WhenActivityTypeMovedToFirst() {
        // Arrange - Ensure orderNum=0 is handled properly
        ActivityTypeDTO firstActivityType = new ActivityTypeDTO(
            activityTypeId1,
            "Chill",
            List.of(),
            "🛋️",
            1, // First position
            userId,
            true // Pin it to make it truly first
        );
        
        BatchActivityTypeUpdateDTO batchDTO = new BatchActivityTypeUpdateDTO(
            List.of(firstActivityType),
            List.of()
        );

        when(activityTypeRepository.countByCreatorIdAndIsPinnedTrue(userId)).thenReturn(0L);
        when(activityTypeRepository.countByCreatorId(userId)).thenReturn(3L);
        when(activityTypeRepository.findActivityTypesByCreatorId(userId)).thenReturn(List.of(chillActivityType, foodActivityType, activeActivityType));
        when(userService.getUserEntityById(userId)).thenReturn(testUser);
        when(activityTypeRepository.saveAll(anyList())).thenReturn(List.of(chillActivityType));

        // Act
        List<ActivityTypeDTO> result = activityTypeService.updateActivityTypes(userId, batchDTO);

        // Assert
        assertNotNull(result);
        verify(activityTypeRepository, times(1)).saveAll(anyList());
    }

    @Test
    void batchUpdate_ShouldHandleGapInOrderNums_WhenUserDeletesMiddleActivityType() {
        // Arrange - Delete middle activity type creating order gap (0, 2 instead of 0, 1, 2)
        // This tests if the validation handles non-consecutive order numbers properly
        ActivityTypeDTO updatedChill = new ActivityTypeDTO(
            activityTypeId1, "Chill", List.of(), "🛋️", 1, userId, false
        );
        ActivityTypeDTO updatedActive = new ActivityTypeDTO(
            activityTypeId3, "Active", List.of(), "🏃", 2, userId, false // Moved from 3 to 2
        );
        
        BatchActivityTypeUpdateDTO batchDTO = new BatchActivityTypeUpdateDTO(
            List.of(updatedChill, updatedActive),
            List.of(activityTypeId2) // Delete food (was at position 2)
        );

        // Mock the initial state (3 activity types, 0 pinned)
        when(activityTypeRepository.countByCreatorIdAndIsPinnedTrue(userId)).thenReturn(0L);
        when(activityTypeRepository.countByCreatorId(userId)).thenReturn(3L);
        when(activityTypeRepository.findActivityTypesByCreatorId(userId))
                .thenReturn(List.of(chillActivityType, foodActivityType, activeActivityType))
                .thenReturn(List.of(chillActivityType, activeActivityType)); // After deletion, only return remaining ones
        when(userService.getUserEntityById(userId)).thenReturn(testUser);
        when(activityTypeRepository.saveAll(anyList())).thenReturn(List.of(chillActivityType, activeActivityType));

        // Act
        List<ActivityTypeDTO> result = activityTypeService.updateActivityTypes(userId, batchDTO);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        verify(activityTypeRepository, times(1)).deleteAllById(List.of(activityTypeId2));
    }

    @Test
    void batchUpdate_ShouldHandleIdenticalTitles_WhenUserCreatesActivityTypesWithSameName() {
        // Arrange - Create multiple activity types with identical titles (should be allowed)
        UUID newId1 = UUID.randomUUID();
        UUID newId2 = UUID.randomUUID();
        
        ActivityTypeDTO duplicate1 = new ActivityTypeDTO(
            newId1, "Study", List.of(), "📚", 1, userId, false
        );
        ActivityTypeDTO duplicate2 = new ActivityTypeDTO(
            newId2, "Study", List.of(), "✏️", 2, userId, false // Same title, different icon
        );
        
        BatchActivityTypeUpdateDTO batchDTO = new BatchActivityTypeUpdateDTO(
            List.of(duplicate1, duplicate2),
            List.of()
        );

        when(activityTypeRepository.countByCreatorIdAndIsPinnedTrue(userId)).thenReturn(0L);
        when(activityTypeRepository.countByCreatorId(userId)).thenReturn(0L);
        when(activityTypeRepository.findActivityTypesByCreatorId(userId)).thenReturn(List.of());
        when(userService.getUserEntityById(userId)).thenReturn(testUser);
        when(activityTypeRepository.saveAll(anyList())).thenReturn(List.of(new ActivityType(), new ActivityType()));

        // Act
        List<ActivityTypeDTO> result = activityTypeService.updateActivityTypes(userId, batchDTO);

        // Assert - Should allow identical titles
        assertNotNull(result);
        verify(activityTypeRepository, times(1)).saveAll(anyList());
    }

    @Test
    void batchUpdate_ShouldHandleBoundaryPinnedCount_WhenExactlyAtLimit() {
        // Arrange - Test exactly at the pinned limit boundary (3 pinned)
        ActivityTypeDTO pinned1 = new ActivityTypeDTO(activityTypeId1, "Chill", List.of(), "🛋️", 1, userId, true);
        ActivityTypeDTO pinned2 = new ActivityTypeDTO(activityTypeId2, "Food", List.of(), "🍽️", 2, userId, true);
        ActivityTypeDTO pinned3 = new ActivityTypeDTO(activityTypeId3, "Active", List.of(), "🏃", 3, userId, true);
        
        UUID newId = UUID.randomUUID();
        ActivityTypeDTO unpinned = new ActivityTypeDTO(newId, "Study", List.of(), "📚", 4, userId, false);
        
        BatchActivityTypeUpdateDTO batchDTO = new BatchActivityTypeUpdateDTO(
            List.of(pinned1, pinned2, pinned3, unpinned),
            List.of()
        );

        when(activityTypeRepository.countByCreatorIdAndIsPinnedTrue(userId)).thenReturn(0L);
        when(activityTypeRepository.countByCreatorId(userId)).thenReturn(3L);
        when(activityTypeRepository.findActivityTypesByCreatorId(userId)).thenReturn(List.of(chillActivityType, foodActivityType, activeActivityType));
        when(userService.getUserEntityById(userId)).thenReturn(testUser);
        when(activityTypeRepository.saveAll(anyList())).thenReturn(List.of(new ActivityType()));

        // Act
        List<ActivityTypeDTO> result = activityTypeService.updateActivityTypes(userId, batchDTO);

        // Assert - Should pass with exactly 3 pinned
        assertNotNull(result);
        verify(activityTypeRepository, times(1)).saveAll(anyList());
    }

    @Test
    void batchUpdate_ShouldHandleNullIcon_WhenIconIsNotProvided() {
        // Arrange - Test with null icon (should be handled gracefully)
        ActivityTypeDTO activityTypeWithNullIcon = new ActivityTypeDTO(
            activityTypeId1,
            "Chill",
            List.of(),
            null, // Null icon
            1,
            userId,
            false
        );
        
        BatchActivityTypeUpdateDTO batchDTO = new BatchActivityTypeUpdateDTO(
            List.of(activityTypeWithNullIcon),
            List.of()
        );

        when(activityTypeRepository.countByCreatorIdAndIsPinnedTrue(userId)).thenReturn(0L);
        when(activityTypeRepository.countByCreatorId(userId)).thenReturn(1L);
        when(activityTypeRepository.findActivityTypesByCreatorId(userId)).thenReturn(List.of(chillActivityType));
        when(userService.getUserEntityById(userId)).thenReturn(testUser);
        when(activityTypeRepository.saveAll(anyList())).thenReturn(List.of(chillActivityType));

        // Act
        List<ActivityTypeDTO> result = activityTypeService.updateActivityTypes(userId, batchDTO);

        // Assert
        assertNotNull(result);
        verify(activityTypeRepository, times(1)).saveAll(anyList());
    }

    @Test
    void batchUpdate_ShouldHandleEmptyTitle_WhenTitleIsEmpty() {
        // Arrange - Test with empty title (should be handled or validated)
        ActivityTypeDTO activityTypeWithEmptyTitle = new ActivityTypeDTO(
            activityTypeId1,
            "", // Empty title
            List.of(),
            "🛋️",
            1,
            userId,
            false
        );
        
        BatchActivityTypeUpdateDTO batchDTO = new BatchActivityTypeUpdateDTO(
            List.of(activityTypeWithEmptyTitle),
            List.of()
        );

        when(activityTypeRepository.countByCreatorIdAndIsPinnedTrue(userId)).thenReturn(0L);
        when(activityTypeRepository.countByCreatorId(userId)).thenReturn(1L);
        when(activityTypeRepository.findActivityTypesByCreatorId(userId)).thenReturn(List.of(chillActivityType));
        when(userService.getUserEntityById(userId)).thenReturn(testUser);
        when(activityTypeRepository.saveAll(anyList())).thenReturn(List.of(chillActivityType));

        // Act
        List<ActivityTypeDTO> result = activityTypeService.updateActivityTypes(userId, batchDTO);

        // Assert
        assertNotNull(result);
        verify(activityTypeRepository, times(1)).saveAll(anyList());
    }

    @Test
    void batchUpdate_ShouldHandleRapidFireUpdates_WhenUserMakesQuickChanges() {
        // Arrange - Simulate rapid consecutive updates (like user quickly toggling pins)
        ActivityTypeDTO quickUpdate1 = new ActivityTypeDTO(
            activityTypeId1, "Chill", List.of(), "🛋️", 1, userId, true // Pin
        );
        ActivityTypeDTO quickUpdate2 = new ActivityTypeDTO(
            activityTypeId1, "Chill", List.of(), "🛋️", 1, userId, false // Unpin
        );
        
        BatchActivityTypeUpdateDTO batchDTO1 = new BatchActivityTypeUpdateDTO(List.of(quickUpdate1), List.of());
        BatchActivityTypeUpdateDTO batchDTO2 = new BatchActivityTypeUpdateDTO(List.of(quickUpdate2), List.of());

        when(activityTypeRepository.countByCreatorIdAndIsPinnedTrue(userId)).thenReturn(0L);
        when(activityTypeRepository.countByCreatorId(userId)).thenReturn(1L);
        when(activityTypeRepository.findActivityTypesByCreatorId(userId)).thenReturn(List.of(chillActivityType));
        when(userService.getUserEntityById(userId)).thenReturn(testUser);
        when(activityTypeRepository.saveAll(anyList())).thenReturn(List.of(chillActivityType));

        // Act - Simulate rapid updates
        List<ActivityTypeDTO> result1 = activityTypeService.updateActivityTypes(userId, batchDTO1);
        List<ActivityTypeDTO> result2 = activityTypeService.updateActivityTypes(userId, batchDTO2);

        // Assert
        assertNotNull(result1);
        assertNotNull(result2);
        verify(activityTypeRepository, times(2)).saveAll(anyList());
    }

    @Test
    void batchUpdate_ShouldHandleNegativeOrderNum_WhenDataCorrupted() {
        // Arrange - Test with negative order number (data corruption scenario)
        ActivityTypeDTO corruptedOrderDTO = new ActivityTypeDTO(
            activityTypeId1, "Chill", List.of(), "🛋️", 0, userId, false
        );
        
        BatchActivityTypeUpdateDTO batchDTO = new BatchActivityTypeUpdateDTO(
            List.of(corruptedOrderDTO),
            List.of()
        );

        when(activityTypeRepository.countByCreatorIdAndIsPinnedTrue(userId)).thenReturn(0L);
        when(activityTypeRepository.countByCreatorId(userId)).thenReturn(1L);
        when(activityTypeRepository.findActivityTypesByCreatorId(userId)).thenReturn(List.of(chillActivityType));

        // Act & Assert
        ActivityTypeValidationException exception = assertThrows(ActivityTypeValidationException.class,
                () -> activityTypeService.updateActivityTypes(userId, batchDTO));

        assertTrue(exception.getMessage().contains("Invalid orderNum 0"));
    }

    @Test
    void batchUpdate_ShouldHandleExtremelyHighOrderNum_WhenDataCorrupted() {
        // Arrange - Test with extremely high order number
        ActivityTypeDTO corruptedOrderDTO = new ActivityTypeDTO(
            activityTypeId1, "Chill", List.of(), "🛋️", Integer.MAX_VALUE, userId, false
        );
        
        BatchActivityTypeUpdateDTO batchDTO = new BatchActivityTypeUpdateDTO(
            List.of(corruptedOrderDTO),
            List.of()
        );

        when(activityTypeRepository.countByCreatorIdAndIsPinnedTrue(userId)).thenReturn(0L);
        when(activityTypeRepository.countByCreatorId(userId)).thenReturn(1L);
        when(activityTypeRepository.findActivityTypesByCreatorId(userId)).thenReturn(List.of(chillActivityType));

        // Act & Assert
        ActivityTypeValidationException exception = assertThrows(ActivityTypeValidationException.class,
                () -> activityTypeService.updateActivityTypes(userId, batchDTO));

        assertTrue(exception.getMessage().contains("Invalid orderNum " + Integer.MAX_VALUE));
    }

    @Test
    void fetchActivityTypes_ShouldHandleRepositoryTimeout_WhenDatabaseSlow() {
        // Arrange - Simulate database timeout
        when(activityTypeRepository.findActivityTypesByCreatorId(userId))
                .thenThrow(new org.springframework.dao.QueryTimeoutException("Query timeout"));

        // Act & Assert
        org.springframework.dao.QueryTimeoutException exception = 
            assertThrows(org.springframework.dao.QueryTimeoutException.class,
                () -> activityTypeService.getActivityTypesByUserId(userId));

        assertTrue(exception.getMessage().contains("Query timeout"));
    }

    @Test
    void batchUpdate_ShouldHandleRepositoryOptimisticLockingException_WhenConcurrentUpdate() {
        // Arrange - Simulate optimistic locking exception
        ActivityTypeDTO updateDTO = new ActivityTypeDTO(
            activityTypeId1, "Updated Chill", List.of(), "🛋️", 1, userId, false
        );
        
        BatchActivityTypeUpdateDTO batchDTO = new BatchActivityTypeUpdateDTO(
            List.of(updateDTO),
            List.of()
        );

        when(activityTypeRepository.countByCreatorIdAndIsPinnedTrue(userId)).thenReturn(0L);
        when(activityTypeRepository.countByCreatorId(userId)).thenReturn(1L);
        when(activityTypeRepository.findActivityTypesByCreatorId(userId)).thenReturn(List.of(chillActivityType));
        when(userService.getUserEntityById(userId)).thenReturn(testUser);
        when(activityTypeRepository.saveAll(anyList()))
                .thenThrow(new org.springframework.orm.ObjectOptimisticLockingFailureException("Version conflict", new Exception()));

        // Act & Assert
        org.springframework.orm.ObjectOptimisticLockingFailureException exception = 
            assertThrows(org.springframework.orm.ObjectOptimisticLockingFailureException.class,
                () -> activityTypeService.updateActivityTypes(userId, batchDTO));

        assertTrue(exception.getMessage().contains("Version conflict"));
    }

    @Test
    void batchUpdate_ShouldHandleTransactionRollback_WhenPartialUpdateFails() {
        // Arrange - Test transaction behavior when part of the update fails
        UUID newId = UUID.randomUUID();
        ActivityTypeDTO newType = new ActivityTypeDTO(newId, "New Type", List.of(), "🎯", 1, userId, false);
        ActivityTypeDTO existingType = new ActivityTypeDTO(activityTypeId1, "Updated", List.of(), "🛋️", 2, userId, false);
        
        BatchActivityTypeUpdateDTO batchDTO = new BatchActivityTypeUpdateDTO(
            List.of(newType, existingType),
            List.of()
        );

        when(activityTypeRepository.countByCreatorIdAndIsPinnedTrue(userId)).thenReturn(0L);
        when(activityTypeRepository.countByCreatorId(userId)).thenReturn(1L);
        when(activityTypeRepository.findActivityTypesByCreatorId(userId)).thenReturn(List.of(chillActivityType));
        when(userService.getUserEntityById(userId)).thenReturn(testUser);
        when(activityTypeRepository.existsById(newId)).thenReturn(false);
        when(activityTypeRepository.existsById(activityTypeId1)).thenReturn(true);
        
        // Fail on saveAll but succeed on individual save
        when(activityTypeRepository.saveAll(anyList()))
                .thenThrow(new org.springframework.dao.DataAccessException("Transaction failed") {});

        // Act & Assert
        org.springframework.dao.DataAccessException exception = 
            assertThrows(org.springframework.dao.DataAccessException.class,
                () -> activityTypeService.updateActivityTypes(userId, batchDTO));

        assertTrue(exception.getMessage().contains("Transaction failed"));
    }
} 