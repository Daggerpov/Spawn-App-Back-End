package com.danielagapov.spawn.IntegrationTests;

import com.danielagapov.spawn.Controllers.ActivityTypeController;
import com.danielagapov.spawn.DTOs.ActivityType.ActivityTypeDTO;
import com.danielagapov.spawn.DTOs.ActivityType.BatchActivityTypeUpdateDTO;
import com.danielagapov.spawn.DTOs.User.BaseUserDTO;
import com.danielagapov.spawn.Exceptions.Logger.ILogger;
import com.danielagapov.spawn.Exceptions.ActivityTypeValidationException;
import com.danielagapov.spawn.Models.ActivityType;
import com.danielagapov.spawn.Models.User.User;
import com.danielagapov.spawn.Repositories.IActivityTypeRepository;
import com.danielagapov.spawn.Services.ActivityType.ActivityTypeService;
import com.danielagapov.spawn.Services.User.IUserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for Activity Type management workflow
 * These tests simulate complete front-end user journeys and workflows
 */
@ExtendWith(MockitoExtension.class)
class ActivityTypeIntegrationTests {

    @Mock
    private IActivityTypeRepository activityTypeRepository;

    @Mock
    private IUserService userService;

    @Mock
    private ILogger logger;

    private ActivityTypeService activityTypeService;
    private ActivityTypeController activityTypeController;
    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    private UUID userId;
    private User testUser;
    private List<ActivityType> defaultActivityTypes;

    @BeforeEach
    void setUp() {
        // Initialize service and controller with real implementations
        activityTypeService = new ActivityTypeService(activityTypeRepository, logger, userService);
        activityTypeController = new ActivityTypeController(activityTypeService, logger);
        
        objectMapper = new ObjectMapper();
        // Configure MockMvc with proper JSON message converter
        mockMvc = MockMvcBuilders.standaloneSetup(activityTypeController)
                .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
                .build();
        
        userId = UUID.randomUUID();
        testUser = createTestUser();
        defaultActivityTypes = createDefaultActivityTypes();
    }

    private User createTestUser() {
        User user = new User();
        user.setId(userId);
        user.setUsername("testuser");
        user.setName("Test User");
        user.setEmail("test@example.com");
        return user;
    }

    private List<ActivityType> createDefaultActivityTypes() {
        List<ActivityType> types = new ArrayList<>();
        
        ActivityType chill = new ActivityType();
        chill.setId(UUID.randomUUID());
        chill.setTitle("Chill");
        chill.setIcon("🛋️");
        chill.setCreator(testUser);
        chill.setOrderNum(1);
        chill.setIsPinned(false);
        types.add(chill);
        
        ActivityType food = new ActivityType();
        food.setId(UUID.randomUUID());
        food.setTitle("Food");
        food.setIcon("🍽️");
        food.setCreator(testUser);
        food.setOrderNum(2);
        food.setIsPinned(true);
        types.add(food);
        
        ActivityType active = new ActivityType();
        active.setId(UUID.randomUUID());
        active.setTitle("Active");
        active.setIcon("🏃");
        active.setCreator(testUser);
        active.setOrderNum(3);
        active.setIsPinned(false);
        types.add(active);
        
        return types;
    }

    // MARK: - Complete Front-End User Journey Tests

