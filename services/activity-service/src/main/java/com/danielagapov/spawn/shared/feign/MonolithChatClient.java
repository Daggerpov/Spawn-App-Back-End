package com.danielagapov.spawn.shared.feign;

import com.danielagapov.spawn.chat.api.dto.FullActivityChatMessageDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;
import java.util.UUID;

/**
 * Feign client for fetching chat data from the chat-service.
 * The activity-service needs chat messages for building full activity DTOs.
 */
@FeignClient(
        name = "chat-service-client",
        url = "${services.chat-service.url:http://localhost:8083}",
        fallbackFactory = MonolithChatClientFallbackFactory.class
)
public interface MonolithChatClient {

    /**
     * Get full chat messages for a specific activity.
     */
    @GetMapping("/api/v1/chat-messages/by-activity/{activityId}")
    List<FullActivityChatMessageDTO> getChatMessagesByActivityId(@PathVariable("activityId") UUID activityId);
}
