package com.danielagapov.spawn.Controllers.User.Profile;

import com.danielagapov.spawn.DTOs.User.Profile.UpdateUserSocialMediaDTO;
import com.danielagapov.spawn.DTOs.User.Profile.UserSocialMediaDTO;
import com.danielagapov.spawn.Exceptions.Logger.ILogger;
import com.danielagapov.spawn.Services.UserSocialMedia.IUserSocialMediaService;
import com.danielagapov.spawn.Util.LoggingUtils;
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
        logger.info("Getting user social media for user: " + LoggingUtils.formatUserIdInfo(userId));
        try {
            UserSocialMediaDTO socialMedia = userSocialMediaService.getUserSocialMedia(userId);
            logger.info("User social media retrieved successfully for user: " + LoggingUtils.formatUserIdInfo(userId));
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
        logger.info("Updating user social media for user: " + LoggingUtils.formatUserIdInfo(userId));
        try {
            UserSocialMediaDTO updatedSocialMedia = userSocialMediaService.updateUserSocialMedia(userId, updateDTO);
            logger.info("User social media updated successfully for user: " + LoggingUtils.formatUserIdInfo(userId));
            return ResponseEntity.ok(updatedSocialMedia);
        } catch (Exception e) {
            logger.error("Error updating user social media for user: " + LoggingUtils.formatUserIdInfo(userId) + ": " + e.getMessage());
            throw e;
        }
    }
} 