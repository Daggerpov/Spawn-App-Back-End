package com.danielagapov.spawn.shared.util;

import com.danielagapov.spawn.chat.api.dto.ChatMessageDTO;
import com.danielagapov.spawn.activity.internal.domain.Activity;
import com.danielagapov.spawn.activity.internal.domain.ChatMessage;
import com.danielagapov.spawn.user.internal.domain.User;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public final class ChatMessageMapper {

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

    public static ChatMessage toEntity(ChatMessageDTO dto, User userSender, Activity activity) {
        return new ChatMessage(
                dto.getId(), // null for new entities, existing ID for updates
                dto.getContent(),
                dto.getTimestamp(),
                userSender,
                activity
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

    public static List<ChatMessage> toEntityList(List<ChatMessageDTO> chatMessageDTOs, List<User> users, List<Activity> activities) {
        return chatMessageDTOs.stream()
                .map(dto -> {
                    User userSender = users.stream()
                            .filter(user -> user.getId().equals(dto.getSenderUserId()))
                            .findFirst()
                            .orElse(null);
                    Activity activity = activities.stream()
                            .filter(ev -> ev.getId().equals(dto.getActivityId()))
                            .findFirst()
                            .orElse(null);
                    return toEntity(dto, userSender, activity);
                })
                .collect(Collectors.toList());
    }
}