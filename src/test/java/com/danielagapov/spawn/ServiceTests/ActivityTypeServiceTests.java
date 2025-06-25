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
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessException;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ActivityTypeServiceTests {

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
        
        testActivityTypeDTO = new ActivityTypeDTO(
            activityTypeId,
            "Test Activity Type",
            List.of(),
            "🎯",
            1
        );
    }

    @Test
    void getActivityTypesByUserId_ShouldReturnActivityTypes_WhenUserHasActivityTypes() {
        // Arrange
        List<ActivityType> activityTypes = Arrays.asList(testActivityType);
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
                .thenReturn(Arrays.asList());

        // Act
        List<ActivityTypeDTO> result = activityTypeService.getActivityTypesByUserId(userId);

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(activityTypeRepository, times(1)).findActivityTypesByCreatorId(userId);
    }

    @Test
    void createActivityType_ShouldCreateActivityType_WhenValidInput() {
        // Arrange
        when(userService.getUserEntityById(userId)).thenReturn(testUser);
        when(activityTypeRepository.findMaxOrderNumberByCreatorId(userId)).thenReturn(0);
        when(activityTypeRepository.save(any(ActivityType.class))).thenReturn(testActivityType);

        // Act
        ActivityTypeDTO result = activityTypeService.createActivityType(userId, testActivityTypeDTO);

        // Assert
        assertNotNull(result);
        verify(userService, times(1)).getUserEntityById(userId);
        verify(activityTypeRepository, times(1)).save(any(ActivityType.class));
    }

    @Test
    void createActivityType_ShouldThrowException_WhenUserNotFound() {
        // Arrange
        when(userService.getUserEntityById(userId)).thenThrow(new BaseNotFoundException(null, userId));

        // Act & Assert
        assertThrows(BaseNotFoundException.class, 
                () -> activityTypeService.createActivityType(userId, testActivityTypeDTO));
    }

    @Test
    void updateActivityTypes_ShouldUpdateActivityTypes_WhenValidInput() {
        // Arrange
        BatchActivityTypeUpdateDTO batchDTO = new BatchActivityTypeUpdateDTO();
        batchDTO.setUpdatedActivityTypes(Arrays.asList(testActivityTypeDTO));
        batchDTO.setDeletedActivityTypeIds(Arrays.asList());

        when(userService.getUserEntityById(userId)).thenReturn(testUser);
        when(activityTypeRepository.saveAll(anyList())).thenReturn(Arrays.asList(testActivityType));

        // Act
        BatchActivityTypeUpdateDTO result = activityTypeService.updateActivityTypes(userId, batchDTO);

        // Assert
        assertNotNull(result);
        assertEquals(batchDTO, result);
        verify(userService, times(1)).getUserEntityById(userId);
        verify(activityTypeRepository, times(1)).saveAll(anyList());
    }

    @Test
    void deleteActivityType_ShouldDeleteActivityType_WhenExists() {
        // Arrange
        when(activityTypeRepository.existsById(activityTypeId)).thenReturn(true);

        // Act
        assertDoesNotThrow(() -> activityTypeService.deleteActivityType(activityTypeId));

        // Assert
        verify(activityTypeRepository, times(1)).existsById(activityTypeId);
        verify(activityTypeRepository, times(1)).deleteById(activityTypeId);
    }

    @Test
    void deleteActivityType_ShouldThrowException_WhenNotExists() {
        // Arrange
        when(activityTypeRepository.existsById(activityTypeId)).thenReturn(false);

        // Act & Assert
        assertThrows(BaseNotFoundException.class, 
                () -> activityTypeService.deleteActivityType(activityTypeId));
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