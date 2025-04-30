package com.danielagapov.spawn.Controllers;

import com.danielagapov.spawn.DTOs.DeviceTokenDTO;
import com.danielagapov.spawn.DTOs.Notification.NotificationPreferencesDTO;
import com.danielagapov.spawn.Services.PushNotification.FCMService;
import com.danielagapov.spawn.Services.PushNotification.NotificationService;
import com.danielagapov.spawn.Services.PushNotification.NotificationVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/notifications")
public class NotificationController {

    private final NotificationService notificationService;
    private final FCMService fcmService;

    @Autowired
    public NotificationController(NotificationService notificationService, FCMService fcmService) {
        this.notificationService = notificationService;
        this.fcmService = fcmService;
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
            return new ResponseEntity<>(notificationService.getNotificationPreferences(userId), HttpStatus.OK);
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

    // full path: /api/v1/notifications
    @Deprecated(since = "for testing purposes")
    @GetMapping("/notification")
    public ResponseEntity<?> testNotification(@RequestParam String deviceToken) {
        try {
            fcmService.sendMessageToToken(new NotificationVO(deviceToken, "Test", "This is a test notification sent from Spawn Backend", new HashMap<>()));
            return new ResponseEntity<>(HttpStatus.OK);
        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
} 