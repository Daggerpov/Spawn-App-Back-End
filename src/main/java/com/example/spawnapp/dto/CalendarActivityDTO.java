package com.example.spawnapp.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CalendarActivityDTO {
    private UUID id;
    private String title;
    private String date; // ISO format: YYYY-MM-DD
    private String activityType; // e.g., "music", "sports", "food", "travel", etc.
    private UUID eventId; // Optional, if the activity is linked to a spawn event
    private UUID userId; // Optional, user associated with this activity
} 