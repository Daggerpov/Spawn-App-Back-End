package com.danielagapov.spawn.Services.PushNotification;

import com.danielagapov.spawn.Enums.DeviceType;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Strategy interface for different notification providers
 */
public interface NotificationStrategy {
    /**
     * Send notification to a specific device
     *
     * @param deviceToken the token for the device to send to
     * @param title       notification title
     * @param message     notification message
     * @param data        additional data to include
     */
    void sendNotificationToDevice(String deviceToken, String title, String message, Map<String, String> data);
    
    /**
     * Get the device type that this strategy supports
     *
     * @return the device type
     */
    DeviceType getDeviceType();
} 