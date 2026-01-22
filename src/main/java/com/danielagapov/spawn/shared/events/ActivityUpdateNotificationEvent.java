package com.danielagapov.spawn.shared.events;

import com.danielagapov.spawn.shared.util.NotificationType;

import java.util.List;
import java.util.UUID;

/**
 * Event for when an activity is updated.
 * 
 * Part of Phase 3: Shared Data Resolution - this event now receives
 * participant IDs directly instead of using a repository reference,
 * which maintains proper module boundaries.
 */
public class ActivityUpdateNotificationEvent extends NotificationEvent {
    private final UUID creatorId;
    private final String creatorUsername;
    private final UUID activityId;
    private final String activityTitle;
    private final List<UUID> participantIds;

    /**
     * Create a notification event for an activity update.
     * 
     * @param creatorId ID of the activity creator
     * @param creatorUsername Username of the creator (for notification message)
     * @param activityId ID of the activity
     * @param activityTitle Title of the activity (for notification message)
     * @param participantIds List of participant user IDs (already filtered by participating status)
     */
    public ActivityUpdateNotificationEvent(UUID creatorId, String creatorUsername, UUID activityId, 
                                           String activityTitle, List<UUID> participantIds) {
        super(NotificationType.Activity_UPDATE);
        
        this.creatorId = creatorId;
        this.creatorUsername = creatorUsername;
        this.activityId = activityId;
        this.activityTitle = activityTitle;
        this.participantIds = participantIds;
        
        // Set data
        addData("activityId", activityId.toString());
        addData("creatorId", creatorId.toString());
        
        // Set title and message
        setTitle("Activity Update");
        setMessage(creatorUsername + " has updated an activity that you're attending: " + activityTitle);
        
        // Find who should be notified
        findTargetUsers();
    }
    
    @Override
    public void findTargetUsers() {
        // Get all users participating in the activity and notify them
        for (UUID participantId : participantIds) {
            // Don't notify the creator about their own update
            if (!participantId.equals(creatorId)) {
                addTargetUser(participantId);
            }
        }
    }
} 