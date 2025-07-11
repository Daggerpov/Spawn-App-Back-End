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
     * Get calendar activities for a user based on optional month and year filters
     * If month and year are provided, returns activities for that month/year
     * If not provided, returns all activities for the user
     * 
     * @param userId User ID to get activities for
     * @param month Optional month number (1-12)
     * @param year Optional year (e.g., 2024)
     * @return List of calendar activities based on the provided filters
     */
    List<CalendarActivityDTO> getCalendarActivitiesWithFilters(UUID userId, Integer month, Integer year);
    
    /**
     * Clear the calendar cache for a specific user
     * This should be called when Activities are created, updated, or deleted,
     * or when a user's participation status changes.
     * 
     * @param userId User ID whose cache should be cleared
     */
    void clearCalendarCache(UUID userId);
    
    /**
     * Clear all calendar caches for all users
     * This should be called after schema changes or major updates to ensure fresh data
     */
    void clearAllCalendarCaches();
} 