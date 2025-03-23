package com.danielagapov.spawn.Events;

import com.danielagapov.spawn.DTOs.ChatMessage.ChatMessageDTO;
import com.danielagapov.spawn.Enums.NotificationType;
import com.danielagapov.spawn.Enums.ParticipationStatus;
import com.danielagapov.spawn.Models.Event;
import com.danielagapov.spawn.Models.EventUser;
import com.danielagapov.spawn.Models.User;
import com.danielagapov.spawn.Repositories.IEventUserRepository;

import java.util.List;
import java.util.UUID;

/**
 * Event for when a new comment is added to an event
 */
public class NewCommentNotificationEvent extends NotificationEvent {
    private final User sender;
    private final Event event;
    private final ChatMessageDTO messageDTO;
    private final IEventUserRepository eventUserRepository;

    /**
     * Create a notification event for a new comment
     */
    public NewCommentNotificationEvent(User sender, Event event, ChatMessageDTO messageDTO, IEventUserRepository eventUserRepository) {
        super(NotificationType.NEW_COMMENT);
        
        this.sender = sender;
        this.event = event;
        this.messageDTO = messageDTO;
        this.eventUserRepository = eventUserRepository;
        
        // Set common data for all notifications
        addData("eventId", event.getId().toString());
        addData("messageId", messageDTO.getId().toString());
        addData("senderId", sender.getId().toString());
        
        // Find who should be notified
        findTargetUsers();
    }

    @Override
    public void findTargetUsers() {
        UUID senderUserId = sender.getId();
        
        // 1. Notify event creator if they're not the sender
        if (!event.getCreator().getId().equals(senderUserId)) {
            UUID creatorId = event.getCreator().getId();
            addTargetUser(creatorId);
            
            // Creator gets special message
            if (getTargetUserIds().indexOf(creatorId) == 0) {
                setTitle("New Comment on Your Event");
                setMessage(sender.getUsername() + " commented on " + event.getTitle() + ": " + messageDTO.getContent());
            }
        }

        // 2. Find participating users (except the sender and event creator)
        List<EventUser> participants = eventUserRepository.findEventsByEvent_IdAndStatus(event.getId(), ParticipationStatus.participating);
        
        for (EventUser participant : participants) {
            UUID participantId = participant.getUser().getId();
            // Skip if participant is the sender or the event creator (already notified)
            if (!participantId.equals(senderUserId) && !participantId.equals(event.getCreator().getId())) {
                addTargetUser(participantId);
                
                // If this is first user (no creator was added), set participant message
                if (getTargetUserIds().size() == 1) {
                    setTitle("New Comment on Event");
                    setMessage(sender.getUsername() + " commented on an event you're participating in: " + event.getTitle());
                }
            }
        }
    }
} 