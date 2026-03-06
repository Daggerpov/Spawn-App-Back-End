package com.danielagapov.spawn.shared.events;

import com.danielagapov.spawn.shared.util.NotificationType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Base class for notification Activities in the application
 */
public abstract class NotificationEvent {
    private final NotificationType type;
    private final Map<String, String> data;
    private final List<UUID> targetUserIds = new ArrayList<>();
    private String title;
    private String message;

    protected NotificationEvent(NotificationType type) {
        this.type = type;
        this.data = new HashMap<>();
        this.data.put("type", type.name().toLowerCase());
    }

    /**
     * Add a target user to receive this notification
     */
    protected void addTargetUser(UUID userId) {
        if (userId != null && !targetUserIds.contains(userId)) {
            targetUserIds.add(userId);
        }
    }

    /**
     * Add multiple target users to receive this notification
     */
    protected void addTargetUsers(List<UUID> userIds) {
        if (userIds != null) {
            userIds.forEach(this::addTargetUser);
        }
    }

    /**
     * Add data to the notification
     */
    protected void addData(String key, String value) {
        data.put(key, value);
    }

    /**
     * Get the type of the notification
     */
    public NotificationType getType() {
        return type;
    }

    /**
     * Get the target user IDs
     */
    public List<UUID> getTargetUserIds() {
        return targetUserIds;
    }

    /**
     * Get the notification data
     */
    public Map<String, String> getData() {
        return data;
    }

    /**
     * Get the notification title
     */
    public String getTitle() {
        return title;
    }

    /**
     * Set the notification title
     */
    protected void setTitle(String title) {
        this.title = title;
    }

    /**
     * Get the notification message
     */
    public String getMessage() {
        return message;
    }

    /**
     * Set the notification message
     */
    protected void setMessage(String message) {
        this.message = message;
    }
    
    /**
     * Find the target users for this notification.
     * This method should be implemented by subclasses to determine
     * which users should receive the notification.
     */
    public abstract void findTargetUsers();
} 