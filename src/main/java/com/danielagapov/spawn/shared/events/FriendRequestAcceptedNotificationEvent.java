package com.danielagapov.spawn.shared.events;

import com.danielagapov.spawn.shared.util.NotificationType;
import com.danielagapov.spawn.user.internal.domain.User;

import java.util.UUID;

/**
 * Activity for when a friend request is accepted
 */
public class FriendRequestAcceptedNotificationEvent extends NotificationEvent {
    private final UUID senderId;

    public FriendRequestAcceptedNotificationEvent(User receiver, UUID senderId) {
        super(NotificationType.FRIEND_REQUEST_ACCEPTED);
        
        this.senderId = senderId;
        
        // Set data
        addData("receiverId", receiver.getId().toString());
        
        // Override the default type mapping to match what the mobile app expects
        addData("type", "friend-accepted");
        
        // Set title and message
        setTitle("Friend Request Accepted");
        setMessage(receiver.getUsername() + " has accepted your friend request");
        
        // Find who should be notified
        findTargetUsers();
    }
    
    @Override
    public void findTargetUsers() {
        // Friend request accepted notifications are simple - only the original sender gets notified
        addTargetUser(senderId);
    }
} 