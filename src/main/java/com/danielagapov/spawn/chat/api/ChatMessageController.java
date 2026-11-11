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
import com.danielagapov.spawn.shared.util.AuthenticatedUserProvider;
import com.danielagapov.spawn.shared.util.LoggingUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@RestController()
@RequestMapping("api/v1/chat-messages")
public class ChatMessageController {
    private final IChatMessageService chatMessageService;
    private final ILogger logger;
    private final AuthenticatedUserProvider authenticatedUserProvider;

    @Autowired
    public ChatMessageController(IChatMessageService chatMessageService, ILogger logger,
                                 AuthenticatedUserProvider authenticatedUserProvider) {
        this.chatMessageService = chatMessageService;
        this.logger = logger;
        this.authenticatedUserProvider = authenticatedUserProvider;
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


    // Used by the iOS Chatroom's long-press menu to delete your own message.
    // Only the message's sender can delete it.
    // full path: /api/v1/chat-messages/{id}
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteChatMessage(@PathVariable UUID id) {
        if (id == null) {
            logger.error("Invalid parameter: chat message ID is null");
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
        UUID requesterId = authenticatedUserProvider.getCurrentUserId().orElse(null);
        if (requesterId == null) {
            logger.error("Unauthenticated request to delete chat message: " + id);
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }
        try {
            boolean isDeleted = chatMessageService.deleteChatMessageById(id, requesterId);
            if (isDeleted) {
                return new ResponseEntity<>(HttpStatus.NO_CONTENT);
            } else {
                logger.error("Failed to delete chat message: " + id);
                return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
            }
        } catch (BaseNotFoundException e) {
            logger.error("Chat message not found for deletion: " + id + ": " + e.getMessage());
            return new ResponseEntity<>(null, HttpStatus.NOT_FOUND);
        } catch (AccessDeniedException e) {
            logger.warn("User " + LoggingUtils.formatUserIdInfo(requesterId) + " tried to delete chat message they didn't send: " + id);
            return new ResponseEntity<>(HttpStatus.FORBIDDEN);
        } catch (Exception e) {
            logger.error("Error deleting chat message: " + id + ": " + e.getMessage());
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }


    // Used by the iOS Chatroom's long-press menu to like a message. You can only like as yourself,
    // and liking twice is a no-op.
    // full path: /api/v1/chat-messages/{chatMessageId}/likes/{userId}
    @PostMapping("/{chatMessageId}/likes/{userId}")
    public ResponseEntity<ChatMessageLikesDTO> createChatMessageLike(@PathVariable UUID chatMessageId, @PathVariable UUID userId) {
        if (chatMessageId == null || userId == null) {
            logger.error("Invalid parameters: chatMessageId or userId is null");
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
        HttpStatus callerError = checkCallerIs(userId);
        if (callerError != null) {
            return new ResponseEntity<>(callerError);
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



    // Used by the iOS Chatroom's long-press menu to unlike a message. You can only unlike as yourself,
    // and unliking a message you haven't liked is a no-op.
    // full path: /api/v1/chat-messages/{chatMessageId}/likes/{userId}
    @DeleteMapping("/{chatMessageId}/likes/{userId}")
    public ResponseEntity<?> deleteChatMessageLike(@PathVariable UUID chatMessageId, @PathVariable UUID userId) {
        if (chatMessageId == null || userId == null) {
            logger.error("Invalid parameters: chatMessageId or userId is null");
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
        HttpStatus callerError = checkCallerIs(userId);
        if (callerError != null) {
            return new ResponseEntity<>(callerError);
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

    /**
     * Checks that the signed-in user is the one the request acts for.
     *
     * @return UNAUTHORIZED if the caller can't be resolved, FORBIDDEN if they're acting for someone else,
     * or null if it's them
     */
    private HttpStatus checkCallerIs(UUID userId) {
        UUID callerId = authenticatedUserProvider.getCurrentUserId().orElse(null);
        if (callerId == null) {
            logger.error("Unauthenticated chat message like request for user: " + LoggingUtils.formatUserIdInfo(userId));
            return HttpStatus.UNAUTHORIZED;
        }
        if (!callerId.equals(userId)) {
            logger.warn("User " + LoggingUtils.formatUserIdInfo(callerId) + " tried to change a like for user: "
                    + LoggingUtils.formatUserIdInfo(userId));
            return HttpStatus.FORBIDDEN;
        }
        return null;
    }
}