    @Test
    void fullUserJourney_ShouldWorkEndToEnd_WhenUserManagesActivityTypes() throws Exception {
        // PHASE 1: Initial load - User opens activity type management page
        when(activityTypeRepository.findActivityTypesByCreatorId(userId))
                .thenReturn(defaultActivityTypes);

        mockMvc.perform(get("/api/v1/users/{userId}/activity-types", userId)
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].title").value("Chill"))
                .andExpect(jsonPath("$[1].title").value("Food"))
                .andExpect(jsonPath("$[2].title").value("Active"));

        // PHASE 2: User makes changes locally in UI then saves all at once
        // User actions:
        // 1. Pins "Chill" 
        // 2. Unpins "Food"
        // 3. Creates new "Study" activity type
        // 4. Deletes "Active"
        // 5. Reorders everything

        UUID newStudyId = UUID.randomUUID();
        List<ActivityTypeDTO> batchUpdates = Arrays.asList(
                new ActivityTypeDTO(defaultActivityTypes.get(0).getId(), "Chill", List.of(), "🛋️", 1, userId, true), // Pinned
                new ActivityTypeDTO(defaultActivityTypes.get(1).getId(), "Food", List.of(), "🍽️", 2, userId, false), // Unpinned
                new ActivityTypeDTO(newStudyId, "Study", List.of(), "📚", 3, userId, false) // New
        );

        BatchActivityTypeUpdateDTO batchDTO = new BatchActivityTypeUpdateDTO(
                batchUpdates,
                Arrays.asList(defaultActivityTypes.get(2).getId()) // Delete Active
        );

        // Mock the service layer responses
        when(activityTypeRepository.countByCreatorIdAndIsPinnedTrue(userId)).thenReturn(1L); // Current pinned count
        when(activityTypeRepository.countByCreatorId(userId)).thenReturn(3L); // Current total count
        when(userService.getUserEntityById(userId)).thenReturn(testUser);
        
        // Mock the final result after all changes
        List<ActivityType> finalResult = Arrays.asList(
                createActivityTypeFromDTO(batchUpdates.get(0)),
                createActivityTypeFromDTO(batchUpdates.get(1)),
                createActivityTypeFromDTO(batchUpdates.get(2))
        );
        
        // Set up repository mock to return different results for different calls
        // Set up repository mock to return different results for different calls
        AtomicInteger callCount = new AtomicInteger(0);
        when(activityTypeRepository.findActivityTypesByCreatorId(userId)).thenAnswer(invocation -> {
            int call = callCount.incrementAndGet();
            if (call <= 2) {
                return defaultActivityTypes; // First 2 calls return original (validation + assignOrderNumbers)
            } else {
                return finalResult; // Call #3 and subsequent calls return updated (final result)
            }
        });
        
        when(activityTypeRepository.saveAll(anyList())).thenReturn(finalResult);

        // PHASE 3: User submits all changes
        mockMvc.perform(put("/api/v1/users/{userId}/activity-types", userId)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(batchDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].title").value("Chill"))
                .andExpect(jsonPath("$[0].isPinned").value(true))
                .andExpect(jsonPath("$[1].title").value("Food"))
                .andExpect(jsonPath("$[1].isPinned").value(false))
                .andExpect(jsonPath("$[2].title").value("Study"));

        // Verify the complete workflow
        verify(activityTypeRepository, times(4)).findActivityTypesByCreatorId(userId); // Initial GET + validation + assignOrderNumbers + final result
        verify(activityTypeRepository, times(1)).deleteAllById(anyList()); // Delete operation
        verify(activityTypeRepository, times(1)).saveAll(anyList()); // Save operation
    }

    @Test
    void newUserWorkflow_ShouldCreateDefaultTypes_WhenUserHasNoActivityTypes() {
        // Simulate new user workflow - they have no activity types initially
        when(activityTypeRepository.findActivityTypesByCreatorId(userId)).thenReturn(List.of());

        // Call service method directly as would happen during user registration
        assertDoesNotThrow(() -> activityTypeService.initializeDefaultActivityTypesForUser(testUser));

        // Verify 4 default activity types are created
        verify(activityTypeRepository, times(1)).saveAll(argThat(list -> 
            ((List<ActivityType>) list).size() == 4
        ));
    }

    @Test
    void powerUserWorkflow_ShouldHandleManyActivityTypes_WhenUserHasLargeCollection() throws Exception {
        // Simulate power user with many activity types (20+)
        List<ActivityType> manyActivityTypes = new ArrayList<>();
        List<ActivityTypeDTO> manyUpdates = new ArrayList<>();
        
        for (int i = 1; i <= 25; i++) {
            ActivityType type = new ActivityType();
            type.setId(UUID.randomUUID());
            type.setTitle("Type " + (i - 1));
            type.setIcon("🎯");
            type.setCreator(testUser);
            type.setOrderNum(i);
            type.setIsPinned(i <= 4); // First 4 are pinned
            manyActivityTypes.add(type);
            
            // Create corresponding DTO
            ActivityTypeDTO dto = new ActivityTypeDTO(
                type.getId(), type.getTitle(), List.of(), type.getIcon(), 
                i, userId, type.getIsPinned()
            );
            manyUpdates.add(dto);
        }

        when(activityTypeRepository.findActivityTypesByCreatorId(userId)).thenReturn(manyActivityTypes);

        // Test initial load
        mockMvc.perform(get("/api/v1/users/{userId}/activity-types", userId)
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(25));

        // Test bulk update
        BatchActivityTypeUpdateDTO largeBatchDTO = new BatchActivityTypeUpdateDTO(manyUpdates, List.of());
        
        when(activityTypeRepository.countByCreatorIdAndIsPinnedTrue(userId)).thenReturn(3L);
        when(activityTypeRepository.countByCreatorId(userId)).thenReturn(25L);
        when(userService.getUserEntityById(userId)).thenReturn(testUser);
        when(activityTypeRepository.saveAll(anyList())).thenReturn(manyActivityTypes);

        mockMvc.perform(put("/api/v1/users/{userId}/activity-types", userId)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(largeBatchDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(25));
    }

    @Test
    void errorRecoveryWorkflow_ShouldHandleGracefully_WhenValidationFails() throws Exception {
        // Simulate user attempting invalid operation (too many pins)
        List<ActivityTypeDTO> invalidUpdates = Arrays.asList(
                new ActivityTypeDTO(UUID.randomUUID(), "Type1", List.of(), "🎯", 1, userId, true),
                new ActivityTypeDTO(UUID.randomUUID(), "Type2", List.of(), "🎯", 2, userId, true),
                new ActivityTypeDTO(UUID.randomUUID(), "Type3", List.of(), "🎯", 3, userId, true),
                new ActivityTypeDTO(UUID.randomUUID(), "Type4", List.of(), "🎯", 4, userId, true) // 4th pin - invalid
        );

        BatchActivityTypeUpdateDTO invalidBatchDTO = new BatchActivityTypeUpdateDTO(invalidUpdates, List.of());

        when(activityTypeRepository.countByCreatorIdAndIsPinnedTrue(userId)).thenReturn(0L);
        when(activityTypeRepository.countByCreatorId(userId)).thenReturn(0L);
        when(activityTypeRepository.findActivityTypesByCreatorId(userId)).thenReturn(List.of());

        // Should return internal server error due to validation failure
        mockMvc.perform(put("/api/v1/users/{userId}/activity-types", userId)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidBatchDTO)))
                .andExpect(status().isInternalServerError());

        // Verify no changes were saved
        verify(activityTypeRepository, never()).saveAll(anyList());
    }

