package com.danielagapov.spawn.user.api;

import com.danielagapov.spawn.activity.api.dto.CalendarActivityDTO;
import com.danielagapov.spawn.shared.exceptions.Logger.ILogger;
import com.danielagapov.spawn.shared.feign.ActivityServiceClient;
import com.danielagapov.spawn.shared.util.LoggingUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users/{userId}/calendar")
public class CalendarController {

    private final ActivityServiceClient activityServiceClient;
    private final ILogger logger;

    @Autowired
    public CalendarController(ActivityServiceClient activityServiceClient, ILogger logger) {
        this.activityServiceClient = activityServiceClient;
        this.logger = logger;
    }

    @GetMapping()
    public ResponseEntity<List<CalendarActivityDTO>> getCalendarActivities(
            @PathVariable UUID userId,
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Integer year) {
        
        if (userId == null) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }

        try {
            List<CalendarActivityDTO> activities = activityServiceClient.getCalendarActivities(userId, month, year);
            return ResponseEntity.ok(activities);
        } catch (Exception e) {
            logger.error("Error getting calendar activities for user: " + LoggingUtils.formatUserIdInfo(userId) + ": " + e.getMessage());
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/all")
    public ResponseEntity<List<CalendarActivityDTO>> getAllCalendarActivities(
            @PathVariable UUID userId) {
        if (userId == null) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }

        try {
            List<CalendarActivityDTO> activities = activityServiceClient.getCalendarActivities(userId, null, null);
            return ResponseEntity.ok(activities);
        } catch (Exception e) {
            logger.error("Error getting all calendar activities for user: " + LoggingUtils.formatUserIdInfo(userId) + ": " + e.getMessage());
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
} 