package com.example.spawnapp.controller;

import com.example.spawnapp.dto.CalendarActivityDTO;
import com.example.spawnapp.service.CalendarService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/calendar")
public class CalendarController {

    private final CalendarService calendarService;

    @Autowired
    public CalendarController(CalendarService calendarService) {
        this.calendarService = calendarService;
    }

    @GetMapping("/activities")
    public ResponseEntity<List<CalendarActivityDTO>> getCalendarActivities(
            @RequestParam(required = true) int month,
            @RequestParam(required = true) int year,
            @RequestParam(required = false) String userId) {
        
        List<CalendarActivityDTO> activities;
        
        if (userId != null && !userId.isEmpty()) {
            // Get activities for a specific user if userId is provided
            activities = calendarService.getCalendarActivitiesForUser(month, year, userId);
        } else {
            // Get all activities for the month/year if no userId is provided
            activities = calendarService.getCalendarActivities(month, year);
        }
        
        return ResponseEntity.ok(activities);
    }
} 