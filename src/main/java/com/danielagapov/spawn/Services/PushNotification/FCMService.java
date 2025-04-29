package com.danielagapov.spawn.Services.PushNotification;

import com.danielagapov.spawn.Exceptions.Logger.ILogger;
import com.google.firebase.messaging.*;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ExecutionException;

@Service
public class FCMService {
    private ILogger logger;
    @Value("${apns.bundle.id}")
    private String appBundleId;

    public FCMService(ILogger logger) {
        this.logger = logger;
    }

    public void sendMessageToToken(String deviceToken, String title, String message, Map<String, String> data)
            throws InterruptedException, ExecutionException {
        Message messageToSend = getPreconfiguredMessageToToken(deviceToken, title, message, data);
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        String jsonOutput = gson.toJson(messageToSend);
        String response = sendAndGetResponse(messageToSend);
        logger.info("Sent message to token. Device token: " + deviceToken + ", " + response + " msg " + jsonOutput);
    }

    private String sendAndGetResponse(Message message) throws InterruptedException, ExecutionException {
        return FirebaseMessaging.getInstance().sendAsync(message).get();
    }

    private ApnsConfig getApnsConfig(String topic) {
        return ApnsConfig.builder()
                .setAps(Aps.builder().setCategory(topic).setThreadId(topic).build()).build();
    }

    private Message getPreconfiguredMessageToToken(String deviceToken, String title, String message, Map<String, String> data) {
        return getPreconfiguredMessageBuilder(deviceToken, title, message, data).setToken(deviceToken)
                .build();
    }

    private Message.Builder getPreconfiguredMessageBuilder(String deviceToken, String title, String message, Map<String, String> data) {
        ApnsConfig apnsConfig = getApnsConfig(appBundleId);
        Notification notification = Notification.builder()
                .setTitle(title)
                .setBody(message)
                .build();
        return Message.builder()
                .setApnsConfig(apnsConfig).setNotification(notification);
    }
}
