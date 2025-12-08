package com.danielagapov.spawn.user.api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserStatsDTO implements Serializable {
    private int peopleMet; // Users they've participated in Activities with
    private int spawnsMade; // Activities created
    private int spawnsJoined; // Activities participated in (not created by them)
} 