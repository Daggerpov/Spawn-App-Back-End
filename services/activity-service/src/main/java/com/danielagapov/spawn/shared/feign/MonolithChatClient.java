package com.danielagapov.spawn.shared.feign;

import com.danielagapov.spawn.chat.api.dto.FullActivityChatMessageDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;
import java.util.UUID;

/**
 * Feign client for fetching chat data from the monolith.
 * The activity-service needs chat messages for building full activity DTOs.
 * Chat module remains in the monolith; this client calls its endpoints.
 */
@FeignClient(
        name = "monolith-chat-client",
        url = "${services.monolith.url}",
        fallbackFactory = MonolithChatClientFallbackFactory.class
)
public interface MonolithChatClient {

    /**
     * Get full chat messages for a specific activity.
     * Calls the monolith's activity chats endpoint (which still exists in the monolith
     * for backward compatibility and because the chat module lives there).
     */
    @GetMapping("/api/v1/chat-messages/by-activity/{activityId}")
    List<FullActivityChatMessageDTO> getChatMessagesByActivityId(@PathVariable("activityId") UUID activityId);
}
