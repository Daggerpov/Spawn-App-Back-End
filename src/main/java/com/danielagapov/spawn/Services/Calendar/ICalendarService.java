package com.danielagapov.spawn.Services.Calendar;

import com.danielagapov.spawn.DTOs.CalendarActivityDTO;
import java.util.List;

/**
 * Service for handling calendar activities
 */
public interface ICalendarService {
    /**
     * Get calendar activities for a specific user, month, and year
     * 
     * @param month Month number (1-12)
     * @param year Year (e.g., 2024)
     * @param userId User ID to get activities for
     * @return List of calendar activities for the user
     */
    List<CalendarActivityDTO> getCalendarActivitiesForUser(int month, int year, String userId);
} 