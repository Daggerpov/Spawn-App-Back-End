package com.danielagapov.spawn.Services.UserStats;

import com.danielagapov.spawn.DTOs.User.Profile.UserStatsDTO;

import java.util.UUID;

public interface IUserStatsService {
    /**
     * Get statistics for a user including people met, spawns made and spawns joined
     * @param userId The ID of the user
     * @return DTO containing user statistics
     */
    UserStatsDTO getUserStats(UUID userId);
} 