package com.danielagapov.spawn.shared.events;

import java.util.List;
import java.util.UUID;

/**
 * Domain events for Chat module inter-module communication.
 * Used to break circular dependencies between Activity and Chat modules.
 */
public final class ChatEvents {
    
    private ChatEvents() {
        // Utility class - prevent instantiation
    }
    
    /**
     * Query event to request chat message IDs for a single activity.
     * Published by Activity module, consumed by Chat module.
     */
    public record GetChatMessageIdsQuery(
        UUID activityId,
        UUID requestId  // Correlation ID for async response matching
    ) {}
    
    /**
     * Response event containing chat message IDs for an activity.
     * Published by Chat module in response to GetChatMessageIdsQuery.
     */
    public record ChatMessageIdsResponse(
        UUID activityId,
        UUID requestId,  // Correlation ID to match with query
        List<UUID> messageIds
    ) {}
    
    /**
     * Query event to request chat message IDs for multiple activities (batch).
     * Published by Activity module, consumed by Chat module.
     */
    public record GetBatchChatMessageIdsQuery(
        List<UUID> activityIds,
        UUID requestId  // Correlation ID for async response matching
    ) {}
    
    /**
     * Single activity-messages pair for batch response.
     */
    public record ActivityMessageIds(
        UUID activityId,
        List<UUID> messageIds
    ) {}
    
    /**
     * Response event containing chat message IDs for multiple activities.
     * Published by Chat module in response to GetBatchChatMessageIdsQuery.
     */
    public record BatchChatMessageIdsResponse(
        UUID requestId,  // Correlation ID to match with query
        List<ActivityMessageIds> activityMessageIds
    ) {}
    
    /**
     * Query event to request full chat messages for an activity.
     * Published by Activity module, consumed by Chat module.
     */
    public record GetFullChatMessagesQuery(
        UUID activityId,
        UUID requestId  // Correlation ID for async response matching
    ) {}
    
    /**
     * Simplified chat message data for cross-module communication.
     * Avoids circular dependency by not using domain entities directly.
     */
    public record ChatMessageData(
        UUID id,
        String content,
        java.time.Instant timestamp,
        UUID senderUserId,
        UUID activityId,
        List<UUID> likedByUserIds
    ) {}
    
    /**
     * Response event containing full chat messages for an activity.
     * Published by Chat module in response to GetFullChatMessagesQuery.
     */
    public record FullChatMessagesResponse(
        UUID activityId,
        UUID requestId,  // Correlation ID to match with query
        List<ChatMessageData> messages
    ) {}
}

