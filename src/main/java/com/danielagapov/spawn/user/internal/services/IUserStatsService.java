package com.danielagapov.spawn.user.internal.services;

import com.danielagapov.spawn.user.api.dto.Profile.UserStatsDTO;

import java.util.UUID;

public interface IUserStatsService {
    /**
     * Get statistics for a user including people met, spawns made and spawns joined
     * @param userId The ID of the user
     * @return DTO containing user statistics
     */
    UserStatsDTO getUserStats(UUID userId);
} 