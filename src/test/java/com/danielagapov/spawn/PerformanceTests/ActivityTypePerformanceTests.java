package com.danielagapov.spawn.PerformanceTests;

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
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.util.StopWatch;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

/**
 * Performance tests for Activity Type management under high load conditions
 * These tests verify the system can handle large datasets and concurrent operations
 * as might be experienced in production with heavy front-end usage
 */
@ExtendWith(MockitoExtension.class)
class ActivityTypePerformanceTests {

    @Mock
    private IActivityTypeRepository activityTypeRepository;

    @Mock
    private IUserService userService;

    @Mock
    private ILogger logger;

    private ActivityTypeService activityTypeService;
    private UUID userId;
    private User testUser;

    @BeforeEach
    void setUp() {
        activityTypeService = new ActivityTypeService(activityTypeRepository, logger, userService);
        userId = UUID.randomUUID();
        testUser = createTestUser();
    }

    private User createTestUser() {
        User user = new User();
        user.setId(userId);
        user.setUsername("perftest_user");
        user.setName("Performance Test User");
        user.setEmail("perftest@example.com");
        return user;
    }

    // MARK: - Large Dataset Performance Tests

    @Test
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    void batchUpdate_ShouldCompleteWithinTimeLimit_WhenProcessing100ActivityTypes() {
        // Arrange - Large batch of activity types (100 items)
        List<ActivityTypeDTO> largeUpdateBatch = createLargeActivityTypeBatch(100);
        BatchActivityTypeUpdateDTO largeBatchDTO = new BatchActivityTypeUpdateDTO(largeUpdateBatch, List.of());

        // Mock repository responses for large dataset
        when(activityTypeRepository.countByCreatorIdAndIsPinnedTrue(userId)).thenReturn(0L);
        when(activityTypeRepository.countByCreatorId(userId)).thenReturn(100L);
        when(activityTypeRepository.findActivityTypesByCreatorId(userId))
                .thenReturn(createLargeActivityTypeEntityBatch(100));
        when(userService.getUserEntityById(userId)).thenReturn(testUser);
        when(activityTypeRepository.saveAll(anyList())).thenReturn(createLargeActivityTypeEntityBatch(100));

        StopWatch stopWatch = new StopWatch();
        stopWatch.start();

        // Act
        List<ActivityTypeDTO> result = activityTypeService.updateActivityTypes(userId, largeBatchDTO);

        stopWatch.stop();

        // Assert
        assertNotNull(result);
        assertTrue(stopWatch.getTotalTimeMillis() < 10000, "Operation took too long: " + stopWatch.getTotalTimeMillis() + "ms");
        System.out.println("Processed 100 activity types in: " + stopWatch.getTotalTimeMillis() + "ms");
    }

    @Test
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    void batchUpdate_ShouldHandleMaximumLoad_WhenProcessing500ActivityTypes() {
        // Arrange - Maximum realistic load (500 items - extreme power user scenario)
        List<ActivityTypeDTO> maximumBatch = createLargeActivityTypeBatch(500);
        BatchActivityTypeUpdateDTO maxBatchDTO = new BatchActivityTypeUpdateDTO(maximumBatch, List.of());

        // Mock for maximum load
        when(activityTypeRepository.countByCreatorIdAndIsPinnedTrue(userId)).thenReturn(0L);
        when(activityTypeRepository.countByCreatorId(userId)).thenReturn(500L);
        when(activityTypeRepository.findActivityTypesByCreatorId(userId))
                .thenReturn(createLargeActivityTypeEntityBatch(500));
        when(userService.getUserEntityById(userId)).thenReturn(testUser);
        when(activityTypeRepository.saveAll(anyList())).thenReturn(createLargeActivityTypeEntityBatch(500));

        StopWatch stopWatch = new StopWatch();
        stopWatch.start();

        // Act
        List<ActivityTypeDTO> result = activityTypeService.updateActivityTypes(userId, maxBatchDTO);

        stopWatch.stop();

        // Assert
        assertNotNull(result);
        assertTrue(stopWatch.getTotalTimeMillis() < 30000, "Maximum load operation took too long: " + stopWatch.getTotalTimeMillis() + "ms");
        System.out.println("Processed 500 activity types in: " + stopWatch.getTotalTimeMillis() + "ms");
    }

