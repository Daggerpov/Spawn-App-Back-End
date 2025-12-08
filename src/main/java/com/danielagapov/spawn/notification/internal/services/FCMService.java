package com.danielagapov.spawn.notification.internal.services;

import com.danielagapov.spawn.shared.exceptions.ILogger;
import com.google.firebase.messaging.*;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
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
        Message messageToSend = getPreconfiguredMessageToToken(notification);
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        String jsonOutput = gson.toJson(messageToSend);
        String response = sendAndGetResponse(messageToSend);
        logger.info("Sent message to token. Device token: " + notification.getDeviceToken() + ", " + response + " msg " + jsonOutput);
    }


    /**
     * Sends the message asynchronously using FirebaseMessaging and returns the message ID.
     *
     * @param message the FCM message to send
     * @return the message ID returned by FCM
     */
    private String sendAndGetResponse(Message message) throws InterruptedException, ExecutionException {
        return FirebaseMessaging.getInstance().sendAsync(message).get();
    }

    /**
     * Builds the APNs (Apple Push Notification Service) configuration for iOS devices.
     *
     * @param topic the iOS topic, usually the app bundle ID
     * @return the APNs configuration
     */
    private ApnsConfig getApnsConfig(String topic) {
        return ApnsConfig.builder()
                .setAps(Aps.builder().setCategory(topic).setThreadId(topic).build()).build();
    }


    /**
     * Builds the Android-specific notification configuration.
     *
     * @param topic a tag or category to associate with the Android notification
     * @return the Android configuration
     */
    private AndroidConfig getAndroidConfig(String topic) {
        // To fully implement later -- e.g. extending this with priority, TTL, sound, etc., as needed
        return AndroidConfig.builder()
                .setNotification(
                        AndroidNotification.builder()
                                .setTag(topic)
                                .build())
                .build();
    }


    /**
     * Builds the complete FCM message targeting a single device, including data and platform-specific settings.
     *
     * @param notification the notification value object
     * @return a fully built FCM Message object
     */
    private Message getPreconfiguredMessageToToken(NotificationVO notification) {
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
        AndroidConfig androidConfig = getAndroidConfig("android");
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
