package com.danielagapov.spawn.Services.ChatMessage;

import com.danielagapov.spawn.DTOs.ChatMessageDTO;
import com.danielagapov.spawn.DTOs.ChatMessageLikesDTO;
import com.danielagapov.spawn.DTOs.FriendTagDTO;
import com.danielagapov.spawn.DTOs.UserDTO;
import com.danielagapov.spawn.Enums.EntityType;
import com.danielagapov.spawn.Exceptions.Base.*;
import com.danielagapov.spawn.Exceptions.EntityAlreadyExistsException;
import com.danielagapov.spawn.Mappers.ChatMessageLikesMapper;
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
import com.danielagapov.spawn.Services.FriendTag.IFriendTagService;
import com.danielagapov.spawn.Services.User.IUserService;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ChatMessageService implements IChatMessageService {
    private final IChatMessageRepository chatMessageRepository;
    private final IUserService userService;
    private final IEventRepository eventRepository;
    private final IFriendTagService ftService;
    private final IUserRepository userRepository;

    private final IChatMessageLikesRepository chatMessageLikesRepository;

    public ChatMessageService(IChatMessageRepository chatMessageRepository, IUserService userService,
                              IEventRepository eventRepository, IChatMessageLikesRepository chatMessageLikesRepository,
                              IFriendTagService ftService, IUserRepository userRepository) {
        this.chatMessageRepository = chatMessageRepository;
        this.userService = userService;
        this.eventRepository = eventRepository;
        this.chatMessageLikesRepository = chatMessageLikesRepository;
        this.ftService = ftService;
        this.userRepository = userRepository;
    }

    public List<ChatMessageDTO> getAllChatMessages() {
        try {
            List<ChatMessage> chatMessages = chatMessageRepository.findAll();

            // Fetch the userSender and likedBy data for each chatMessage
            Map<ChatMessage, UserDTO> userSenderMap = chatMessages.stream()
                    .collect(Collectors.toMap(chatMessage -> chatMessage, chatMessage -> userService.getUserById(chatMessage.getUserSender().getId())));

            Map<ChatMessage, List<UserDTO>> likedByMap = chatMessages.stream()
                    .collect(Collectors.toMap(
                            chatMessage -> chatMessage,
                            chatMessage -> getChatMessageLikes(chatMessage.getId())
                    ));

            return ChatMessageMapper.toDTOList(chatMessages, userSenderMap, likedByMap);
        } catch (DataAccessException e) {
            throw new BasesNotFoundException(EntityType.ChatMessage);
        }
    }

    public ChatMessageDTO getChatMessageById(UUID id) {
        return chatMessageRepository.findById(id)
                .map(chatMessage -> {
                    UserDTO userSender = userService.getUserById(chatMessage.getUserSender().getId());
                    List<UserDTO> likedBy = getChatMessageLikes(chatMessage.getId());
                    return ChatMessageMapper.toDTO(chatMessage, userSender, likedBy);
                })
                .orElseThrow(() -> new BaseNotFoundException(EntityType.ChatMessage, id));
    }

    // Other methods remain mostly the same but updated to work with mappings
    public ChatMessageDTO saveChatMessage(ChatMessageDTO chatMessageDTO) {
        try {
            User userSender = userRepository.findById(chatMessageDTO.userSender().id())
                    .orElseThrow(() -> new BaseNotFoundException(EntityType.ChatMessage, chatMessageDTO.userSender().id()));
            Event event = eventRepository.findById(chatMessageDTO.eventId())
                    .orElseThrow(() -> new BaseNotFoundException(EntityType.ChatMessage, chatMessageDTO.eventId()));

            ChatMessage chatMessageEntity = ChatMessageMapper.toEntity(chatMessageDTO, userSender, event);

            UserDTO userSenderDTO = userService.getUserById(chatMessageDTO.userSender().id());

            chatMessageRepository.save(chatMessageEntity);

            return ChatMessageMapper.toDTO(chatMessageEntity, userSenderDTO, List.of()); // Empty likedBy list
        } catch (DataAccessException e) {
            throw new BaseSaveException("Failed to save chatMessage: " + e.getMessage());
        }
    }

    public boolean deleteChatMessageById(UUID id) {
        if (!chatMessageRepository.existsById(id)) {
            throw new BaseNotFoundException(EntityType.ChatMessage, id);
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
                .orElseThrow(() -> new BaseNotFoundException(EntityType.ChatMessageLike, chatMessageId));

        List<ChatMessageLikes> likes = chatMessageLikesRepository.findByChatMessage(chatMessage);

        return likes.stream()
                .map(like -> {
                    List<UserDTO> friends = userService.getFriendsByUserId(like.getUser().getId());
                    List<FriendTagDTO> friendTags = ftService.getFriendTagsByOwnerId(like.getUser().getId());
                    return UserMapper.toDTO(like.getUser(), friends, friendTags);
                })
                .collect(Collectors.toList());
    }


    public void deleteChatMessageLike(UUID chatMessageId, UUID userId) {
        try {
            boolean exists = chatMessageLikesRepository.existsByChatMessage_IdAndUser_Id(chatMessageId, userId);
            if (!exists) {
                throw new BasesNotFoundException(EntityType.ChatMessage);
            }
            chatMessageLikesRepository.deleteByChatMessage_IdAndUser_Id(chatMessageId, userId);
        } catch (Exception e) {
            throw new BaseDeleteException("An error occurred while deleting the like for chatMessageId: "
                    + chatMessageId + " and userId: " + userId + ". Error: " + e.getMessage(), e);
        }
    }

    public List<ChatMessageDTO> getChatMessagesByEventId(UUID eventId) {
        // TODO
        return List.of();
    }
}