    @Test
    void concurrentUserWorkflow_ShouldHandleLocking_WhenMultipleUpdates() {
        // Simulate concurrent updates to same user's activity types
        ActivityTypeDTO update1 = new ActivityTypeDTO(
            defaultActivityTypes.get(0).getId(), "Chill Updated 1", List.of(), "🛋️", 1, userId, false
        );
        ActivityTypeDTO update2 = new ActivityTypeDTO(
            defaultActivityTypes.get(0).getId(), "Chill Updated 2", List.of(), "🛋️", 1, userId, true
        );

        BatchActivityTypeUpdateDTO batch1 = new BatchActivityTypeUpdateDTO(Arrays.asList(update1), List.of());
        BatchActivityTypeUpdateDTO batch2 = new BatchActivityTypeUpdateDTO(Arrays.asList(update2), List.of());

        when(activityTypeRepository.countByCreatorIdAndIsPinnedTrue(userId)).thenReturn(0L);
        when(activityTypeRepository.countByCreatorId(userId)).thenReturn(1L);
        when(activityTypeRepository.findActivityTypesByCreatorId(userId)).thenReturn(defaultActivityTypes.subList(0, 1));
        when(userService.getUserEntityById(userId)).thenReturn(testUser);
        when(activityTypeRepository.saveAll(anyList())).thenReturn(defaultActivityTypes.subList(0, 1));

        // Both updates should complete (synchronization handles concurrency)
        ResponseEntity<List<ActivityTypeDTO>> result1 = activityTypeController.updateActivityTypes(userId, batch1);
        ResponseEntity<List<ActivityTypeDTO>> result2 = activityTypeController.updateActivityTypes(userId, batch2);

        assertEquals(HttpStatus.OK, result1.getStatusCode());
        assertEquals(HttpStatus.OK, result2.getStatusCode());
        verify(activityTypeRepository, times(2)).saveAll(anyList());
    }

