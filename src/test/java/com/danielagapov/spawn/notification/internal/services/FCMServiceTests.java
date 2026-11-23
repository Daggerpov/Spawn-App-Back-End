package com.danielagapov.spawn.notification.internal.services;

import com.danielagapov.spawn.shared.exceptions.Logger.ILogger;
import com.google.firebase.messaging.Message;
import com.google.gson.Gson;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class FCMServiceTests {
    private FCMService fcmService;

    @BeforeEach
    void setUp() {
        fcmService = new FCMService(mock(ILogger.class));
        ReflectionTestUtils.setField(fcmService, "appBundleId", "danielagapov.Spawn-App-iOS-SwiftUI");
    }

    private String messageJson() {
        Message message = fcmService.getPreconfiguredMessageToToken(
                new NotificationVO("token", "New friend request", "Sam wants to be friends", Map.of("type", "friendRequest")));
        return new Gson().toJson(message);
    }

    @Test
    void iosPushesPlayTheDefaultSound() {
        assertTrue(messageJson().contains("\"aps\":{\"sound\":\"default\""));
    }

    @Test
    void androidPushesUseTheAppChannelAndDoNotReplaceEachOther() {
        String json = messageJson();
        assertTrue(json.contains("\"channelId\":\"" + FCMService.ANDROID_CHANNEL_ID + "\""), json);
        assertTrue(json.contains("\"priority\":\"high\""), json);
        assertFalse(json.contains("\"tag\""), json);
    }
}
