package com.danielagapov.spawn.Controllers.User.Profile;

import com.danielagapov.spawn.DTOs.User.Profile.UserStatsDTO;
import com.danielagapov.spawn.Services.UserStatsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/users/{userId}/stats")
public class UserStatsController {

    private final UserStatsService userStatsService;

    @Autowired
    public UserStatsController(UserStatsService userStatsService) {
        this.userStatsService = userStatsService;
    }

    @GetMapping
    public ResponseEntity<UserStatsDTO> getUserStats(@PathVariable UUID userId) {
        UserStatsDTO stats = userStatsService.getUserStats(userId);
        return ResponseEntity.ok(stats);
    }
} 