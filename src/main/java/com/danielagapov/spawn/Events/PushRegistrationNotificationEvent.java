package com.danielagapov.spawn.Activities;

import com.danielagapov.spawn.Enums.NotificationType;
import com.danielagapov.spawn.Models.User.User;

/**
 * Activity for when a device is registered for push notifications
 */
public class PushRegistrationNotificationActivity extends NotificationActivity {
    private final User user;

    public PushRegistrationNotificationActivity(User user) {
        super(NotificationType.PUSH_REGISTRATION);
        
        this.user = user;
        
        // Set data
        addData("type", "registration");
        
        // Set title and message
        setTitle("Push Notifications Enabled");
        setMessage("You will now receive notifications from Spawn App");
        
        // Find who should be notified
        findTargetUsers();
    }
    
    @Override
    public void findTargetUsers() {
        // Only notify the user who registered the device
        addTargetUser(user.getId());
    }
} 