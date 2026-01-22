package com.danielagapov.spawn.chat.internal.services;

import com.danielagapov.spawn.chat.api.dto.ChatMessageDTO;
import com.danielagapov.spawn.shared.events.ChatEvents.*;
import com.danielagapov.spawn.shared.exceptions.Logger.ILogger;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Event listener for Chat module.
 * Handles query events from other modules (primarily Activity) and publishes responses.
 * This breaks the circular dependency between Activity and Chat modules.
 */
@Service
public class ChatEventListener {
    
    private final IChatMessageService chatMessageService;
    private final ApplicationEventPublisher eventPublisher;
    private final ILogger logger;
    
    public ChatEventListener(
            IChatMessageService chatMessageService,
            ApplicationEventPublisher eventPublisher,
            ILogger logger) {
        this.chatMessageService = chatMessageService;
        this.eventPublisher = eventPublisher;
        this.logger = logger;
    }
    
    /**
     * Handles query for chat message IDs for a single activity.
     * Publishes ChatMessageIdsResponse with the results.
     */
    @EventListener
    public void handleGetChatMessageIdsQuery(GetChatMessageIdsQuery query) {
        try {
            logger.info("Handling GetChatMessageIdsQuery for activity: " + query.activityId());
            
            List<UUID> messageIds = chatMessageService.getChatMessageIdsByActivityId(query.activityId());
            
            eventPublisher.publishEvent(new ChatMessageIdsResponse(
                query.activityId(),
                query.requestId(),
                messageIds
            ));
            
            logger.info("Published ChatMessageIdsResponse with " + messageIds.size() + " message IDs");
        } catch (Exception e) {
            logger.error("Error handling GetChatMessageIdsQuery: " + e.getMessage());
            // Publish empty response on error to prevent blocking
            eventPublisher.publishEvent(new ChatMessageIdsResponse(
                query.activityId(),
                query.requestId(),
                List.of()
            ));
        }
    }
    
    /**
     * Handles batch query for chat message IDs for multiple activities.
     * Publishes BatchChatMessageIdsResponse with the results.
     */
    @EventListener
    public void handleGetBatchChatMessageIdsQuery(GetBatchChatMessageIdsQuery query) {
        try {
            logger.info("Handling GetBatchChatMessageIdsQuery for " + query.activityIds().size() + " activities");
            
            List<Object[]> results = chatMessageService.getChatMessageIdsByActivityIds(query.activityIds());
            
            // Group results by activity ID
            Map<UUID, List<UUID>> groupedResults = results.stream()
                .collect(Collectors.groupingBy(
                    row -> (UUID) row[0],
                    Collectors.mapping(
                        row -> (UUID) row[1],
                        Collectors.toList()
                    )
                ));
            
            // Convert to response format
            List<ActivityMessageIds> activityMessageIds = query.activityIds().stream()
                .map(activityId -> new ActivityMessageIds(
                    activityId,
                    groupedResults.getOrDefault(activityId, List.of())
                ))
                .collect(Collectors.toList());
            
            eventPublisher.publishEvent(new BatchChatMessageIdsResponse(
                query.requestId(),
                activityMessageIds
            ));
            
            logger.info("Published BatchChatMessageIdsResponse for " + activityMessageIds.size() + " activities");
        } catch (Exception e) {
            logger.error("Error handling GetBatchChatMessageIdsQuery: " + e.getMessage());
            // Publish empty response on error
            eventPublisher.publishEvent(new BatchChatMessageIdsResponse(
                query.requestId(),
                List.of()
            ));
        }
    }
    
    /**
     * Handles query for full chat messages for an activity.
     * Publishes FullChatMessagesResponse with the results.
     */
    @EventListener
    public void handleGetFullChatMessagesQuery(GetFullChatMessagesQuery query) {
        try {
            logger.info("Handling GetFullChatMessagesQuery for activity: " + query.activityId());
            
            List<ChatMessageDTO> messages = chatMessageService.getChatMessagesByActivityId(query.activityId());
            
            // Convert to event data format
            List<ChatMessageData> messageData = messages.stream()
                .map(msg -> new ChatMessageData(
                    msg.getId(),
                    msg.getContent(),
                    msg.getTimestamp(),
                    msg.getSenderUserId(),
                    msg.getActivityId(),
                    msg.getLikedByUserIds()
                ))
                .collect(Collectors.toList());
            
            eventPublisher.publishEvent(new FullChatMessagesResponse(
                query.activityId(),
                query.requestId(),
                messageData
            ));
            
            logger.info("Published FullChatMessagesResponse with " + messageData.size() + " messages");
        } catch (Exception e) {
            logger.error("Error handling GetFullChatMessagesQuery: " + e.getMessage());
            // Publish empty response on error
            eventPublisher.publishEvent(new FullChatMessagesResponse(
                query.activityId(),
                query.requestId(),
                List.of()
            ));
        }
    }
}

