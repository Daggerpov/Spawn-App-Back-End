package com.danielagapov.spawn.Services.ChatMessage;

import java.util.List;
import java.util.UUID;

import com.danielagapov.spawn.DTOs.ChatMessageDTO;

public interface IChatMessageService {
    public List<ChatMessageDTO> getAllChatMessages();
    public ChatMessageDTO getChatMessageById(UUID id);
    public List<ChatMessageDTO> getChatMessagesByTagId(UUID tagId);
    public ChatMessageDTO saveChatMessage(ChatMessageDTO chatMessage);
}
