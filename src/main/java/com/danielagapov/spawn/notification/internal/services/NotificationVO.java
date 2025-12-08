package com.danielagapov.spawn.notification.internal.services;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Map;

@Getter
@AllArgsConstructor
public class NotificationVO {
    private String deviceToken;
    private String title;
    private String message;
    private Map<String, String> data;
}
