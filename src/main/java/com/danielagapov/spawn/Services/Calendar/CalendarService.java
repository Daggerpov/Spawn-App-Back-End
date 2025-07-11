package com.danielagapov.spawn.Services.Calendar;

import com.danielagapov.spawn.DTOs.CalendarActivityDTO;
import com.danielagapov.spawn.Enums.ParticipationStatus;
import com.danielagapov.spawn.Exceptions.Logger.ILogger;
import com.danielagapov.spawn.Models.Activity;
import com.danielagapov.spawn.Models.ActivityUser;
import com.danielagapov.spawn.Repositories.IActivityRepository;
import com.danielagapov.spawn.Repositories.IActivityUserRepository;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class CalendarService implements ICalendarService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    // Cache names
    private static final String CALENDAR_ACTIVITIES_CACHE = "calendarActivities";
    private static final String ALL_CALENDAR_ACTIVITIES_CACHE = "allCalendarActivities";
    private static final String FILTERED_CALENDAR_ACTIVITIES_CACHE = "filteredCalendarActivities";
    private final ILogger logger;
    private final IActivityRepository ActivityRepository;
    private final IActivityUserRepository activityUserRepository;
    private final CacheManager cacheManager;

    public CalendarService(
        ILogger logger, 
        IActivityRepository ActivityRepository, 
        IActivityUserRepository activityUserRepository,
        CacheManager cacheManager
    ) {
        this.logger = logger;
        this.ActivityRepository = ActivityRepository;
        this.activityUserRepository = activityUserRepository;
        this.cacheManager = cacheManager;
    }

    /**
     * Get calendar activities for a user based on optional month and year filters
     */
    @Override
    @Cacheable(value = FILTERED_CALENDAR_ACTIVITIES_CACHE, key = "{#userId, #month, #year}")
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
    @Cacheable(value = CALENDAR_ACTIVITIES_CACHE, key = "{#userId, #month, #year}")
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
    @Cacheable(value = ALL_CALENDAR_ACTIVITIES_CACHE, key = "#userId")
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
            List<Activity> createdActivities = ActivityRepository.findByCreatorId(userId);
            logger.info("Found " + createdActivities.size() + " Activities created by user: " + userId);
            
            // 2. Get Activities the user is participating in
            List<ActivityUser> participatingActivities = activityUserRepository.findByUser_IdAndStatus(userId, ParticipationStatus.participating);
            logger.info("Found " + participatingActivities.size() + " Activities user is participating in, userId: " + userId);
            
            // Process Activities created by the user
            for (Activity Activity : createdActivities) {
                try {
                    LocalDate ActivityDate = Activity.getStartTime().toLocalDate();
                    
                    // Apply date filtering if specified
                    if (isDateInRange(ActivityDate, startDate, endDate)) {
                        activities.add(createCalendarActivityFromActivity(Activity, userId, "creator"));
                    }
                } catch (Exception e) {
                    logger.error("Error processing created Activity: " + Activity.getId() + " for user: " + userId + 
                                ". Error: " + e.getMessage() + ", Stack trace: " + Arrays.toString(e.getStackTrace()));
                    // Continue processing other Activities
                }
            }
            
            // Process Activities the user is participating in
            for (ActivityUser ActivityUser : participatingActivities) {
                try {
                    Activity Activity = ActivityUser.getActivity();
                    LocalDate ActivityDate = Activity.getStartTime().toLocalDate();
                    
                    // Apply date filtering if specified
                    if (isDateInRange(ActivityDate, startDate, endDate)) {
                        // Avoid adding duplicate entries for Activities the user both created and is participating in
                        if (!Activity.getCreator().getId().equals(userId)) {
                            activities.add(createCalendarActivityFromActivity(Activity, userId, "participant"));
                        }
                    }
                } catch (Exception e) {
                    logger.error("Error processing participating Activity: " + 
                                (ActivityUser.getActivity() != null ? ActivityUser.getActivity().getId() : "null") + 
                                " for user: " + userId + ". Error: " + e.getMessage() + 
                                ", Stack trace: " + Arrays.toString(e.getStackTrace()));
                    // Continue processing other Activities
                }
            }
            
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
        try {
            logger.info("Clearing calendar cache for user: " + userId);
            
            // Clear the cache for all calendar activities
            if (cacheManager.getCache(ALL_CALENDAR_ACTIVITIES_CACHE) != null) {
                cacheManager.getCache(ALL_CALENDAR_ACTIVITIES_CACHE).evict(userId);
                logger.info("Cleared ALL_CALENDAR_ACTIVITIES_CACHE for user: " + userId);
            } else {
                logger.warn("Cache " + ALL_CALENDAR_ACTIVITIES_CACHE + " not found when clearing for user: " + userId);
            }
            
            // Clear the filtered calendar activities cache
            if (cacheManager.getCache(FILTERED_CALENDAR_ACTIVITIES_CACHE) != null) {
                cacheManager.getCache(FILTERED_CALENDAR_ACTIVITIES_CACHE).clear();
                logger.info("Cleared FILTERED_CALENDAR_ACTIVITIES_CACHE for all users");
            } else {
                logger.warn("Cache " + FILTERED_CALENDAR_ACTIVITIES_CACHE + " not found when clearing");
            }
            
            // Clear the specific month/year caches by evicting all entries
            if (cacheManager.getCache(CALENDAR_ACTIVITIES_CACHE) != null) {
                cacheManager.getCache(CALENDAR_ACTIVITIES_CACHE).clear();
                logger.info("Cleared CALENDAR_ACTIVITIES_CACHE for all users");
            } else {
                logger.warn("Cache " + CALENDAR_ACTIVITIES_CACHE + " not found when clearing");
            }
        } catch (Exception e) {
            logger.error("Error clearing calendar cache for user: " + userId + 
                         ". Error: " + e.getMessage() + ", Stack trace: " + Arrays.toString(e.getStackTrace()));
            throw e;
        }
    }
    
    /**
     * Clear all calendar caches for all users
     * This should be called after schema changes or major updates to ensure fresh data
     */
    public void clearAllCalendarCaches() {
        try {
            logger.info("Clearing ALL calendar caches for all users");
            
            // Clear all calendar activity caches
            if (cacheManager.getCache(ALL_CALENDAR_ACTIVITIES_CACHE) != null) {
                cacheManager.getCache(ALL_CALENDAR_ACTIVITIES_CACHE).clear();
                logger.info("Cleared ALL_CALENDAR_ACTIVITIES_CACHE for all users");
            }
            
            if (cacheManager.getCache(FILTERED_CALENDAR_ACTIVITIES_CACHE) != null) {
                cacheManager.getCache(FILTERED_CALENDAR_ACTIVITIES_CACHE).clear();
                logger.info("Cleared FILTERED_CALENDAR_ACTIVITIES_CACHE for all users");
            }
            
            if (cacheManager.getCache(CALENDAR_ACTIVITIES_CACHE) != null) {
                cacheManager.getCache(CALENDAR_ACTIVITIES_CACHE).clear();
                logger.info("Cleared CALENDAR_ACTIVITIES_CACHE for all users");
            }
            
            logger.info("Successfully cleared all calendar caches");
        } catch (Exception e) {
            logger.error("Error clearing all calendar caches: " + e.getMessage() + 
                         ", Stack trace: " + Arrays.toString(e.getStackTrace()));
            throw e;
        }
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
            // If no date range is specified, include all dates
            if (startDate == null && endDate == null) {
                return true;
            }
            
            // Check lower bound if specified
            boolean afterStart = (startDate == null || !date.isBefore(startDate));
            
            // Check upper bound if specified
            boolean beforeEnd = (endDate == null || !date.isAfter(endDate));
            
            return afterStart && beforeEnd;
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
            
            return CalendarActivityDTO.builder()
                    .id(Activity.getId())
                    .date(Activity.getStartTime().toLocalDate().format(DATE_FORMATTER))
                    .title(Activity.getTitle() != null ? Activity.getTitle() : "Untitled Activity")
                    .icon(Activity.getIcon() != null ? Activity.getIcon() : "⭐")
                    .colorHexCode(Activity.getColorHexCode() != null ? Activity.getColorHexCode() : "#6B73FF")
                    .activityId(Activity.getId())
                    .build();
        } catch (Exception e) {
            logger.error("Error creating calendar activity from Activity: " + 
                        (Activity != null ? Activity.getId() : "null Activity") + 
                        " for user: " + userId + ", role: " + role + 
                        ". Error: " + e.getMessage() + ", Stack trace: " + Arrays.toString(e.getStackTrace()));
            throw e;
        }
    }
}