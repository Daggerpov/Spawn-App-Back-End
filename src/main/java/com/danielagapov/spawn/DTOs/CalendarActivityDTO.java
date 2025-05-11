package com.danielagapov.spawn.DTOs;

import com.danielagapov.spawn.Enums.EventCategory;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CalendarActivityDTO {
    private UUID id;
    private String title;
    private String date; // ISO format: YYYY-MM-DD
    private EventCategory eventCategory; 
    private String icon; // Icon for the calendar event (emoji)
    private UUID eventId; // Optional, if the activity is linked to a spawn event
    private UUID userId; // Optional, user associated with this activity
} 