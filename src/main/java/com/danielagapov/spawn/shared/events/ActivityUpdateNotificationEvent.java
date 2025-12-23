package com.danielagapov.spawn.shared.events;

import com.danielagapov.spawn.shared.util.NotificationType;
import com.danielagapov.spawn.shared.util.ParticipationStatus;
import com.danielagapov.spawn.activity.internal.domain.Activity;
import com.danielagapov.spawn.activity.internal.domain.ActivityUser;
import com.danielagapov.spawn.user.internal.domain.User;
import com.danielagapov.spawn.activity.internal.repositories.IActivityUserRepository;

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