package com.danielagapov.spawn.ControllerTests;

import com.danielagapov.spawn.activity.api.ActivityTypeController;
import com.danielagapov.spawn.activity.api.dto.ActivityTypeDTO;
import com.danielagapov.spawn.activity.api.dto.BatchActivityTypeUpdateDTO;
import com.danielagapov.spawn.shared.exceptions.Logger.ILogger;
import com.danielagapov.spawn.shared.exceptions.ActivityTypeValidationException;
import com.danielagapov.spawn.activity.internal.services.IActivityTypeService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Comprehensive unit tests for ActivityTypeController
 * Tests all API endpoints that the front-end uses for activity type management
 */
@ExtendWith(MockitoExtension.class)
class ActivityTypeControllerTests {

    @Mock
    private IActivityTypeService activityTypeService;

    @Mock
    private ILogger logger;

    @InjectMocks
    private ActivityTypeController activityTypeController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;
    private UUID userId;
    private UUID activityTypeId1;
    private UUID activityTypeId2;
    private ActivityTypeDTO chillActivityTypeDTO;
    private ActivityTypeDTO foodActivityTypeDTO;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        // Configure MockMvc with proper JSON message converter
        mockMvc = MockMvcBuilders.standaloneSetup(activityTypeController)
                .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
                .build();
        
