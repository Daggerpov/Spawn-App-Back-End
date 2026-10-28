package com.danielagapov.spawn.chat.api;

import com.danielagapov.spawn.chat.api.dto.ChatMessageDTO;
import com.danielagapov.spawn.chat.api.dto.ChatMessageLikesDTO;
import com.danielagapov.spawn.chat.api.dto.CreateChatMessageDTO;
import com.danielagapov.spawn.chat.api.dto.FullActivityChatMessageDTO;
import com.danielagapov.spawn.chat.internal.services.ChatMessageService;
import com.danielagapov.spawn.user.api.dto.BaseUserDTO;
import com.danielagapov.spawn.shared.exceptions.Base.BaseNotFoundException;
import com.danielagapov.spawn.shared.exceptions.Logger.ILogger;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("api/v1/chat-messages")
public class ChatMessageController {

    private final ChatMessageService chatMessageService;
    private final ILogger logger;

    public ChatMessageController(ChatMessageService chatMessageService, ILogger logger) {
        this.chatMessageService = chatMessageService;
        this.logger = logger;
    }

    @GetMapping("/by-activity/{activityId}")
    public ResponseEntity<List<FullActivityChatMessageDTO>> getChatMessagesByActivity(@PathVariable UUID activityId) {
        try {
            List<FullActivityChatMessageDTO> messages = chatMessageService.getFullChatMessagesByActivityId(activityId);
            return ResponseEntity.ok(messages);
        } catch (Exception e) {
            logger.error("Error getting chat messages for activity: " + activityId + ": " + e.getMessage());
            return ResponseEntity.ok(new ArrayList<>());
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<ChatMessageDTO> getChatMessageById(@PathVariable UUID id) {
        try {
            return ResponseEntity.ok(chatMessageService.getChatMessageById(id));
        } catch (BaseNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping
    public ResponseEntity<FullActivityChatMessageDTO> createChatMessage(@RequestBody CreateChatMessageDTO newChatMessage) {
        try {
            FullActivityChatMessageDTO created = chatMessageService.createChatMessage(newChatMessage);
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } catch (Exception e) {
            logger.error("Error creating chat message: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteChatMessage(@PathVariable UUID id) {
        if (id == null) {
            return ResponseEntity.badRequest().build();
        }
        try {
            boolean deleted = chatMessageService.deleteChatMessageById(id);
            return deleted ? ResponseEntity.noContent().build() : ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        } catch (BaseNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            logger.error("Error deleting chat message: " + id + ": " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/{chatMessageId}/likes/{userId}")
    public ResponseEntity<ChatMessageLikesDTO> createChatMessageLike(@PathVariable UUID chatMessageId, @PathVariable UUID userId) {
        if (chatMessageId == null || userId == null) {
            return ResponseEntity.badRequest().build();
        }
        try {
            ChatMessageLikesDTO created = chatMessageService.createChatMessageLike(chatMessageId, userId);
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } catch (BaseNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            logger.error("Error creating like: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/{chatMessageId}/likes")
    public ResponseEntity<List<BaseUserDTO>> getChatMessageLikes(@PathVariable UUID chatMessageId) {
        if (chatMessageId == null) {
            return ResponseEntity.badRequest().build();
        }
        try {
            return ResponseEntity.ok(chatMessageService.getChatMessageLikes(chatMessageId));
        } catch (BaseNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            logger.error("Error getting likes: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @DeleteMapping("/{chatMessageId}/likes/{userId}")
    public ResponseEntity<Void> deleteChatMessageLike(@PathVariable UUID chatMessageId, @PathVariable UUID userId) {
        if (chatMessageId == null || userId == null) {
            return ResponseEntity.badRequest().build();
        }
        try {
            chatMessageService.deleteChatMessageLike(chatMessageId, userId);
            return ResponseEntity.noContent().build();
        } catch (BaseNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            logger.error("Error deleting like: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
