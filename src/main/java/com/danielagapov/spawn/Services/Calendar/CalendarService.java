package com.danielagapov.spawn.Services.Calendar;

import com.danielagapov.spawn.DTOs.CalendarActivityDTO;
import com.danielagapov.spawn.Enums.EventCategory;
import com.danielagapov.spawn.Enums.ParticipationStatus;
import com.danielagapov.spawn.Exceptions.Logger.ILogger;
import com.danielagapov.spawn.Models.Event;
import com.danielagapov.spawn.Models.EventUser;
import com.danielagapov.spawn.Repositories.IEventRepository;
import com.danielagapov.spawn.Repositories.IEventUserRepository;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class CalendarService implements ICalendarService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private final ILogger logger;
    private final IEventRepository eventRepository;
    private final IEventUserRepository eventUserRepository;
    private final CacheManager cacheManager;
    
    // Cache names
    private static final String CALENDAR_ACTIVITIES_CACHE = "calendarActivities";
    private static final String ALL_CALENDAR_ACTIVITIES_CACHE = "allCalendarActivities";
    private static final String FILTERED_CALENDAR_ACTIVITIES_CACHE = "filteredCalendarActivities";

    public CalendarService(
        ILogger logger, 
        IEventRepository eventRepository, 
        IEventUserRepository eventUserRepository,
        CacheManager cacheManager
    ) {
        this.logger = logger;
        this.eventRepository = eventRepository;
        this.eventUserRepository = eventUserRepository;
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
            
            // 1. Get events the user created
            List<Event> createdEvents = eventRepository.findByCreatorId(userId);
            logger.info("Found " + createdEvents.size() + " events created by user: " + userId);
            
            // 2. Get events the user is participating in
            List<EventUser> participatingEvents = eventUserRepository.findByUser_IdAndStatus(userId, ParticipationStatus.participating);
            logger.info("Found " + participatingEvents.size() + " events user is participating in, userId: " + userId);
            
            // Process events created by the user
            for (Event event : createdEvents) {
                try {
                    LocalDate eventDate = event.getStartTime().toLocalDate();
                    
                    // Apply date filtering if specified
                    if (isDateInRange(eventDate, startDate, endDate)) {
                        activities.add(createCalendarActivityFromEvent(event, userId, "creator"));
                    }
                } catch (Exception e) {
                    logger.error("Error processing created event: " + event.getId() + " for user: " + userId + 
                                ". Error: " + e.getMessage() + ", Stack trace: " + Arrays.toString(e.getStackTrace()));
                    // Continue processing other events
                }
            }
            
            // Process events the user is participating in
            for (EventUser eventUser : participatingEvents) {
                try {
                    Event event = eventUser.getEvent();
                    LocalDate eventDate = event.getStartTime().toLocalDate();
                    
                    // Apply date filtering if specified
                    if (isDateInRange(eventDate, startDate, endDate)) {
                        // Avoid adding duplicate entries for events the user both created and is participating in
                        if (!event.getCreator().getId().equals(userId)) {
                            activities.add(createCalendarActivityFromEvent(event, userId, "participant"));
                        }
                    }
                } catch (Exception e) {
                    logger.error("Error processing participating event: " + 
                                (eventUser.getEvent() != null ? eventUser.getEvent().getId() : "null") + 
                                " for user: " + userId + ". Error: " + e.getMessage() + 
                                ", Stack trace: " + Arrays.toString(e.getStackTrace()));
                    // Continue processing other events
                }
            }
            
            return activities;
            
        } catch (Exception e) {
            logger.error("Error fetching calendar activities for user: " + userId + 
                        ". Error: " + e.getMessage() + ", Stack trace: " + Arrays.toString(e.getStackTrace()));
            return new ArrayList<>();
        }
    }
    
    /**
     * Clear the calendar cache for a specific user
     * This should be called when events are created, updated, or deleted,
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
            return false;
        }
    }
    
    /**
     * Create a CalendarActivityDTO from an Event
     */
    private CalendarActivityDTO createCalendarActivityFromEvent(Event event, UUID userId, String role) {
        try {
            return CalendarActivityDTO.builder()
                    .id(event.getId())
                    .date(event.getStartTime().toLocalDate().format(DATE_FORMATTER))
                    .eventCategory(event.getCategory())
                    .icon(event.getIcon())
                    .eventId(event.getId())
                    .build();
        } catch (Exception e) {
            logger.error("Error creating calendar activity from event: " + event.getId() + 
                        " for user: " + userId + ", role: " + role + 
                        ". Error: " + e.getMessage() + ", Stack trace: " + Arrays.toString(e.getStackTrace()));
            throw e;
        }
    }
}