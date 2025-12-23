package com.danielagapov.spawn.activity.internal.services;

import com.danielagapov.spawn.chat.api.dto.FullActivityChatMessageDTO;
import com.danielagapov.spawn.shared.events.ChatEvents.*;
import com.danielagapov.spawn.shared.exceptions.Logger.ILogger;
import com.danielagapov.spawn.user.api.dto.BaseUserDTO;
import com.danielagapov.spawn.user.internal.services.IUserService;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

/**
 * Service for querying chat data from the Chat module via events.
 * This replaces the direct dependency on IChatMessageService in ActivityService,
 * breaking the circular dependency between Activity and Chat modules.
 */
@Service
public class ChatQueryService {
    
    private static final long QUERY_TIMEOUT_MS = 5000; // 5 second timeout
    
    private final ApplicationEventPublisher eventPublisher;
    private final IUserService userService;
    private final ILogger logger;
    
    // Pending query futures for async response matching
    private final ConcurrentHashMap<UUID, CompletableFuture<List<UUID>>> pendingIdQueries = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, CompletableFuture<List<ActivityMessageIds>>> pendingBatchIdQueries = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, CompletableFuture<List<ChatMessageData>>> pendingFullMessageQueries = new ConcurrentHashMap<>();
    
    public ChatQueryService(
            ApplicationEventPublisher eventPublisher,
            IUserService userService,
            ILogger logger) {
        this.eventPublisher = eventPublisher;
        this.userService = userService;
        this.logger = logger;
    }
    
    /**
     * Get chat message IDs for a single activity via event query.
     */
    public List<UUID> getChatMessageIdsByActivityId(UUID activityId) {
        UUID requestId = UUID.randomUUID();
        CompletableFuture<List<UUID>> future = new CompletableFuture<>();
        
        pendingIdQueries.put(requestId, future);
        
        try {
            // Publish query event
            eventPublisher.publishEvent(new GetChatMessageIdsQuery(activityId, requestId));
            
            // Wait for response with timeout
            return future.get(QUERY_TIMEOUT_MS, TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            logger.warn("Timeout waiting for chat message IDs for activity " + activityId);
            return Collections.emptyList();
        } catch (Exception e) {
            logger.error("Error getting chat message IDs for activity " + activityId + ": " + e.getMessage());
            return Collections.emptyList();
        } finally {
            pendingIdQueries.remove(requestId);
        }
    }
    
    /**
     * Batch get chat message IDs for multiple activities via event query.
     */
    public Map<UUID, List<UUID>> getChatMessageIdsByActivityIds(List<UUID> activityIds) {
        if (activityIds.isEmpty()) {
            return Collections.emptyMap();
        }
        
        UUID requestId = UUID.randomUUID();
        CompletableFuture<List<ActivityMessageIds>> future = new CompletableFuture<>();
        
        pendingBatchIdQueries.put(requestId, future);
        
        try {
            // Publish query event
            eventPublisher.publishEvent(new GetBatchChatMessageIdsQuery(activityIds, requestId));
            
            // Wait for response with timeout
            List<ActivityMessageIds> results = future.get(QUERY_TIMEOUT_MS, TimeUnit.MILLISECONDS);
            
            // Convert to map
            return results.stream()
                .collect(Collectors.toMap(
                    ActivityMessageIds::activityId,
                    ActivityMessageIds::messageIds
                ));
        } catch (TimeoutException e) {
            logger.warn("Timeout waiting for batch chat message IDs for " + activityIds.size() + " activities");
            return Collections.emptyMap();
        } catch (Exception e) {
            logger.error("Error getting batch chat message IDs: " + e.getMessage());
            return Collections.emptyMap();
        } finally {
            pendingBatchIdQueries.remove(requestId);
        }
    }
    
    /**
     * Get full chat messages for an activity via event query.
     * Converts ChatMessageData to FullActivityChatMessageDTO with user lookups.
     */
    public List<FullActivityChatMessageDTO> getFullChatMessagesByActivityId(UUID activityId) {
        UUID requestId = UUID.randomUUID();
        CompletableFuture<List<ChatMessageData>> future = new CompletableFuture<>();
        
        pendingFullMessageQueries.put(requestId, future);
        
        try {
            // Publish query event
            eventPublisher.publishEvent(new GetFullChatMessagesQuery(activityId, requestId));
            
            // Wait for response with timeout
            List<ChatMessageData> messageDataList = future.get(QUERY_TIMEOUT_MS, TimeUnit.MILLISECONDS);
            
            // Convert to FullActivityChatMessageDTO with user lookups
            return messageDataList.stream()
                .map(this::convertToFullActivityChatMessageDTO)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        } catch (TimeoutException e) {
            logger.warn("Timeout waiting for full chat messages for activity " + activityId);
            return Collections.emptyList();
        } catch (Exception e) {
            logger.error("Error getting full chat messages for activity " + activityId + ": " + e.getMessage());
            return Collections.emptyList();
        } finally {
            pendingFullMessageQueries.remove(requestId);
        }
    }
    
    /**
     * Converts ChatMessageData to FullActivityChatMessageDTO by looking up user details.
     */
    private FullActivityChatMessageDTO convertToFullActivityChatMessageDTO(ChatMessageData data) {
        try {
            // Look up sender user
            BaseUserDTO senderUser = userService.getBaseUserById(data.senderUserId());
            
            // Look up liked by users
            List<BaseUserDTO> likedByUsers = data.likedByUserIds().stream()
                .map(userId -> {
                    try {
                        return userService.getBaseUserById(userId);
                    } catch (Exception e) {
                        logger.warn("Could not load user " + userId + " for chat message like");
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
            
            return new FullActivityChatMessageDTO(
                data.id(),
                data.content(),
                data.timestamp(),
                senderUser,
                data.activityId(),
                likedByUsers
            );
        } catch (Exception e) {
            logger.warn("Error converting chat message data to DTO: " + e.getMessage());
            return null;
        }
    }
    
    // ========== Event Listeners for Response Handling ==========
    
    /**
     * Handle response for single activity chat message ID query.
     */
    @EventListener
    public void handleChatMessageIdsResponse(ChatMessageIdsResponse response) {
        CompletableFuture<List<UUID>> future = pendingIdQueries.get(response.requestId());
        if (future != null) {
            future.complete(response.messageIds());
        }
    }
    
    /**
     * Handle response for batch chat message ID query.
     */
    @EventListener
    public void handleBatchChatMessageIdsResponse(BatchChatMessageIdsResponse response) {
        CompletableFuture<List<ActivityMessageIds>> future = pendingBatchIdQueries.get(response.requestId());
        if (future != null) {
            future.complete(response.activityMessageIds());
        }
    }
    
    /**
     * Handle response for full chat messages query.
     */
    @EventListener
    public void handleFullChatMessagesResponse(FullChatMessagesResponse response) {
        CompletableFuture<List<ChatMessageData>> future = pendingFullMessageQueries.get(response.requestId());
        if (future != null) {
            future.complete(response.messages());
        }
    }
}

