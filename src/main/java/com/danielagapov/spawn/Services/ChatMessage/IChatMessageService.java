package com.danielagapov.spawn.Services.ChatMessage;

import java.util.List;

import com.danielagapov.spawn.DTOs.ChatMessageDTO;

public interface IChatMessageService {
    public List<ChatMessageDTO> getAllChatMessages();
    public ChatMessageDTO getChatMessageById(Long id);
    public List<ChatMessageDTO> getChatMessagesByTagId(Long tagId);
    public ChatMessageDTO saveChatMessage(ChatMessageDTO chatMessage);
}
