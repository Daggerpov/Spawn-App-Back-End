package com.danielagapov.spawn.Services.ChatMessage;

import com.danielagapov.spawn.DTOs.ChatMessageDTO;
import com.danielagapov.spawn.DTOs.ChatMessageLikesDTO;
import com.danielagapov.spawn.DTOs.FullChatMessageDTO;
import com.danielagapov.spawn.DTOs.UserDTO;

import java.util.List;
import java.util.UUID;

public interface IChatMessageService {
    List<ChatMessageDTO> getAllChatMessages();

    // CRUD operations:
    ChatMessageDTO getChatMessageById(UUID id);
    ChatMessageDTO saveChatMessage(ChatMessageDTO chatMessage);
    boolean deleteChatMessageById(UUID id);

    // by event:
    List<ChatMessageDTO> getChatMessagesByEventId(UUID eventId);
    List<UUID> getChatMessageIdsByEventId(UUID eventId);

    // chat message likes:
    ChatMessageLikesDTO createChatMessageLike(UUID chatMessageId, UUID userId);
    List<UserDTO> getChatMessageLikes(UUID chatMessageId);
    void deleteChatMessageLike(UUID chatMessageId, UUID userId);
    List<UUID> getChatMessageLikeUserIds(UUID chatMessageId);


    // full chat messages:
    FullChatMessageDTO getFullChatMessageByChatMessage(ChatMessageDTO chatMessage);
    FullChatMessageDTO getFullChatMessageById(UUID id);
    List<FullChatMessageDTO> getFullChatMessagesByEventId(UUID eventId);
}
