package com.danielagapov.spawn.chat.api;

import com.danielagapov.spawn.chat.api.dto.AbstractChatMessageDTO;
import com.danielagapov.spawn.chat.api.dto.ChatMessageDTO;
import com.danielagapov.spawn.chat.api.dto.ChatMessageLikesDTO;
import com.danielagapov.spawn.chat.api.dto.CreateChatMessageDTO;
import com.danielagapov.spawn.chat.api.dto.FullActivityChatMessageDTO;
import com.danielagapov.spawn.user.api.dto.BaseUserDTO;
import com.danielagapov.spawn.shared.util.EntityType;
import com.danielagapov.spawn.shared.exceptions.Base.BaseNotFoundException;
import com.danielagapov.spawn.shared.exceptions.Base.BasesNotFoundException;
import com.danielagapov.spawn.shared.exceptions.Logger.ILogger;
import com.danielagapov.spawn.chat.internal.services.IChatMessageService;
import com.danielagapov.spawn.shared.util.LoggingUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@RestController()
@RequestMapping("api/v1/chat-messages")
public final class ChatMessageController {
    private final IChatMessageService chatMessageService;
    private final ILogger logger;

    @Autowired
    public ChatMessageController(IChatMessageService chatMessageService, ILogger logger) {
        this.chatMessageService = chatMessageService;
        this.logger = logger;
    }

    // full path: /api/v1/chat-messages
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
    // full path: /api/v1/chat-messages/{id}
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
    // full path: /api/v1/chat-messages/{chatMessageId}/likes/{userId}
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
    // full path: /api/v1/chat-messages/{chatMessageId}/likes
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
    // full path: /api/v1/chat-messages/{chatMessageId}/likes/{userId}
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
