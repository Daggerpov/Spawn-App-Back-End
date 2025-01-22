package com.danielagapov.spawn.Services.ChatMessage;

import com.danielagapov.spawn.DTOs.ChatMessageDTO;
import com.danielagapov.spawn.DTOs.ChatMessageLikesDTO;
import com.danielagapov.spawn.DTOs.FullChatMessageDTO;
import com.danielagapov.spawn.DTOs.UserDTO;
import com.danielagapov.spawn.Models.ChatMessage;

import java.util.List;
import java.util.UUID;

public interface IChatMessageService {
    List<ChatMessageDTO> getAllChatMessages();
    ChatMessageDTO getChatMessageById(UUID id);
    FullChatMessageDTO getFullChatMessageById(UUID id);
    ChatMessageDTO saveChatMessage(ChatMessageDTO chatMessage);
    boolean deleteChatMessageById(UUID id);
    ChatMessageLikesDTO createChatMessageLike(UUID chatMessageId, UUID userId);
    List<UserDTO> getChatMessageLikes(UUID chatMessageId);
    void deleteChatMessageLike(UUID chatMessageId, UUID userId);
    List<ChatMessageDTO> getChatMessagesByEventId(UUID eventId);
    List<UUID> getChatMessageLikeUserIds(UUID chatMessageId);
    List<UUID> getChatMessageIdsByEventId(UUID eventId);
    FullChatMessageDTO getFullChatMessageByChatMessage(ChatMessageDTO chatMessage);
}
