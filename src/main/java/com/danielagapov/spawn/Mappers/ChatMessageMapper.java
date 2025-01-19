package com.danielagapov.spawn.Mappers;

import com.danielagapov.spawn.DTOs.ChatMessageDTO;
import com.danielagapov.spawn.DTOs.UserDTO;
import com.danielagapov.spawn.Models.ChatMessage;
import com.danielagapov.spawn.Models.Event;
import com.danielagapov.spawn.Models.User;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public class ChatMessageMapper {

    public static ChatMessageDTO toDTO(ChatMessage entity, List<UUID> likedByUserIds) {
        return new ChatMessageDTO(
                entity.getId(),
                entity.getContent(),
                entity.getTimestamp(),
                entity.getUserSender().getId(),
                entity.getEvent().getId(),
                likedByUserIds
        );
    }

    public static ChatMessage toEntity(ChatMessageDTO dto, User userSender, Event event) {
        return new ChatMessage(
                dto.id(),
                dto.content(),
                dto.timestamp(),
                userSender,
                event
        );
    }

    public static List<ChatMessageDTO> toDTOList(
            List<ChatMessage> chatMessages,
            Map<ChatMessage, UserDTO> userSenderMap,
            Map<ChatMessage, List<UserDTO>> likedByMap
    ) {
        return chatMessages.stream()
                .map(chatMessage -> toDTO(
                        chatMessage,
                        userSenderMap.getOrDefault(chatMessage, null), // Default to null if senderUserId is missing
                        likedByMap.getOrDefault(chatMessage, List.of()) // Default to an empty list if likedByUserIds is missing
                ))
                .collect(Collectors.toList());
    }

    public static List<ChatMessage> toEntityList(List<ChatMessageDTO> chatMessageDTOs, List<User> users, List<Event> events) {
        return chatMessageDTOs.stream()
                .map(dto -> {
                    User userSender = users.stream()
                            .filter(user -> user.getId().equals(dto.senderUserId().id()))
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