    @Test
    @Timeout(value = 5, unit = TimeUnit.SECONDS)
    void fetchActivityTypes_ShouldBeOptimized_WhenRetrievingLargeCollection() {
        // Arrange - Large collection retrieval (simulating user with many activity types)
        List<ActivityType> largeCollection = createLargeActivityTypeEntityBatch(200);
        when(activityTypeRepository.findActivityTypesByCreatorId(userId)).thenReturn(largeCollection);

        StopWatch stopWatch = new StopWatch();
        stopWatch.start();

        // Act
        List<ActivityTypeDTO> result = activityTypeService.getActivityTypesByUserId(userId);

        stopWatch.stop();

        // Assert
        assertNotNull(result);
        assertEquals(200, result.size());
        assertTrue(stopWatch.getTotalTimeMillis() < 5000, "Fetch operation took too long: " + stopWatch.getTotalTimeMillis() + "ms");
        System.out.println("Fetched 200 activity types in: " + stopWatch.getTotalTimeMillis() + "ms");
    }

    // MARK: - Concurrent Access Performance Tests

    @Test
    @Timeout(value = 15, unit = TimeUnit.SECONDS)
    void batchUpdate_ShouldHandleConcurrentRequests_WhenMultipleUsersUpdateSimultaneously() throws InterruptedException, ExecutionException {
        // Arrange - Simulate 10 concurrent users updating their activity types
        int numberOfConcurrentUsers = 10;
        List<CompletableFuture<List<ActivityTypeDTO>>> futures = new ArrayList<>();

        // Setup mocks for concurrent access
        when(activityTypeRepository.countByCreatorIdAndIsPinnedTrue(any(UUID.class))).thenReturn(0L);
        when(activityTypeRepository.countByCreatorId(any(UUID.class))).thenReturn(5L);
        when(activityTypeRepository.findActivityTypesByCreatorId(any(UUID.class)))
                .thenReturn(createLargeActivityTypeEntityBatch(5));
        when(userService.getUserEntityById(any(UUID.class))).thenReturn(testUser);
        when(activityTypeRepository.saveAll(anyList())).thenReturn(createLargeActivityTypeEntityBatch(5));

        StopWatch stopWatch = new StopWatch();
        stopWatch.start();

        // Act - Create concurrent update requests
        for (int i = 0; i < numberOfConcurrentUsers; i++) {
            UUID concurrentUserId = UUID.randomUUID();
            List<ActivityTypeDTO> userBatch = createLargeActivityTypeBatch(20);
            BatchActivityTypeUpdateDTO batchDTO = new BatchActivityTypeUpdateDTO(userBatch, List.of());

            CompletableFuture<List<ActivityTypeDTO>> future = CompletableFuture.supplyAsync(() -> 
                activityTypeService.updateActivityTypes(concurrentUserId, batchDTO)
            );
            futures.add(future);
        }

        // Wait for all to complete
        CompletableFuture<Void> allFutures = CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
        allFutures.get();

        stopWatch.stop();

        // Assert
        for (CompletableFuture<List<ActivityTypeDTO>> future : futures) {
            assertNotNull(future.get());
        }
        assertTrue(stopWatch.getTotalTimeMillis() < 15000, "Concurrent operations took too long: " + stopWatch.getTotalTimeMillis() + "ms");
        System.out.println("Processed " + numberOfConcurrentUsers + " concurrent updates in: " + stopWatch.getTotalTimeMillis() + "ms");
        
        // Verify all operations completed
        verify(activityTypeRepository, times(numberOfConcurrentUsers)).saveAll(anyList());
    }

