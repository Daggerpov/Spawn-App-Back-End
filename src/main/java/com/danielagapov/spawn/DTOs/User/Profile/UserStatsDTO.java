package com.danielagapov.spawn.DTOs.User.Profile;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserStatsDTO {
    private int peopleMet; // Users they've participated in Activities with
    private int spawnsMade; // Activities created
    private int spawnsJoined; // Activities participated in (not created by them)
} 