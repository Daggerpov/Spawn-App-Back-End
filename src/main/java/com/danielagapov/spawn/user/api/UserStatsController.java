package com.danielagapov.spawn.user.api;

import com.danielagapov.spawn.user.api.dto.UserStatsDTO;
import com.danielagapov.spawn.shared.exceptions.Logger.ILogger;
import com.danielagapov.spawn.user.internal.services.IUserStatsService;
import com.danielagapov.spawn.shared.util.LoggingUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users/{userId}/stats")
public final class UserStatsController {

    private final IUserStatsService userStatsService;
    private final ILogger logger;

    @Autowired
    public UserStatsController(IUserStatsService userStatsService, ILogger logger) {
        this.userStatsService = userStatsService;
        this.logger = logger;
    }

    @GetMapping
    public ResponseEntity<UserStatsDTO> getUserStats(@PathVariable UUID userId) {
        try {
            UserStatsDTO stats = userStatsService.getUserStats(userId);
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            logger.error("Error getting user stats for user: " + LoggingUtils.formatUserIdInfo(userId) + ": " + e.getMessage());
            throw e;
        }
    }
} 