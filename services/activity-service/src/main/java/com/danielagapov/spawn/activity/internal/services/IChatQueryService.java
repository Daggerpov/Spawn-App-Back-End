package com.danielagapov.spawn.activity.internal.services;

import com.danielagapov.spawn.chat.api.dto.FullActivityChatMessageDTO;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Interface for querying chat data from the Chat module.
 * In the microservice, this uses a Feign client to call the monolith
 * (where the Chat module still lives) instead of in-process events.
 */
public interface IChatQueryService {
    
    /**
     * Get chat message IDs for a single activity via event query.
     * 
     * @param activityId The activity ID
     * @return List of chat message IDs, or empty list if none found or on error
     */
    List<UUID> getChatMessageIdsByActivityId(UUID activityId);
    
    /**
     * Batch get chat message IDs for multiple activities via event query.
     * 
     * @param activityIds List of activity IDs
     * @return Map of activity ID to list of chat message IDs
     */
    Map<UUID, List<UUID>> getChatMessageIdsByActivityIds(List<UUID> activityIds);
    
    /**
     * Get full chat messages for an activity via event query.
     * Returns full chat message DTOs from chat-service.
     * 
     * @param activityId The activity ID
     * @return List of full chat message DTOs, or empty list if none found or on error
     */
    List<FullActivityChatMessageDTO> getFullChatMessagesByActivityId(UUID activityId);
}


