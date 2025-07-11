package com.danielagapov.spawn.Controllers.User.Profile;

import com.danielagapov.spawn.DTOs.CalendarActivityDTO;
import com.danielagapov.spawn.Exceptions.Base.BaseNotFoundException;
import com.danielagapov.spawn.Exceptions.Logger.ILogger;
import com.danielagapov.spawn.Services.Calendar.ICalendarService;
import com.danielagapov.spawn.Util.LoggingUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
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
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Integer year) {
        
        logger.info("Calendar API called for user: " + userId + 
                   (month != null ? ", month: " + month : "") + 
                   (year != null ? ", year: " + year : ""));
        
        if (userId == null) {
            logger.error("Invalid parameter: userId is null");
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }

        try {
            List<CalendarActivityDTO> activities = calendarService.getCalendarActivitiesWithFilters(userId, month, year);
            
            logger.info("Successfully retrieved " + activities.size() + " calendar activities for user: " + userId);
            
            // Log sample activities for debugging
            if (!activities.isEmpty()) {
                logger.info("Sample calendar activities:");
                for (int i = 0; i < Math.min(activities.size(), 3); i++) {
                    CalendarActivityDTO activity = activities.get(i);
                    logger.info("  " + (i + 1) + ". " + activity.getTitle() + " on " + activity.getDate() + 
                               " (ID: " + activity.getId() + ")");
                }
            } else {
                logger.warn("No calendar activities found for user: " + userId + 
                           (month != null ? ", month: " + month : "") + 
                           (year != null ? ", year: " + year : ""));
            }
            
            return ResponseEntity.ok(activities);
        } catch (BaseNotFoundException e) {
            logger.error("User not found for calendar activities: " + LoggingUtils.formatUserIdInfo(userId) + ": " + e.getMessage());
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            logger.error("Error getting calendar activities for user: " + LoggingUtils.formatUserIdInfo(userId) + 
                        ": " + e.getMessage() + ", Stack trace: " + java.util.Arrays.toString(e.getStackTrace()));
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/all")
    public ResponseEntity<List<CalendarActivityDTO>> getAllCalendarActivities(
            @PathVariable UUID userId) {
        if (userId == null) {
            logger.error("Invalid parameter: userId is null");
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }

        try {
            List<CalendarActivityDTO> activities = calendarService.getCalendarActivitiesWithFilters(userId, null, null);
            return ResponseEntity.ok(activities);
        } catch (BaseNotFoundException e) {
            logger.error("User not found for all calendar activities: " + LoggingUtils.formatUserIdInfo(userId) + ": " + e.getMessage());
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            logger.error("Error getting all calendar activities for user: " + LoggingUtils.formatUserIdInfo(userId) + ": " + e.getMessage());
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
} 