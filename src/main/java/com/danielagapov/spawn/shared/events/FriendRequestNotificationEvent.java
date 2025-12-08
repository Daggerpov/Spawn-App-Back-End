package com.danielagapov.spawn.shared.events;

import com.danielagapov.spawn.shared.util.NotificationType;
import com.danielagapov.spawn.user.internal.domain.User;

import java.util.UUID;

/**
 * Activity for when a friend request is sent
 */
public class FriendRequestNotificationEvent extends NotificationEvent {
    private final UUID receiverId;

    public FriendRequestNotificationEvent(User sender, UUID receiverId) {
        super(NotificationType.FRIEND_REQUEST);
        
        this.receiverId = receiverId;
        
        // Set data
        addData("senderId", sender.getId().toString());
        
        // Override the default type mapping to match what the mobile app expects
        addData("type", "friend-request");
        
        // Set title and message
        setTitle("New Friend Request");
        setMessage(sender.getUsername() + " sent you a friend request");
        
        // Find who should be notified
        findTargetUsers();
    }
    
    @Override
    public void findTargetUsers() {
        // Friend request notifications are simple - only the receiver gets notified
        addTargetUser(receiverId);
    }
} 