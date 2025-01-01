package com.danielagapov.spawn.Services.ChatMessage;

import com.danielagapov.spawn.DTOs.ChatMessageDTO;
import com.danielagapov.spawn.DTOs.ChatMessageLikesDTO;
import com.danielagapov.spawn.DTOs.UserDTO;

import java.util.List;
import java.util.UUID;

public interface IChatMessageService {
    public List<ChatMessageDTO> getAllChatMessages();
    public ChatMessageDTO getChatMessageById(UUID id);
    public List<ChatMessageDTO> getChatMessagesByTagId(UUID tagId);
    public ChatMessageDTO saveChatMessage(ChatMessageDTO chatMessage);
    public boolean deleteChatMessageById(UUID id);
    public ChatMessageLikesDTO createChatMessageLike(UUID chatMessageId, UUID userId);
    public List<UserDTO> getChatMessageLikes(UUID chatMessageId);
    public void deleteChatMessageLike(UUID chatMessageId, UUID userId);
}