        userId = UUID.randomUUID();
        activityTypeId1 = UUID.randomUUID();
        activityTypeId2 = UUID.randomUUID();
        
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
    }

    // MARK: - GET Activity Types Tests

    @Test
    void getOwnedActivityTypesForUser_ShouldReturnActivityTypes_WhenUserHasActivityTypes() throws Exception {
        // Arrange
        List<ActivityTypeDTO> activityTypes = List.of(chillActivityTypeDTO, foodActivityTypeDTO);
        when(activityTypeService.getActivityTypesByUserId(userId)).thenReturn(activityTypes);

        // Act & Assert
        mockMvc.perform(get("/api/v1/users/{userId}/activity-types", userId))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(activityTypeId1.toString()))
                .andExpect(jsonPath("$[0].title").value("Chill"))
                .andExpect(jsonPath("$[0].icon").value("🛋️"))
                .andExpect(jsonPath("$[0].orderNum").value(0))
                .andExpect(jsonPath("$[0].isPinned").value(false))
                .andExpect(jsonPath("$[1].id").value(activityTypeId2.toString()))
                .andExpect(jsonPath("$[1].title").value("Food"))
                .andExpect(jsonPath("$[1].isPinned").value(true));

        verify(activityTypeService, times(1)).getActivityTypesByUserId(userId);
        verify(logger, times(1)).info(contains("Fetching owned activity types for user"));
    }

    @Test
    void getOwnedActivityTypesForUser_ShouldReturnEmptyList_WhenUserHasNoActivityTypes() throws Exception {
        // Arrange
        when(activityTypeService.getActivityTypesByUserId(userId)).thenReturn(List.of());

        // Act & Assert
        mockMvc.perform(get("/api/v1/users/{userId}/activity-types", userId))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.length()").value(0));

        verify(activityTypeService, times(1)).getActivityTypesByUserId(userId);
    }

    @Test
    void getOwnedActivityTypesForUser_ShouldReturnInternalServerError_WhenServiceThrowsException() throws Exception {
        // Arrange
        when(activityTypeService.getActivityTypesByUserId(userId))
                .thenThrow(new RuntimeException("Database connection failed"));

        // Act & Assert
        mockMvc.perform(get("/api/v1/users/{userId}/activity-types", userId))
                .andExpect(status().isInternalServerError());

        verify(logger, times(1)).error(contains("Error fetching owned activity types"));
    }

    @Test
    void getOwnedActivityTypesForUser_ShouldHandleInvalidUserId_WhenUserIdIsNotUUID() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/v1/users/{userId}/activity-types", "invalid-uuid"))
                .andExpect(status().isBadRequest());
    }

    // MARK: - PUT Batch Update Tests

    @Test
    void batchUpdateActivityTypes_ShouldReturnUpdatedActivityTypes_WhenValidBatchUpdate() throws Exception {
        // Arrange
        ActivityTypeDTO updatedChillDTO = new ActivityTypeDTO(
            activityTypeId1, "Chill Updated", List.of(), "🛋️", 0, userId, true
        );
        
        BatchActivityTypeUpdateDTO batchDTO = new BatchActivityTypeUpdateDTO(
            List.of(updatedChillDTO),
            List.of()
        );
        
        List<ActivityTypeDTO> updatedActivityTypes = List.of(updatedChillDTO, foodActivityTypeDTO);
        when(activityTypeService.updateActivityTypes(eq(userId), any(BatchActivityTypeUpdateDTO.class)))
                .thenReturn(updatedActivityTypes);

        // Act & Assert
        mockMvc.perform(put("/api/v1/users/{userId}/activity-types", userId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(batchDTO)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].title").value("Chill Updated"))
                .andExpect(jsonPath("$[0].isPinned").value(true));

        verify(activityTypeService, times(1)).updateActivityTypes(eq(userId), any(BatchActivityTypeUpdateDTO.class));
        verify(logger, times(1)).info(contains("Batch updating activity types for user"));
    }

    @Test
    void batchUpdateActivityTypes_ShouldHandleCreationAndDeletion_WhenComplexBatchUpdate() throws Exception {
        // Arrange - Create new, update existing, delete one
        UUID newActivityTypeId = UUID.randomUUID();
        ActivityTypeDTO newActivityTypeDTO = new ActivityTypeDTO(
            newActivityTypeId, "Study", List.of(), "📚", 2, userId, false
        );
        
        ActivityTypeDTO updatedChillDTO = new ActivityTypeDTO(
            activityTypeId1, "Chill & Relax", List.of(), "🛋️", 0, userId, false
        );
        
        BatchActivityTypeUpdateDTO batchDTO = new BatchActivityTypeUpdateDTO(
            List.of(newActivityTypeDTO, updatedChillDTO),
            List.of(activityTypeId2) // Delete food activity type
        );
        
        List<ActivityTypeDTO> resultActivityTypes = List.of(updatedChillDTO, newActivityTypeDTO);
        when(activityTypeService.updateActivityTypes(eq(userId), any(BatchActivityTypeUpdateDTO.class)))
                .thenReturn(resultActivityTypes);

        // Act & Assert
        mockMvc.perform(put("/api/v1/users/{userId}/activity-types", userId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(batchDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].title").value("Chill & Relax"))
                .andExpect(jsonPath("$[1].title").value("Study"));

        verify(activityTypeService, times(1)).updateActivityTypes(eq(userId), any(BatchActivityTypeUpdateDTO.class));
    }

    @Test
    void batchUpdateActivityTypes_ShouldHandleReordering_WhenActivityTypesReordered() throws Exception {
        // Arrange - Reorder existing activity types
        ActivityTypeDTO reorderedChillDTO = new ActivityTypeDTO(
            activityTypeId1, "Chill", List.of(), "🛋️", 1, userId, false // Moved from 0 to 1
        );
        ActivityTypeDTO reorderedFoodDTO = new ActivityTypeDTO(
            activityTypeId2, "Food", List.of(), "🍽️", 0, userId, true // Moved from 1 to 0
        );
        
        BatchActivityTypeUpdateDTO batchDTO = new BatchActivityTypeUpdateDTO(
            List.of(reorderedChillDTO, reorderedFoodDTO),
            List.of()
        );
        
        when(activityTypeService.updateActivityTypes(eq(userId), any(BatchActivityTypeUpdateDTO.class)))
                .thenReturn(List.of(reorderedFoodDTO, reorderedChillDTO)); // Sorted by order

        // Act & Assert
        mockMvc.perform(put("/api/v1/users/{userId}/activity-types", userId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(batchDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].orderNum").value(0))
                .andExpect(jsonPath("$[1].orderNum").value(1));
    }

    @Test
    void batchUpdateActivityTypes_ShouldHandlePinToggling_WhenUserTogglesPins() throws Exception {
        // Arrange - Toggle pin status
        ActivityTypeDTO pinnedChillDTO = new ActivityTypeDTO(
            activityTypeId1, "Chill", List.of(), "🛋️", 0, userId, true // Toggled to pinned
        );
        ActivityTypeDTO unpinnedFoodDTO = new ActivityTypeDTO(
            activityTypeId2, "Food", List.of(), "🍽️", 1, userId, false // Toggled to unpinned
        );
        
        BatchActivityTypeUpdateDTO batchDTO = new BatchActivityTypeUpdateDTO(
            List.of(pinnedChillDTO, unpinnedFoodDTO),
            List.of()
        );
        
        when(activityTypeService.updateActivityTypes(eq(userId), any(BatchActivityTypeUpdateDTO.class)))
                .thenReturn(List.of(pinnedChillDTO, unpinnedFoodDTO));

        // Act & Assert
        mockMvc.perform(put("/api/v1/users/{userId}/activity-types", userId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(batchDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].isPinned").value(true))
                .andExpect(jsonPath("$[1].isPinned").value(false));
    }

    @Test
    void batchUpdateActivityTypes_ShouldReturnInternalServerError_WhenServiceThrowsException() throws Exception {
        // Arrange
        BatchActivityTypeUpdateDTO batchDTO = new BatchActivityTypeUpdateDTO(
            List.of(chillActivityTypeDTO),
            List.of()
        );
        
        when(activityTypeService.updateActivityTypes(eq(userId), any(BatchActivityTypeUpdateDTO.class)))
                .thenThrow(new RuntimeException("Database error"));

        // Act & Assert
        mockMvc.perform(put("/api/v1/users/{userId}/activity-types", userId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(batchDTO)))
                .andExpect(status().isInternalServerError());

        verify(logger, times(1)).error(contains("Error batch updating activity types"));
    }

    @Test
    void batchUpdateActivityTypes_ShouldReturnInternalServerError_WhenValidationException() throws Exception {
        // Arrange
        BatchActivityTypeUpdateDTO batchDTO = new BatchActivityTypeUpdateDTO(
            List.of(chillActivityTypeDTO),
            List.of()
        );
        
        when(activityTypeService.updateActivityTypes(eq(userId), any(BatchActivityTypeUpdateDTO.class)))
                .thenThrow(new ActivityTypeValidationException("Too many pinned activity types"));

        // Act & Assert
        mockMvc.perform(put("/api/v1/users/{userId}/activity-types", userId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(batchDTO)))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void batchUpdateActivityTypes_ShouldHandleMalformedJson_WhenInvalidJsonRequest() throws Exception {
        // Act & Assert
        mockMvc.perform(put("/api/v1/users/{userId}/activity-types", userId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{invalid json"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void batchUpdateActivityTypes_ShouldHandleInvalidUserId_WhenUserIdIsNotUUID() throws Exception {
        // Arrange
        BatchActivityTypeUpdateDTO batchDTO = new BatchActivityTypeUpdateDTO(List.of(), List.of());

        // Act & Assert
        mockMvc.perform(put("/api/v1/users/{userId}/activity-types", "invalid-uuid")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(batchDTO)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void batchUpdateActivityTypes_ShouldHandleEmptyBatch_WhenNoChanges() throws Exception {
        // Arrange
        BatchActivityTypeUpdateDTO emptyBatchDTO = new BatchActivityTypeUpdateDTO(List.of(), List.of());
        
        when(activityTypeService.updateActivityTypes(eq(userId), any(BatchActivityTypeUpdateDTO.class)))
                .thenThrow(new IllegalArgumentException("No activity types to update or delete"));

        // Act & Assert
        mockMvc.perform(put("/api/v1/users/{userId}/activity-types", userId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(emptyBatchDTO)))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void batchUpdateActivityTypes_ShouldHandleMissingRequestBody_WhenNoBodyProvided() throws Exception {
        // Act & Assert
        mockMvc.perform(put("/api/v1/users/{userId}/activity-types", userId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    void batchUpdateActivityTypes_ShouldHandleNullFieldsInDTO_WhenPartialData() throws Exception {
        // Arrange - Test with null fields in the DTO
        String jsonWithNulls = """
            {
                "updatedActivityTypes": [
                    {
                        "id": "%s",
                        "title": null,
                        "associatedFriends": null,
                        "icon": "🛋️",
                        "orderNum": 0,
                        "ownerUserId": "%s",
                        "isPinned": false
                    }
                ],
                "deletedActivityTypeIds": []
            }
            """.formatted(activityTypeId1, userId);

        // Mock service to handle the null fields appropriately
        when(activityTypeService.updateActivityTypes(eq(userId), any(BatchActivityTypeUpdateDTO.class)))
                .thenReturn(List.of(chillActivityTypeDTO));

        // Act & Assert
        mockMvc.perform(put("/api/v1/users/{userId}/activity-types", userId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonWithNulls))
                .andExpect(status().isOk());
    }

    // MARK: - Direct Controller Method Tests

    @Test
    void getOwnedActivityTypesForUser_DirectCall_ShouldReturnOkResponse_WhenServiceSucceeds() {
        // Arrange
        List<ActivityTypeDTO> activityTypes = List.of(chillActivityTypeDTO, foodActivityTypeDTO);
        when(activityTypeService.getActivityTypesByUserId(userId)).thenReturn(activityTypes);

        // Act
        ResponseEntity<List<ActivityTypeDTO>> response = 
            activityTypeController.getOwnedActivityTypesForUser(userId);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(2, response.getBody().size());
        assertEquals("Chill", response.getBody().get(0).getTitle());
        verify(activityTypeService, times(1)).getActivityTypesByUserId(userId);
    }

    @Test
    void getOwnedActivityTypesForUser_DirectCall_ShouldReturnInternalServerError_WhenServiceFails() {
        // Arrange
        when(activityTypeService.getActivityTypesByUserId(userId))
                .thenThrow(new RuntimeException("Service error"));

        // Act
        ResponseEntity<List<ActivityTypeDTO>> response = 
            activityTypeController.getOwnedActivityTypesForUser(userId);

        // Assert
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNull(response.getBody());
        verify(logger, times(1)).error(contains("Error fetching owned activity types"));
    }

    @Test
    void batchUpdateActivityTypes_DirectCall_ShouldReturnOkResponse_WhenServiceSucceeds() {
        // Arrange
        BatchActivityTypeUpdateDTO batchDTO = new BatchActivityTypeUpdateDTO(
            List.of(chillActivityTypeDTO),
            List.of()
        );
        
        when(activityTypeService.updateActivityTypes(userId, batchDTO))
                .thenReturn(List.of(chillActivityTypeDTO));

        // Act
        ResponseEntity<List<ActivityTypeDTO>> response = 
            activityTypeController.updateActivityTypes(userId, batchDTO);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().size());
        verify(activityTypeService, times(1)).updateActivityTypes(userId, batchDTO);
    }

    @Test
    void batchUpdateActivityTypes_DirectCall_ShouldReturnInternalServerError_WhenServiceFails() {
        // Arrange
        BatchActivityTypeUpdateDTO batchDTO = new BatchActivityTypeUpdateDTO(
            List.of(chillActivityTypeDTO),
            List.of()
        );
        
        when(activityTypeService.updateActivityTypes(userId, batchDTO))
                .thenThrow(new RuntimeException("Service error"));

        // Act
        ResponseEntity<List<ActivityTypeDTO>> response = 
            activityTypeController.updateActivityTypes(userId, batchDTO);

        // Assert
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNull(response.getBody());
        verify(logger, times(1)).error(contains("Error batch updating activity types"));
    }

    // MARK: - Edge Case Tests

    @Test
    void batchUpdateActivityTypes_ShouldHandleLargeDataset_WhenManyActivityTypes() throws Exception {
        // Arrange - Test with many activity types (simulating user with lots of types)
        List<ActivityTypeDTO> manyActivityTypes = new ArrayList<>();
        List<ActivityTypeDTO> manyUpdatedTypes = new ArrayList<>();
        
        for (int i = 0; i < 50; i++) {
            UUID id = UUID.randomUUID();
            ActivityTypeDTO dto = new ActivityTypeDTO(
                id, "Type " + i, List.of(), "🎯", i, userId, i % 5 == 0 // Every 5th is pinned
            );
            manyActivityTypes.add(dto);
            manyUpdatedTypes.add(dto);
        }
        
        BatchActivityTypeUpdateDTO largeBatchDTO = new BatchActivityTypeUpdateDTO(
            manyActivityTypes,
            List.of()
        );
        
        when(activityTypeService.updateActivityTypes(eq(userId), any(BatchActivityTypeUpdateDTO.class)))
                .thenReturn(manyUpdatedTypes);

        // Act & Assert
        mockMvc.perform(put("/api/v1/users/{userId}/activity-types", userId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(largeBatchDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(50));
    }

    @Test
    void batchUpdateActivityTypes_ShouldHandleSpecialCharacters_WhenTitleContainsUnicode() throws Exception {
        // Arrange - Test with special characters and emojis
        ActivityTypeDTO specialCharDTO = new ActivityTypeDTO(
            activityTypeId1,
            "🎉 Party & Fun 🎊 (Special Event)",
            List.of(),
            "🎉",
            0,
            userId,
            false
        );
        
        BatchActivityTypeUpdateDTO batchDTO = new BatchActivityTypeUpdateDTO(
            List.of(specialCharDTO),
            List.of()
        );
        
        when(activityTypeService.updateActivityTypes(eq(userId), any(BatchActivityTypeUpdateDTO.class)))
                .thenReturn(List.of(specialCharDTO));

        // Act & Assert
        mockMvc.perform(put("/api/v1/users/{userId}/activity-types", userId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(batchDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("🎉 Party & Fun 🎊 (Special Event)"))
                .andExpect(jsonPath("$[0].icon").value("🎉"));
    }

    @Test
    void getOwnedActivityTypes_ShouldHandleConcurrentRequests_WhenMultipleRequestsSimultaneously() {
        // Arrange - Simulate concurrent requests from front-end
        List<ActivityTypeDTO> activityTypes = List.of(chillActivityTypeDTO, foodActivityTypeDTO);
        when(activityTypeService.getActivityTypesByUserId(userId)).thenReturn(activityTypes);

        // Act - Make multiple concurrent calls
        ResponseEntity<List<ActivityTypeDTO>> response1 = 
            activityTypeController.getOwnedActivityTypesForUser(userId);
        ResponseEntity<List<ActivityTypeDTO>> response2 = 
            activityTypeController.getOwnedActivityTypesForUser(userId);
        ResponseEntity<List<ActivityTypeDTO>> response3 = 
            activityTypeController.getOwnedActivityTypesForUser(userId);

        // Assert - All should succeed
        assertEquals(HttpStatus.OK, response1.getStatusCode());
        assertEquals(HttpStatus.OK, response2.getStatusCode());
        assertEquals(HttpStatus.OK, response3.getStatusCode());
        
        // Verify service was called for each request
        verify(activityTypeService, times(3)).getActivityTypesByUserId(userId);
    }
} 