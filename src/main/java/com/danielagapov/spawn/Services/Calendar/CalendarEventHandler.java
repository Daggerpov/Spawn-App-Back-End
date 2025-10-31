package com.danielagapov.spawn.Services.Calendar;

import com.danielagapov.spawn.Events.ActivityInviteNotificationEvent;
import com.danielagapov.spawn.Events.ActivityUpdateNotificationEvent;
import com.danielagapov.spawn.Events.ActivityParticipationNotificationEvent;
import com.danielagapov.spawn.Exceptions.Logger.ILogger;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Event handler for calendar-related events.
 * Clears calendar cache when activities are created, updated, or deleted.
 */
@Component
public final class CalendarEventHandler {

    private final ICalendarService calendarService;
    private final ILogger logger;

    public CalendarEventHandler(ICalendarService calendarService, ILogger logger) {
        this.calendarService = calendarService;
        this.logger = logger;
    }

    /**
     * Handler for Activity invite notifications.
     * Clears the calendar cache for all users involved.
     */
    @EventListener
    public void handleActivityInviteNotification(ActivityInviteNotificationEvent event) {
        // Get the Activity ID from the notification data
        String activityIdStr = event.getData().get("activityId");
        String creatorIdStr = event.getData().get("creatorId");
        
        if (creatorIdStr != null) {
            try {
                UUID creatorId = UUID.fromString(creatorIdStr);
                clearCacheForUser(creatorId);
            } catch (IllegalArgumentException e) {
                logger.error("Invalid creator ID format in activity notification: " + creatorIdStr);
            }
        }
        
        // Clear cache for all target users (invitees)
        event.getTargetUserIds().forEach(this::clearCacheForUser);
    }

    /**
     * Handler for Activity update notifications.
     * Clears the calendar cache for all users involved.
     */
    @EventListener
    public void handleActivityUpdateNotification(ActivityUpdateNotificationEvent event) {
        // Get the Activity ID and creator ID from the notification data
        String activityIdStr = event.getData().get("activityId");
        String creatorIdStr = event.getData().get("creatorId");
        
        if (creatorIdStr != null) {
            try {
                UUID creatorId = UUID.fromString(creatorIdStr);
                clearCacheForUser(creatorId);
            } catch (IllegalArgumentException e) {
                logger.error("Invalid creator ID format in activity notification: " + creatorIdStr);
            }
        }
        
        // Clear cache for all target users
        event.getTargetUserIds().forEach(this::clearCacheForUser);
    }

    /**
     * Handler for Activity participation changes.
     * Clears the calendar cache for the user whose participation status changed.
     */
    @EventListener
    public void handleActivityParticipationChange(ActivityParticipationNotificationEvent event) {
        // Get the user ID from the notification data
        String userIdStr = event.getData().get("userId");
        if (userIdStr != null) {
            try {
                UUID userId = UUID.fromString(userIdStr);
                clearCacheForUser(userId);
            } catch (IllegalArgumentException e) {
                logger.error("Invalid user ID format in participation notification: " + userIdStr);
            }
        }
        
        // Clear cache for activity creator (who is the target of the notification)
        event.getTargetUserIds().forEach(this::clearCacheForUser);
    }

    /**
     * Helper method to clear the calendar cache for a specific user.
     */
    private void clearCacheForUser(UUID userId) {
        try {
            calendarService.clearCalendarCache(userId);
            logger.info("Cleared calendar cache for user: " + userId);
        } catch (Exception e) {
            logger.error("Error clearing calendar cache for user " + userId + ": " + e.getMessage());
        }
    }
} 