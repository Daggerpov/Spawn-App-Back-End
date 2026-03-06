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
    private String date;
    private String title;
    private String icon;
    private String colorHexCode;
    private UUID activityId;
}
