package com.danielagapov.spawn.activity.internal.services;

import com.danielagapov.spawn.activity.api.dto.CalendarActivityDTO;
import com.danielagapov.spawn.shared.util.ParticipationStatus;
import com.danielagapov.spawn.shared.exceptions.Logger.ILogger;
import com.danielagapov.spawn.activity.internal.domain.Activity;
import com.danielagapov.spawn.activity.internal.domain.ActivityUser;
import com.danielagapov.spawn.activity.internal.repositories.IActivityRepository;
import com.danielagapov.spawn.activity.internal.repositories.IActivityUserRepository;
import com.danielagapov.spawn.shared.util.CacheEvictionHelper;
import com.danielagapov.spawn.shared.util.CacheNames;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class CalendarService implements ICalendarService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private final ILogger logger;
    private final IActivityRepository ActivityRepository;
    private final IActivityUserRepository activityUserRepository;
    private final CacheEvictionHelper cacheEvictionHelper;

    public CalendarService(
        ILogger logger, 
        IActivityRepository ActivityRepository, 
        IActivityUserRepository activityUserRepository,
        CacheEvictionHelper cacheEvictionHelper
    ) {
        this.logger = logger;
        this.ActivityRepository = ActivityRepository;
        this.activityUserRepository = activityUserRepository;
        this.cacheEvictionHelper = cacheEvictionHelper;
    }

    /**
     * Get calendar activities for a user based on optional month and year filters
     */
    @Override
    @Cacheable(value = CacheNames.FILTERED_CALENDAR_ACTIVITIES, key = "{#userId, #month, #year}")
    public List<CalendarActivityDTO> getCalendarActivitiesWithFilters(UUID userId, Integer month, Integer year) {
        logger.info("Getting calendar activities with filters for user: " + userId + 
                   (month != null ? ", month: " + month : "") + 
                   (year != null ? ", year: " + year : ""));
        
        try {
            // If month and year are provided, get activities for that specific month
            if (month != null && year != null) {
                logger.info("Using month/year filter for user: " + userId);
                List<CalendarActivityDTO> activities = getCalendarActivitiesForUser(month, year, userId);
                logger.info("Found " + activities.size() + " activities with month/year filter for user: " + userId);
                return activities;
            }
            // Otherwise, get all activities
            else {
                logger.info("No filters provided, getting all activities for user: " + userId);
                List<CalendarActivityDTO> activities = getAllCalendarActivitiesForUser(userId);
                logger.info("Found " + activities.size() + " total activities (no filters) for user: " + userId);
                return activities;
            }
        } catch (Exception e) {
            logger.error("Error getting calendar activities with filters for user: " + userId + 
                        (month != null ? ", month: " + month : "") + 
                        (year != null ? ", year: " + year : "") + 
                        ". Error: " + e.getMessage() + 
                        ", Stack trace: " + Arrays.toString(e.getStackTrace()));
            throw e;
        }
    }

    /**
     * Get calendar activities for a specific user, month, and year
     */
    @Override
    @Cacheable(value = CacheNames.CALENDAR_ACTIVITIES, key = "{#userId, #month, #year}")
    public List<CalendarActivityDTO> getCalendarActivitiesForUser(int month, int year, UUID userId) {
        logger.info("Getting calendar activities for user: " + userId + ", month: " + month + ", year: " + year);
        
        try {
            // Define the month range for filtering
            YearMonth yearMonth = YearMonth.of(year, month);
            LocalDate startOfMonth = yearMonth.atDay(1);
            LocalDate endOfMonth = yearMonth.atEndOfMonth();
            
            // Call the helper method with month/year filtering
            List<CalendarActivityDTO> activities = fetchCalendarActivities(userId, startOfMonth, endOfMonth);
            
            logger.info("Found " + activities.size() + " calendar activities for user: " + userId + 
                       " in month: " + month + ", year: " + year);
            return activities;
        } catch (Exception e) {
            logger.error("Error getting calendar activities for user: " + userId + ", month: " + month + ", year: " + year + 
                         ". Error: " + e.getMessage() + ", Stack trace: " + Arrays.toString(e.getStackTrace()));
            throw e;
        }
    }
    
    /**
     * Get all calendar activities for a specific user
     */
    @Override
    @Cacheable(value = CacheNames.ALL_CALENDAR_ACTIVITIES, key = "#userId")
    public List<CalendarActivityDTO> getAllCalendarActivitiesForUser(UUID userId) {
        logger.info("Getting all calendar activities for user: " + userId);
        
        try {
            // Call the helper method with no date filtering
            List<CalendarActivityDTO> activities = fetchCalendarActivities(userId, null, null);
            
            logger.info("Found " + activities.size() + " total calendar activities for user: " + userId);
            return activities;
        } catch (Exception e) {
            logger.error("Error getting all calendar activities for user: " + userId + 
                         ". Error: " + e.getMessage() + ", Stack trace: " + Arrays.toString(e.getStackTrace()));
            throw e;
        }
    }
    
    /**
     * Helper method to fetch calendar activities with optional date filtering
     * 
     * @param userId User ID to get activities for
     * @param startDate Start date for filtering (inclusive), or null for no start date filter
     * @param endDate End date for filtering (inclusive), or null for no end date filter
     * @return List of calendar activities matching the criteria
     */
    private List<CalendarActivityDTO> fetchCalendarActivities(UUID userId, LocalDate startDate, LocalDate endDate) {
        List<CalendarActivityDTO> activities = new ArrayList<>();
        
        try {
            logger.info("Fetching calendar activities for user: " + userId + 
                       (startDate != null ? ", startDate: " + startDate : "") + 
                       (endDate != null ? ", endDate: " + endDate : ""));
            
            // 1. Get Activities the user created
            logger.info("About to query Activities created by user: " + userId);
            List<Activity> createdActivities = ActivityRepository.findByCreatorId(userId);
            logger.info("Found " + createdActivities.size() + " Activities created by user: " + userId);
            
            // 2. Get Activities the user is participating in
            logger.info("About to query Activities user is participating in for userId: " + userId);
            List<ActivityUser> participatingActivities = activityUserRepository.findByUser_IdAndStatus(userId, ParticipationStatus.participating);
            logger.info("Found " + participatingActivities.size() + " Activities user is participating in, userId: " + userId);
            
            // Process Activities created by the user
            logger.info("Starting to process " + createdActivities.size() + " created Activities for user: " + userId);
            for (Activity Activity : createdActivities) {
                try {
                    logger.info("Processing created Activity ID: " + Activity.getId() + " for user: " + userId);
                    
                    // Add null safety check for startTime
                    if (Activity.getStartTime() == null) {
                        logger.warn("Skipping Activity " + Activity.getId() + " - null startTime");
                        continue;
                    }
                    
                    // Convert to UTC-based LocalDate for consistent date filtering
                    LocalDate ActivityDate = Activity.getStartTime().atZoneSameInstant(java.time.ZoneId.of("UTC")).toLocalDate();
                    
                    // Apply date filtering if specified
                    if (isDateInRange(ActivityDate, startDate, endDate)) {
                        logger.info("Activity " + Activity.getId() + " is in date range, creating CalendarActivityDTO");
                        activities.add(createCalendarActivityFromActivity(Activity, userId, "creator"));
                        logger.info("Successfully created CalendarActivityDTO for Activity: " + Activity.getId());
                    } else {
                        logger.info("Activity " + Activity.getId() + " is outside date range, skipping");
                    }
                } catch (Exception e) {
                    logger.error("Error processing created Activity: " + 
                                (Activity != null ? Activity.getId() : "null Activity") + " for user: " + userId + 
                                ". Error: " + e.getMessage() + ", Stack trace: " + Arrays.toString(e.getStackTrace()));
                    // Continue processing other Activities
                }
            }
            logger.info("Finished processing created Activities. Current activities list size: " + activities.size());
            
            // Process Activities the user is participating in
            logger.info("Starting to process " + participatingActivities.size() + " participating Activities for user: " + userId);
            for (ActivityUser ActivityUser : participatingActivities) {
                try {
                    logger.info("Processing participating ActivityUser for user: " + userId);
                    Activity Activity = ActivityUser.getActivity();
                    
                    // Add null safety checks
                    if (Activity == null) {
                        logger.warn("Skipping ActivityUser - null Activity");
                        continue;
                    }
                    if (Activity.getStartTime() == null) {
                        logger.warn("Skipping Activity " + Activity.getId() + " - null startTime");
                        continue;
                    }
                    
                    logger.info("Got Activity " + Activity.getId() + " from ActivityUser for user: " + userId);
                    
                    // Convert to UTC-based LocalDate for consistent date filtering
                    LocalDate ActivityDate = Activity.getStartTime().atZoneSameInstant(java.time.ZoneId.of("UTC")).toLocalDate();
                    
                    // Apply date filtering if specified
                    if (isDateInRange(ActivityDate, startDate, endDate)) {
                        // Avoid adding duplicate entries for Activities the user both created and is participating in
                        if (!Activity.getCreator().getId().equals(userId)) {
                            logger.info("Activity " + Activity.getId() + " is not created by user, adding as participant");
                            activities.add(createCalendarActivityFromActivity(Activity, userId, "participant"));
                            logger.info("Successfully created CalendarActivityDTO for participating Activity: " + Activity.getId());
                        } else {
                            logger.info("Activity " + Activity.getId() + " was created by user, skipping to avoid duplicate");
                        }
                    } else {
                        logger.info("Activity " + Activity.getId() + " is outside date range, skipping");
                    }
                } catch (Exception e) {
                    logger.error("Error processing participating Activity: " + 
                                (ActivityUser != null && ActivityUser.getActivity() != null ? ActivityUser.getActivity().getId() : "null") + 
                                " for user: " + userId + ". Error: " + e.getMessage() + 
                                ", Stack trace: " + Arrays.toString(e.getStackTrace()));
                    // Continue processing other Activities
                }
            }
            logger.info("Finished processing participating Activities. Final activities list size: " + activities.size());
            
            return activities;
            
        } catch (Exception e) {
            logger.error("Error fetching calendar activities for user: " + userId + 
                        ". Error: " + e.getMessage() + ", Stack trace: " + Arrays.toString(e.getStackTrace()));
            throw e;
        }
    }
    
    /**
     * Clear the calendar cache for a specific user
     * This should be called when Activities are created, updated, or deleted,
     * or when a user's participation status changes.
     */
    public void clearCalendarCache(UUID userId) {
        logger.info("Clearing calendar cache for user: " + userId);
        
        // Clear the cache for all calendar activities for the specific user
        cacheEvictionHelper.evictCache(CacheNames.ALL_CALENDAR_ACTIVITIES, userId);
        
        // Clear the filtered and monthly calendar caches for all users
        // (since one user's changes might affect other users' filtered views)
        cacheEvictionHelper.clearCaches(
            CacheNames.FILTERED_CALENDAR_ACTIVITIES,
            CacheNames.CALENDAR_ACTIVITIES
        );
    }
    
    /**
     * Clear all calendar caches for all users
     * This should be called after schema changes or major updates to ensure fresh data
     */
    public void clearAllCalendarCaches() {
        logger.info("Clearing ALL calendar caches for all users");
        cacheEvictionHelper.clearAllCalendarCaches();
    }
    
    /**
     * Check if a date falls within the specified range
     * 
     * @param date The date to check
     * @param startDate Start of the range (inclusive), or null for no lower bound
     * @param endDate End of the range (inclusive), or null for no upper bound
     * @return true if the date is within the range, false otherwise
     */
    private boolean isDateInRange(LocalDate date, LocalDate startDate, LocalDate endDate) {
        try {
            // Add null safety for date parameter
            if (date == null) {
                logger.warn("isDateInRange called with null date parameter");
                return false;
            }
            
            // If no date range is specified, include all dates
            if (startDate == null && endDate == null) {
                logger.info("No date range specified, including date: " + date);
                return true;
            }
            
            // Check lower bound if specified
            boolean afterStart = (startDate == null || !date.isBefore(startDate));
            
            // Check upper bound if specified
            boolean beforeEnd = (endDate == null || !date.isAfter(endDate));
            
            boolean inRange = afterStart && beforeEnd;
            
            logger.info("Date range check for " + date + 
                        " (start: " + startDate + ", end: " + endDate + "): " + 
                        (inRange ? "INCLUDED" : "EXCLUDED") + 
                        " (afterStart: " + afterStart + ", beforeEnd: " + beforeEnd + ")");
            
            return inRange;
        } catch (Exception e) {
            logger.error("Error checking if date is in range. Date: " + date + 
                        ", startDate: " + startDate + ", endDate: " + endDate + 
                        ". Error: " + e.getMessage() + ", Stack trace: " + Arrays.toString(e.getStackTrace()));
            throw e;
        }
    }
    
    /**
     * Create a CalendarActivityDTO from an Activity
     */
    private CalendarActivityDTO createCalendarActivityFromActivity(Activity Activity, UUID userId, String role) {
        try {
            // Add null safety checks
            if (Activity == null) {
                throw new IllegalArgumentException("Activity cannot be null");
            }
            if (Activity.getId() == null) {
                throw new IllegalArgumentException("Activity ID cannot be null");
            }
            if (Activity.getStartTime() == null) {
                throw new IllegalArgumentException("Activity start time cannot be null for Activity ID: " + Activity.getId());
            }
            
            // Use UTC timezone for consistent date formatting across different server timezones
            LocalDate activityDate = Activity.getStartTime().atZoneSameInstant(java.time.ZoneId.of("UTC")).toLocalDate();
            String formattedDate = activityDate.format(DATE_FORMATTER);
            
            logger.info("Creating CalendarActivityDTO for Activity: " + Activity.getId() + 
                       ", StartTime: " + Activity.getStartTime() + 
                       ", Formatted Date: " + formattedDate + 
                       ", Role: " + role);
            
            CalendarActivityDTO calendarActivityDTO = CalendarActivityDTO.builder()
                    .id(Activity.getId())
                    .date(formattedDate)
                    .title(Activity.getTitle() != null ? Activity.getTitle() : "Untitled Activity")
                    .icon(Activity.getIcon() != null ? Activity.getIcon() : "⭐")
                    .colorHexCode(Activity.getColorHexCode() != null ? Activity.getColorHexCode() : "#6B73FF")
                    .activityId(Activity.getId())
                    .build();
            
            logger.info("Successfully created CalendarActivityDTO: " + calendarActivityDTO.getId() + 
                       ", Date: " + calendarActivityDTO.getDate() + 
                       ", Title: " + calendarActivityDTO.getTitle());
            
            return calendarActivityDTO;
        } catch (Exception e) {
            logger.error("Error creating calendar activity from Activity: " + 
                        (Activity != null ? Activity.getId() : "null Activity") + 
                        " for user: " + userId + ", role: " + role + 
                        ". Error: " + e.getMessage() + ", Stack trace: " + Arrays.toString(e.getStackTrace()));
            throw e;
        }
    }
}