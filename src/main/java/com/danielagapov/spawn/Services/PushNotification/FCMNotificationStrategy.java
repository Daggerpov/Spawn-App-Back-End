package com.danielagapov.spawn.Services.PushNotification;

import com.danielagapov.spawn.Enums.DeviceType;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Implementation of NotificationStrategy for Firebase Cloud Messaging (Android)
 * This is a placeholder implementation that can be expanded when Android support is needed
 */
@Service
public class FCMNotificationStrategy implements NotificationStrategy {

    @Override
    public void sendNotificationToDevice(String deviceToken, String title, String message, Map<String, String> data) {
        // Placeholder for FCM implementation
        System.out.println("FCM notification would be sent to: " + deviceToken);
        System.out.println("Title: " + title);
        System.out.println("Message: " + message);
        System.out.println("Data: " + data);
    }

    @Override
    public DeviceType getDeviceType() {
        return DeviceType.ANDROID;
    }
} 