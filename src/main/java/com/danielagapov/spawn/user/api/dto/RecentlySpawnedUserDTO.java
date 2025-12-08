package com.danielagapov.spawn.user.api.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.OffsetDateTime;

@AllArgsConstructor
@Getter
public class RecentlySpawnedUserDTO {
    private BaseUserDTO user;
    private OffsetDateTime dateTime;
}
