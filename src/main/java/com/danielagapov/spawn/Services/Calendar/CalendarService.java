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
     * Get calendar activities for a specific user, month, and year
     */
    @Override
    @Cacheable(value = CALENDAR_ACTIVITIES_CACHE, key = "{#userId, #month, #year}")
    public List<CalendarActivityDTO> getCalendarActivitiesForUser(int month, int year, UUID userId) {
        logger.info("Getting calendar activities for user: " + userId + ", month: " + month + ", year: " + year);
        
        // Define the month range for filtering
        YearMonth yearMonth = YearMonth.of(year, month);
        LocalDate startOfMonth = yearMonth.atDay(1);
        LocalDate endOfMonth = yearMonth.atEndOfMonth();
        
        // Call the helper method with month/year filtering
        List<CalendarActivityDTO> activities = fetchCalendarActivities(userId, startOfMonth, endOfMonth);
        
        logger.info("Found " + activities.size() + " calendar activities for user: " + userId + 
                   " in month: " + month + ", year: " + year);
        return activities;
    }
    
    /**
     * Get all calendar activities for a specific user
     */
    @Override
    @Cacheable(value = ALL_CALENDAR_ACTIVITIES_CACHE, key = "#userId")
    public List<CalendarActivityDTO> getAllCalendarActivitiesForUser(UUID userId) {
        logger.info("Getting all calendar activities for user: " + userId);
        
        // Call the helper method with no date filtering
        List<CalendarActivityDTO> activities = fetchCalendarActivities(userId, null, null);
        
        logger.info("Found " + activities.size() + " total calendar activities for user: " + userId);
        return activities;
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
            // 1. Get events the user created
            List<Event> createdEvents = eventRepository.findByCreatorId(userId);
            
            // 2. Get events the user is participating in
            List<EventUser> participatingEvents = eventUserRepository.findByUser_IdAndStatus(userId, ParticipationStatus.participating);
            
            // Process events created by the user
            for (Event event : createdEvents) {
                LocalDate eventDate = event.getStartTime().toLocalDate();
                
                // Apply date filtering if specified
                if (isDateInRange(eventDate, startDate, endDate)) {
                    activities.add(createCalendarActivityFromEvent(event, userId, "creator"));
                }
            }
            
            // Process events the user is participating in
            for (EventUser eventUser : participatingEvents) {
                Event event = eventUser.getEvent();
                LocalDate eventDate = event.getStartTime().toLocalDate();
                
                // Apply date filtering if specified
                if (isDateInRange(eventDate, startDate, endDate)) {
                    // Avoid adding duplicate entries for events the user both created and is participating in
                    if (!event.getCreator().getId().equals(userId)) {
                        activities.add(createCalendarActivityFromEvent(event, userId, "participant"));
                    }
                }
            }
            
            return activities;
            
        } catch (Exception e) {
            logger.error("Error retrieving calendar activities: " + e.getMessage());
            return new ArrayList<>();
        }
    }
    
    /**
     * Clear the calendar cache for a specific user
     * This should be called when events are created, updated, or deleted,
     * or when a user's participation status changes.
     */
    public void clearCalendarCache(UUID userId) {
        logger.info("Clearing calendar cache for user: " + userId);
        
        // Clear the cache for all calendar activities
        cacheManager.getCache(ALL_CALENDAR_ACTIVITIES_CACHE).evict(userId);
        
        // Clear the specific month/year caches by evicting all entries
        // This is a simple approach - for more targeted clearing we'd need to track which months need updating
        cacheManager.getCache(CALENDAR_ACTIVITIES_CACHE).clear();
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
        // If no date range is specified, include all dates
        if (startDate == null && endDate == null) {
            return true;
        }
        
        // Check lower bound if specified
        boolean afterStart = (startDate == null || !date.isBefore(startDate));
        
        // Check upper bound if specified
        boolean beforeEnd = (endDate == null || !date.isAfter(endDate));
        
        return afterStart && beforeEnd;
    }
    
    /**
     * Create a CalendarActivityDTO from an Event
     */
    private CalendarActivityDTO createCalendarActivityFromEvent(Event event, UUID userId, String role) {
        return CalendarActivityDTO.builder()
                .id(event.getId())
                .date(event.getStartTime().toLocalDate().format(DATE_FORMATTER))
                .eventCategory(event.getCategory())
                .icon(event.getIcon())
                .eventId(event.getId())
                .build();
    }
}