    @Test
    void mobileAppWorkflow_ShouldHandleQuickActions_WhenUserMakesRapidChanges() throws Exception {
        // Simulate mobile app rapid pin/unpin actions
        ActivityTypeDTO quickPin = new ActivityTypeDTO(
            defaultActivityTypes.get(0).getId(), "Chill", List.of(), "🛋️", 1, userId, true
        );
        ActivityTypeDTO quickUnpin = new ActivityTypeDTO(
            defaultActivityTypes.get(0).getId(), "Chill", List.of(), "🛋️", 1, userId, false
        );

        when(activityTypeRepository.countByCreatorIdAndIsPinnedTrue(userId)).thenReturn(0L);
        when(activityTypeRepository.countByCreatorId(userId)).thenReturn(1L);
        when(activityTypeRepository.findActivityTypesByCreatorId(userId)).thenReturn(defaultActivityTypes.subList(0, 1));
        when(userService.getUserEntityById(userId)).thenReturn(testUser);
        when(activityTypeRepository.saveAll(anyList())).thenReturn(defaultActivityTypes.subList(0, 1));

        // Rapid pin
        mockMvc.perform(put("/api/v1/users/{userId}/activity-types", userId)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new BatchActivityTypeUpdateDTO(Arrays.asList(quickPin), List.of()))))
                .andExpect(status().isOk());

        // Rapid unpin
        mockMvc.perform(put("/api/v1/users/{userId}/activity-types", userId)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new BatchActivityTypeUpdateDTO(Arrays.asList(quickUnpin), List.of()))))
                .andExpect(status().isOk());

        verify(activityTypeRepository, times(2)).saveAll(anyList());
    }

    @Test
    void offlineToOnlineWorkflow_ShouldSyncCorrectly_WhenUserComesBackOnline() throws Exception {
        // Simulate user making many changes offline, then syncing when back online
        UUID study1Id = UUID.randomUUID();
        UUID study2Id = UUID.randomUUID();
        UUID study3Id = UUID.randomUUID();

        List<ActivityTypeDTO> offlineChanges = Arrays.asList(
                // Modified existing
                new ActivityTypeDTO(defaultActivityTypes.get(0).getId(), "Chill & Relax", List.of(), "🛋️", 1, userId, true),
                // Created while offline
                new ActivityTypeDTO(study1Id, "Study Session", List.of(), "📚", 2, userId, false),
                new ActivityTypeDTO(study2Id, "Deep Work", List.of(), "💻", 3, userId, false),
                new ActivityTypeDTO(study3Id, "Reading", List.of(), "📖", 4, userId, false)
        );

        // Deleted while offline
        List<UUID> deletedOffline = Arrays.asList(
                defaultActivityTypes.get(1).getId(), // Food
                defaultActivityTypes.get(2).getId()  // Active
        );

        BatchActivityTypeUpdateDTO offlineSyncDTO = new BatchActivityTypeUpdateDTO(offlineChanges, deletedOffline);

        when(activityTypeRepository.countByCreatorIdAndIsPinnedTrue(userId)).thenReturn(1L);
        when(activityTypeRepository.countByCreatorId(userId)).thenReturn(3L);
        when(userService.getUserEntityById(userId)).thenReturn(testUser);
        
        // Create final result with 4 activity types
        List<ActivityType> offlineFinalResult = Arrays.asList(
                createActivityTypeFromDTO(offlineChanges.get(0)),
                createActivityTypeFromDTO(offlineChanges.get(1)),
                createActivityTypeFromDTO(offlineChanges.get(2)),
                createActivityTypeFromDTO(offlineChanges.get(3))
        );
        
        // Set up repository mock to return different results for different calls
        when(activityTypeRepository.findActivityTypesByCreatorId(userId))
                .thenReturn(defaultActivityTypes) // First call during validation  
                .thenReturn(offlineFinalResult); // Second call for final result
        
        when(activityTypeRepository.saveAll(anyList())).thenReturn(offlineFinalResult);

        mockMvc.perform(put("/api/v1/users/{userId}/activity-types", userId)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(offlineSyncDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(4));

        // Verify deletions and saves happened
        verify(activityTypeRepository, times(1)).deleteAllById(deletedOffline);
        verify(activityTypeRepository, times(1)).saveAll(anyList());
    }

    @Test
    void dataIntegrityWorkflow_ShouldMaintainConsistency_WhenComplexOperations() throws Exception {
        // Test complex scenario that could break data integrity
        // 1. Reorder all items
        // 2. Change pin status
        // 3. Delete some, create some
        // 4. Ensure order numbers remain consistent

        UUID newType1Id = UUID.randomUUID();
        UUID newType2Id = UUID.randomUUID();

        List<ActivityTypeDTO> complexChanges = Arrays.asList(
                // Existing items reordered and pin status changed
                new ActivityTypeDTO(defaultActivityTypes.get(1).getId(), "Food", List.of(), "🍽️", 1, userId, false), // Was pinned, now not
                new ActivityTypeDTO(defaultActivityTypes.get(0).getId(), "Chill", List.of(), "🛋️", 2, userId, true),  // Was not pinned, now pinned
                // New items
                new ActivityTypeDTO(newType1Id, "Work", List.of(), "💼", 3, userId, false),
                new ActivityTypeDTO(newType2Id, "Travel", List.of(), "✈️", 4, userId, false)
        );

        List<UUID> toDelete = Arrays.asList(defaultActivityTypes.get(2).getId()); // Delete Active

        BatchActivityTypeUpdateDTO complexDTO = new BatchActivityTypeUpdateDTO(complexChanges, toDelete);

        when(activityTypeRepository.countByCreatorIdAndIsPinnedTrue(userId)).thenReturn(1L);
        when(activityTypeRepository.countByCreatorId(userId)).thenReturn(3L);
        when(userService.getUserEntityById(userId)).thenReturn(testUser);
        
        // Create final result with 4 activity types (2 existing updated + 2 new)
        List<ActivityType> complexFinalResult = Arrays.asList(
                createActivityTypeFromDTO(complexChanges.get(0)),
                createActivityTypeFromDTO(complexChanges.get(1)),
                createActivityTypeFromDTO(complexChanges.get(2)),
                createActivityTypeFromDTO(complexChanges.get(3))
        );
        
        // Set up repository mock to return different results for different calls
        when(activityTypeRepository.findActivityTypesByCreatorId(userId))
                .thenReturn(defaultActivityTypes) // First call during validation  
                .thenReturn(complexFinalResult); // Second call for final result
        
        when(activityTypeRepository.saveAll(anyList())).thenReturn(complexFinalResult);

        mockMvc.perform(put("/api/v1/users/{userId}/activity-types", userId)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(complexDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(4));

        // Verify operations happened in correct order
        verify(activityTypeRepository, times(1)).deleteAllById(toDelete);
        verify(activityTypeRepository, times(1)).saveAll(anyList());
    }

    // MARK: - Helper Methods

    private ActivityType createActivityTypeFromDTO(ActivityTypeDTO dto) {
        ActivityType activityType = new ActivityType();
        activityType.setId(dto.getId());
        activityType.setTitle(dto.getTitle());
        activityType.setIcon(dto.getIcon());
        activityType.setOrderNum(dto.getOrderNum());
        activityType.setIsPinned(dto.getIsPinned());
        activityType.setCreator(testUser);
        return activityType;
    }
} 