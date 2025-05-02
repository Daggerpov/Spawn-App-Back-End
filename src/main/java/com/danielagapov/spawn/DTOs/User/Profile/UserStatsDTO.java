package com.danielagapov.spawn.DTOs;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserStatsDTO {
    private int peopleMet; // Users they've participated in events with
    private int spawnsMade; // Events created
    private int spawnsJoined; // Events participated in (not created by them)
} 