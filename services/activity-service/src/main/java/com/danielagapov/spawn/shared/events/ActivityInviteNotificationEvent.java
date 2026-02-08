package com.danielagapov.spawn.shared.events;

import com.danielagapov.spawn.shared.util.NotificationType;
import com.danielagapov.spawn.activity.internal.domain.Activity;
import com.danielagapov.spawn.user.internal.domain.User;

import java.util.Set;
import java.util.UUID;

/**
 * Event for when users are invited to an activity
 */
public class ActivityInviteNotificationEvent extends NotificationEvent {
    private final Set<UUID> invitedUserIds;
    private final User creator;

    public ActivityInviteNotificationEvent(User creator, Activity activity, Set<UUID> invitedUserIds) {
        super(NotificationType.Activity_INVITE);
        
        this.invitedUserIds = invitedUserIds;
        this.creator = creator;
        
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
        // Add all invited users as targets, except the creator (no self-notifications)
        invitedUserIds.stream()
            .filter(userId -> !userId.equals(creator.getId()))
            .forEach(this::addTargetUser);
    }
} 