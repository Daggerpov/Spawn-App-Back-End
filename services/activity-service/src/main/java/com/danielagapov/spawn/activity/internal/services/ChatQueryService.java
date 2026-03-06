package com.danielagapov.spawn.activity.internal.services;

import com.danielagapov.spawn.chat.api.dto.FullActivityChatMessageDTO;
import com.danielagapov.spawn.shared.exceptions.Logger.ILogger;
import com.danielagapov.spawn.shared.feign.MonolithChatClient;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for querying chat data from the chat-service via Feign client.
 * All methods gracefully return empty results on failure — chat messages
 * are non-critical for the activity feed to function.
 */
@Service
public class ChatQueryService implements IChatQueryService {
    
    private final MonolithChatClient monolithChatClient;
    private final ILogger logger;
    
    public ChatQueryService(MonolithChatClient monolithChatClient, ILogger logger) {
        this.monolithChatClient = monolithChatClient;
        this.logger = logger;
    }
    
    /**
     * Get chat message IDs for a single activity via Feign call to monolith.
     */
    @Override
    public List<UUID> getChatMessageIdsByActivityId(UUID activityId) {
        try {
            List<FullActivityChatMessageDTO> messages = monolithChatClient.getChatMessagesByActivityId(activityId);
            return messages.stream()
                    .map(FullActivityChatMessageDTO::getId)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            logger.warn("Could not fetch chat message IDs for activity " + activityId + ": " + e.getMessage());
            return Collections.emptyList();
        }
    }
    
    /**
     * Batch get chat message IDs for multiple activities.
     * Makes individual calls per activity (could be optimized with a batch endpoint later).
     */
    @Override
    public Map<UUID, List<UUID>> getChatMessageIdsByActivityIds(List<UUID> activityIds) {
        if (activityIds == null || activityIds.isEmpty()) {
            return Collections.emptyMap();
        }
        
        Map<UUID, List<UUID>> result = new HashMap<>();
        for (UUID activityId : activityIds) {
            List<UUID> messageIds = getChatMessageIdsByActivityId(activityId);
            if (!messageIds.isEmpty()) {
                result.put(activityId, messageIds);
            }
        }
        return result;
    }
    
    /**
     * Get full chat messages for an activity via Feign call to monolith.
     */
    @Override
    public List<FullActivityChatMessageDTO> getFullChatMessagesByActivityId(UUID activityId) {
        try {
            return monolithChatClient.getChatMessagesByActivityId(activityId);
        } catch (Exception e) {
            logger.warn("Could not fetch full chat messages for activity " + activityId + ": " + e.getMessage());
            return Collections.emptyList();
        }
    }
}
