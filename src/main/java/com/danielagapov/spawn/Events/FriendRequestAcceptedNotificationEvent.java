package com.danielagapov.spawn.Activities;

import com.danielagapov.spawn.Enums.NotificationType;
import com.danielagapov.spawn.Models.User.User;

import java.util.UUID;

/**
 * Activity for when a friend request is accepted
 */
public class FriendRequestAcceptedNotificationActivity extends NotificationActivity {
    private final UUID senderId;

    public FriendRequestAcceptedNotificationActivity(User receiver, UUID senderId) {
        super(NotificationType.FRIEND_REQUEST_ACCEPTED);
        
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