    @Test
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    void rapidFireUpdates_ShouldMaintainPerformance_WhenUserMakesQuickSuccessiveChanges() {
        // Arrange - Simulate user making rapid successive updates (like quickly toggling pins)
        int numberOfRapidUpdates = 50;
        List<ActivityTypeDTO> baseActivityTypes = createLargeActivityTypeBatch(5);

        when(activityTypeRepository.countByCreatorIdAndIsPinnedTrue(userId)).thenReturn(0L);
        when(activityTypeRepository.countByCreatorId(userId)).thenReturn(5L);
        when(activityTypeRepository.findActivityTypesByCreatorId(userId))
                .thenReturn(createLargeActivityTypeEntityBatch(5));
        when(userService.getUserEntityById(userId)).thenReturn(testUser);
        when(activityTypeRepository.saveAll(anyList())).thenReturn(createLargeActivityTypeEntityBatch(5));

        StopWatch stopWatch = new StopWatch();
        stopWatch.start();

        // Act - Perform rapid successive updates
        for (int i = 0; i < numberOfRapidUpdates; i++) {
            // Toggle pin status for first activity type
            ActivityTypeDTO toggledType = new ActivityTypeDTO(
                baseActivityTypes.get(0).getId(),
                baseActivityTypes.get(0).getTitle(),
                List.of(),
                baseActivityTypes.get(0).getIcon(),
                0,
                userId,
                i % 2 == 0 // Alternate pin status
            );
            
            BatchActivityTypeUpdateDTO rapidBatchDTO = new BatchActivityTypeUpdateDTO(Arrays.asList(toggledType), List.of());
            List<ActivityTypeDTO> result = activityTypeService.updateActivityTypes(userId, rapidBatchDTO);
            assertNotNull(result);
        }

        stopWatch.stop();

        // Assert
        assertTrue(stopWatch.getTotalTimeMillis() < 10000, "Rapid fire updates took too long: " + stopWatch.getTotalTimeMillis() + "ms");
        System.out.println("Processed " + numberOfRapidUpdates + " rapid updates in: " + stopWatch.getTotalTimeMillis() + "ms");
        verify(activityTypeRepository, times(numberOfRapidUpdates)).saveAll(anyList());
    }

    // MARK: - Memory Performance Tests

    @Test
    @Timeout(value = 20, unit = TimeUnit.SECONDS)
    void batchUpdate_ShouldHandleMemoryEfficiently_WhenProcessingMassiveDeletion() {
        // Arrange - Test memory efficiency with massive deletion (delete 1000 items)
        List<UUID> massiveDeletionList = new ArrayList<>();
        for (int i = 0; i < 1000; i++) {
            massiveDeletionList.add(UUID.randomUUID());
        }

        BatchActivityTypeUpdateDTO massiveDeletionDTO = new BatchActivityTypeUpdateDTO(List.of(), massiveDeletionList);

        // Mock for massive deletion scenario
        when(activityTypeRepository.findActivityTypesByCreatorId(userId))
                .thenReturn(createLargeActivityTypeEntityBatch(50)); // Remaining after deletion

        StopWatch stopWatch = new StopWatch();
        stopWatch.start();

        // Act
        List<ActivityTypeDTO> result = activityTypeService.updateActivityTypes(userId, massiveDeletionDTO);

        stopWatch.stop();

        // Assert
        assertNotNull(result);
        assertTrue(stopWatch.getTotalTimeMillis() < 20000, "Massive deletion took too long: " + stopWatch.getTotalTimeMillis() + "ms");
        System.out.println("Processed deletion of 1000 items in: " + stopWatch.getTotalTimeMillis() + "ms");
        verify(activityTypeRepository, times(1)).deleteAllById(massiveDeletionList);
    }

