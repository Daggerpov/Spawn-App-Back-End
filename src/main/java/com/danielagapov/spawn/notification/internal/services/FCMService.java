package com.danielagapov.spawn.notification.internal.services;

import com.danielagapov.spawn.shared.exceptions.Logger.ILogger;
import com.google.firebase.messaging.*;
import com.google.firebase.FirebaseApp;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.concurrent.ExecutionException;

@Service
public class FCMService {
    private final ILogger logger;
    @Value("${apns.bundle.id}")
    private String appBundleId;

    public FCMService(ILogger logger) {
        this.logger = logger;
    }

    /**
     * Sends a push notification to a specific device using its FCM token.
     *
     * @param notification the notification payload including title, message, device token, and custom data
     * @throws InterruptedException if the async FCM call is interrupted
     * @throws ExecutionException   if the FCM send fails
     */
    public void sendMessageToToken(NotificationVO notification)
            throws InterruptedException, ExecutionException {
        if (FirebaseApp.getApps().isEmpty()) {
            logger.error("FCM unavailable: Firebase Admin is not initialized");
            throw new IllegalStateException("Firebase Admin is not initialized");
        }
        try {
            FirebaseMessaging.getInstance().sendAsync(getPreconfiguredMessageToToken(notification)).get();
            logger.info("FCM accepted a notification");
        } catch (ExecutionException e) {
            if (e.getCause() instanceof FirebaseMessagingException providerError) {
                logger.error("FCM rejected a notification: errorCode=" + providerError.getErrorCode()
                        + ", messagingErrorCode=" + providerError.getMessagingErrorCode());
            } else {
                logger.error("FCM send failed before returning a provider error code");
            }
            throw e;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.warn("FCM send interrupted");
            throw e;
        }
    }

    /**
     * Builds the APNs (Apple Push Notification Service) configuration for iOS devices.
     *
     * @param topic the iOS topic, usually the app bundle ID
     * @return the APNs configuration
     */
    private ApnsConfig getApnsConfig(String topic) {
        return ApnsConfig.builder()
                .setAps(Aps.builder().setSound("default").setCategory(topic).setThreadId(topic).build()).build();
    }


    /** Must match Push.CHANNEL_ID in the Android app. */
    static final String ANDROID_CHANNEL_ID = "spawn_default";

    /**
     * Builds the Android-specific notification configuration. No tag, so each push gets its own
     * notification instead of replacing the previous one.
     *
     * @return the Android configuration
     */
    private AndroidConfig getAndroidConfig() {
        return AndroidConfig.builder()
                .setPriority(AndroidConfig.Priority.HIGH)
                .setNotification(
                        AndroidNotification.builder()
                                .setChannelId(ANDROID_CHANNEL_ID)
                                .setSound("default")
                                .build())
                .build();
    }


    /**
     * Builds the complete FCM message targeting a single device, including data and platform-specific settings.
     *
     * @param notification the notification value object
     * @return a fully built FCM Message object
     */
    Message getPreconfiguredMessageToToken(NotificationVO notification) {
        return getPreconfiguredMessageBuilder(notification)
                .setToken(notification.getDeviceToken())
                .putAllData(notification.getData())
                .build();
    }

    /**
     * Constructs the base message builder with title, body, and platform configs (iOS and Android).
     * FCM automatically applies the correct config based on the device token's platform.
     *
     * @param notificationVO the notification payload
     * @return a preconfigured Message.Builder
     */
    private Message.Builder getPreconfiguredMessageBuilder(NotificationVO notificationVO) {
        ApnsConfig apnsConfig = getApnsConfig(appBundleId);
        AndroidConfig androidConfig = getAndroidConfig();
        Notification notification = Notification.builder()
                .setTitle(notificationVO.getTitle())
                .setBody(notificationVO.getMessage())
                .build();
        return Message.builder()
                .setApnsConfig(apnsConfig)
                .setAndroidConfig(androidConfig)
                .setNotification(notification);
    }
}
