package com.danielagapov.spawn.DTOs;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.OffsetDateTime;
import java.util.UUID;

@AllArgsConstructor
@Getter
public class UserIdEventTimeDTO {
    private UUID userId;
    private OffsetDateTime startTime;
}
