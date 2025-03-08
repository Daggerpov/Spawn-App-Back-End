package com.danielagapov.spawn.Services.PushNotification;

import com.danielagapov.spawn.Enums.DeviceType;
import com.danielagapov.spawn.Models.DeviceToken;
import com.danielagapov.spawn.Models.User;
import com.danielagapov.spawn.Repositories.DeviceTokenRepository;
import com.notnoop.apns.APNS;
import com.notnoop.apns.ApnsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Service for sending push notifications to user devices.
 * Supports Apple Push Notification Service (APNS) for iOS.
 */
@Service
public class PushNotificationService {

    @Value("${apns.certificate.path}")
    private String apnsCertificatePath;

    @Value("${apns.certificate.password}")
    private String apnsCertificatePassword;

    @Value("${apns.production:false}")
    private boolean apnsProduction;

    private final DeviceTokenRepository deviceTokenRepository;
    private ApnsService apnsService;

    @Autowired
    public PushNotificationService(DeviceTokenRepository deviceTokenRepository) {
        this.deviceTokenRepository = deviceTokenRepository;
    }

    @PostConstruct
    public void initialize() {
        // Initialize APNS
        if (apnsProduction) {
            apnsService = APNS.newService()
                    .withCert(apnsCertificatePath, apnsCertificatePassword)
                    .withProductionDestination()
                    .build();
        } else {
            apnsService = APNS.newService()
                    .withCert(apnsCertificatePath, apnsCertificatePassword)
                    .withSandboxDestination()
                    .build();
        }
    }

    /**
     * Register a device token for a user
     *
     * @param user       the user to register the token for
     * @param token      the device token
     * @param deviceType the type of device (IOS, WEB)
     * @return the created DeviceToken entity
     */
    public DeviceToken registerDeviceToken(User user, String token, DeviceType deviceType) {
        // Check if token already exists
        if (deviceTokenRepository.existsByToken(token)) {
            deviceTokenRepository.deleteByToken(token);
        }

        DeviceToken deviceToken = new DeviceToken();
        deviceToken.setUser(user);
        deviceToken.setToken(token);
        deviceToken.setDeviceType(deviceType);
        return deviceTokenRepository.save(deviceToken);
    }

    /**
     * Unregister a device token
     *
     * @param token the device token to unregister
     */
    public void unregisterDeviceToken(String token) {
        if (deviceTokenRepository.existsByToken(token)) {
            deviceTokenRepository.deleteByToken(token);
        }
    }

    /**
     * Send a push notification to a specific user
     *
     * @param userId      the ID of the user to send the notification to
     * @param title       the notification title
     * @param message     the notification message
     * @param customData  optional custom data to include in the notification
     */
    public void sendNotificationToUser(UUID userId, String title, String message, Map<String, String> customData) {
        List<DeviceToken> deviceTokens = deviceTokenRepository.findByUserId(userId);
        
        for (DeviceToken deviceToken : deviceTokens) {
            if (deviceToken.getDeviceType() == DeviceType.IOS) {
                sendApnsNotification(deviceToken.getToken(), title, message, customData);
            }
        }
    }

    /**
     * Send push notification to multiple users
     *
     * @param userIds     the IDs of the users to send the notification to
     * @param title       the notification title
     * @param message     the notification message
     * @param customData  optional custom data to include in the notification
     */
    public void sendNotificationToUsers(List<UUID> userIds, String title, String message, Map<String, String> customData) {
        for (UUID userId : userIds) {
            sendNotificationToUser(userId, title, message, customData);
        }
    }

    /**
     * Send a notification via Apple Push Notification Service (APNS)
     *
     * @param token      the device token
     * @param title      the notification title
     * @param message    the notification message
     * @param customData optional custom data
     */
    private void sendApnsNotification(String token, String title, String message, Map<String, String> customData) {
        try {
            String payload = APNS.newPayload()
                    .alertTitle(title)
                    .alertBody(message)
                    .sound("default")
                    .customFields(customData)
                    .build();

            apnsService.push(token, payload);
        } catch (Exception e) {
            // Log error but continue processing other tokens
            System.err.println("Error sending APNS notification: " + e.getMessage());
        }
    }
} 