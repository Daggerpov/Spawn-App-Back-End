package com.danielagapov.spawn.analytics.api;

import com.danielagapov.spawn.activity.api.dto.FullFeedActivityDTO;
import com.danielagapov.spawn.user.api.dto.BaseUserDTO;
import com.danielagapov.spawn.shared.util.ShareLinkType;
import com.danielagapov.spawn.analytics.internal.domain.ShareLink;
import com.danielagapov.spawn.activity.internal.services.ActivityService;
import com.danielagapov.spawn.analytics.internal.services.ShareLinkService;
import com.danielagapov.spawn.user.internal.services.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Controller for managing share links with human-readable codes
 */
@RestController
@RequestMapping("/api/v1/share")
@RequiredArgsConstructor
@Slf4j
public final class ShareLinkController {
    
    private final ShareLinkService shareLinkService;
    private final ActivityService activityService;
    private final UserService userService;
    
    /**
     * Generate a share code for an activity
     * @param activityId The activity ID
     * @return Share code response
     */
    @PostMapping("/activity/{activityId}")
    public ResponseEntity<Map<String, String>> generateActivityShareCode(@PathVariable UUID activityId) {
        try {
            // Get the activity DTO to access start and end times
            com.danielagapov.spawn.DTOs.Activity.ActivityDTO activity = activityService.getActivityById(activityId);
            if (activity == null) {
                return ResponseEntity.notFound().build();
            }
            
            String shareCode = shareLinkService.generateActivityShareLink(activityId, activity.getStartTime(), activity.getEndTime(), activity.getCreatedAt());
            
            Map<String, String> response = new HashMap<>();
            response.put("shareCode", shareCode);
            response.put("shareUrl", "https://getspawn.com/activity/" + shareCode);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error generating activity share code for ID: {}", activityId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Generate a share code for a user profile
     * @param userId The user ID
     * @return Share code response
     */
    @PostMapping("/profile/{userId}")
    public ResponseEntity<Map<String, String>> generateProfileShareCode(@PathVariable UUID userId) {
        try {
            com.danielagapov.spawn.DTOs.User.UserDTO user = userService.getUserById(userId);
            if (user == null) {
                return ResponseEntity.notFound().build();
            }
            
            String shareCode = shareLinkService.generateProfileShareLink(userId);
            
            Map<String, String> response = new HashMap<>();
            response.put("shareCode", shareCode);
            response.put("shareUrl", "https://getspawn.com/profile/" + shareCode);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error generating profile share code for ID: {}", userId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Resolve a share code to get activity details
     * @param shareCode The share code to resolve
     * @return Activity details if found and valid
     */
    @GetMapping("/activity/{shareCode}")
    public ResponseEntity<FullFeedActivityDTO> resolveActivityShareCode(@PathVariable String shareCode) {
        try {
            Optional<ShareLink> shareLink = shareLinkService.resolveShareCode(shareCode);
            
            if (shareLink.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            
            ShareLink link = shareLink.get();
            if (link.getType() != ShareLinkType.ACTIVITY) {
                return ResponseEntity.badRequest().build();
            }
            
            // Get the activity details
            FullFeedActivityDTO activity = activityService.getFullActivityById(link.getTargetId(), null);
            if (activity == null) {
                // Activity was deleted, clean up the share link
                shareLinkService.deleteShareLinksForTarget(link.getTargetId(), ShareLinkType.ACTIVITY);
                return ResponseEntity.notFound().build();
            }
            
            return ResponseEntity.ok(activity);
        } catch (Exception e) {
            log.error("Error resolving activity share code: {}", shareCode, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Resolve a share code to get user profile details
     * @param shareCode The share code to resolve
     * @return User profile details if found and valid
     */
    @GetMapping("/profile/{shareCode}")
    public ResponseEntity<BaseUserDTO> resolveProfileShareCode(@PathVariable String shareCode) {
        try {
            Optional<ShareLink> shareLink = shareLinkService.resolveShareCode(shareCode);
            
            if (shareLink.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            
            ShareLink link = shareLink.get();
            if (link.getType() != ShareLinkType.PROFILE) {
                return ResponseEntity.badRequest().build();
            }
            
            // Get the user profile details - need to create a BaseUserDTO from UserDTO
            com.danielagapov.spawn.DTOs.User.UserDTO user = userService.getUserById(link.getTargetId());
            if (user == null) {
                // User was deleted, clean up the share link
                shareLinkService.deleteShareLinksForTarget(link.getTargetId(), ShareLinkType.PROFILE);
                return ResponseEntity.notFound().build();
            }
            
            // Convert UserDTO to BaseUserDTO
            BaseUserDTO baseUser = new BaseUserDTO(user.getId(), user.getName(), user.getEmail(), user.getUsername(), user.getBio(), user.getProfilePicture(), user.getHasCompletedOnboarding());
            
            return ResponseEntity.ok(baseUser);
        } catch (Exception e) {
            log.error("Error resolving profile share code: {}", shareCode, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Check if a share code is valid and get basic info about what it points to
     * @param shareCode The share code to check
     * @return Basic information about the share code
     */
    @GetMapping("/validate/{shareCode}")
    public ResponseEntity<Map<String, Object>> validateShareCode(@PathVariable String shareCode) {
        try {
            Optional<ShareLink> shareLink = shareLinkService.resolveShareCode(shareCode);
            
            if (shareLink.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            
            ShareLink link = shareLink.get();
            Map<String, Object> response = new HashMap<>();
            response.put("valid", true);
            response.put("type", link.getType().toString().toLowerCase());
            response.put("targetId", link.getTargetId());
            response.put("expiresAt", link.getExpiresAt());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error validating share code: {}", shareCode, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
} 