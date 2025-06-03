package com.danielagapov.spawn.DTOs.ChatMessage;

import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
public class ChatMessageDTO extends AbstractChatMessageDTO{
    UUID senderUserId;
    List<UUID> likedByUserIds;
    public ChatMessageDTO(UUID id, String content, Instant timestamp, UUID senderUserId, UUID activityId, List<UUID> likedByUserIds) {
        super(id, content, timestamp, activityId);
        this.senderUserId = senderUserId;
        this.likedByUserIds = likedByUserIds;
    }
}