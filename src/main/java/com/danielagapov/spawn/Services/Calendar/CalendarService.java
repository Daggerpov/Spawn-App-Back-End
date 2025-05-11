package com.danielagapov.spawn.Services.Calendar;

import com.danielagapov.spawn.DTOs.CalendarActivityDTO;
import com.danielagapov.spawn.Enums.EventCategory;
import com.danielagapov.spawn.Enums.ParticipationStatus;
import com.danielagapov.spawn.Exceptions.Logger.ILogger;
import com.danielagapov.spawn.Models.Event;
import com.danielagapov.spawn.Models.EventUser;
import com.danielagapov.spawn.Repositories.IEventRepository;
import com.danielagapov.spawn.Repositories.IEventUserRepository;
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

    public CalendarService(ILogger logger, IEventRepository eventRepository, IEventUserRepository eventUserRepository) {
        this.logger = logger;
        this.eventRepository = eventRepository;
        this.eventUserRepository = eventUserRepository;
    }

    /**
     * Get calendar activities for a specific user, month, and year
     */
    @Override
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