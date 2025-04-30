package com.danielagapov.spawn.Services.PushNotification;

import com.danielagapov.spawn.Exceptions.Logger.ILogger;
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

    public void sendMessageToToken(NotificationVO notification)
            throws InterruptedException, ExecutionException {
        Message messageToSend = getPreconfiguredMessageToToken(notification);
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        String jsonOutput = gson.toJson(messageToSend);
        String response = sendAndGetResponse(messageToSend);
        logger.info("Sent message to token. Device token: " + notification.getDeviceToken() + ", " + response + " msg " + jsonOutput);
    }

    private String sendAndGetResponse(Message message) throws InterruptedException, ExecutionException {
        return FirebaseMessaging.getInstance().sendAsync(message).get();
    }

    private ApnsConfig getApnsConfig(String topic) {
        return ApnsConfig.builder()
                .setAps(Aps.builder().setCategory(topic).setThreadId(topic).build()).build();
    }

    private AndroidConfig getAndroidConfig(String topic) {
        // To fully implement later
        return AndroidConfig.builder()
                .setNotification(
                        AndroidNotification.builder()
                                .setTag(topic)
                                .build())
                .build();
    }

    private Message getPreconfiguredMessageToToken(NotificationVO notification) {
        return getPreconfiguredMessageBuilder(notification)
                .setToken(notification.getDeviceToken())
                .putAllData(notification.getData())
                .build();
    }

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
