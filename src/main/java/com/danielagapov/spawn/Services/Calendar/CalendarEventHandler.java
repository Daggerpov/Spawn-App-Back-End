package com.danielagapov.spawn.Services.Calendar;

import com.danielagapov.spawn.Events.EventInviteNotificationEvent;
import com.danielagapov.spawn.Events.EventUpdateNotificationEvent;
import com.danielagapov.spawn.Events.EventParticipationNotificationEvent;
import com.danielagapov.spawn.Exceptions.Logger.ILogger;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Event handler for calendar-related events.
 * Clears calendar cache when events are created, updated, or deleted.
 */
@Component
public class CalendarEventHandler {

    private final ICalendarService calendarService;
    private final ILogger logger;

    public CalendarEventHandler(ICalendarService calendarService, ILogger logger) {
        this.calendarService = calendarService;
        this.logger = logger;
    }

    /**
     * Handler for event invite notifications.
     * Clears the calendar cache for all users involved.
     */
    @EventListener
    public void handleEventInviteNotification(EventInviteNotificationEvent event) {
        // Get the event ID from the notification data
        String eventIdStr = event.getData().get("eventId");
        String creatorIdStr = event.getData().get("creatorId");
        
        if (creatorIdStr != null) {
            try {
                UUID creatorId = UUID.fromString(creatorIdStr);
                clearCacheForUser(creatorId);
            } catch (IllegalArgumentException e) {
                logger.error("Invalid creator ID format in event notification: " + creatorIdStr);
            }
        }
        
        // Clear cache for all target users (invitees)
        event.getTargetUserIds().forEach(this::clearCacheForUser);
    }

    /**
     * Handler for event update notifications.
     * Clears the calendar cache for all users involved.
     */
    @EventListener
    public void handleEventUpdateNotification(EventUpdateNotificationEvent event) {
        // Get the event ID and creator ID from the notification data
        String eventIdStr = event.getData().get("eventId");
        String creatorIdStr = event.getData().get("creatorId");
        
        if (creatorIdStr != null) {
            try {
                UUID creatorId = UUID.fromString(creatorIdStr);
                clearCacheForUser(creatorId);
            } catch (IllegalArgumentException e) {
                logger.error("Invalid creator ID format in event notification: " + creatorIdStr);
            }
        }
        
        // Clear cache for all target users
        event.getTargetUserIds().forEach(this::clearCacheForUser);
    }

    /**
     * Handler for event participation changes.
     * Clears the calendar cache for the user whose participation status changed.
     */
    @EventListener
    public void handleEventParticipationChange(EventParticipationNotificationEvent event) {
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
        
        // Clear cache for event creator (who is the target of the notification)
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