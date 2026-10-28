package com.danielagapov.spawn.shared.feign;

import com.danielagapov.spawn.chat.api.dto.ChatMessageDTO;
import com.danielagapov.spawn.chat.api.dto.FullActivityChatMessageDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;
import java.util.UUID;

/**
 * Feign client for the chat-service.
 * Used by the monolith when chat has been extracted to the chat microservice.
 */
@FeignClient(
        name = "chat-service-client",
        url = "${services.chat-service.url:http://localhost:8083}"
)
public interface ChatServiceClient {

    @GetMapping("/api/v1/chat-messages/{id}")
    ChatMessageDTO getChatMessageById(@PathVariable("id") UUID id);

    @GetMapping("/api/v1/chat-messages/by-activity/{activityId}")
    List<FullActivityChatMessageDTO> getChatMessagesByActivityId(@PathVariable("activityId") UUID activityId);
}
