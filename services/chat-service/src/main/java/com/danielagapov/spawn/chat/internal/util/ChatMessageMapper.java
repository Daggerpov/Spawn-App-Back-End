package com.danielagapov.spawn.chat.internal.util;

import com.danielagapov.spawn.chat.api.dto.ChatMessageDTO;
import com.danielagapov.spawn.chat.internal.domain.ActivityRef;
import com.danielagapov.spawn.chat.internal.domain.ChatMessage;
import com.danielagapov.spawn.chat.internal.domain.UserRef;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public final class ChatMessageMapper {

    private ChatMessageMapper() {}

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

    public static ChatMessage toEntity(ChatMessageDTO dto, UUID senderUserId, UUID activityId) {
        return new ChatMessage(
                dto.getId(),
                dto.getContent(),
                dto.getTimestamp(),
                new UserRef(senderUserId),
                new ActivityRef(activityId)
        );
    }
}
