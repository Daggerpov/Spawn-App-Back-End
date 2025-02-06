package com.danielagapov.spawn.Mappers;

import com.danielagapov.spawn.DTOs.ChatMessageLikesDTO;
import com.danielagapov.spawn.Models.ChatMessage;
import com.danielagapov.spawn.Models.ChatMessageLikes;
import com.danielagapov.spawn.Models.CompositeKeys.ChatMessageLikesId;
import com.danielagapov.spawn.Models.User;

public class ChatMessageLikesMapper {

    // Convert entity to DTO
    public static ChatMessageLikesDTO toDTO(ChatMessageLikes chatMessageLikes) {
        return new ChatMessageLikesDTO(
                chatMessageLikes.getChatMessage().getId(),
                chatMessageLikes.getUser().getId()
        );
    }

    // Convert DTO to entity
    public static ChatMessageLikes toEntity(ChatMessageLikesDTO dto, ChatMessage chatMessage, User user) {
        ChatMessageLikesId id = new ChatMessageLikesId(dto.chatMessageId(), dto.userId());

        return new ChatMessageLikes(
                id,
                chatMessage,
                user
        );
    }
}
