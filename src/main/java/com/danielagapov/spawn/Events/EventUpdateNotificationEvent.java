package com.danielagapov.spawn.Events;

import com.danielagapov.spawn.Enums.NotificationType;
import com.danielagapov.spawn.Enums.ParticipationStatus;
import com.danielagapov.spawn.Models.Event;
import com.danielagapov.spawn.Models.EventUser;
import com.danielagapov.spawn.Models.User;
import com.danielagapov.spawn.Repositories.IEventUserRepository;

import java.util.List;
import java.util.UUID;

/**
 * Event for when an event is updated
 */
public class EventUpdateNotificationEvent extends NotificationEvent {
    private final User creator;
    private final Event event;
    private final IEventUserRepository eventUserRepository;

    public EventUpdateNotificationEvent(User creator, Event event, IEventUserRepository eventUserRepository) {
        super(NotificationType.EVENT_UPDATE);
        
        this.creator = creator;
        this.event = event;
        this.eventUserRepository = eventUserRepository;
        
        // Set data
        addData("eventId", event.getId().toString());
        addData("creatorId", creator.getId().toString());
        
        // Set title and message
        setTitle("Event Update");
        setMessage(creator.getUsername() + " has updated an event that you're attending: " + event.getTitle());
        
        // Find who should be notified
        findTargetUsers();
    }
    
    @Override
    public void findTargetUsers() {
        // Get all users participating in the event and notify them
        List<EventUser> participants = eventUserRepository.findEventsByEvent_IdAndStatus(
                event.getId(), ParticipationStatus.participating);
                
        for (EventUser participant : participants) {
            // Don't notify the creator about their own update
            if (!participant.getUser().getId().equals(creator.getId())) {
                addTargetUser(participant.getUser().getId());
            }
        }
    }
} 