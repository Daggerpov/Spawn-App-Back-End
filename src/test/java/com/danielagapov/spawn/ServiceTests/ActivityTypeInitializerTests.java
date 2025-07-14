package com.danielagapov.spawn.ServiceTests;

import com.danielagapov.spawn.Config.ActivityTypeInitializer;
import com.danielagapov.spawn.DTOs.ActivityType.ActivityTypeDTO;
import com.danielagapov.spawn.Exceptions.Logger.ILogger;
import com.danielagapov.spawn.Models.User.User;
import com.danielagapov.spawn.Repositories.User.IUserRepository;
import com.danielagapov.spawn.Services.ActivityType.IActivityTypeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.CommandLineRunner;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ActivityTypeInitializer to ensure it properly initializes activity types
 * for existing users who don't have them (e.g., after database wipe and redeploy).
 */
@ExtendWith(MockitoExtension.class)
@Order(3)
class ActivityTypeInitializerTests {

    @Mock
    private IUserRepository userRepository;

    @Mock
    private IActivityTypeService activityTypeService;

    @Mock
    private ILogger logger;

    private ActivityTypeInitializer activityTypeInitializer;
    private List<User> testUsers;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        activityTypeInitializer = new ActivityTypeInitializer();
        
        // Create test users
        testUsers = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            User user = new User();
            user.setId(UUID.randomUUID());
            user.setEmail("user" + i + "@example.com");
            user.setUsername("user" + i);
            user.setName("User " + i);
            user.setDateCreated(new Date());
            testUsers.add(user);
        }
    }

    @Test
    void initializeActivityTypes_ShouldInitializeForUsersWithoutActivityTypes() throws Exception {
        // Arrange
        when(userRepository.findAll()).thenReturn(testUsers);
        
        // Mock first user has no activity types
        when(activityTypeService.getActivityTypesByUserId(testUsers.get(0).getId()))
            .thenReturn(new ArrayList<>());
        
        // Mock second user has activity types
        List<ActivityTypeDTO> existingActivityTypes = List.of(
            new ActivityTypeDTO(UUID.randomUUID(), "Existing Type", List.of(), "🎯", 0, testUsers.get(1).getId(), false)
        );
        when(activityTypeService.getActivityTypesByUserId(testUsers.get(1).getId()))
            .thenReturn(existingActivityTypes);
        
        // Mock third user has no activity types
        when(activityTypeService.getActivityTypesByUserId(testUsers.get(2).getId()))
            .thenReturn(new ArrayList<>());

        // Act
        CommandLineRunner runner = activityTypeInitializer.initializeActivityTypes(
            userRepository, activityTypeService, logger
        );
        runner.run();

        // Assert
        verify(userRepository, times(1)).findAll();
        verify(activityTypeService, times(3)).getActivityTypesByUserId(any(UUID.class));
        
        // Should initialize for users 0 and 2 (who don't have activity types)
        verify(activityTypeService, times(1)).initializeDefaultActivityTypesForUser(testUsers.get(0));
        verify(activityTypeService, times(1)).initializeDefaultActivityTypesForUser(testUsers.get(2));
        
        // Should NOT initialize for user 1 (who already has activity types)
        verify(activityTypeService, never()).initializeDefaultActivityTypesForUser(testUsers.get(1));
        
        // Should log appropriately
        verify(logger, times(1)).info("Starting activity type initialization for existing users");
        verify(logger, times(1)).info("Found " + testUsers.size() + " users in the database");
        verify(logger, times(1)).info(contains("Activity type initialization completed"));
    }

    @Test
    void initializeActivityTypes_ShouldSkipUsersWithExistingActivityTypes() throws Exception {
        // Arrange
        when(userRepository.findAll()).thenReturn(testUsers);
        
        // Mock all users have activity types
        List<ActivityTypeDTO> existingActivityTypes = List.of(
            new ActivityTypeDTO(UUID.randomUUID(), "Existing Type", List.of(), "🎯", 0, UUID.randomUUID(), false)
        );
        
        for (User user : testUsers) {
            when(activityTypeService.getActivityTypesByUserId(user.getId()))
                .thenReturn(existingActivityTypes);
        }

        // Act
        CommandLineRunner runner = activityTypeInitializer.initializeActivityTypes(
            userRepository, activityTypeService, logger
        );
        runner.run();

        // Assert
        verify(userRepository, times(1)).findAll();
        verify(activityTypeService, times(testUsers.size())).getActivityTypesByUserId(any(UUID.class));
        
        // Should NOT initialize for any user
        verify(activityTypeService, never()).initializeDefaultActivityTypesForUser(any(User.class));
        
        // Should log completion with 0 users initialized
        verify(logger, times(1)).info(contains("0 users initialized"));
        verify(logger, times(1)).info(contains(testUsers.size() + " users skipped"));
    }

    @Test
    void initializeActivityTypes_ShouldHandleDataIntegrityViolationGracefully() throws Exception {
        // Arrange
        when(userRepository.findAll()).thenReturn(List.of(testUsers.get(0)));
        when(activityTypeService.getActivityTypesByUserId(testUsers.get(0).getId()))
            .thenReturn(new ArrayList<>());
        
        // Mock first call throws DataIntegrityViolationException
        doThrow(new DataIntegrityViolationException("Duplicate entry"))
            .when(activityTypeService).initializeDefaultActivityTypesForUser(testUsers.get(0));
        
        // Mock second call to check if user now has activity types
        List<ActivityTypeDTO> recoveredActivityTypes = List.of(
            new ActivityTypeDTO(UUID.randomUUID(), "Recovered Type", List.of(), "🎯", 0, testUsers.get(0).getId(), false)
        );
        when(activityTypeService.getActivityTypesByUserId(testUsers.get(0).getId()))
            .thenReturn(new ArrayList<>()) // First call
            .thenReturn(recoveredActivityTypes); // Second call after exception

        // Act
        CommandLineRunner runner = activityTypeInitializer.initializeActivityTypes(
            userRepository, activityTypeService, logger
        );
        runner.run();

        // Assert
        verify(activityTypeService, times(1)).initializeDefaultActivityTypesForUser(testUsers.get(0));
        verify(activityTypeService, times(2)).getActivityTypesByUserId(testUsers.get(0).getId());
        
        // Should log warning about constraint violation
        verify(logger, times(1)).warn(contains("Constraint violation during initialization"));
        verify(logger, times(1)).info(contains("Initialization appears to have succeeded despite constraint error"));
    }

    @Test
    void initializeActivityTypes_ShouldHandleEmptyUserList() throws Exception {
        // Arrange
        when(userRepository.findAll()).thenReturn(new ArrayList<>());

        // Act
        CommandLineRunner runner = activityTypeInitializer.initializeActivityTypes(
            userRepository, activityTypeService, logger
        );
        runner.run();

        // Assert
        verify(userRepository, times(1)).findAll();
        verify(activityTypeService, never()).getActivityTypesByUserId(any(UUID.class));
        verify(activityTypeService, never()).initializeDefaultActivityTypesForUser(any(User.class));
        
        // Should log about 0 users found
        verify(logger, times(1)).info("Found 0 users in the database");
        verify(logger, times(1)).info(contains("0 users initialized"));
    }

    @Test
    void initializeActivityTypes_ShouldHandleUnexpectedExceptions() throws Exception {
        // Arrange
        when(userRepository.findAll()).thenReturn(List.of(testUsers.get(0)));
        when(activityTypeService.getActivityTypesByUserId(testUsers.get(0).getId()))
            .thenReturn(new ArrayList<>());
        
        // Mock unexpected exception
        doThrow(new RuntimeException("Unexpected error"))
            .when(activityTypeService).initializeDefaultActivityTypesForUser(testUsers.get(0));

        // Act
        CommandLineRunner runner = activityTypeInitializer.initializeActivityTypes(
            userRepository, activityTypeService, logger
        );
        runner.run();

        // Assert
        verify(activityTypeService, times(1)).initializeDefaultActivityTypesForUser(testUsers.get(0));
        
        // Should log error about unexpected exception
        verify(logger, times(1)).error(contains("Unexpected error during initialization"));
        verify(logger, times(1)).info(contains("1 users with errors"));
    }


} 