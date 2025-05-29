package com.danielagapov.spawn.Events;

import com.danielagapov.spawn.Enums.NotificationType;
import com.danielagapov.spawn.Enums.ParticipationStatus;
import com.danielagapov.spawn.Models.Activity;
import com.danielagapov.spawn.Models.ActivityUser;
import com.danielagapov.spawn.Models.User.User;
import com.danielagapov.spawn.Repositories.IActivityUserRepository;

import java.util.List;

/**
 * Activity for when an activity is updated
 */
public class ActivityUpdateNotificationEvent extends NotificationEvent {
    private final User creator;
    private final Activity activity;
    private final IActivityUserRepository activityUserRepository;

    public ActivityUpdateNotificationEvent(User creator, Activity activity, IActivityUserRepository activityUserRepository) {
        super(NotificationType.Activity_UPDATE);
        
        this.creator = creator;
        this.activity = activity;
        this.activityUserRepository = activityUserRepository;
        
        // Set data
        addData("activityId", activity.getId().toString());
        addData("creatorId", creator.getId().toString());
        
        // Set title and message
        setTitle("Activity Update");
        setMessage(creator.getUsername() + " has updated an activity that you're attending: " + activity.getTitle());
        
        // Find who should be notified
        findTargetUsers();
    }
    
    @Override
    public void findTargetUsers() {
        // Get all users participating in the activity and notify them
        List<ActivityUser> participants = activityUserRepository.findActivitiesByActivity_IdAndStatus(
                activity.getId(), ParticipationStatus.participating);
                
        for (ActivityUser participant : participants) {
            // Don't notify the creator about their own update
            if (!participant.getUser().getId().equals(creator.getId())) {
                addTargetUser(participant.getUser().getId());
            }
        }
    }
} 