package com.danielagapov.spawn.Events;

import com.danielagapov.spawn.DTOs.ChatMessage.ChatMessageDTO;
import com.danielagapov.spawn.Enums.NotificationType;
import com.danielagapov.spawn.Enums.ParticipationStatus;
import com.danielagapov.spawn.Models.Activity;
import com.danielagapov.spawn.Models.ActivityUser;
import com.danielagapov.spawn.Models.User.User;
import com.danielagapov.spawn.Repositories.IActivityUserRepository;

import java.util.List;
import java.util.UUID;

/**
 * Activity for when a new comment is added to an activity
 */
public class NewCommentNotificationEvent extends NotificationEvent {
    private final User sender;
    private final Activity activity;
    private final ChatMessageDTO messageDTO;
    private final IActivityUserRepository activityUserRepository;

    /**
     * Create a notification Activity for a new comment
     */
    public NewCommentNotificationEvent(User sender, Activity activity, ChatMessageDTO messageDTO, IActivityUserRepository activityUserRepository) {
        super(NotificationType.NEW_COMMENT);
        
        this.sender = sender;
        this.activity = activity;
        this.messageDTO = messageDTO;
        this.activityUserRepository = activityUserRepository;
        
        // Set common data for all notifications
        addData("activityId", activity.getId().toString());
        addData("messageId", messageDTO.getId().toString());
        addData("senderId", sender.getId().toString());
        
        // Find who should be notified
        findTargetUsers();
    }

    @Override
    public void findTargetUsers() {
        UUID senderUserId = sender.getId();
        UUID creatorId = activity.getCreator().getId();
        
        // Check if the sender is the activity creator
        boolean senderIsCreator = senderUserId.equals(creatorId);
        
        // 1. Notify activity creator if they're not the sender
        if (!senderIsCreator) {
            addTargetUser(creatorId);
            
            // Creator gets special message
            if (getTargetUserIds().indexOf(creatorId) == 0) {
                setTitle("New Comment on Your Activity");
                setMessage(sender.getUsername() + " commented on " + activity.getTitle() + ": " + messageDTO.getContent());
            }
        }

        // 2. Find participating users (except the sender and activity creator)
        List<ActivityUser> participants = activityUserRepository.findActivitiesByActivity_IdAndStatus(activity.getId(), ParticipationStatus.participating);
        
        for (ActivityUser participant : participants) {
            UUID participantId = participant.getUser().getId();
            // Skip if participant is the sender or the activity creator (already notified)
            if (!participantId.equals(senderUserId) && !participantId.equals(creatorId)) {
                addTargetUser(participantId);
                
                // If this is first user (no creator was added), set participant message
                if (getTargetUserIds().size() == 1) {
                    setTitle("New Comment on Activity");
                    setMessage(sender.getUsername() + " commented on an activity you're participating in: " + activity.getTitle());
                }
            }
        }
    }
} 