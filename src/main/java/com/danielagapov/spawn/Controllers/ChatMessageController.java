package com.danielagapov.spawn.Controllers;

import com.danielagapov.spawn.DTOs.ChatMessageDTO;
import com.danielagapov.spawn.Services.ChatMessage.IChatMessageService;
import org.springframework.web.bind.annotation.*;

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
    public ChatMessageDTO getChatMessage(@PathVariable Long id) {
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
}


