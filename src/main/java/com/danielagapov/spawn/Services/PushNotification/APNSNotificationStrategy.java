package com.danielagapov.spawn.Services.PushNotification;

import com.danielagapov.spawn.Enums.DeviceType;
import com.notnoop.apns.APNS;
import com.notnoop.apns.ApnsService;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.util.Base64;
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

    @Override
    public void sendNotificationToDevice(String deviceToken, String title, String message, Map<String, String> data) {
        try {
            String payload = APNS.newPayload()
                    .alertTitle(title)
                    .alertBody(message)
                    .sound("default")
                    .customFields(data)
                    .build();

            apnsService.push(deviceToken, payload);
        } catch (Exception e) {
            // Log error but don't interrupt the flow
            System.err.println("Error sending APNS notification: " + e.getMessage());
        }
    }

    @Override
    public DeviceType getDeviceType() {
        return DeviceType.IOS;
    }
} 