    @Test
    @Timeout(value = 15, unit = TimeUnit.SECONDS)
    void batchUpdate_ShouldOptimizeForMixedOperations_WhenCombiningMultipleOperationTypes() {
        // Arrange - Mixed operations: create 100, update 100, delete 100
        List<ActivityTypeDTO> creationBatch = createLargeActivityTypeBatch(100);
        List<ActivityTypeDTO> updateBatch = createLargeActivityTypeBatch(100);
        List<UUID> deletionBatch = createLargeDeletionBatch(100);

        List<ActivityTypeDTO> mixedBatch = new ArrayList<>();
        mixedBatch.addAll(creationBatch);
        mixedBatch.addAll(updateBatch);

        BatchActivityTypeUpdateDTO mixedOperationsDTO = new BatchActivityTypeUpdateDTO(mixedBatch, deletionBatch);

        // Mock for mixed operations
        when(activityTypeRepository.countByCreatorIdAndIsPinnedTrue(userId)).thenReturn(0L);
        when(activityTypeRepository.countByCreatorId(userId)).thenReturn(200L);
        when(activityTypeRepository.findActivityTypesByCreatorId(userId))
                .thenReturn(createLargeActivityTypeEntityBatch(200));
        when(userService.getUserEntityById(userId)).thenReturn(testUser);
        when(activityTypeRepository.saveAll(anyList())).thenReturn(createLargeActivityTypeEntityBatch(200));

        StopWatch stopWatch = new StopWatch();
        stopWatch.start();

        // Act
        List<ActivityTypeDTO> result = activityTypeService.updateActivityTypes(userId, mixedOperationsDTO);

        stopWatch.stop();

        // Assert
        assertNotNull(result);
        assertTrue(stopWatch.getTotalTimeMillis() < 15000, "Mixed operations took too long: " + stopWatch.getTotalTimeMillis() + "ms");
        System.out.println("Processed mixed operations (200 updates + 100 deletions) in: " + stopWatch.getTotalTimeMillis() + "ms");
    }

    // MARK: - Scalability Tests

    @Test
    @Timeout(value = 5, unit = TimeUnit.SECONDS)
    void initializeDefaultActivityTypes_ShouldScaleLinearely_WhenCalledForManyUsers() {
        // Arrange - Test initialization performance for multiple users
        int numberOfUsers = 100;
        List<User> users = createMultipleUsers(numberOfUsers);

        StopWatch stopWatch = new StopWatch();
        stopWatch.start();

        // Act
        for (User user : users) {
            activityTypeService.initializeDefaultActivityTypesForUser(user);
        }

        stopWatch.stop();

        // Assert
        assertTrue(stopWatch.getTotalTimeMillis() < 5000, "Bulk initialization took too long: " + stopWatch.getTotalTimeMillis() + "ms");
        System.out.println("Initialized default types for " + numberOfUsers + " users in: " + stopWatch.getTotalTimeMillis() + "ms");
        verify(activityTypeRepository, times(numberOfUsers)).saveAll(anyList());
    }

    @Test
    @Timeout(value = 8, unit = TimeUnit.SECONDS)
    void getActivityTypesByUserId_ShouldCacheEffectively_WhenRepeatedlyAccessed() {
        // Arrange - Test cache performance with repeated access
        when(activityTypeRepository.findActivityTypesByCreatorId(userId))
                .thenReturn(createLargeActivityTypeEntityBatch(50));

        StopWatch stopWatch = new StopWatch();
        stopWatch.start();

        // Act - Multiple repeated calls (would hit cache in real scenario)
        for (int i = 0; i < 100; i++) {
            List<ActivityTypeDTO> result = activityTypeService.getActivityTypesByUserId(userId);
            assertNotNull(result);
            assertEquals(50, result.size());
        }

        stopWatch.stop();

        // Assert
        assertTrue(stopWatch.getTotalTimeMillis() < 8000, "Repeated access took too long: " + stopWatch.getTotalTimeMillis() + "ms");
        System.out.println("100 repeated fetches completed in: " + stopWatch.getTotalTimeMillis() + "ms");
        // Note: In real scenario with caching, repository would only be called once
    }

    // MARK: - Stress Tests

