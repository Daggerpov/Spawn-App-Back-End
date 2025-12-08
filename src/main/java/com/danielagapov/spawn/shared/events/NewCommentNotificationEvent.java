package com.danielagapov.spawn.shared.events;

import com.danielagapov.spawn.chat.api.dto.ChatMessageDTO;
import com.danielagapov.spawn.shared.util.NotificationType;
import com.danielagapov.spawn.shared.util.ParticipationStatus;
import com.danielagapov.spawn.activity.internal.domain.Activity;
import com.danielagapov.spawn.activity.internal.domain.ActivityUser;
import com.danielagapov.spawn.user.internal.domain.User;
import com.danielagapov.spawn.activity.internal.repositories.IActivityUserRepository;

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
                setTitle("New Comment");
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