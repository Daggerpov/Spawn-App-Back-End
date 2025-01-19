package com.danielagapov.spawn.Services.ChatMessage;

import com.danielagapov.spawn.DTOs.ChatMessageDTO;
import com.danielagapov.spawn.DTOs.ChatMessageLikesDTO;
import com.danielagapov.spawn.DTOs.UserDTO;
import com.danielagapov.spawn.Enums.EntityType;
import com.danielagapov.spawn.Exceptions.Base.BaseDeleteException;
import com.danielagapov.spawn.Exceptions.Base.BaseNotFoundException;
import com.danielagapov.spawn.Exceptions.Base.BaseSaveException;
import com.danielagapov.spawn.Exceptions.Base.BasesNotFoundException;
import com.danielagapov.spawn.Exceptions.EntityAlreadyExistsException;
import com.danielagapov.spawn.Helpers.Logger.ILogger;
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
    private final ILogger logger;

    public ChatMessageService(IChatMessageRepository chatMessageRepository, IUserService userService,
                              IEventRepository eventRepository, IChatMessageLikesRepository chatMessageLikesRepository,
                              IFriendTagService ftService, IUserRepository userRepository, ILogger logger) {
        this.chatMessageRepository = chatMessageRepository;
        this.userService = userService;
        this.eventRepository = eventRepository;
        this.chatMessageLikesRepository = chatMessageLikesRepository;
        this.ftService = ftService;
        this.userRepository = userRepository;
        this.logger = logger;
    }

    public List<ChatMessageDTO> getAllChatMessages() {
        try {
            List<ChatMessage> chatMessages = chatMessageRepository.findAll();

            Map<ChatMessage, List<UUID>> likedByMap = chatMessages.stream()
                    .collect(Collectors.toMap(
                            chatMessage -> chatMessage,
                            chatMessage -> getChatMessageLikeUserIds(chatMessage.getId())
                    ));

            // Return the mapped DTOs including the sender and likedByUserIds
            return ChatMessageMapper.toDTOList(chatMessages, likedByMap);
        } catch (DataAccessException e) {
            logger.log(e.getMessage());
            throw new BasesNotFoundException(EntityType.ChatMessage);
        } catch (Exception e) {
            logger.log(e.getMessage());
            throw e;
        }
    }

    public List<UUID> getChatMessageLikeUserIds(UUID chatMessageId) {
        // Retrieve the ChatMessage by its ID
        ChatMessage chatMessage = chatMessageRepository.findById(chatMessageId)
                .orElseThrow(() -> new BaseNotFoundException(EntityType.ChatMessage, chatMessageId));

        // Retrieve all the likes for the given chat message
        List<ChatMessageLikes> likes = chatMessageLikesRepository.findByChatMessage(chatMessage);

        // Extract the user IDs of the users who liked the chat message
        return likes.stream()
                .map(like -> like.getUser().getId())  // Extract the user ID from each like
                .collect(Collectors.toList());  // Collect them into a list
    }


    public ChatMessageDTO getChatMessageById(UUID id) {
        return chatMessageRepository.findById(id)
                .map(chatMessage -> {
                    List<UUID> likedByUserIds = getChatMessageLikeUserIds(chatMessage.getId());
                    return ChatMessageMapper.toDTO(chatMessage, likedByUserIds );
                })
                .orElseThrow(() -> new BaseNotFoundException(EntityType.ChatMessage, id));
    }


    // Other methods remain mostly the same but updated to work with mappings
    public ChatMessageDTO saveChatMessage(ChatMessageDTO chatMessageDTO) {
        try {
            User userSender = userRepository.findById(chatMessageDTO.senderUserId())
                    .orElseThrow(() -> new BaseNotFoundException(EntityType.ChatMessage, chatMessageDTO.senderUserId()));
            Event event = eventRepository.findById(chatMessageDTO.eventId())
                    .orElseThrow(() -> new BaseNotFoundException(EntityType.ChatMessage, chatMessageDTO.eventId()));

            ChatMessage chatMessageEntity = ChatMessageMapper.toEntity(chatMessageDTO, userSender, event);

            UserDTO userSenderDTO = userService.getUserById(chatMessageDTO.senderUserId());

            chatMessageRepository.save(chatMessageEntity);

            return ChatMessageMapper.toDTO(chatMessageEntity, List.of()); // Empty likedByUserIds list
        } catch (DataAccessException e) {
            logger.log(e.getMessage());
            throw new BaseSaveException("Failed to save chatMessage: " + e.getMessage());
        } catch (Exception e) {
            logger.log(e.getMessage());
            throw e;
        }
    }

    public boolean deleteChatMessageById(UUID id) {
        if (!chatMessageRepository.existsById(id)) {
            throw new BaseNotFoundException(EntityType.ChatMessage, id);
        }

        try {
            chatMessageRepository.deleteById(id);
            return true;
        } catch (Exception e) {
            logger.log(e.getMessage());
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
            logger.log(e.getMessage());
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
                    List<UUID> friendsUserIds = userService.getFriendUserIdsByUserId(like.getUser().getId());
                    List<UUID> friendTagIds = ftService.getFriendTagIdsByOwnerUserId(like.getUser().getId());
                    return UserMapper.toDTO(like.getUser(), friendsUserIds, friendTagIds);
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
            logger.log(e.getMessage());
            throw new BaseDeleteException("An error occurred while deleting the like for chatMessageId: "
                    + chatMessageId + " and userId: " + userId + ". Error: " + e.getMessage(), e);
        }
    }

    public List<ChatMessageDTO> getChatMessagesByEventId(UUID eventId) {
        // TODO
        return List.of();
    }
}
