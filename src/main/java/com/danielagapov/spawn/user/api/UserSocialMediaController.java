package com.danielagapov.spawn.user.api;

import com.danielagapov.spawn.user.api.dto.Profile.UpdateUserSocialMediaDTO;
import com.danielagapov.spawn.user.api.dto.Profile.UserSocialMediaDTO;
import com.danielagapov.spawn.shared.exceptions.Logger.ILogger;
import com.danielagapov.spawn.user.internal.services.IUserSocialMediaService;
import com.danielagapov.spawn.shared.util.LoggingUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users/{userId}/social-media")
public class UserSocialMediaController {

    private final IUserSocialMediaService userSocialMediaService;
    private final ILogger logger;

    @Autowired
    public UserSocialMediaController(IUserSocialMediaService userSocialMediaService, ILogger logger) {
        this.userSocialMediaService = userSocialMediaService;
        this.logger = logger;
    }

    @GetMapping
    public ResponseEntity<UserSocialMediaDTO> getUserSocialMedia(@PathVariable UUID userId) {
        try {
            UserSocialMediaDTO socialMedia = userSocialMediaService.getUserSocialMedia(userId);
            return ResponseEntity.ok(socialMedia);
        } catch (Exception e) {
            logger.error("Error getting user social media for user: " + LoggingUtils.formatUserIdInfo(userId) + ": " + e.getMessage());
            throw e;
        }
    }

    @PutMapping
    public ResponseEntity<UserSocialMediaDTO> updateUserSocialMedia(
            @PathVariable UUID userId,
            @RequestBody UpdateUserSocialMediaDTO updateDTO) {
        try {
            UserSocialMediaDTO updatedSocialMedia = userSocialMediaService.updateUserSocialMedia(userId, updateDTO);
            return ResponseEntity.ok(updatedSocialMedia);
        } catch (Exception e) {
            logger.error("Error updating user social media for user: " + LoggingUtils.formatUserIdInfo(userId) + ": " + e.getMessage());
            throw e;
        }
    }
} 