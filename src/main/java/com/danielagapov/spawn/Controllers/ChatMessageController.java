package com.danielagapov.spawn.Controllers;

import com.danielagapov.spawn.DTOs.ChatMessage.AbstractChatMessageDTO;
import com.danielagapov.spawn.DTOs.ChatMessage.ChatMessageDTO;
import com.danielagapov.spawn.DTOs.ChatMessage.ChatMessageLikesDTO;
import com.danielagapov.spawn.DTOs.ChatMessage.CreateChatMessageDTO;
import com.danielagapov.spawn.DTOs.User.BaseUserDTO;
import com.danielagapov.spawn.Exceptions.Base.BaseNotFoundException;
import com.danielagapov.spawn.Exceptions.Base.BasesNotFoundException;
import com.danielagapov.spawn.Services.ChatMessage.IChatMessageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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


    // Despite this not currently being used, let's not delete it.
    // Recently, I noticed that our `EventController::getFullEventById()` endpoint
    // wasn't being used -> so I removed it, but had to add it back for a new feature.
    // So, let's consider removing it in the future, but keep for now. 
    @Deprecated(since = "Not being used on mobile currently.")
    // full path: /api/v1/chatMessages?full=full
    @GetMapping
    public ResponseEntity<List<? extends AbstractChatMessageDTO>> getChatMessages(@RequestParam(value = "full", required = false) boolean full) {
        try {
            if (full) {
                return new ResponseEntity<>(chatMessageService.convertChatMessagesToFullFeedEventChatMessages(chatMessageService.getAllChatMessages()), HttpStatus.OK);
            } else {
                return new ResponseEntity<>(chatMessageService.getAllChatMessages(), HttpStatus.OK);
            }
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }


    // Despite this not currently being used, let's not delete it.
    // Recently, I noticed that our `EventController::getFullEventById()` endpoint
    // wasn't being used -> so I removed it, but had to add it back for a new feature.
    // So, let's consider removing it in the future, but keep for now. 
    @Deprecated(since = "Not being used on mobile currently.")
    // full path: /api/v1/chatMessages/{id}?full=full
    @GetMapping("/{id}")
    public ResponseEntity<AbstractChatMessageDTO> getChatMessage(@PathVariable UUID id, @RequestParam(value = "full", required = false) boolean full) {
        try {
            if (full) {
                return new ResponseEntity<>(chatMessageService.getFullChatMessageById(id), HttpStatus.OK);
            } else {
                return new ResponseEntity<>(chatMessageService.getChatMessageById(id), HttpStatus.OK);
            }
        } catch (BaseNotFoundException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // full path: /api/v1/chatMessages
    @PostMapping
    public ResponseEntity<ChatMessageDTO> createChatMessage(@RequestBody CreateChatMessageDTO newChatMessage) {
        try {
            return new ResponseEntity<>(chatMessageService.createChatMessage(newChatMessage), HttpStatus.CREATED);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }


    // Despite this not currently being used, let's not delete it.
    // Recently, I noticed that our `EventController::getFullEventById()` endpoint
    // wasn't being used -> so I removed it, but had to add it back for a new feature.
    // So, let's consider removing it in the future, but keep for now. 
    @Deprecated(since = "Not being used on mobile currently. " +
            "Pending mobile feature implementation, per:" +
            "https://github.com/Daggerpov/Spawn-App-iOS-SwiftUI/issues/142")
    // full path: /api/v1/chatMessages/{id}
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteChatMessage(@PathVariable UUID id) {
        if (id == null) return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        try {
            boolean isDeleted = chatMessageService.deleteChatMessageById(id);
            if (isDeleted) {
                return new ResponseEntity<>(HttpStatus.NO_CONTENT);
            } else {
                return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
            }
        } catch (BaseNotFoundException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }


    // Despite this not currently being used, let's not delete it.
    // Recently, I noticed that our `EventController::getFullEventById()` endpoint
    // wasn't being used -> so I removed it, but had to add it back for a new feature.
    // So, let's consider removing it in the future, but keep for now. 
    @Deprecated(since = "Not being used on mobile currently. " +
            "Pending mobile feature implementation, per:" +
            "https://github.com/Daggerpov/Spawn-App-iOS-SwiftUI/issues/142")
    // full path: /api/v1/chatMessages/{chatMessageId}/likes/{userId}
    @PostMapping("/{chatMessageId}/likes/{userId}")
    public ResponseEntity<ChatMessageLikesDTO> createChatMessageLike(@PathVariable UUID chatMessageId, @PathVariable UUID userId) {
        if (chatMessageId == null || userId == null) return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        try {
            return new ResponseEntity<>(chatMessageService.createChatMessageLike(chatMessageId, userId), HttpStatus.CREATED);
        } catch (BaseNotFoundException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // Despite this not currently being used, let's not delete it.
    // Recently, I noticed that our `EventController::getFullEventById()` endpoint
    // wasn't being used -> so I removed it, but had to add it back for a new feature.
    // So, let's consider removing it in the future, but keep for now.
    @Deprecated(since = "Not being used on mobile currently. " +
            "Pending mobile feature implementation, per:" +
            "https://github.com/Daggerpov/Spawn-App-iOS-SwiftUI/issues/142")
    // full path: /api/v1/chatMessages/{chatMessageId}/likes
    @GetMapping("/{chatMessageId}/likes")
    public ResponseEntity<List<BaseUserDTO>> getChatMessageLikes(@PathVariable UUID chatMessageId) {
        if (chatMessageId == null) return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        try {
            return new ResponseEntity<>(chatMessageService.getChatMessageLikes(chatMessageId), HttpStatus.OK);
        } catch (BaseNotFoundException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }



    // Despite this not currently being used, let's not delete it.
    // Recently, I noticed that our `EventController::getFullEventById()` endpoint
    // wasn't being used -> so I removed it, but had to add it back for a new feature.
    // So, let's consider removing it in the future, but keep for now.
    @Deprecated(since = "Not being used on mobile currently. " +
            "Pending mobile feature implementation, per:" +
            "https://github.com/Daggerpov/Spawn-App-iOS-SwiftUI/issues/142")
    @DeleteMapping("/{chatMessageId}/likes/{userId}")
    public ResponseEntity<Void> deleteChatMessageLike(@PathVariable UUID chatMessageId, @PathVariable UUID userId) {
        if (chatMessageId == null || userId == null) return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        try {
            chatMessageService.deleteChatMessageLike(chatMessageId, userId);
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        } catch (BasesNotFoundException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
