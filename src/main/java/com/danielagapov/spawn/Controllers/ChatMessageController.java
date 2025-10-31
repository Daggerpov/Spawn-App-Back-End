package com.danielagapov.spawn.Controllers;

import com.danielagapov.spawn.DTOs.ChatMessage.AbstractChatMessageDTO;
import com.danielagapov.spawn.DTOs.ChatMessage.ChatMessageDTO;
import com.danielagapov.spawn.DTOs.ChatMessage.ChatMessageLikesDTO;
import com.danielagapov.spawn.DTOs.ChatMessage.CreateChatMessageDTO;
import com.danielagapov.spawn.DTOs.ChatMessage.FullActivityChatMessageDTO;
import com.danielagapov.spawn.DTOs.User.BaseUserDTO;
import com.danielagapov.spawn.Enums.EntityType;
import com.danielagapov.spawn.Exceptions.Base.BaseNotFoundException;
import com.danielagapov.spawn.Exceptions.Base.BasesNotFoundException;
import com.danielagapov.spawn.Exceptions.Logger.ILogger;
import com.danielagapov.spawn.Services.ChatMessage.IChatMessageService;
import com.danielagapov.spawn.Util.LoggingUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@RestController()
@RequestMapping("api/v1/chatMessages")
public final class ChatMessageController {
    private final IChatMessageService chatMessageService;
    private final ILogger logger;

    @Autowired
    public ChatMessageController(IChatMessageService chatMessageService, ILogger logger) {
        this.chatMessageService = chatMessageService;
        this.logger = logger;
    }

    // full path: /api/v1/chatMessages
    @PostMapping
    public ResponseEntity<FullActivityChatMessageDTO> createChatMessage(@RequestBody CreateChatMessageDTO newChatMessage) {
        try {
            FullActivityChatMessageDTO createdMessage = chatMessageService.createChatMessage(newChatMessage);
            return new ResponseEntity<>(createdMessage, HttpStatus.CREATED);
        } catch (Exception e) {
            logger.error("Error creating chat message: " + e.getMessage());
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }


    // TL;DR: Don't remove this endpoint; it may become useful. 
    @Deprecated(since = "Not being used on mobile currently. " +
            "Pending mobile feature implementation, per:" +
            "https://github.com/Daggerpov/Spawn-App-iOS-SwiftUI/issues/142")
    // full path: /api/v1/chatMessages/{id}
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteChatMessage(@PathVariable UUID id) {
        if (id == null) {
            logger.error("Invalid parameter: chat message ID is null");
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
        try {
            boolean isDeleted = chatMessageService.deleteChatMessageById(id);
            if (isDeleted) {
                return new ResponseEntity<>(HttpStatus.NO_CONTENT);
            } else {
                logger.error("Failed to delete chat message: " + id);
                return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
            }
        } catch (BaseNotFoundException e) {
            logger.error("Chat message not found for deletion: " + id + ": " + e.getMessage());
            return new ResponseEntity<>(null, HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            logger.error("Error deleting chat message: " + id + ": " + e.getMessage());
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }


    // TL;DR: Don't remove this endpoint; it may become useful. 
    @Deprecated(since = "Not being used on mobile currently. " +
            "Pending mobile feature implementation, per:" +
            "https://github.com/Daggerpov/Spawn-App-iOS-SwiftUI/issues/142")
    // full path: /api/v1/chatMessages/{chatMessageId}/likes/{userId}
    @PostMapping("/{chatMessageId}/likes/{userId}")
    public ResponseEntity<ChatMessageLikesDTO> createChatMessageLike(@PathVariable UUID chatMessageId, @PathVariable UUID userId) {
        if (chatMessageId == null || userId == null) {
            logger.error("Invalid parameters: chatMessageId or userId is null");
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
        try {
            ChatMessageLikesDTO createdLike = chatMessageService.createChatMessageLike(chatMessageId, userId);
            return new ResponseEntity<>(createdLike, HttpStatus.CREATED);
        } catch (BaseNotFoundException e) {
            logger.error("Chat message or user not found for like creation: " + e.getMessage());
            return new ResponseEntity<ChatMessageLikesDTO>(HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            logger.error("Error creating chat message like for message: " + chatMessageId + " by user: " + LoggingUtils.formatUserIdInfo(userId) + ": " + e.getMessage());
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // TL;DR: Don't remove this endpoint; it may become useful.
    @Deprecated(since = "Not being used on mobile currently. " +
            "Pending mobile feature implementation, per:" +
            "https://github.com/Daggerpov/Spawn-App-iOS-SwiftUI/issues/142")
    // full path: /api/v1/chatMessages/{chatMessageId}/likes
    @GetMapping("/{chatMessageId}/likes")
    public ResponseEntity<List<BaseUserDTO>> getChatMessageLikes(@PathVariable UUID chatMessageId) {
        if (chatMessageId == null) {
            logger.error("Invalid parameter: chatMessageId is null");
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
        try {
            return new ResponseEntity<>(chatMessageService.getChatMessageLikes(chatMessageId), HttpStatus.OK);
        } catch (BaseNotFoundException e) {
            logger.error("Chat message not found for likes retrieval: " + chatMessageId + ": " + e.getMessage());
            return new ResponseEntity<List<BaseUserDTO>>(HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            logger.error("Error getting chat message likes for message: " + chatMessageId + ": " + e.getMessage());
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }



    // TL;DR: Don't remove this endpoint; it may become useful.
    @Deprecated(since = "Not being used on mobile currently. " +
            "Pending mobile feature implementation, per:" +
            "https://github.com/Daggerpov/Spawn-App-iOS-SwiftUI/issues/142")
    @DeleteMapping("/{chatMessageId}/likes/{userId}")
    public ResponseEntity<?> deleteChatMessageLike(@PathVariable UUID chatMessageId, @PathVariable UUID userId) {
        if (chatMessageId == null || userId == null) {
            logger.error("Invalid parameters: chatMessageId or userId is null");
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
        try {
            chatMessageService.deleteChatMessageLike(chatMessageId, userId);
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        } catch (BaseNotFoundException e) {
            logger.error("Chat message like not found for deletion: " + e.getMessage());
            return new ResponseEntity<>(e.entityType, HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            logger.error("Error deleting chat message like for message: " + chatMessageId + " by user: " + LoggingUtils.formatUserIdInfo(userId) + ": " + e.getMessage());
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
