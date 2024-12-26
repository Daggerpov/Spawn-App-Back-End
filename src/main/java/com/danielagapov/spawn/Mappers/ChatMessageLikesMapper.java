package com.danielagapov.spawn.Mappers;

import com.danielagapov.spawn.DTOs.ChatMessageLikesDTO;
import com.danielagapov.spawn.Models.ChatMessageLikes;

public class ChatMessageLikesMapper {

    // Convert entity to DTO
    public static ChatMessageLikesDTO toDTO(ChatMessageLikes chatMessageLikes) {
        return new ChatMessageLikesDTO();
    }

    // Convert DTO to entity
    public static ChatMessageLikes toEntity(ChatMessageLikesDTO dto) {
        return new ChatMessageLikes();
    }
}
