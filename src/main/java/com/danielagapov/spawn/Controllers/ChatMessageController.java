package com.danielagapov.spawn.Controllers;

import com.danielagapov.spawn.DTOs.ChatMessageDTO;
import com.danielagapov.spawn.Exceptions.Base.BaseDeleteException;
import com.danielagapov.spawn.Exceptions.Base.BaseNotFoundException;
import com.danielagapov.spawn.Exceptions.Base.BasesNotFoundException;

import com.danielagapov.spawn.Services.ChatMessage.IChatMessageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.danielagapov.spawn.DTOs.UserDTO;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.danielagapov.spawn.DTOs.ChatMessageLikesDTO;

import java.util.List;
import java.util.UUID;

@RestController()
@RequestMapping("api/v1/chatMessages")
public class ChatMessageController {
    private final IChatMessageService chatMessageService;

    @Autowired
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


    // full path: /api/v1/chatMessages/{id}
    @DeleteMapping("/{id}")
    public ResponseEntity<HttpStatus> deleteChatMessage(@PathVariable UUID id) {
        try {
            boolean isDeleted = chatMessageService.deleteChatMessageById(id);
            if (isDeleted) {
                return new ResponseEntity<>(HttpStatus.NO_CONTENT); // Success
            } else {
                return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR); // Deletion failed
            }
        } catch (BaseNotFoundException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND); // Resource not found
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR); // Unexpected error
        }
    }

    // full path: /api/v1/chatMessages/{chatMessageId}/likes
    @PostMapping("/{chatMessageId}/likes/{userId}")
    public ChatMessageLikesDTO createChatMessageLike(@PathVariable UUID chatMessageId, @PathVariable UUID userId) {
        return chatMessageService.createChatMessageLike(chatMessageId, userId);
    }

    // full path: /api/v1/chatMessages/{chatMessageId}/likes
    @GetMapping("/{chatMessageId}/likes")
    public List<UserDTO> getChatMessageLikes(@PathVariable UUID chatMessageId) {
        return chatMessageService.getChatMessageLikes(chatMessageId);
    }

    // full path: /api/v1/chatMessages/{chatMessageId}/likes
    @DeleteMapping("/{chatMessageId}/likes")
    public ResponseEntity<Void> deleteChatMessageLike(@PathVariable UUID chatMessageId, @PathVariable UUID userId) {
        try {
            chatMessageService.deleteChatMessageLike(chatMessageId, userId);
            return ResponseEntity.noContent().build(); // 204 success (no content)
        } catch (BasesNotFoundException e) {
            return ResponseEntity.notFound().build(); // 404
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build(); // 500
        }
    }
}


