package com.danielagapov.spawn.Events;

import com.danielagapov.spawn.Enums.NotificationType;
import com.danielagapov.spawn.Models.Event;
import com.danielagapov.spawn.Models.User;

import java.util.Set;
import java.util.UUID;

/**
 * Event for when users are invited to an event
 */
public class EventInviteNotificationEvent extends NotificationEvent {
    private final Set<UUID> invitedUserIds;

    public EventInviteNotificationEvent(User creator, Event event, Set<UUID> invitedUserIds) {
        super(NotificationType.EVENT_INVITE);
        
        this.invitedUserIds = invitedUserIds;
        
        // Set common data
        addData("eventId", event.getId().toString());
        addData("creatorId", creator.getId().toString());
        
        // Set title and message
        setTitle("Event Invite");
        setMessage(creator.getUsername() + " has invited you to an event: " + event.getTitle());
        
        // Find who should be notified
        findTargetUsers();
    }
    
    @Override
    public void findTargetUsers() {
        // Add all invited users as targets
        invitedUserIds.forEach(this::addTargetUser);
    }
} 