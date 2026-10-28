package com.danielagapov.spawn.activity.api.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

@AllArgsConstructor
@NoArgsConstructor
@Getter
public class UserIdActivityTimeDTO {
    private UUID userId;
    private OffsetDateTime startTime;
}
