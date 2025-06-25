package com.danielagapov.spawn.ServiceTests;

import com.danielagapov.spawn.DTOs.ActivityType.ActivityTypeDTO;
import com.danielagapov.spawn.DTOs.ActivityType.BatchActivityTypeUpdateDTO;
import com.danielagapov.spawn.Exceptions.Logger.ILogger;
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

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

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
    private UUID activityTypeId;
    private User testUser;
    private ActivityType testActivityType;
    private ActivityTypeDTO testActivityTypeDTO;

    @BeforeEach
    void setUp() {
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
        testActivityType.setAssociatedFriends(List.of());
        testActivityType.setIsPinned(false);
        
        testActivityTypeDTO = new ActivityTypeDTO(
            activityTypeId,
            "Test Activity Type",
                List.of(),
            "🎯",
            1,
            userId,
            false
        );
    }

    @Test
    void getActivityTypesByUserId_ShouldReturnActivityTypes_WhenUserHasActivityTypes() {
        // Arrange
        List<ActivityType> activityTypes = Collections.singletonList(testActivityType);
        when(activityTypeRepository.findActivityTypesByCreatorId(userId))
                .thenReturn(activityTypes);

        // Act
        List<ActivityTypeDTO> result = activityTypeService.getActivityTypesByUserId(userId);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(activityTypeRepository, times(1)).findActivityTypesByCreatorId(userId);
    }

    @Test
    void getActivityTypesByUserId_ShouldReturnEmptyList_WhenUserHasNoActivityTypes() {
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

    @Test
    void updateActivityTypes_ShouldUpdateActivityTypes_WhenValidInput() {
        // Arrange
        BatchActivityTypeUpdateDTO batchDTO = new BatchActivityTypeUpdateDTO(
                Collections.singletonList(testActivityTypeDTO),
                List.of()
        );

        when(userService.getUserEntityById(userId)).thenReturn(testUser);
        when(activityTypeRepository.saveAll(anyList())).thenReturn(Collections.singletonList(testActivityType));

        // Act
        BatchActivityTypeUpdateDTO result = activityTypeService.updateActivityTypes(batchDTO);

        // Assert
        assertNotNull(result);
        assertEquals(batchDTO, result);
        verify(userService, times(1)).getUserEntityById(userId);
        verify(activityTypeRepository, times(1)).saveAll(anyList());
    }

    @Test
    void initializeDefaultActivityTypesForUser_ShouldCreateDefaultActivityTypes() {
        // Act
        assertDoesNotThrow(() -> activityTypeService.initializeDefaultActivityTypesForUser(testUser));

        // Assert
        verify(activityTypeRepository, times(4)).save(any(ActivityType.class));
    }

    @Test
    void setOrderNumber_ShouldSetCorrectOrderNumber() {
        // Arrange
        when(activityTypeRepository.findMaxOrderNumberByCreatorId(userId)).thenReturn(5);

        // Act
        activityTypeService.setOrderNumber(testActivityType);

        // Assert
        assertEquals(6, testActivityType.getOrderNum());
    }
} 