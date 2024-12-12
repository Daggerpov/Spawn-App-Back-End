package com.danielagapov.spawn.Controllers;

import com.danielagapov.spawn.DTOs.ChatMessageDTO;
import com.danielagapov.spawn.Services.ChatMessage.IChatMessageService;
import org.springframework.web.bind.annotation.*;
import com.danielagapov.spawn.DTOs.UserDTO;

import java.util.List;
import java.util.UUID;

@RestController()
@RequestMapping("api/v1/chatMessages")
public class ChatMessageController {
    private final IChatMessageService chatMessageService;

    public ChatMessageController(IChatMessageService chatMessageService) {
        this.chatMessageService = chatMessageService;
    }

    // full path: /api/v1/chatMessages
    @GetMapping
    public String getChatMessages() {
        return "These are the chatMessages: " + chatMessageService.getAllChatMessages();
    }

    // full path: /api/v1/chatMessages/{id}
    @GetMapping("/{id}")
    public ChatMessageDTO getChatMessage(@PathVariable UUID id) {
        return chatMessageService.getChatMessageById(id);
    }

    // full path: /api/v1/chatMessages/mock-endpoint
    @GetMapping("/mock-endpoint")
    public String getMockEndpoint() {
        return "This is the mock endpoint for chatMessages. Everything is working with it.";
    }

    // full path: /api/v1/chatMessages
    @PostMapping
    public ChatMessageDTO createChatMessage(@RequestBody ChatMessageDTO newChatMessage) {
        return chatMessageService.saveChatMessage(newChatMessage);
    }

    // full path: /api/v1/chatMessages/{chatMessageId}/likes/{userId}
    @PostMapping("/{chatMessageId}/likes/{userId}")
    public void createChatMessageLike(@PathVariable UUID chatMessageId, @PathVariable UUID userId) {
        chatMessageService.createChatMessageLike(chatMessageId, userId);
    }

    // full path: /api/v1/chatMessages/{chatMessageId}/likes
    @GetMapping("/{chatMessageId}/likes")
    public List<UserDTO> getChatMessageLikes(@PathVariable UUID chatMessageId) {
        return chatMessageService.getChatMessageLikes(chatMessageId);
    }

    // full path: /api/v1/chatMessages/{chatMessageId}/likes/{userId}
    @DeleteMapping("/{chatMessageId}/likes/{userId}")
    public void deleteChatMessageLike(@PathVariable UUID chatMessageId, @PathVariable UUID userId) {
        chatMessageService.deleteChatMessageLike(chatMessageId, userId);
    }
}


