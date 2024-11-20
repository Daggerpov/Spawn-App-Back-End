package com.danielagapov.spawn.Services.ChatMessage;

import com.danielagapov.spawn.DTOs.ChatMessageDTO;
import com.danielagapov.spawn.Exceptions.Base.BasesNotFoundException;
import com.danielagapov.spawn.Exceptions.Base.BaseNotFoundException;
import com.danielagapov.spawn.Exceptions.Base.BaseSaveException;
import com.danielagapov.spawn.Mappers.ChatMessageMapper;
import com.danielagapov.spawn.Models.ChatMessage.ChatMessage;
import com.danielagapov.spawn.Repositories.IChatMessageRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class ChatMessageService implements IChatMessageService {
    private final IChatMessageRepository repository;

    @Autowired
    public ChatMessageService(IChatMessageRepository repository) {
        this.repository = repository;
    }

    public List<ChatMessageDTO> getAllChatMessages() {
        try {
            return ChatMessageMapper.toDTOList(repository.findAll());
        } catch (DataAccessException e) {
            throw new BasesNotFoundException();
        }
    }

    public ChatMessageDTO getChatMessageById(UUID id) {
        return ChatMessageMapper.toDTO(repository.findById(id)
                .orElseThrow(() -> new BaseNotFoundException(id)));
    }

    public List<ChatMessageDTO> getChatMessagesByTagId(UUID tagId) {
        // TODO: change this logic later, once tags are setup.
        try {
            return ChatMessageMapper.toDTOList(repository.findAll());
        } catch (DataAccessException e) {
            throw new RuntimeException("Error retrieving chatMessages", e);
        }
    }

    public ChatMessageDTO saveChatMessage(ChatMessageDTO chatMessage) {
        try {
            ChatMessage chatMessageEntity = ChatMessageMapper.toEntity(chatMessage);
            repository.save(chatMessageEntity);
            return ChatMessageMapper.toDTO(chatMessageEntity);
        } catch (DataAccessException e) {
            throw new BaseSaveException("Failed to save chatMessage: " + e.getMessage());
        }
    }
}
