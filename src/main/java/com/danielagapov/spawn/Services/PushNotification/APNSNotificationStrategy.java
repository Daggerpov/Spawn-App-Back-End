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
import java.util.Collection;
import java.util.Map;

/**
 * Implementation of NotificationStrategy for Apple Push Notification Service
 */
@Service
public class APNSNotificationStrategy implements NotificationStrategy {

    @Value("${apns.certificate.path}")
    private String apnsCertificate;

    @Value("${apns.certificate.password}")
    private String apnsCertificatePassword;

    @Value("${apns.production:false}")
    private boolean apnsProduction;

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
            
            logger.info("Initializing APNS service");
            
            // Initialize APNS with the certificate from environment variable
            if (apnsProduction) {
                logger.info("Using production APNS destination");
                apnsService = APNS.newService()
                        .withCert(new ByteArrayInputStream(certificateBytes), apnsCertificatePassword)
                        .withProductionDestination()
                        .build();
            } else {
                logger.info("Using sandbox APNS destination");
                apnsService = APNS.newService()
                        .withCert(new ByteArrayInputStream(certificateBytes), apnsCertificatePassword)
                        .withSandboxDestination()
                        .build();
            }
            
            logger.info("APNS service successfully initialized");
        } catch (Exception e) {
            logger.error("Error initializing APNS service: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public void sendNotificationToDevice(String deviceToken, String title, String message, Map<String, String> data) {
        try {
            logger.info("Preparing to send APNS notification to device: " + deviceToken);
            
            String payload = APNS.newPayload()
                    .alertTitle(title)
                    .alertBody(message)
                    .sound("default")
                    .customFields(data)
                    .build();
            
            logger.info("APNS payload created: " + payload);

            ApnsNotification notification = apnsService.push(deviceToken, payload);
            
            if (notification  != null) {
                logger.info("APNS notification sent with ID: " + notification.getIdentifier() +
                                ", Message ID: " + notification.getIdentifier() +
                                ", Expiry: " + notification.getExpiry() +
                                ", Device Token: " + Arrays.toString(notification.getDeviceToken()) +
                                ", Payload: " + Arrays.toString(notification.getPayload()));
            } else {
                logger.info("No APNS response received or empty response");
            }
        } catch (Exception e) {
            // Log error but don't interrupt the flow
            logger.error("Error sending APNS notification: " + e.getMessage());
        }
    }

    @Override
    public DeviceType getDeviceType() {
        return DeviceType.IOS;
    }
} 