package com.danielagapov.spawn.Controllers;

import com.danielagapov.spawn.DTOs.DeviceTokenDTO;
import com.danielagapov.spawn.Enums.DeviceType;
import com.danielagapov.spawn.Models.DeviceToken;
import com.danielagapov.spawn.Models.User;
import com.danielagapov.spawn.Services.PushNotification.PushNotificationService;
import com.danielagapov.spawn.Services.User.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/device-tokens")
public class DeviceTokenController {

    private final PushNotificationService pushNotificationService;
    private final UserService userService;

    @Autowired
    public DeviceTokenController(PushNotificationService pushNotificationService, UserService userService) {
        this.pushNotificationService = pushNotificationService;
        this.userService = userService;
    }

    @PostMapping("/register")
    public ResponseEntity<?> registerDeviceToken(Authentication authentication, @RequestBody DeviceTokenDTO deviceTokenDTO) {
        try {
            User user = userService.getUserFromAuthentication(authentication);
            DeviceToken deviceToken = pushNotificationService.registerDeviceToken(
                    user, 
                    deviceTokenDTO.getToken(), 
                    deviceTokenDTO.getDeviceType()
            );
            
            // Send a test notification to confirm registration
            Map<String, String> data = new HashMap<>();
            data.put("type", "registration");
            pushNotificationService.sendNotificationToUser(
                    user.getId(), 
                    "Push Notifications Enabled", 
                    "You will now receive notifications from Spawn App", 
                    data
            );
            
            return ResponseEntity.ok(deviceToken);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error registering device token: " + e.getMessage());
        }
    }

    @DeleteMapping("/unregister")
    public ResponseEntity<?> unregisterDeviceToken(Authentication authentication, @RequestBody DeviceTokenDTO deviceTokenDTO) {
        try {
            User user = userService.getUserFromAuthentication(authentication);
            // Delete the device token from the repository
            pushNotificationService.unregisterDeviceToken(deviceTokenDTO.getToken());
            return ResponseEntity.ok("Device token unregistered successfully");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error unregistering device token: " + e.getMessage());
        }
    }
} 