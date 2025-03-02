package com.danielagapov.spawn.Services.ChatMessage;

import com.danielagapov.spawn.DTOs.ChatMessage.ChatMessageDTO;
import com.danielagapov.spawn.DTOs.ChatMessage.FullEventChatMessageDTO;
import com.danielagapov.spawn.DTOs.ChatMessageLikesDTO;
import com.danielagapov.spawn.DTOs.User.BaseUserDTO;

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

    List<BaseUserDTO> getChatMessageLikes(UUID chatMessageId);

    void deleteChatMessageLike(UUID chatMessageId, UUID userId);

    List<UUID> getChatMessageLikeUserIds(UUID chatMessageId);


    // full chat messages:
    FullEventChatMessageDTO getFullChatMessageByChatMessage(ChatMessageDTO chatMessage);

    FullEventChatMessageDTO getFullChatMessageById(UUID id);

    List<FullEventChatMessageDTO> getFullChatMessagesByEventId(UUID eventId);

    List<FullEventChatMessageDTO> convertChatMessagesToFullFeedEventChatMessages(List<ChatMessageDTO> chatMessages);
}
