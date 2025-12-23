package com.danielagapov.spawn.shared.events;

import com.danielagapov.spawn.chat.api.dto.ChatMessageDTO;
import com.danielagapov.spawn.shared.util.NotificationType;

import java.util.List;
import java.util.UUID;

/**
 * Event for when a new comment is added to an activity.
 * 
 * Part of Phase 3: Shared Data Resolution - this event now receives
 * participant IDs directly instead of using a repository reference,
 * which maintains proper module boundaries.
 */
public class NewCommentNotificationEvent extends NotificationEvent {
    private final UUID senderUserId;
    private final String senderUsername;
    private final UUID activityId;
    private final String activityTitle;
    private final UUID creatorId;
    private final ChatMessageDTO messageDTO;
    private final List<UUID> participantIds;

    /**
     * Create a notification event for a new comment.
     * 
     * @param senderUserId ID of the user who sent the comment
     * @param senderUsername Username of the sender (for notification message)
     * @param activityId ID of the activity
     * @param activityTitle Title of the activity (for notification message)
     * @param creatorId ID of the activity creator
     * @param messageDTO The chat message DTO
     * @param participantIds List of participant user IDs (already filtered by participating status)
     */
    public NewCommentNotificationEvent(UUID senderUserId, String senderUsername, UUID activityId, 
                                       String activityTitle, UUID creatorId, ChatMessageDTO messageDTO, 
                                       List<UUID> participantIds) {
        super(NotificationType.NEW_COMMENT);
        
        this.senderUserId = senderUserId;
        this.senderUsername = senderUsername;
        this.activityId = activityId;
        this.activityTitle = activityTitle;
        this.creatorId = creatorId;
        this.messageDTO = messageDTO;
        this.participantIds = participantIds;
        
        // Set common data for all notifications
        addData("activityId", activityId.toString());
        addData("messageId", messageDTO.getId().toString());
        addData("senderId", senderUserId.toString());
        
        // Find who should be notified
        findTargetUsers();
    }

    @Override
    public void findTargetUsers() {
        // Check if the sender is the activity creator
        boolean senderIsCreator = senderUserId.equals(creatorId);
        
        // 1. Notify activity creator if they're not the sender
        if (!senderIsCreator) {
            addTargetUser(creatorId);
            
            // Creator gets special message
            if (getTargetUserIds().indexOf(creatorId) == 0) {
                setTitle("New Comment");
                setMessage(senderUsername + " commented on " + activityTitle + ": " + messageDTO.getContent());
            }
        }

        // 2. Find participating users (except the sender and activity creator)
        for (UUID participantId : participantIds) {
            // Skip if participant is the sender or the activity creator (already notified)
            if (!participantId.equals(senderUserId) && !participantId.equals(creatorId)) {
                addTargetUser(participantId);
                
                // If this is first user (no creator was added), set participant message
                if (getTargetUserIds().size() == 1) {
                    setTitle("New Comment on Activity");
                    setMessage(senderUsername + " commented on an activity you're participating in: " + activityTitle);
                }
            }
        }
    }
} 