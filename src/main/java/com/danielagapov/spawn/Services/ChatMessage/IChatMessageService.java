package com.danielagapov.spawn.Services.ChatMessage;

import java.util.List;
import com.danielagapov.spawn.Models.ChatMessage.ChatMessage;

public interface IChatMessageService {
    public List<ChatMessage> getAllChatMessages();
    public ChatMessage getChatMessageById(Long id);
    public List<ChatMessage> getChatMessagesByTagId(Long tagId);
    public ChatMessage saveChatMessage(ChatMessage event);
}
