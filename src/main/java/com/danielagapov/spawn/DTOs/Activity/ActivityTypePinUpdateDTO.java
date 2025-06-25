package com.danielagapov.spawn.DTOs.Activity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ActivityTypePinUpdateDTO {
    private UUID activityTypeId;
    private Boolean isPinned;
} 