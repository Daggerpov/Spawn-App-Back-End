package com.danielagapov.spawn.Controllers.User;

import com.danielagapov.spawn.DTOs.UpdateUserSocialMediaDTO;
import com.danielagapov.spawn.DTOs.UserSocialMediaDTO;
import com.danielagapov.spawn.Services.UserSocialMediaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/users/{userId}/social-media")
public class UserSocialMediaController {

    private final UserSocialMediaService userSocialMediaService;

    @Autowired
    public UserSocialMediaController(UserSocialMediaService userSocialMediaService) {
        this.userSocialMediaService = userSocialMediaService;
    }

    @GetMapping
    public ResponseEntity<UserSocialMediaDTO> getUserSocialMedia(@PathVariable UUID userId) {
        UserSocialMediaDTO socialMedia = userSocialMediaService.getUserSocialMedia(userId);
        return ResponseEntity.ok(socialMedia);
    }

    @PostMapping
    public ResponseEntity<UserSocialMediaDTO> updateUserSocialMedia(
            @PathVariable UUID userId,
            @RequestBody UpdateUserSocialMediaDTO updateDTO) {
        UserSocialMediaDTO updatedSocialMedia = userSocialMediaService.updateUserSocialMedia(userId, updateDTO);
        return ResponseEntity.ok(updatedSocialMedia);
    }
} 