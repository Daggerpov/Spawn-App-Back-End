package com.danielagapov.spawn.Mappers;

import com.danielagapov.spawn.Models.ChatMessage.ChatMessage;
import com.danielagapov.spawn.DTOs.ChatMessageDTO;

import java.util.List;
import java.util.stream.Collectors;

public class ChatMessageMapper {
    // by far the simplest mapping, since it's essentially 1-to-1

    public static ChatMessageDTO toDTO(ChatMessage entity) {
        return new ChatMessageDTO(
                entity.getId(),
                entity.getTimestamp(),
                entity.getUserSenderId(),
                entity.getContent(),
                entity.getEventId()
        );
    }

    public static ChatMessage toEntity(ChatMessageDTO dto) {
        return new ChatMessage(
                dto.id(),
                dto.timestamp(),
                dto.userSenderId(),
                dto.content(),
                dto.eventId()
        );
    }

    public static List<ChatMessageDTO> toDTOList(List<ChatMessage> chatMessages) {
        return chatMessages.stream()
                .map(ChatMessageMapper::toDTO)
                .collect(Collectors.toList());
    }

    public static List<ChatMessage> toEntityList(List<ChatMessageDTO> chatMessageDTOs) {
        return chatMessageDTOs.stream()
                .map(ChatMessageMapper::toEntity)
                .collect(Collectors.toList());
    }
}