package com.danielagapov.spawn.DTOs.User;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.OffsetDateTime;

@AllArgsConstructor
@Getter
public class RecentlySpawnedUserDTO {
    private BaseUserDTO user;
    private OffsetDateTime dateTime;
}
