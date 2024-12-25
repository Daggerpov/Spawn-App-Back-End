package com.danielagapov.spawn.Services.ChatMessage;

import com.danielagapov.spawn.DTOs.ChatMessageDTO;
import com.danielagapov.spawn.Enums.EntityType;
import com.danielagapov.spawn.Exceptions.Base.*;
import com.danielagapov.spawn.Mappers.ChatMessageMapper;
import com.danielagapov.spawn.Mappers.UserMapper;
import com.danielagapov.spawn.Models.ChatMessage;
import com.danielagapov.spawn.Models.ChatMessageLikes;
import com.danielagapov.spawn.Models.Event;
import com.danielagapov.spawn.Models.User;
import com.danielagapov.spawn.Repositories.IChatMessageLikesRepository;
import com.danielagapov.spawn.Repositories.IChatMessageRepository;
import com.danielagapov.spawn.Repositories.IEventRepository;
import com.danielagapov.spawn.Repositories.IUserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

import com.danielagapov.spawn.DTOs.UserDTO;
import com.danielagapov.spawn.DTOs.ChatMessageLikesDTO;

import com.danielagapov.spawn.Mappers.ChatMessageLikesMapper;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ChatMessageService implements IChatMessageService {
    private final IChatMessageRepository chatMessageRepository;
    private final IUserRepository userRepository;
    private final IEventRepository eventRepository;

    private final IChatMessageLikesRepository chatMessageLikesRepository;
    @Autowired
    public ChatMessageService(IChatMessageRepository chatMessageRepository, IUserRepository userRepository,
                              IEventRepository eventRepository, IChatMessageLikesRepository chatMessageLikesRepository) {
        this.chatMessageRepository = chatMessageRepository;
        this.userRepository = userRepository;
        this.eventRepository = eventRepository;
        this.chatMessageLikesRepository = chatMessageLikesRepository;
    }

    public List<ChatMessageDTO> getAllChatMessages() {
        try {
            return ChatMessageMapper.toDTOList(chatMessageRepository.findAll());
        } catch (DataAccessException e) {
            throw new BasesNotFoundException("chatMessages");
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
            User userSender = userRepository.findById(chatMessageDTO.userSenderId())
                    .orElseThrow(() -> new BaseNotFoundException(chatMessageDTO.userSenderId()));
            Event event = eventRepository.findById(chatMessageDTO.eventId())
                    .orElseThrow(() -> new BaseNotFoundException(chatMessageDTO.eventId()));

            ChatMessage chatMessageEntity = ChatMessageMapper.toEntity(chatMessageDTO, userSender, event);

            chatMessageRepository.save(chatMessageEntity);

            return ChatMessageMapper.toDTO(chatMessageEntity);
        } catch (DataAccessException e) {
            throw new BaseSaveException("Failed to save chatMessage: " + e.getMessage());
        }
    }


    public boolean deleteChatMessageById(UUID id) {
        if (!chatMessageRepository.existsById(id)) {
            throw new BaseNotFoundException(id);
        }

        try {
            chatMessageRepository.deleteById(id);
            return true;
        } catch (DataAccessException e) {
            return false;
        }
    }

    public ChatMessageLikesDTO createChatMessageLike(UUID chatMessageId, UUID userId) {
        try {
            boolean exists = chatMessageLikesRepository.existsByChatMessage_IdAndUser_Id(chatMessageId, userId);
            if (exists) {
                throw new EntityAlreadyExistsException(EntityType.ChatMessage, chatMessageId);
            }
            ChatMessage chatMessage = chatMessageRepository.findById(chatMessageId)
                    .orElseThrow(() -> new BaseSaveException("ChatMessageId: " + chatMessageId));
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new BaseSaveException("UserId: " + userId));

            ChatMessageLikes chatMessageLikes = new ChatMessageLikes();
            chatMessageLikes.setChatMessage(chatMessage);
            chatMessageLikes.setUser(user);

            chatMessageLikesRepository.save(chatMessageLikes);
            return ChatMessageLikesMapper.toDTO(chatMessageLikes);

        } catch (Exception e) {
            throw new BaseSaveException("Like: chatMessageId: " + chatMessageId + " userId: "
                    + userId + ". Error: " + e.getMessage());
        }
    }

    public List<UserDTO> getChatMessageLikes(UUID chatMessageId) {

        ChatMessage chatMessage = chatMessageRepository.findById(chatMessageId)
                .orElseThrow(() -> new BaseNotFoundException(chatMessageId));

        List<ChatMessageLikes> likes = chatMessageLikesRepository.findByChatMessage(chatMessage);

        return likes.stream().map(like -> UserMapper.toDTO(like.getUser())).collect(Collectors.toList());
    }

    public void deleteChatMessageLike(UUID chatMessageId, UUID userId) {
        try {
            boolean exists = chatMessageLikesRepository.existsByChatMessage_IdAndUser_Id(chatMessageId, userId);
            if (!exists) {
                throw new BasesNotFoundException(EntityType.ChatMessage, chatMessageId);
            }
            chatMessageLikesRepository.deleteByChatMessage_IdAndUser_Id(chatMessageId, userId);
        } catch (Exception e) {
            throw new BaseDeleteException("An error occurred while deleting the like for chatMessageId: "
                    + chatMessageId + " and userId: " + userId + ". Error: " + e.getMessage(), e);
        }
    }
}
