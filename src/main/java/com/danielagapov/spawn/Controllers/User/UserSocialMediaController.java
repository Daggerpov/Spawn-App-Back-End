package com.danielagapov.spawn.Controllers.User;

import com.danielagapov.spawn.DTOs.User.Profile.UpdateUserSocialMediaDTO;
import com.danielagapov.spawn.DTOs.User.Profile.UserSocialMediaDTO;
import com.danielagapov.spawn.Services.UserSocialMedia.IUserSocialMediaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users/{userId}/social-media")
public class UserSocialMediaController {

    private final IUserSocialMediaService userSocialMediaService;

    @Autowired
    public UserSocialMediaController(IUserSocialMediaService userSocialMediaService) {
        this.userSocialMediaService = userSocialMediaService;
    }

    @GetMapping
    public ResponseEntity<UserSocialMediaDTO> getUserSocialMedia(@PathVariable UUID userId) {
        UserSocialMediaDTO socialMedia = userSocialMediaService.getUserSocialMedia(userId);
        return ResponseEntity.ok(socialMedia);
    }

    @PutMapping
    public ResponseEntity<UserSocialMediaDTO> updateUserSocialMedia(
            @PathVariable UUID userId,
            @RequestBody UpdateUserSocialMediaDTO updateDTO) {
        UserSocialMediaDTO updatedSocialMedia = userSocialMediaService.updateUserSocialMedia(userId, updateDTO);
        return ResponseEntity.ok(updatedSocialMedia);
    }
} 