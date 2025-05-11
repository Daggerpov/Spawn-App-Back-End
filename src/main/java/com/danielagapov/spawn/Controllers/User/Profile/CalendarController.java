package com.danielagapov.spawn.Controllers.User.Profile;

import com.danielagapov.spawn.DTOs.CalendarActivityDTO;
import com.danielagapov.spawn.Exceptions.Logger.ILogger;
import com.danielagapov.spawn.Services.Calendar.ICalendarService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users/{userId}/calendar")
public class CalendarController {

    private final ICalendarService calendarService;
    private final ILogger logger;

    @Autowired
    public CalendarController(ICalendarService calendarService, ILogger logger) {
        this.calendarService = calendarService;
        this.logger = logger;
    }

    @GetMapping()
    public ResponseEntity<List<CalendarActivityDTO>> getCalendarActivities(
            @PathVariable UUID userId,
            @RequestParam(required = true) int month,
            @RequestParam(required = true) int year) {
        
        logger.info("Fetching calendar activities for user: " + userId + ", month: " + month + ", year: " + year);
        
        List<CalendarActivityDTO> activities = calendarService.getCalendarActivitiesForUser(month, year, userId);
        
        logger.info("Found " + activities.size() + " activities for user: " + userId);
        
        return ResponseEntity.ok(activities);
    }
} 