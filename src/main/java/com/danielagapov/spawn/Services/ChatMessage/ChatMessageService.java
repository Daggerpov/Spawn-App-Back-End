package com.danielagapov.spawn.Services.ChatMessage;

import com.danielagapov.spawn.Exceptions.Base.BasesNotFoundException;
import com.danielagapov.spawn.Exceptions.Base.BaseNotFoundException;
import com.danielagapov.spawn.Exceptions.Base.BaseSaveException;
import com.danielagapov.spawn.Models.ChatMessage.ChatMessage;
import com.danielagapov.spawn.Repositories.IChatMessageRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ChatMessageService implements IChatMessageService {
    private final IChatMessageRepository repository;

    @Autowired
    public ChatMessageService(IChatMessageRepository repository) {
        this.repository = repository;
    }

    public List<ChatMessage> getAllChatMessages() {
        try {
            return repository.findAll();
        } catch (DataAccessException e) {
            throw new BasesNotFoundException();
        }
    }

    public ChatMessage getChatMessageById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new BaseNotFoundException(id));
    }

    public List<ChatMessage> getChatMessagesByTagId(Long tagId) {
        // TODO: change this logic later, once tags are setup.
        try {
            return repository.findAll();
        } catch (DataAccessException e) {
            throw new RuntimeException("Error retrieving chatMessages", e);
        }
    }

    public ChatMessage saveChatMessage(ChatMessage chatMessage) {
        try {
            return repository.save(chatMessage);
        } catch (DataAccessException e) {
            throw new BaseSaveException("Failed to save chatMessage: " + e.getMessage());
        }
    }
}
