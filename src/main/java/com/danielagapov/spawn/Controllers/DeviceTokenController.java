package com.danielagapov.spawn.Controllers;

import com.danielagapov.spawn.DTOs.DeviceTokenDTO;
import com.danielagapov.spawn.Services.PushNotification.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/device-tokens")
public class DeviceTokenController {

    private final NotificationService notificationService;

    @Autowired
    public DeviceTokenController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @PostMapping("/register")
    public ResponseEntity<Void> registerDeviceToken(@RequestBody DeviceTokenDTO deviceTokenDTO) {
        notificationService.registerDeviceToken(deviceTokenDTO);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/unregister")
    public ResponseEntity<Void> unregisterDeviceToken(@RequestBody DeviceTokenDTO deviceTokenDTO) {
        notificationService.unregisterDeviceToken(deviceTokenDTO.getToken());
        return ResponseEntity.ok().build();
    }
} 