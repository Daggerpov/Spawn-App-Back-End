package com.danielagapov.spawn.shared.util;

import com.danielagapov.spawn.chat.api.dto.ChatMessageLikesDTO;
import com.danielagapov.spawn.activity.internal.domain.ChatMessage;
import com.danielagapov.spawn.activity.internal.domain.ChatMessageLikes;
import com.danielagapov.spawn.activity.internal.domain.ChatMessageLikesId;
import com.danielagapov.spawn.user.internal.domain.User;

public final class ChatMessageLikesMapper {

    // Convert entity to DTO
    public static ChatMessageLikesDTO toDTO(ChatMessageLikes chatMessageLikes) {
        return new ChatMessageLikesDTO(
                chatMessageLikes.getChatMessage().getId(),
                chatMessageLikes.getUser().getId()
        );
    }

    // Convert DTO to entity
    public static ChatMessageLikes toEntity(ChatMessageLikesDTO dto, ChatMessage chatMessage, User user) {
        ChatMessageLikesId id = new ChatMessageLikesId(dto.getChatMessageId(), dto.getUserId());

        return new ChatMessageLikes(
                id,
                chatMessage,
                user
        );
    }
}
