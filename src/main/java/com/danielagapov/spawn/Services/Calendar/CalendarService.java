package com.danielagapov.spawn.Services.Calendar;

import com.danielagapov.spawn.DTOs.CalendarActivityDTO;
import com.danielagapov.spawn.Exceptions.Logger.ILogger;
import org.springframework.stereotype.Service;

import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class CalendarService implements ICalendarService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private final ILogger logger;

    public CalendarService(ILogger logger) {
        this.logger = logger;
    }

    /**
     * Get all calendar activities for a specific month and year
     */
    public List<CalendarActivityDTO> getCalendarActivities(int month, int year) {
        // This would typically query events from the database
        // For now, we're generating mock data
        return generateMockActivities(month, year);
    }

    /**
     * Get calendar activities for a specific user, month, and year
     */
    @Override
    public List<CalendarActivityDTO> getCalendarActivitiesForUser(int month, int year, String userId) {
        // In a real implementation, we would:
        // 1. Find events the user is hosting
        // 2. Find events the user is attending
        // 3. Combine them into activities
        
        logger.info("Getting calendar activities for user: " + userId + ", month: " + month + ", year: " + year);
        
        // For now, return mock data
        UUID userUuid;
        try {
            userUuid = UUID.fromString(userId);
        } catch (IllegalArgumentException e) {
            logger.error("Invalid user ID format: " + userId);
            return Collections.emptyList();
        }
        
        return generateMockActivitiesForUser(month, year, userUuid);
    }

    /**
     * Generate mock activities for testing the calendar
     */
    private List<CalendarActivityDTO> generateMockActivities(int month, int year) {
        List<CalendarActivityDTO> activities = new ArrayList<>();
        YearMonth yearMonth = YearMonth.of(year, month);
        int daysInMonth = yearMonth.lengthOfMonth();
        
        // Activity types for variety
        String[] activityTypes = {"music", "sports", "food", "travel", "gaming", "outdoors"};
        Random random = new Random();
        
        // Generate some random activities throughout the month
        for (int day = 1; day <= daysInMonth; day++) {
            // Only create activities for ~30% of days
            if (random.nextDouble() > 0.7) {
                String date = String.format("%04d-%02d-%02d", year, month, day);
                
                // Create 1-3 activities for this day
                int activitiesForDay = random.nextInt(3) + 1;
                for (int i = 0; i < activitiesForDay; i++) {
                    String activityType = activityTypes[random.nextInt(activityTypes.length)];
                    
                    activities.add(CalendarActivityDTO.builder()
                            .id(UUID.randomUUID())
                            .title("Activity " + day + "-" + (i + 1))
                            .date(date)
                            .activityType(activityType)
                            .build());
                }
            }
        }
        
        return activities;
    }
    
    /**
     * Generate mock activities specific to a user
     */
    private List<CalendarActivityDTO> generateMockActivitiesForUser(int month, int year, UUID userId) {
        List<CalendarActivityDTO> activities = generateMockActivities(month, year);
        
        logger.info("Generated " + activities.size() + " potential activities for user: " + userId);
        
        // Filter to only ~50% of activities and assign the user ID
        return activities.stream()
                .filter(a -> new Random().nextBoolean())
                .map(activity -> {
                    activity.setUserId(userId);
                    return activity;
                })
                .collect(Collectors.toList());
    }
} 