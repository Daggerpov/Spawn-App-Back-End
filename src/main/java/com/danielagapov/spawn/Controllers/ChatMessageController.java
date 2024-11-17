package com.danielagapov.spawn.Controllers;

import com.danielagapov.spawn.Models.ChatMessage.ChatMessage;
import com.danielagapov.spawn.Services.ChatMessage.IChatMessageService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController()
@RequestMapping("/api/v1/chatMessages")
public class ChatMessageController {
    private final IChatMessageService chatMessageService;

    // here: we could either supply MockChatMessageService or ChatMessageService
    public ChatMessageController(IChatMessageService chatMessageService) {
        this.chatMessageService = chatMessageService;
    }

    // /api/v1/chatMessages
    @GetMapping("/")
    public String getChatMessages() {
        return "These are the chatMessages: " + chatMessageService.getAllChatMessages();
    }

    // /api/v1/chatMessages/mock-endpoint
    @GetMapping("/mock-endpoint")
    public String getMockEndpoint() {
        return "This is the mock endpoint for chatMessages. Everything is working with it.";
    }

    // /api/v1/chatMessages/{id}
    @GetMapping("/{id}")
    public ChatMessage getChatMessage(@PathVariable Long id) {
        return chatMessageService.getChatMessageById(id);
    }

    // /api/v1/chatMessages/tag/{id}

    // get chatMessage by tag
    @GetMapping("/tag/{id}")
    public List<ChatMessage> getChatMessagesByTagId(@PathVariable Long id) {
        return chatMessageService.getChatMessagesByTagId(id);
    }

    // /api/v1/chatMessages/
    @PostMapping("/")
    public ChatMessage createChatMessage(@RequestBody ChatMessage newChatMessage) {
        return chatMessageService.saveChatMessage(newChatMessage);
    }
}


