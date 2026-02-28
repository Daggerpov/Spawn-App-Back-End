package com.danielagapov.spawn.activity.internal.services;

import com.danielagapov.spawn.chat.api.dto.FullActivityChatMessageDTO;
import com.danielagapov.spawn.shared.exceptions.Logger.ILogger;
import com.danielagapov.spawn.shared.feign.ChatServiceClient;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Chat query implementation that calls the chat-service via Feign.
 * Replaces the in-process event-based ChatQueryService now that chat is extracted.
 */
@Service
@Primary
public class ChatQueryServiceFeignImpl implements IChatQueryService {

    private final ChatServiceClient chatServiceClient;
    private final ILogger logger;

    public ChatQueryServiceFeignImpl(ChatServiceClient chatServiceClient, ILogger logger) {
        this.chatServiceClient = chatServiceClient;
        this.logger = logger;
    }

    @Override
    public List<UUID> getChatMessageIdsByActivityId(UUID activityId) {
        try {
            List<FullActivityChatMessageDTO> messages = chatServiceClient.getChatMessagesByActivityId(activityId);
            return messages.stream()
                    .map(FullActivityChatMessageDTO::getId)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            logger.warn("Could not fetch chat message IDs for activity " + activityId + ": " + e.getMessage());
            return Collections.emptyList();
        }
    }

    @Override
    public Map<UUID, List<UUID>> getChatMessageIdsByActivityIds(List<UUID> activityIds) {
        if (activityIds == null || activityIds.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<UUID, List<UUID>> result = new java.util.HashMap<>();
        for (UUID activityId : activityIds) {
            List<UUID> ids = getChatMessageIdsByActivityId(activityId);
            if (!ids.isEmpty()) {
                result.put(activityId, ids);
            }
        }
        return result;
    }

    @Override
    public List<FullActivityChatMessageDTO> getFullChatMessagesByActivityId(UUID activityId) {
        try {
            return chatServiceClient.getChatMessagesByActivityId(activityId);
        } catch (Exception e) {
            logger.warn("Could not fetch full chat messages for activity " + activityId + ": " + e.getMessage());
            return Collections.emptyList();
        }
    }
}
