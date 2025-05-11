package com.danielagapov.spawn.Services.Calendar;

import com.danielagapov.spawn.DTOs.CalendarActivityDTO;
import java.util.List;
import java.util.UUID;

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
    List<CalendarActivityDTO> getCalendarActivitiesForUser(int month, int year, UUID userId);
    
    /**
     * Get all calendar activities for a specific user
     * 
     * @param userId User ID to get activities for
     * @return List of all calendar activities for the user
     */
    List<CalendarActivityDTO> getAllCalendarActivitiesForUser(UUID userId);
    
    /**
     * Clear the calendar cache for a specific user
     * This should be called when events are created, updated, or deleted,
     * or when a user's participation status changes.
     * 
     * @param userId User ID whose cache should be cleared
     */
    void clearCalendarCache(UUID userId);
} 