package com.danielagapov.spawn.notification.internal.services;

import com.danielagapov.spawn.shared.exceptions.Logger.ILogger;
import com.google.api.core.ApiFutures;
import com.google.firebase.ErrorCode;
import com.google.firebase.FirebaseApp;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.MessagingErrorCode;
import com.google.gson.Gson;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class FCMServiceTests {
    private ILogger logger;
    private FCMService fcmService;

    @BeforeEach
    void setUp() {
        logger = mock(ILogger.class);
        fcmService = new FCMService(logger);
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

    @Test
    void logsSafeProviderCodesWhenFirebaseRejectsASend() {
        var client = mock(FirebaseMessaging.class);
        var failure = mock(FirebaseMessagingException.class);
        when(failure.getErrorCode()).thenReturn(ErrorCode.PERMISSION_DENIED);
        when(failure.getMessagingErrorCode()).thenReturn(MessagingErrorCode.SENDER_ID_MISMATCH);
        when(failure.getMessage()).thenReturn("provider details contain private data");
        when(client.sendAsync(any(Message.class))).thenReturn(ApiFutures.immediateFailedFuture(failure));

        try (var apps = mockStatic(FirebaseApp.class); var messaging = mockStatic(FirebaseMessaging.class)) {
            apps.when(FirebaseApp::getApps).thenReturn(List.of(mock(FirebaseApp.class)));
            messaging.when(FirebaseMessaging::getInstance).thenReturn(client);
            var notification = new NotificationVO("private token", "title", "private body", Map.of());

            assertThrows(ExecutionException.class, () -> fcmService.sendMessageToToken(notification));
            verify(logger).error("FCM rejected a notification: errorCode=PERMISSION_DENIED, messagingErrorCode=SENDER_ID_MISMATCH");
            verifyNoMoreInteractions(logger);
        }
    }
}
