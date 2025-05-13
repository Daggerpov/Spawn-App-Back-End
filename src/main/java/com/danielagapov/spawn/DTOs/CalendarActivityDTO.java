package com.danielagapov.spawn.DTOs;

import com.danielagapov.spawn.Enums.EventCategory;
import lombok.*;

import java.io.Serializable;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
public class CalendarActivityDTO implements Serializable {
    private UUID id;
    private String date; // ISO format: YYYY-MM-DD
    private EventCategory eventCategory;
    private String icon; // Icon for the calendar event (emoji)
    private UUID eventId; // Optional, if the activity is linked to a spawn event
} 