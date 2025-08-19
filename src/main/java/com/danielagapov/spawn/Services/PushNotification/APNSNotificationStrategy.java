package com.danielagapov.spawn.Services.PushNotification;

import com.danielagapov.spawn.Enums.DeviceType;
import com.danielagapov.spawn.Exceptions.Logger.ILogger;
import com.notnoop.apns.APNS;
import com.notnoop.apns.ApnsNotification;
import com.notnoop.apns.ApnsService;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.util.Arrays;
import java.util.Base64;

import java.util.Map;
import java.util.Date;
import java.util.HashMap;

/**
 * Implementation of NotificationStrategy for Apple Push Notification Service
 */
@Service
public class APNSNotificationStrategy implements NotificationStrategy {

    @Value("${apns.certificate.path}")
    private String apnsCertificate;

    @Value("${apns.certificate.password}")
    private String apnsCertificatePassword;

    @Value("${apns.production}")
    private boolean apnsProduction;
    
    @Value("${apns.bundle.id}")
    private String appBundleId;

    private ApnsService apnsService;
    
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
            
            if (apnsProduction) {
                logger.info("Initializing APNS service with PRODUCTION certificate (apns.production=true)");
                logger.info("Using production APNS destination");
                apnsService = APNS.newService()
                        .withCert(new ByteArrayInputStream(certificateBytes), apnsCertificatePassword)
                        .withProductionDestination()
                        .build();
            } else {
                logger.info("Initializing APNS service with DEVELOPMENT certificate (apns.production=false)");
                logger.info("Using sandbox APNS destination");
                apnsService = APNS.newService()
                        .withCert(new ByteArrayInputStream(certificateBytes), apnsCertificatePassword)
                        .withSandboxDestination()
                        .build();
            }
            
            // Log bundle ID being used
            logger.info("APNS service initialized with bundle ID: " + appBundleId);
            
            // Validate certificate is working by checking connection
            logger.info("Validating APNS certificate and connection");
            boolean isValid = validateApnsConnection();
            if (isValid) {
                logger.info("APNS service successfully initialized and validated");
            } else {
                logger.error("APNS certificate validation failed. Notifications may not be delivered.");
            }
            
            // Check for device tokens that need to be removed (only in production to avoid dev warnings)
            if (apnsProduction) {
                logger.info("Checking for invalid device tokens in production mode");
                checkForInvalidDeviceTokens();
            } else {
                logger.info("Skipping invalid device token check in development mode to avoid feedback service warnings");
            }
            
        } catch (Exception e) {
            logger.error("Error initializing APNS service: " + e.getMessage());
        }
    }

    private boolean validateApnsConnection() {
        // Skip feedback service validation in non-production environments to avoid warnings
        if (!apnsProduction) {
            logger.info("Skipping APNS feedback service validation in development mode");
            return true; // Consider valid in development mode
        }
        
        try {
            // Only attempt to access the feedback service in production
            apnsService.getInactiveDevices();
            return true;
        } catch (Exception e) {
            logger.error("APNS connection validation failed in production: " + e.getMessage());
            return false;
        }
    }
    
    private void checkForInvalidDeviceTokens() {
        // Only run this in production to avoid feedback service connection warnings
        if (!apnsProduction) {
            logger.info("Skipping invalid device token check - not in production mode");
            return;
        }
        
        try {
            // Get map of tokens and timestamps when they became invalid
            Map<String, Date> inactiveDevices;
            try {
                inactiveDevices = apnsService.getInactiveDevices();
            } catch (Exception e) {
                logger.warn("Could not get inactive devices in production: " + e.getMessage());
                return;
            }
            
            if (inactiveDevices.isEmpty()) {
                logger.info("No invalid device tokens found");
                return;
            }
            
            logger.info("Found " + inactiveDevices.size() + " invalid device tokens");
            
            // Log each invalid token for potential removal from database
            for (Map.Entry<String, Date> entry : inactiveDevices.entrySet()) {
                logger.info("Invalid token: " + entry.getKey() + ", inactive since: " + entry.getValue());
                // TODO: Add code to remove invalid tokens from database
            }
        } catch (Exception e) {
            logger.error("Error checking for invalid device tokens: " + e.getMessage());
        }
    }

    @Override
    public void sendNotificationToDevice(String deviceToken, String title, String message, Map<String, String> data) {
        try {
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

            // Send the notification with the standard push method
            ApnsNotification notification = apnsService.push(deviceToken, payload);
            
            if (notification != null) {
                logger.info("APNS notification sent with ID: " + notification.getIdentifier() +
                                ", Message ID: " + notification.getIdentifier() +
                                ", Expiry: " + notification.getExpiry() +
                                ", Device Token: " + Arrays.toString(notification.getDeviceToken()) +
                                ", Payload: " + Arrays.toString(notification.getPayload()));
            } else {
                logger.warn("No APNS response received or empty response - this might indicate delivery issues");
            }
        } catch (Exception e) {
            // Log error but don't interrupt the flow
            logger.error("Error sending APNS notification: " + e.getMessage());
        }
    }

    private String constructPayload(String title, String message, Map<String, String> data) {
        try {
            // Start with a basic payload builder
            var payloadBuilder = APNS.newPayload()
                .alertTitle(title)
                .alertBody(message)
                .sound("default");
            
            // Set badge number if provided
            if (data != null && data.containsKey("badge")) {
                try {
                    int badgeNumber = Integer.parseInt(data.get("badge"));
                    payloadBuilder.badge(badgeNumber);
                } catch (NumberFormatException e) {
                    logger.warn("Invalid badge number format in data: " + data.get("badge"));
                }
                
                // Remove badge from data to avoid duplication in custom fields
                data.remove("badge");
            }
            
            // Add content-available=1 for background updates if specified
            if (data != null && data.containsKey("content-available") && 
                "1".equals(data.get("content-available"))) {
                payloadBuilder.instantDeliveryOrSilentNotification();
                
                // Remove content-available from data to avoid duplication
                data.remove("content-available");
            }
            
            // Add category if provided
            if (data != null && data.containsKey("category")) {
                payloadBuilder.category(data.get("category"));
                data.remove("category");
            }
            
            // Add custom data fields
            if (data != null && !data.isEmpty()) {
                payloadBuilder.customFields(data);
            }
            
            // Build the final payload
            return payloadBuilder.build();
        } catch (Exception e) {
            logger.error("Error constructing APNS payload: " + e.getMessage());
            
            // Fall back to simple payload if there was an error
            return APNS.newPayload()
                .alertTitle(title)
                .alertBody(message)
                .sound("default")
                .build();
        }
    }

    @Override
    public DeviceType getDeviceType() {
        return DeviceType.IOS;
    }
} 