package com.danielagapov.spawn.activity.api.dto;

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
    private String title; // Title of the activity
    private String icon; // Icon for the calendar Activity (emoji)
    private String colorHexCode; // Color for the calendar Activity
    private UUID activityId; // Optional, if the activity is linked to a spawn Activity
}
