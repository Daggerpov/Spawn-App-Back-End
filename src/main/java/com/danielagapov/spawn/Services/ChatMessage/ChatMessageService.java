package com.danielagapov.spawn.Services.ChatMessage;

import com.danielagapov.spawn.DTOs.ChatMessageDTO;
import com.danielagapov.spawn.Exceptions.Base.BasesNotFoundException;
import com.danielagapov.spawn.Exceptions.Base.BaseNotFoundException;
import com.danielagapov.spawn.Exceptions.Base.BaseSaveException;
import com.danielagapov.spawn.Mappers.ChatMessageMapper;
import com.danielagapov.spawn.Models.ChatMessage.ChatMessage;
import com.danielagapov.spawn.Models.Event.Event;
import com.danielagapov.spawn.Models.User.User;
import com.danielagapov.spawn.Repositories.IChatMessageRepository;
import com.danielagapov.spawn.Repositories.IEventRepository;
import com.danielagapov.spawn.Repositories.IUserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class ChatMessageService implements IChatMessageService {
    private final IChatMessageRepository chatMessageRepository;
    private final IUserRepository userRepository;
    private final IEventRepository eventRepository;

    @Autowired
    public ChatMessageService(IChatMessageRepository chatMessageRepository, IUserRepository userRepository, IEventRepository eventRepository) {
        this.chatMessageRepository = chatMessageRepository;
        this.userRepository = userRepository;
        this.eventRepository = eventRepository;
    }

    public List<ChatMessageDTO> getAllChatMessages() {
        try {
            return ChatMessageMapper.toDTOList(chatMessageRepository.findAll());
        } catch (DataAccessException e) {
            throw new BasesNotFoundException();
        }
    }

    public ChatMessageDTO getChatMessageById(UUID id) {
        return ChatMessageMapper.toDTO(chatMessageRepository.findById(id)
                .orElseThrow(() -> new BaseNotFoundException(id)));
    }

    public List<ChatMessageDTO> getChatMessagesByTagId(UUID tagId) {
        // TODO: change this logic later, once tags are setup.
        try {
            return ChatMessageMapper.toDTOList(chatMessageRepository.findAll());
        } catch (DataAccessException e) {
            throw new RuntimeException("Error retrieving chatMessages", e);
        }
    }

    public ChatMessageDTO saveChatMessage(ChatMessageDTO chatMessageDTO) {
        try {
            // Fetch User and Event entities
            User userSender = userRepository.findById(chatMessageDTO.userSenderId())
                    .orElseThrow(() -> new BaseNotFoundException(chatMessageDTO.userSenderId()));
            Event event = eventRepository.findById(chatMessageDTO.eventId())
                    .orElseThrow(() -> new BaseNotFoundException(chatMessageDTO.eventId()));

            // Convert DTO to Entity
            ChatMessage chatMessageEntity = ChatMessageMapper.toEntity(chatMessageDTO, userSender, event);

            // Save Entity
            chatMessageRepository.save(chatMessageEntity);

            // Convert Entity back to DTO and return
            return ChatMessageMapper.toDTO(chatMessageEntity);
        } catch (DataAccessException e) {
            throw new BaseSaveException("Failed to save chatMessage: " + e.getMessage());
        }
    }
}
