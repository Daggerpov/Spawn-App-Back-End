package com.danielagapov.spawn.Mappers;

import com.danielagapov.spawn.Models.ChatMessage;
import com.danielagapov.spawn.DTOs.ChatMessageDTO;
import com.danielagapov.spawn.Models.Event;
import com.danielagapov.spawn.Models.User;

import java.util.List;
import java.util.stream.Collectors;

public class ChatMessageMapper {
    // by far the simplest mapping, since it's essentially 1-to-1

    public static ChatMessageDTO toDTO(ChatMessage entity) {
        return new ChatMessageDTO(
                entity.getId(),
                entity.getTimestamp(),
                entity.getUserSender().getId(),
                entity.getContent(),
                entity.getEvent().getId()
        );
    }

    public static ChatMessage toEntity(ChatMessageDTO dto, User userSender, Event event) {
        return new ChatMessage(
                dto.id(),
                dto.timestamp(),
                userSender,
                event,
                dto.content()
        );
    }

    public static List<ChatMessageDTO> toDTOList(List<ChatMessage> chatMessages) {
        return chatMessages.stream()
                .map(ChatMessageMapper::toDTO)
                .collect(Collectors.toList());
    }

    public static List<ChatMessage> toEntityList(List<ChatMessageDTO> chatMessageDTOs, List<User> users, List<Event> events) {
        return chatMessageDTOs.stream()
                .map(dto -> {
                    User userSender = users.stream()
                            .filter(user -> user.getId().equals(dto.userSenderId()))
                            .findFirst()
                            .orElse(null);
                    Event event = events.stream()
                            .filter(ev -> ev.getId().equals(dto.eventId()))
                            .findFirst()
                            .orElse(null);
                    return toEntity(dto, userSender, event);
                })
                .collect(Collectors.toList());
    }
}