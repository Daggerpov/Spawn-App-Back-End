package com.danielagapov.spawn.notification.internal.services;

import com.danielagapov.spawn.shared.util.DeviceType;
import com.danielagapov.spawn.shared.exceptions.Logger.ILogger;
import com.eatthepath.pushy.apns.ApnsClient;
import com.eatthepath.pushy.apns.ApnsClientBuilder;
import com.eatthepath.pushy.apns.util.ApnsPayloadBuilder;
import com.eatthepath.pushy.apns.util.SimpleApnsPayloadBuilder;
import com.eatthepath.pushy.apns.util.SimpleApnsPushNotification;
import com.eatthepath.pushy.apns.util.TokenUtil;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Implementation of NotificationStrategy for Apple Push Notification Service.
 * Uses Apple's HTTP/2 provider API (via Pushy); the legacy binary API was retired by Apple in 2021.
 */
@Service
public final class APNSNotificationStrategy implements NotificationStrategy {

    @Value("${apns.certificate.path}")
    private String apnsCertificate;

    @Value("${apns.certificate.password}")
    private String apnsCertificatePassword;

    @Value("${apns.production}")
    private boolean apnsProduction;

    @Value("${apns.bundle.id}")
    private String appBundleId;

    private ApnsClient apnsClient;

    private final ILogger logger;

    @Autowired
    public APNSNotificationStrategy(ILogger logger) {
        this.logger = logger;
    }

    @PostConstruct
    public void initialize() {
        try {
            // Decode the Base64 encoded certificate from environment variable
            byte[] certificateBytes = Base64.getDecoder().decode(apnsCertificate);

            String apnsHost = apnsProduction
                    ? ApnsClientBuilder.PRODUCTION_APNS_HOST
                    : ApnsClientBuilder.DEVELOPMENT_APNS_HOST;
            logger.info("Initializing APNS client with " + (apnsProduction ? "PRODUCTION" : "DEVELOPMENT")
                    + " certificate (apns.production=" + apnsProduction + "), host: " + apnsHost);

            // Fails here if the certificate or its password is invalid
            apnsClient = new ApnsClientBuilder()
                    .setApnsServer(apnsHost)
                    .setClientCredentials(new ByteArrayInputStream(certificateBytes), apnsCertificatePassword)
                    .build();

            logger.info("APNS service initialized with bundle ID: " + appBundleId);
        } catch (Exception e) {
            logger.error("Error initializing APNS service: " + e.getMessage());
        }
    }

    @Override
    public void sendNotificationToDevice(String deviceToken, String title, String message, Map<String, String> data) {
        try {
            if (apnsClient == null) {
                logger.error("APNS client is not initialized. Cannot send APNS notification.");
                return;
            }

            logger.info("Preparing to send APNS notification to device: " + deviceToken);

            // Validate bundle ID before sending
            if (appBundleId == null || appBundleId.trim().isEmpty()) {
                logger.error("Bundle ID is not configured properly. Cannot send APNS notification.");
                return;
            }

            // Make sure data map is not null
            Map<String, String> notificationData = data != null ? new HashMap<>(data) : new HashMap<>();

            // Add the bundle ID to the data (will be included in the payload)
            notificationData.put("topic", appBundleId);

            // Create the payload with proper structure
            String payload = constructPayload(title, message, notificationData);
            logger.info("APNS payload created: " + payload);

            SimpleApnsPushNotification notification = new SimpleApnsPushNotification(
                    TokenUtil.sanitizeTokenString(deviceToken), appBundleId, payload);

            apnsClient.sendNotification(notification).whenComplete((response, cause) -> {
                if (cause != null) {
                    logger.error("Error sending APNS notification to device " + deviceToken + ": " + cause.getMessage());
                } else if (response.isAccepted()) {
                    logger.info("APNS notification accepted, apns-id: " + response.getApnsId()
                            + ", Device Token: " + deviceToken);
                } else {
                    String reason = response.getRejectionReason().orElse("unknown");
                    logger.warn("APNS notification rejected for device " + deviceToken + ": " + reason
                            + response.getTokenInvalidationTimestamp()
                                    .map(ts -> ", token invalid since " + ts)
                                    .orElse(""));
                    // TODO: Remove tokens rejected as Unregistered/BadDeviceToken from the database
                }
            });
        } catch (Exception e) {
            // Log error but don't interrupt the flow
            logger.error("Error sending APNS notification: " + e.getMessage());
        }
    }

    private String constructPayload(String title, String message, Map<String, String> data) {
        try {
            // Start with a basic payload builder
            ApnsPayloadBuilder payloadBuilder = new SimpleApnsPayloadBuilder()
                .setAlertTitle(title)
                .setAlertBody(message)
                .setSound("default");

            // Set badge number if provided
            if (data != null && data.containsKey("badge")) {
                try {
                    int badgeNumber = Integer.parseInt(data.get("badge"));
                    payloadBuilder.setBadgeNumber(badgeNumber);
                } catch (NumberFormatException e) {
                    logger.warn("Invalid badge number format in data: " + data.get("badge"));
                }

                // Remove badge from data to avoid duplication in custom fields
                data.remove("badge");
            }

            // Add content-available=1 for background updates if specified
            if (data != null && data.containsKey("content-available") &&
                "1".equals(data.get("content-available"))) {
                payloadBuilder.setContentAvailable(true);

                // Remove content-available from data to avoid duplication
                data.remove("content-available");
            }

            // Add category if provided
            if (data != null && data.containsKey("category")) {
                payloadBuilder.setCategoryName(data.get("category"));
                data.remove("category");
            }

            // Add custom data fields
            if (data != null) {
                data.forEach(payloadBuilder::addCustomProperty);
            }

            // Build the final payload
            return payloadBuilder.build();
        } catch (Exception e) {
            logger.error("Error constructing APNS payload: " + e.getMessage());

            // Fall back to simple payload if there was an error
            return new SimpleApnsPayloadBuilder()
                .setAlertTitle(title)
                .setAlertBody(message)
                .setSound("default")
                .build();
        }
    }

    @Override
    public DeviceType getDeviceType() {
        return DeviceType.IOS;
    }

    /**
     * Cleanup method to properly close APNS client and release resources.
     * This prevents memory leaks by ensuring connections are properly closed.
     */
    @PreDestroy
    public void cleanup() {
        if (apnsClient != null) {
            try {
                logger.info("Shutting down APNS client and closing connections");
                apnsClient.close().get(10, TimeUnit.SECONDS);
                logger.info("APNS client successfully shut down");
            } catch (Exception e) {
                logger.error("Error shutting down APNS client: " + e.getMessage());
            }
        }
    }
}
