package com.danielagapov.spawn.Controllers;

import com.danielagapov.spawn.DTOs.DeviceTokenDTO;
import com.danielagapov.spawn.DTOs.Notification.NotificationPreferencesDTO;
import com.danielagapov.spawn.Services.PushNotification.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    @Autowired
    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    // full path: /api/v1/notifications/device-tokens/register
    @PostMapping("/device-tokens/register")
    public ResponseEntity<?> registerDeviceToken(@RequestBody DeviceTokenDTO deviceTokenDTO) {
        try {
            notificationService.registerDeviceToken(deviceTokenDTO);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error registering device token: " + e.getMessage());
        }
    }

    // full path: /api/v1/notifications/device-tokens/unregister
    @DeleteMapping("/device-tokens/unregister")
    public ResponseEntity<?> unregisterDeviceToken(@RequestBody DeviceTokenDTO deviceTokenDTO) {
        try {
            notificationService.unregisterDeviceToken(deviceTokenDTO.getToken());
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error unregistering device token: " + e.getMessage());
        }
    }
    
    // full path: /api/v1/notifications/preferences/{userId}
    @GetMapping("/preferences/{userId}")
    public ResponseEntity<?> getNotificationPreferences(@PathVariable UUID userId) {
        if (userId == null) return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        try {
            NotificationPreferencesDTO preferences = notificationService.getNotificationPreferences(userId);
            if (preferences == null) {
                // Return default preferences if not found
                preferences = new NotificationPreferencesDTO(true, true, true, true, userId);
                notificationService.saveNotificationPreferences(preferences);
            }
            return new ResponseEntity<>(preferences, HttpStatus.OK);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error fetching notification preferences: " + e.getMessage());
        }
    }

    // full path: /api/v1/notifications/preferences/{userId}
    @PostMapping("/preferences/{userId}")
    public ResponseEntity<?> updateNotificationPreferences(
            @PathVariable UUID userId,
            @RequestBody NotificationPreferencesDTO preferencesDTO) {
        if (userId == null) return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        try {
            // Ensure user ID in path matches the one in the DTO
            if (!userId.equals(preferencesDTO.getUserId())) {
                return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
            }
            
            NotificationPreferencesDTO savedPreferences = notificationService.saveNotificationPreferences(preferencesDTO);
            return new ResponseEntity<>(savedPreferences, HttpStatus.OK);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error updating notification preferences: " + e.getMessage());
        }
    }
} 