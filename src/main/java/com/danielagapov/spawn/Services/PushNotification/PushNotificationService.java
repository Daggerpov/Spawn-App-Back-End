package com.danielagapov.spawn.Services.PushNotification;

import com.danielagapov.spawn.DTOs.DeviceTokenDTO;
import com.danielagapov.spawn.Enums.DeviceType;
import com.danielagapov.spawn.Models.DeviceToken;
import com.danielagapov.spawn.Models.User;
import com.danielagapov.spawn.Repositories.IDeviceTokenRepository;
import com.danielagapov.spawn.Services.User.IUserService;
import com.notnoop.apns.APNS;
import com.notnoop.apns.ApnsService;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.util.Base64;
import java.util.HashMap;
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
    private String apnsCertificate;

    @Value("${apns.certificate.password}")
    private String apnsCertificatePassword;

    @Value("${apns.production:false}")
    private boolean apnsProduction;

    private final IDeviceTokenRepository IDeviceTokenRepository;
    private final IUserService userService;
    private ApnsService apnsService;


    @Autowired
    public PushNotificationService(IDeviceTokenRepository IDeviceTokenRepository, IUserService userService) {
        this.IDeviceTokenRepository = IDeviceTokenRepository;
        this.userService = userService;
    }

    @PostConstruct
    public void initialize() {
        try {
            // Decode the Base64 encoded certificate from environment variable
            byte[] certificateBytes = Base64.getDecoder().decode(apnsCertificate);
            
            // Initialize APNS with the certificate from environment variable
            if (apnsProduction) {
                apnsService = APNS.newService()
                        .withCert(new ByteArrayInputStream(certificateBytes), apnsCertificatePassword)
                        .withProductionDestination()
                        .build();
            } else {
                apnsService = APNS.newService()
                        .withCert(new ByteArrayInputStream(certificateBytes), apnsCertificatePassword)
                        .withSandboxDestination()
                        .build();
            }
        } catch (Exception e) {
            System.err.println("Error initializing APNS service: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Register a device token for a user
     *
     * @return the created DeviceToken entity
     */
    public DeviceToken registerDeviceToken(DeviceTokenDTO deviceTokenDTO) {
        String token = deviceTokenDTO.getToken();
        User user = userService.getUserEntityById(deviceTokenDTO.getUserId());
        // Check if token already exists
        if (IDeviceTokenRepository.existsByToken(token)) {
            IDeviceTokenRepository.deleteByToken(token);
        }

        DeviceToken deviceToken = new DeviceToken();
        deviceToken.setUser(user);
        deviceToken.setToken(token);
        deviceToken.setDeviceType(deviceTokenDTO.getDeviceType());
        // Send a test notification to confirm registration
        sendTestNotification(user.getId());

        return IDeviceTokenRepository.save(deviceToken);
    }

    /**
     * Unregister a device token
     *
     * @param token the device token to unregister
     */
    public void unregisterDeviceToken(String token) {
        if (IDeviceTokenRepository.existsByToken(token)) {
            IDeviceTokenRepository.deleteByToken(token);
        }
    }

    /**
     * Send a push notification to a specific user
     *
     * @param userId     the ID of the user to send the notification to
     * @param title      the notification title
     * @param message    the notification message
     * @param customData optional custom data to include in the notification
     */
    public void sendNotificationToUser(UUID userId, String title, String message, Map<String, String> customData) {
        List<DeviceToken> deviceTokens = IDeviceTokenRepository.findByUserId(userId);

        for (DeviceToken deviceToken : deviceTokens) {
            if (deviceToken.getDeviceType() == DeviceType.IOS) {
                sendApnsNotification(deviceToken.getToken(), title, message, customData);
            }
        }
    }

    /**
     * Send push notification to multiple users
     *
     * @param userIds    the IDs of the users to send the notification to
     * @param title      the notification title
     * @param message    the notification message
     * @param customData optional custom data to include in the notification
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

    private void sendTestNotification(UUID userId) {
        // Send a test notification to confirm registration
        Map<String, String> data = new HashMap<>();
        data.put("type", "registration");
        sendNotificationToUser(
                userId,
                "Push Notifications Enabled",
                "You will now receive notifications from Spawn App",
                data
        );
    }
} 