package com.danielagapov.spawn.Mappers;

import com.danielagapov.spawn.DTOs.ChatMessage.ChatMessageDTO;
import com.danielagapov.spawn.Models.ChatMessage;
import com.danielagapov.spawn.Models.Activity;
import com.danielagapov.spawn.Models.User.User;

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
                entity.getActivity().getId(),
                likedByUserIds
        );
    }

    public static ChatMessage toEntity(ChatMessageDTO dto, User userSender, Activity Activity) {
        return new ChatMessage(
                dto.getId(),
                dto.getContent(),
                dto.getTimestamp(),
                userSender,
                Activity
        );
    }

    public static List<ChatMessageDTO> toDTOList(
            List<ChatMessage> chatMessages,
            Map<ChatMessage, List< UUID>> likedByUserIdsMap
    ) {
        return chatMessages.stream()
                .map(chatMessage -> toDTO(
                        chatMessage,
                        likedByUserIdsMap.getOrDefault(chatMessage, List.of()) // Default to an empty list if likedByUserIds is missing
                ))
                .collect(Collectors.toList());
    }

    public static List<ChatMessage> toEntityList(List<ChatMessageDTO> chatMessageDTOs, List<User> users, List<Activity> Activities) {
        return chatMessageDTOs.stream()
                .map(dto -> {
                    User userSender = users.stream()
                            .filter(user -> user.getId().equals(dto.getSenderUserId()))
                            .findFirst()
                            .orElse(null);
                    Activity Activity = Activities.stream()
                            .filter(ev -> ev.getId().equals(dto.getActivityId()))
                            .findFirst()
                            .orElse(null);
                    return toEntity(dto, userSender, Activity);
                })
                .collect(Collectors.toList());
    }
}