package com.danielagapov.spawn.Controllers;

import com.danielagapov.spawn.DTOs.DeviceTokenDTO;
import com.danielagapov.spawn.Services.PushNotification.PushNotificationService;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/device-tokens")
@AllArgsConstructor
public class DeviceTokenController {
    private final PushNotificationService pushNotificationService;
    
    @PostMapping("/register")
    public ResponseEntity<?> registerDeviceToken(@RequestBody DeviceTokenDTO deviceTokenDTO) {
        try {
            pushNotificationService.registerDeviceToken(deviceTokenDTO);

            return ResponseEntity.ok(null);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error registering device token: " + e.getMessage());
        }
    }

    @DeleteMapping("/unregister")
    public ResponseEntity<?> unregisterDeviceToken(@RequestBody DeviceTokenDTO deviceTokenDTO) {
        try {
            // Delete the device token from the repository
            pushNotificationService.unregisterDeviceToken(deviceTokenDTO.getToken());
            return ResponseEntity.ok("Device token unregistered successfully");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error unregistering device token: " + e.getMessage());
        }
    }
} 