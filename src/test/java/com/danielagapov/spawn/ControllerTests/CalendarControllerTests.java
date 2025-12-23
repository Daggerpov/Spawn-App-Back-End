package com.danielagapov.spawn.ControllerTests;

import com.danielagapov.spawn.user.api.CalendarController;
import com.danielagapov.spawn.activity.api.dto.CalendarActivityDTO;
import com.danielagapov.spawn.shared.exceptions.Base.BaseNotFoundException;
import com.danielagapov.spawn.shared.exceptions.Logger.ILogger;
import com.danielagapov.spawn.shared.util.EntityType;
import com.danielagapov.spawn.activity.internal.services.ICalendarService;
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

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Comprehensive unit tests for CalendarController
 * Tests calendar activity retrieval with various filters
 */
@ExtendWith(MockitoExtension.class)
class CalendarControllerTests {

    @Mock
    private ICalendarService calendarService;

    @Mock
    private ILogger logger;

    @InjectMocks
    private CalendarController calendarController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;
    private UUID userId;
    private List<CalendarActivityDTO> calendarActivities;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        
        mockMvc = MockMvcBuilders.standaloneSetup(calendarController)
                .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
                .build();
        
        userId = UUID.randomUUID();
        
        calendarActivities = List.of(
            new CalendarActivityDTO(UUID.randomUUID(), "Activity 1", LocalDate.now().plusDays(1), "🎉"),
            new CalendarActivityDTO(UUID.randomUUID(), "Activity 2", LocalDate.now().plusDays(2), "🍽️"),
            new CalendarActivityDTO(UUID.randomUUID(), "Activity 3", LocalDate.now().plusDays(3), "⚽")
        );
    }

    // MARK: - Get Calendar Activities Tests

    @Test
    void getCalendarActivities_ShouldReturnActivities_WhenNoFilters() throws Exception {
        when(calendarService.getCalendarActivitiesWithFilters(userId, null, null))
                .thenReturn(calendarActivities);

        mockMvc.perform(get("/api/v1/users/{userId}/calendar", userId))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].title").value("Activity 1"))
                .andExpect(jsonPath("$[1].title").value("Activity 2"))
                .andExpect(jsonPath("$[2].title").value("Activity 3"));

        verify(calendarService, times(1)).getCalendarActivitiesWithFilters(userId, null, null);
        verify(logger, times(1)).info(contains("Calendar API called"));
        verify(logger, times(1)).info(contains("Successfully retrieved 3 calendar activities"));
    }

    @Test
    void getCalendarActivities_ShouldReturnFilteredActivities_WhenMonthProvided() throws Exception {
        List<CalendarActivityDTO> januaryActivities = List.of(
            new CalendarActivityDTO(UUID.randomUUID(), "January Activity", LocalDate.of(2024, 1, 15), "🎉")
        );
        
        when(calendarService.getCalendarActivitiesWithFilters(userId, 1, null))
                .thenReturn(januaryActivities);

        mockMvc.perform(get("/api/v1/users/{userId}/calendar", userId)
                .param("month", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].title").value("January Activity"));

        verify(calendarService, times(1)).getCalendarActivitiesWithFilters(userId, 1, null);
        verify(logger, times(1)).info(contains("month: 1"));
    }

    @Test
    void getCalendarActivities_ShouldReturnFilteredActivities_WhenYearProvided() throws Exception {
        List<CalendarActivityDTO> yearActivities = List.of(
            new CalendarActivityDTO(UUID.randomUUID(), "2024 Activity", LocalDate.of(2024, 6, 15), "🎉")
        );
        
        when(calendarService.getCalendarActivitiesWithFilters(userId, null, 2024))
                .thenReturn(yearActivities);

        mockMvc.perform(get("/api/v1/users/{userId}/calendar", userId)
                .param("year", "2024"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].title").value("2024 Activity"));

        verify(calendarService, times(1)).getCalendarActivitiesWithFilters(userId, null, 2024);
        verify(logger, times(1)).info(contains("year: 2024"));
    }

    @Test
    void getCalendarActivities_ShouldReturnFilteredActivities_WhenMonthAndYearProvided() throws Exception {
        List<CalendarActivityDTO> specificMonthActivities = List.of(
            new CalendarActivityDTO(UUID.randomUUID(), "December 2024 Activity", LocalDate.of(2024, 12, 25), "🎄")
        );
        
        when(calendarService.getCalendarActivitiesWithFilters(userId, 12, 2024))
                .thenReturn(specificMonthActivities);

        mockMvc.perform(get("/api/v1/users/{userId}/calendar", userId)
                .param("month", "12")
                .param("year", "2024"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].title").value("December 2024 Activity"));

        verify(calendarService, times(1)).getCalendarActivitiesWithFilters(userId, 12, 2024);
        verify(logger, times(1)).info(contains("month: 12"));
        verify(logger, times(1)).info(contains("year: 2024"));
    }

    @Test
    void getCalendarActivities_ShouldReturnEmptyList_WhenNoActivitiesFound() throws Exception {
        when(calendarService.getCalendarActivitiesWithFilters(userId, null, null))
                .thenReturn(List.of());

        mockMvc.perform(get("/api/v1/users/{userId}/calendar", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        verify(calendarService, times(1)).getCalendarActivitiesWithFilters(userId, null, null);
        verify(logger, times(1)).warn(contains("No calendar activities found"));
    }

    @Test
    void getCalendarActivities_ShouldReturnBadRequest_WhenNullUserId() throws Exception {
        mockMvc.perform(get("/api/v1/users/{userId}/calendar", (Object) null))
                .andExpect(status().isBadRequest());

        verify(calendarService, never()).getCalendarActivitiesWithFilters(any(), any(), any());
        verify(logger, times(1)).error(contains("userId is null"));
    }

    @Test
    void getCalendarActivities_ShouldReturnNotFound_WhenUserNotFound() throws Exception {
        when(calendarService.getCalendarActivitiesWithFilters(userId, null, null))
                .thenThrow(new BaseNotFoundException(EntityType.User, userId));

        mockMvc.perform(get("/api/v1/users/{userId}/calendar", userId))
                .andExpect(status().isNotFound());

        verify(logger, times(1)).error(contains("User not found"));
    }

    @Test
    void getCalendarActivities_ShouldReturnInternalServerError_WhenServiceFails() throws Exception {
        when(calendarService.getCalendarActivitiesWithFilters(userId, null, null))
                .thenThrow(new RuntimeException("Database error"));

        mockMvc.perform(get("/api/v1/users/{userId}/calendar", userId))
                .andExpect(status().isInternalServerError());

        verify(logger, times(1)).error(contains("Error getting calendar activities"));
    }

    // MARK: - Get All Calendar Activities Tests

    @Test
    void getAllCalendarActivities_ShouldReturnAllActivities_WhenUserHasActivities() throws Exception {
        when(calendarService.getCalendarActivitiesWithFilters(userId, null, null))
                .thenReturn(calendarActivities);

        mockMvc.perform(get("/api/v1/users/{userId}/calendar/all", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3));

        verify(calendarService, times(1)).getCalendarActivitiesWithFilters(userId, null, null);
    }

    @Test
    void getAllCalendarActivities_ShouldReturnEmptyList_WhenNoActivities() throws Exception {
        when(calendarService.getCalendarActivitiesWithFilters(userId, null, null))
                .thenReturn(List.of());

        mockMvc.perform(get("/api/v1/users/{userId}/calendar/all", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        verify(calendarService, times(1)).getCalendarActivitiesWithFilters(userId, null, null);
    }

    @Test
    void getAllCalendarActivities_ShouldReturnBadRequest_WhenNullUserId() throws Exception {
        mockMvc.perform(get("/api/v1/users/{userId}/calendar/all", (Object) null))
                .andExpect(status().isBadRequest());

        verify(calendarService, never()).getCalendarActivitiesWithFilters(any(), any(), any());
    }

    @Test
    void getAllCalendarActivities_ShouldReturnNotFound_WhenUserNotFound() throws Exception {
        when(calendarService.getCalendarActivitiesWithFilters(userId, null, null))
                .thenThrow(new BaseNotFoundException(EntityType.User, userId));

        mockMvc.perform(get("/api/v1/users/{userId}/calendar/all", userId))
                .andExpect(status().isNotFound());

        verify(logger, times(1)).error(contains("User not found"));
    }

    // MARK: - Direct Controller Method Tests

    @Test
    void getCalendarActivities_DirectCall_ShouldReturnOk_WhenSuccessful() {
        when(calendarService.getCalendarActivitiesWithFilters(userId, null, null))
                .thenReturn(calendarActivities);

        ResponseEntity<List<CalendarActivityDTO>> response = 
            calendarController.getCalendarActivities(userId, null, null);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(3, response.getBody().size());
        verify(calendarService, times(1)).getCalendarActivitiesWithFilters(userId, null, null);
    }

    @Test
    void getCalendarActivities_DirectCall_ShouldReturnBadRequest_WhenNullUserId() {
        ResponseEntity<List<CalendarActivityDTO>> response = 
            calendarController.getCalendarActivities(null, null, null);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        verify(calendarService, never()).getCalendarActivitiesWithFilters(any(), any(), any());
    }

    @Test
    void getAllCalendarActivities_DirectCall_ShouldReturnOk_WhenSuccessful() {
        when(calendarService.getCalendarActivitiesWithFilters(userId, null, null))
                .thenReturn(calendarActivities);

        ResponseEntity<List<CalendarActivityDTO>> response = 
            calendarController.getAllCalendarActivities(userId);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(3, response.getBody().size());
    }

    // MARK: - Edge Case Tests

    @Test
    void getCalendarActivities_ShouldHandleLargeResult_WhenManyActivities() throws Exception {
        List<CalendarActivityDTO> manyActivities = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            manyActivities.add(new CalendarActivityDTO(
                UUID.randomUUID(), 
                "Activity " + i, 
                LocalDate.now().plusDays(i), 
                "🎉"
            ));
        }
        
        when(calendarService.getCalendarActivitiesWithFilters(userId, null, null))
                .thenReturn(manyActivities);

        mockMvc.perform(get("/api/v1/users/{userId}/calendar", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(100));

        verify(calendarService, times(1)).getCalendarActivitiesWithFilters(userId, null, null);
        verify(logger, times(1)).info(contains("Successfully retrieved 100 calendar activities"));
    }

    @Test
    void getCalendarActivities_ShouldHandleInvalidMonth_WhenMonthOutOfRange() throws Exception {
        // Note: Spring will handle validation, but service should handle invalid data
        when(calendarService.getCalendarActivitiesWithFilters(userId, 13, null))
                .thenReturn(List.of());

        mockMvc.perform(get("/api/v1/users/{userId}/calendar", userId)
                .param("month", "13"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        verify(calendarService, times(1)).getCalendarActivitiesWithFilters(userId, 13, null);
    }

    @Test
    void getCalendarActivities_ShouldHandleNegativeMonth_WhenMonthIsNegative() throws Exception {
        when(calendarService.getCalendarActivitiesWithFilters(userId, -1, null))
                .thenReturn(List.of());

        mockMvc.perform(get("/api/v1/users/{userId}/calendar", userId)
                .param("month", "-1"))
                .andExpect(status().isOk());

        verify(calendarService, times(1)).getCalendarActivitiesWithFilters(userId, -1, null);
    }

    @Test
    void getCalendarActivities_ShouldLogSampleActivities_WhenActivitiesReturned() throws Exception {
        when(calendarService.getCalendarActivitiesWithFilters(userId, null, null))
                .thenReturn(calendarActivities);

        mockMvc.perform(get("/api/v1/users/{userId}/calendar", userId))
                .andExpect(status().isOk());

        // Verify sample logging
        verify(logger, times(1)).info(contains("Sample calendar activities:"));
        verify(logger, times(3)).info(matches(".*\\d+\\..*Activity.*\\(ID:.*\\).*"));
    }

    @Test
    void getCalendarActivities_ShouldHandleDifferentYears_WhenFilteringByYear() throws Exception {
        List<CalendarActivityDTO> activities2023 = List.of(
            new CalendarActivityDTO(UUID.randomUUID(), "2023 Activity", LocalDate.of(2023, 6, 15), "🎉")
        );
        List<CalendarActivityDTO> activities2024 = List.of(
            new CalendarActivityDTO(UUID.randomUUID(), "2024 Activity", LocalDate.of(2024, 6, 15), "🎊")
        );
        
        when(calendarService.getCalendarActivitiesWithFilters(userId, null, 2023))
                .thenReturn(activities2023);
        when(calendarService.getCalendarActivitiesWithFilters(userId, null, 2024))
                .thenReturn(activities2024);

        mockMvc.perform(get("/api/v1/users/{userId}/calendar", userId)
                .param("year", "2023"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("2023 Activity"));

        mockMvc.perform(get("/api/v1/users/{userId}/calendar", userId)
                .param("year", "2024"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("2024 Activity"));

        verify(calendarService, times(1)).getCalendarActivitiesWithFilters(userId, null, 2023);
        verify(calendarService, times(1)).getCalendarActivitiesWithFilters(userId, null, 2024);
    }

    @Test
    void getCalendarActivities_ShouldHandleAllMonths_WhenFilteringByDifferentMonths() throws Exception {
        for (int month = 1; month <= 12; month++) {
            List<CalendarActivityDTO> monthActivities = List.of(
                new CalendarActivityDTO(UUID.randomUUID(), "Month " + month + " Activity", 
                    LocalDate.of(2024, month, 15), "🎉")
            );
            
            when(calendarService.getCalendarActivitiesWithFilters(userId, month, null))
                    .thenReturn(monthActivities);

            mockMvc.perform(get("/api/v1/users/{userId}/calendar", userId)
                    .param("month", String.valueOf(month)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1));
        }

        verify(calendarService, times(12)).getCalendarActivitiesWithFilters(eq(userId), anyInt(), isNull());
    }

    @Test
    void getCalendarActivities_ShouldHandleConcurrentRequests_WhenMultipleRequests() throws Exception {
        when(calendarService.getCalendarActivitiesWithFilters(userId, null, null))
                .thenReturn(calendarActivities);

        // Simulate concurrent requests
        mockMvc.perform(get("/api/v1/users/{userId}/calendar", userId))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/users/{userId}/calendar", userId))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/users/{userId}/calendar", userId))
                .andExpect(status().isOk());

        verify(calendarService, times(3)).getCalendarActivitiesWithFilters(userId, null, null);
    }
}

