package com.danielagapov.spawn.Events;

import com.danielagapov.spawn.Enums.NotificationType;
import com.danielagapov.spawn.Models.User;

import java.util.UUID;

/**
 * Event for when a friend request is accepted
 */
public class FriendRequestAcceptedNotificationEvent extends NotificationEvent {
    private final User receiver;
    private final UUID senderId;

    public FriendRequestAcceptedNotificationEvent(User receiver, UUID senderId) {
        super(NotificationType.FRIEND_REQUEST_ACCEPTED);
        
        this.receiver = receiver;
        this.senderId = senderId;
        
        // Set data
        addData("receiverId", receiver.getId().toString());
        
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