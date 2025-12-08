package com.danielagapov.spawn.notification.api;

import com.danielagapov.spawn.notification.api.dto.DeviceTokenDTO;
import com.danielagapov.spawn.notification.api.dto.NotificationPreferencesDTO;
import com.danielagapov.spawn.shared.exceptions.ILogger;
import com.danielagapov.spawn.notification.internal.services.FCMService;
import com.danielagapov.spawn.notification.internal.services.NotificationService;
import com.danielagapov.spawn.notification.internal.services.NotificationVO;
import com.danielagapov.spawn.shared.util.LoggingUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/notifications")
public final class NotificationController {

    private final NotificationService notificationService;
    private final FCMService fcmService;
    private final ILogger logger;

    @Autowired
    public NotificationController(NotificationService notificationService, FCMService fcmService, ILogger logger) {
        this.notificationService = notificationService;
        this.fcmService = fcmService;
        this.logger = logger;
    }

    // full path: /api/v1/notifications/device-tokens/register
    @PostMapping("/device-tokens/register")
    public ResponseEntity<?> registerDeviceToken(@RequestBody DeviceTokenDTO deviceTokenDTO) {
        try {
            notificationService.registerDeviceToken(deviceTokenDTO);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            logger.error("Error registering device token for user: " + LoggingUtils.formatUserIdInfo(deviceTokenDTO.getUserId()) + ": " + e.getMessage());
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
            logger.error("Error unregistering device token: " + deviceTokenDTO.getToken() + ": " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error unregistering device token: " + e.getMessage());
        }
    }

    // full path: /api/v1/notifications/preferences/{userId}
    @GetMapping("/preferences/{userId}")
    public ResponseEntity<?> getNotificationPreferences(@PathVariable UUID userId) {
        if (userId == null) {
            logger.error("Invalid parameter: userId is null");
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
        try {
            return new ResponseEntity<>(notificationService.getNotificationPreferences(userId), HttpStatus.OK);
        } catch (Exception e) {
            logger.error("Error fetching notification preferences for user: " + LoggingUtils.formatUserIdInfo(userId) + ": " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error fetching notification preferences: " + e.getMessage());
        }
    }

    // full path: /api/v1/notifications/preferences/{userId}
    @PostMapping("/preferences/{userId}")
    public ResponseEntity<?> updateNotificationPreferences(
            @PathVariable UUID userId,
            @RequestBody NotificationPreferencesDTO preferencesDTO) {
        if (userId == null) {
            logger.error("Invalid parameter: userId is null");
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
        try {
            // Ensure user ID in path matches the one in the DTO
            if (!userId.equals(preferencesDTO.getUserId())) {
                logger.error("User ID mismatch: path userId " + userId + " does not match DTO userId " + preferencesDTO.getUserId());
                return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
            }

            NotificationPreferencesDTO savedPreferences = notificationService.saveNotificationPreferences(preferencesDTO);
            return new ResponseEntity<>(savedPreferences, HttpStatus.OK);
        } catch (Exception e) {
            logger.error("Error updating notification preferences for user: " + LoggingUtils.formatUserIdInfo(userId) + ": " + e.getMessage());
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
            logger.error("Error sending test notification to device token: " + deviceToken + ": " + e.getMessage());
            e.printStackTrace();
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
} 