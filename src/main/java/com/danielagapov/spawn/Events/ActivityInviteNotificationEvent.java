package com.danielagapov.spawn.Events;

import com.danielagapov.spawn.Enums.NotificationType;
import com.danielagapov.spawn.Models.Activity;
import com.danielagapov.spawn.Models.User.User;

import java.util.Set;
import java.util.UUID;

/**
 * Event for when users are invited to an activity
 */
public class ActivityInviteNotificationEvent extends NotificationEvent {
    private final Set<UUID> invitedUserIds;

    public ActivityInviteNotificationEvent(User creator, Activity activity, Set<UUID> invitedUserIds) {
        super(NotificationType.Activity_INVITE);
        
        this.invitedUserIds = invitedUserIds;
        
        // Set common data
        addData("activityId", activity.getId().toString());
        addData("creatorId", creator.getId().toString());
        
        // Set title and message
        setTitle("Activity Invite");
        setMessage(creator.getUsername() + " has invited you to an activity: " + activity.getTitle());
        
        // Find who should be notified
        findTargetUsers();
    }
    
    @Override
    public void findTargetUsers() {
        // Add all invited users as targets
        invitedUserIds.forEach(this::addTargetUser);
    }
} 