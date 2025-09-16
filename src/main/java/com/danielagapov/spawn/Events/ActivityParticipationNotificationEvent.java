package com.danielagapov.spawn.Events;

import com.danielagapov.spawn.Enums.NotificationType;
import com.danielagapov.spawn.Models.Activity;
import com.danielagapov.spawn.Models.User.User;


/**
 * Event for when a user's participation status changes in an activity
 */
public class ActivityParticipationNotificationEvent extends NotificationEvent {
    private final Activity activity;

    private ActivityParticipationNotificationEvent(User participant, Activity activity, NotificationType type) {
        super(type);

        this.activity = activity;

        // Set data
        addData("activityId", activity.getId().toString());
        addData("userId", participant.getId().toString());

        // Set title and message based on type
        if (type == NotificationType.Activity_PARTICIPATION) {
            setTitle("New Activity Participant");
            setMessage(participant.getUsername() + " is now participating in your activity: " + activity.getTitle());
        } else {
            setTitle("Someone Left");
            setMessage(participant.getUsername() + " is no longer participating in your activity: " + activity.getTitle());
        }

        // Find who should be notified
        findTargetUsers();
    }

    public static ActivityParticipationNotificationEvent forJoining(User participant, Activity activity) {
        return new ActivityParticipationNotificationEvent(
                participant,
                activity,
                NotificationType.Activity_PARTICIPATION
        );
    }

    public static ActivityParticipationNotificationEvent forLeaving(User participant, Activity activity) {
        return new ActivityParticipationNotificationEvent(
                participant,
                activity,
                NotificationType.Activity_PARTICIPATION_REVOKED
        );
    }
    
    @Override
    public void findTargetUsers() {
        // The activity creator should be notified of participation changes
        addTargetUser(activity.getCreator().getId());
    }
} 