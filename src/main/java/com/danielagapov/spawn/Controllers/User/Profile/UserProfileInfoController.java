package com.danielagapov.spawn.Controllers.User.Profile;

import com.danielagapov.spawn.DTOs.User.Profile.UserProfileInfoDTO;
import com.danielagapov.spawn.Exceptions.Logger.ILogger;
import com.danielagapov.spawn.Services.UserProfileInfo.IUserProfileInfoService;
import com.danielagapov.spawn.Util.LoggingUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users/{userId}/profile-info")
public class UserProfileInfoController {

    private final IUserProfileInfoService userProfileInfoService;
    private final ILogger logger;

    @Autowired
    public UserProfileInfoController(IUserProfileInfoService userProfileInfoService, ILogger logger) {
        this.userProfileInfoService = userProfileInfoService;
        this.logger = logger;
    }

    @GetMapping
    public ResponseEntity<UserProfileInfoDTO> getUserProfileInfo(@PathVariable UUID userId) {
        try {
            UserProfileInfoDTO profileInfo = userProfileInfoService.getUserProfileInfo(userId);
            return ResponseEntity.ok(profileInfo);
        } catch (Exception e) {
            logger.error("Error getting user profile info for user: " + LoggingUtils.formatUserIdInfo(userId) + ": " + e.getMessage());
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
} 