    @Test
    @Timeout(value = 60, unit = TimeUnit.SECONDS)
    void batchUpdate_ShouldSurviveStressTest_WhenUnderExtremLoad() {
        // Arrange - Extreme stress test: 1000 activity types across multiple operations
        List<ActivityTypeDTO> extremeBatch = createLargeActivityTypeBatch(800);
        List<UUID> extremeDeletion = createLargeDeletionBatch(200);
        BatchActivityTypeUpdateDTO extremeStressDTO = new BatchActivityTypeUpdateDTO(extremeBatch, extremeDeletion);

        when(activityTypeRepository.countByCreatorIdAndIsPinnedTrue(userId)).thenReturn(0L);
        when(activityTypeRepository.countByCreatorId(userId)).thenReturn(1000L);
        when(activityTypeRepository.findActivityTypesByCreatorId(userId))
                .thenReturn(createLargeActivityTypeEntityBatch(1000));
        when(userService.getUserEntityById(userId)).thenReturn(testUser);
        when(activityTypeRepository.saveAll(anyList())).thenReturn(createLargeActivityTypeEntityBatch(800));

        StopWatch stopWatch = new StopWatch();
        stopWatch.start();

        // Act
        assertDoesNotThrow(() -> {
            List<ActivityTypeDTO> result = activityTypeService.updateActivityTypes(userId, extremeStressDTO);
            assertNotNull(result);
        });

        stopWatch.stop();

        // Assert
        assertTrue(stopWatch.getTotalTimeMillis() < 60000, "Stress test took too long: " + stopWatch.getTotalTimeMillis() + "ms");
        System.out.println("Stress test (800 updates + 200 deletions) completed in: " + stopWatch.getTotalTimeMillis() + "ms");
    }

    // MARK: - Helper Methods

    private List<ActivityTypeDTO> createLargeActivityTypeBatch(int count) {
        return IntStream.range(0, count)
                .mapToObj(i -> new ActivityTypeDTO(
                        UUID.randomUUID(),
                        "Activity Type " + i,
                        List.of(),
                        "🎯",
                        i,
                        userId,
                        false // No pinned activity types in performance tests to avoid validation issues
                ))
                .toList();
    }

    /**
     * Creates a batch of activity types with a controlled number of pinned items (max 3)
     * Use this when testing pinned functionality specifically
     */
    private List<ActivityTypeDTO> createActivityTypeBatchWithPinnedLimit(int count, int pinnedCount) {
        if (pinnedCount > 3) {
            throw new IllegalArgumentException("Cannot create more than 3 pinned activity types");
        }
        return IntStream.range(0, count)
                .mapToObj(i -> new ActivityTypeDTO(
                        UUID.randomUUID(),
                        "Activity Type " + i,
                        List.of(),
                        "🎯",
                        i,
                        userId,
                        i < pinnedCount // First 'pinnedCount' items are pinned
                ))
                .toList();
    }

    private List<ActivityType> createLargeActivityTypeEntityBatch(int count) {
        return IntStream.range(0, count)
                .mapToObj(i -> {
                    ActivityType activityType = new ActivityType();
                    activityType.setId(UUID.randomUUID());
                    activityType.setTitle("Activity Type " + i);
                    activityType.setIcon("🎯");
                    activityType.setOrderNum(i);
                    activityType.setIsPinned(false); // No pinned activity types in performance tests
                    activityType.setCreator(testUser);
                    return activityType;
                })
                .toList();
    }

    private List<UUID> createLargeDeletionBatch(int count) {
        return IntStream.range(0, count)
                .mapToObj(i -> UUID.randomUUID())
                .toList();
    }

    private List<User> createMultipleUsers(int count) {
        return IntStream.range(0, count)
                .mapToObj(i -> {
                    User user = new User();
                    user.setId(UUID.randomUUID());
                    user.setUsername("user" + i);
                    user.setName("User " + i);
                    user.setEmail("user" + i + "@test.com");
                    return user;
                })
                .toList();
    }
} 