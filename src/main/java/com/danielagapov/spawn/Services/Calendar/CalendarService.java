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
        
        List<CalendarActivityDTO> activities = new ArrayList<>();
        
        // Define the month range
        YearMonth yearMonth = YearMonth.of(year, month);
        LocalDate startOfMonth = yearMonth.atDay(1);
        LocalDate endOfMonth = yearMonth.atEndOfMonth();
        
        try {
            // 1. Get events the user created during this month
            List<Event> createdEvents = eventRepository.findByCreatorId(userId);
            
            // 2. Get events the user is participating in
            List<EventUser> participatingEvents = eventUserRepository.findByUser_IdAndStatus(userId, ParticipationStatus.participating);
            
            // Process events created by the user
            for (Event event : createdEvents) {
                // Check if the event falls within the requested month
                LocalDate eventDate = event.getStartTime().toLocalDate();
                if (isDateInMonth(eventDate, startOfMonth, endOfMonth)) {
                    activities.add(createCalendarActivityFromEvent(event, userId, "creator"));
                }
            }
            
            // Process events the user is participating in
            for (EventUser eventUser : participatingEvents) {
                Event event = eventUser.getEvent();
                // Check if the event falls within the requested month
                LocalDate eventDate = event.getStartTime().toLocalDate();
                if (isDateInMonth(eventDate, startOfMonth, endOfMonth)) {
                    // Avoid adding duplicate entries for events the user both created and is participating in
                    if (!event.getCreator().getId().equals(userId)) {
                        activities.add(createCalendarActivityFromEvent(event, userId, "participant"));
                    }
                }
            }
            
            logger.info("Found " + activities.size() + " calendar activities for user: " + userId);
            return activities;
            
        } catch (Exception e) {
            logger.error("Error retrieving calendar activities: " + e.getMessage());
            return new ArrayList<>();
        }
    }
    
    /**
     * Get all calendar activities for a specific user
     */
    @Override
    @Cacheable(value = ALL_CALENDAR_ACTIVITIES_CACHE, key = "#userId")
    public List<CalendarActivityDTO> getAllCalendarActivitiesForUser(UUID userId) {
        logger.info("Getting all calendar activities for user: " + userId);
        
        List<CalendarActivityDTO> activities = new ArrayList<>();
        
        try {
            // 1. Get all events the user created
            List<Event> createdEvents = eventRepository.findByCreatorId(userId);
            
            // 2. Get all events the user is participating in
            List<EventUser> participatingEvents = eventUserRepository.findByUser_IdAndStatus(userId, ParticipationStatus.participating);
            
            // Process events created by the user
            for (Event event : createdEvents) {
                activities.add(createCalendarActivityFromEvent(event, userId, "creator"));
            }
            
            // Process events the user is participating in
            for (EventUser eventUser : participatingEvents) {
                Event event = eventUser.getEvent();
                // Avoid adding duplicate entries for events the user both created and is participating in
                if (!event.getCreator().getId().equals(userId)) {
                    activities.add(createCalendarActivityFromEvent(event, userId, "participant"));
                }
            }
            
            logger.info("Found " + activities.size() + " calendar activities for user: " + userId);
            return activities;
            
        } catch (Exception e) {
            logger.error("Error retrieving all calendar activities: " + e.getMessage());
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
     * Check if a date falls within a specific month range
     */
    private boolean isDateInMonth(LocalDate date, LocalDate startOfMonth, LocalDate endOfMonth) {
        return !date.isBefore(startOfMonth) && !date.isAfter(endOfMonth);
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