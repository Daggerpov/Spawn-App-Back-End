package com.danielagapov.spawn.Services.Calendar;

import com.danielagapov.spawn.Activities.ActivityInviteNotificationActivity;
import com.danielagapov.spawn.Activities.ActivityUpdateNotificationActivity;
import com.danielagapov.spawn.Activities.ActivityParticipationNotificationActivity;
import com.danielagapov.spawn.Exceptions.Logger.ILogger;
import org.springframework.context.Activity.ActivityListener;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Activity handler for calendar-related Activities.
 * Clears calendar cache when Activities are created, updated, or deleted.
 */
@Component
public class CalendarActivityHandler {

    private final ICalendarService calendarService;
    private final ILogger logger;

    public CalendarActivityHandler(ICalendarService calendarService, ILogger logger) {
        this.calendarService = calendarService;
        this.logger = logger;
    }

    /**
     * Handler for Activity invite notifications.
     * Clears the calendar cache for all users involved.
     */
    @ActivityListener
    public void handleActivityInviteNotification(ActivityInviteNotificationActivity Activity) {
        // Get the Activity ID from the notification data
        String ActivityIdStr = Activity.getData().get("ActivityId");
        String creatorIdStr = Activity.getData().get("creatorId");
        
        if (creatorIdStr != null) {
            try {
                UUID creatorId = UUID.fromString(creatorIdStr);
                clearCacheForUser(creatorId);
            } catch (IllegalArgumentException e) {
                logger.error("Invalid creator ID format in Activity notification: " + creatorIdStr);
            }
        }
        
        // Clear cache for all target users (invitees)
        Activity.getTargetUserIds().forEach(this::clearCacheForUser);
    }

    /**
     * Handler for Activity update notifications.
     * Clears the calendar cache for all users involved.
     */
    @ActivityListener
    public void handleActivityUpdateNotification(ActivityUpdateNotificationActivity Activity) {
        // Get the Activity ID and creator ID from the notification data
        String ActivityIdStr = Activity.getData().get("ActivityId");
        String creatorIdStr = Activity.getData().get("creatorId");
        
        if (creatorIdStr != null) {
            try {
                UUID creatorId = UUID.fromString(creatorIdStr);
                clearCacheForUser(creatorId);
            } catch (IllegalArgumentException e) {
                logger.error("Invalid creator ID format in Activity notification: " + creatorIdStr);
            }
        }
        
        // Clear cache for all target users
        Activity.getTargetUserIds().forEach(this::clearCacheForUser);
    }

    /**
     * Handler for Activity participation changes.
     * Clears the calendar cache for the user whose participation status changed.
     */
    @ActivityListener
    public void handleActivityParticipationChange(ActivityParticipationNotificationActivity Activity) {
        // Get the user ID from the notification data
        String userIdStr = Activity.getData().get("userId");
        if (userIdStr != null) {
            try {
                UUID userId = UUID.fromString(userIdStr);
                clearCacheForUser(userId);
            } catch (IllegalArgumentException e) {
                logger.error("Invalid user ID format in participation notification: " + userIdStr);
            }
        }
        
        // Clear cache for Activity creator (who is the target of the notification)
        Activity.getTargetUserIds().forEach(this::clearCacheForUser);
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