package com.danielagapov.spawn.Controllers.User.Profile;

import com.danielagapov.spawn.DTOs.CalendarActivityDTO;
import com.danielagapov.spawn.DTOs.User.FriendUser.RecommendedFriendUserDTO;
import com.danielagapov.spawn.Exceptions.Base.BaseNotFoundException;
import com.danielagapov.spawn.Exceptions.Logger.ILogger;
import com.danielagapov.spawn.Services.Calendar.ICalendarService;
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
        if (userId == null) return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        
        try {
            return ResponseEntity.ok(calendarService.getCalendarActivitiesWithFilters(userId, month, year));
        } catch (BaseNotFoundException e) {
            logger.error("User not found for calendar activities: " + userId);
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            logger.error("Error fetching calendar activities for user " + userId + ": " + e.getMessage());
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    
    @GetMapping("/all")
    public ResponseEntity<List<CalendarActivityDTO>> getAllCalendarActivities(
            @PathVariable UUID userId) {
        if (userId == null) return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        
        try {
            return ResponseEntity.ok(calendarService.getCalendarActivitiesWithFilters(userId, null, null));
        } catch (BaseNotFoundException e) {
            logger.error("User not found for all calendar activities: " + userId);
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            logger.error("Error fetching all calendar activities for user " + userId + ": " + e.getMessage());
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
} 