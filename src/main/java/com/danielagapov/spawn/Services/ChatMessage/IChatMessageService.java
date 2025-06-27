package com.danielagapov.spawn.Services.ChatMessage;

import com.danielagapov.spawn.DTOs.ChatMessage.ChatMessageDTO;
import com.danielagapov.spawn.DTOs.ChatMessage.ChatMessageLikesDTO;
import com.danielagapov.spawn.DTOs.ChatMessage.CreateChatMessageDTO;
import com.danielagapov.spawn.DTOs.ChatMessage.FullActivityChatMessageDTO;
import com.danielagapov.spawn.DTOs.User.BaseUserDTO;

import java.util.List;
import java.util.UUID;

public interface IChatMessageService {
    List<ChatMessageDTO> getAllChatMessages();

    // CRUD operations:
    ChatMessageDTO getChatMessageById(UUID id);

    ChatMessageDTO createChatMessage(CreateChatMessageDTO newChatMessageDTO);

    ChatMessageDTO saveChatMessage(ChatMessageDTO chatMessage);

    boolean deleteChatMessageById(UUID id);

    // by Activity:
    List<ChatMessageDTO> getChatMessagesByActivityId(UUID ActivityId);

    List<UUID> getChatMessageIdsByActivityId(UUID ActivityId);

    // chat message likes:
    ChatMessageLikesDTO createChatMessageLike(UUID chatMessageId, UUID userId);

    List<BaseUserDTO> getChatMessageLikes(UUID chatMessageId);

    void deleteChatMessageLike(UUID chatMessageId, UUID userId);

    List<UUID> getChatMessageLikeUserIds(UUID chatMessageId);


    // full chat messages:
    FullActivityChatMessageDTO getFullChatMessageByChatMessage(ChatMessageDTO chatMessage);

    FullActivityChatMessageDTO getFullChatMessageById(UUID id);

    List<FullActivityChatMessageDTO> getFullChatMessagesByActivityId(UUID activityId);

    List<FullActivityChatMessageDTO> convertChatMessagesToFullFeedActivityChatMessages(List<ChatMessageDTO> chatMessages);
}
