package com.danielagapov.spawn.Events;

import com.danielagapov.spawn.Enums.NotificationType;
import com.danielagapov.spawn.Models.Event;
import com.danielagapov.spawn.Models.User.User;


/**
 * Event for when a user's participation status changes in an event
 */
public class EventParticipationNotificationEvent extends NotificationEvent {
    private final Event event;

    public static EventParticipationNotificationEvent forJoining(User participant, Event event) {
        return new EventParticipationNotificationEvent(
                participant, 
                event,
                NotificationType.EVENT_PARTICIPATION
        );
    }

    public static EventParticipationNotificationEvent forLeaving(User participant, Event event) {
        return new EventParticipationNotificationEvent(
                participant,
                event,
                NotificationType.EVENT_PARTICIPATION_REVOKED
        );
    }

    private EventParticipationNotificationEvent(User participant, Event event, NotificationType type) {
        super(type);
        
        this.event = event;
        
        // Set data
        addData("eventId", event.getId().toString());
        addData("userId", participant.getId().toString());
        
        // Set title and message based on type
        if (type == NotificationType.EVENT_PARTICIPATION) {
            setTitle("New Event Participant");
            setMessage(participant.getUsername() + " is now participating in your event: " + event.getTitle());
        } else {
            setTitle("Event Participation Revoked");
            setMessage(participant.getUsername() + " is no longer participating in your event: " + event.getTitle());
        }
        
        // Find who should be notified
        findTargetUsers();
    }
    
    @Override
    public void findTargetUsers() {
        // The event creator should be notified of participation changes
        addTargetUser(event.getCreator().getId());
    }
} 