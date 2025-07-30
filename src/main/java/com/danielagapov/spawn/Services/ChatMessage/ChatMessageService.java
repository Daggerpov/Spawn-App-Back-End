package com.danielagapov.spawn.Services.ChatMessage;

import com.danielagapov.spawn.DTOs.ChatMessage.ChatMessageDTO;
import com.danielagapov.spawn.DTOs.ChatMessage.ChatMessageLikesDTO;
import com.danielagapov.spawn.DTOs.ChatMessage.CreateChatMessageDTO;
import com.danielagapov.spawn.DTOs.ChatMessage.FullActivityChatMessageDTO;
import com.danielagapov.spawn.DTOs.User.BaseUserDTO;
import com.danielagapov.spawn.Enums.EntityType;
import com.danielagapov.spawn.Events.NewCommentNotificationEvent;
import com.danielagapov.spawn.Exceptions.Base.BaseDeleteException;
import com.danielagapov.spawn.Exceptions.Base.BaseNotFoundException;
import com.danielagapov.spawn.Exceptions.Base.BaseSaveException;
import com.danielagapov.spawn.Exceptions.Base.BasesNotFoundException;
import com.danielagapov.spawn.Exceptions.EntityAlreadyExistsException;
import com.danielagapov.spawn.Exceptions.Logger.ILogger;
import com.danielagapov.spawn.Mappers.ChatMessageLikesMapper;
import com.danielagapov.spawn.Mappers.ChatMessageMapper;
import com.danielagapov.spawn.Mappers.UserMapper;
import com.danielagapov.spawn.Models.Activity;
import com.danielagapov.spawn.Models.ChatMessage;
import com.danielagapov.spawn.Models.ChatMessageLikes;
import com.danielagapov.spawn.Models.CompositeKeys.ChatMessageLikesId;
import com.danielagapov.spawn.Models.User.User;
import com.danielagapov.spawn.Repositories.IActivityRepository;
import com.danielagapov.spawn.Repositories.IActivityUserRepository;
import com.danielagapov.spawn.Repositories.IChatMessageLikesRepository;
import com.danielagapov.spawn.Repositories.IChatMessageRepository;
import com.danielagapov.spawn.Repositories.User.IUserRepository;
import com.danielagapov.spawn.Services.FriendTag.IFriendTagService;
import com.danielagapov.spawn.Services.User.IUserService;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ChatMessageService implements IChatMessageService {
    private final IChatMessageRepository chatMessageRepository;
    private final IUserService userService;
    private final IActivityRepository ActivityRepository;
    private final IFriendTagService ftService;
    private final IUserRepository userRepository;
    private final IChatMessageLikesRepository chatMessageLikesRepository;
    private final ILogger logger;
    private final IActivityUserRepository activityUserRepository;
    private final ApplicationEventPublisher eventPublisher;

    public ChatMessageService(IChatMessageRepository chatMessageRepository, IUserService userService,
                              IActivityRepository ActivityRepository, IChatMessageLikesRepository chatMessageLikesRepository,
                              IFriendTagService ftService, IUserRepository userRepository, ILogger logger,
                              IActivityUserRepository activityUserRepository,
                              ApplicationEventPublisher eventPublisher) {
        this.chatMessageRepository = chatMessageRepository;
        this.userService = userService;
        this.ActivityRepository = ActivityRepository;
        this.chatMessageLikesRepository = chatMessageLikesRepository;
        this.ftService = ftService;
        this.userRepository = userRepository;
        this.logger = logger;
        this.activityUserRepository = activityUserRepository;
        this.eventPublisher = eventPublisher;
    }

    @Override
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
            logger.error(e.getMessage());
            throw new BasesNotFoundException(EntityType.ChatMessage);
        } catch (Exception e) {
            logger.error(e.getMessage());
            throw e;
        }
    }

    @Override
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


    @Override
    public ChatMessageDTO getChatMessageById(UUID id) {
        return chatMessageRepository.findById(id)
                .map(chatMessage -> {
                    List<UUID> likedByUserIds = getChatMessageLikeUserIds(chatMessage.getId());
                    return ChatMessageMapper.toDTO(chatMessage, likedByUserIds);
                })
                .orElseThrow(() -> new BaseNotFoundException(EntityType.ChatMessage, id));
    }

    @Override
    @Caching(evict = {
            @CacheEvict(value = "ActivityById", key = "#newChatMessageDTO.activityId"),
            @CacheEvict(value = "fullActivityById", allEntries = true),
            @CacheEvict(value = "feedActivities", allEntries = true),
            @CacheEvict(value = "filteredFeedActivities", allEntries = true)
    })
    public FullActivityChatMessageDTO createChatMessage(CreateChatMessageDTO newChatMessageDTO) {
        ChatMessageDTO chatMessageDTO = new ChatMessageDTO(
                null, // Let Hibernate auto-generate the ID
                newChatMessageDTO.getContent(),
                Instant.now(),
                newChatMessageDTO.getSenderUserId(),
                newChatMessageDTO.getActivityId(),
                List.of()
        );

        ChatMessageDTO savedMessage = saveChatMessage(chatMessageDTO);

        // Get the Activity and sender details
        Activity activity = ActivityRepository.findById(savedMessage.getActivityId())
                .orElseThrow(() -> new BaseNotFoundException(EntityType.Activity, savedMessage.getActivityId()));
        User sender = userRepository.findById(savedMessage.getSenderUserId())
                .orElseThrow(() -> new BaseNotFoundException(EntityType.User, savedMessage.getSenderUserId()));

        // Create and publish notification Activity
        eventPublisher.publishEvent(new NewCommentNotificationEvent(
                sender, activity, savedMessage, activityUserRepository));

        // Convert to FullActivityChatMessageDTO before returning
        return getFullChatMessageByChatMessage(savedMessage);
    }

    @Override
    public FullActivityChatMessageDTO getFullChatMessageById(UUID id) {
        return getFullChatMessageByChatMessage(getChatMessageById(id));
    }

    @Override
    public List<FullActivityChatMessageDTO> getFullChatMessagesByActivityId(UUID activityId) {
        ArrayList<FullActivityChatMessageDTO> fullChatMessages = new ArrayList<>();
        for (ChatMessageDTO cm : getChatMessagesByActivityId(activityId)) {
            fullChatMessages.add(getFullChatMessageByChatMessage(cm));
        }
        return fullChatMessages;
    }


    // Other methods remain mostly the same but updated to work with mappings
    @Override
    public ChatMessageDTO saveChatMessage(ChatMessageDTO chatMessageDTO) {
        try {
            User userSender = userRepository.findById(chatMessageDTO.getSenderUserId())
                    .orElseThrow(() -> new BaseNotFoundException(EntityType.User, chatMessageDTO.getSenderUserId()));
            Activity activity = ActivityRepository.findById(chatMessageDTO.getActivityId())
                    .orElseThrow(() -> new BaseNotFoundException(EntityType.Activity, chatMessageDTO.getActivityId()));

            ChatMessage chatMessageEntity = ChatMessageMapper.toEntity(chatMessageDTO, userSender, activity);

            ChatMessage savedEntity = chatMessageRepository.save(chatMessageEntity);

            return ChatMessageMapper.toDTO(savedEntity, List.of()); // Empty likedByUserIds list
        } catch (DataAccessException e) {
            logger.error(e.getMessage());
            throw new BaseSaveException("Failed to save chatMessage: " + e.getMessage());
        } catch (Exception e) {
            logger.error(e.getMessage());
            throw e;
        }
    }

    @Override
    public List<UUID> getChatMessageIdsByActivityId(UUID activityId) {
        try {
            // Retrieve all chat messages for the specified Activity
            List<ChatMessage> chatMessages = chatMessageRepository.getChatMessagesByActivityIdOrderByTimestampDesc(activityId);

            // Extract the IDs of the chat messages and return them as a list
            return chatMessages.stream()
                    .map(ChatMessage::getId)
                    .collect(Collectors.toList());
        } catch (DataAccessException e) {
            logger.error(e.getMessage());
            throw new BaseNotFoundException(EntityType.ChatMessage, activityId);
        } catch (Exception e) {
            logger.error(e.getMessage());
            throw e;
        }
    }


    @Override
    public boolean deleteChatMessageById(UUID id) {
        if (!chatMessageRepository.existsById(id)) {
            throw new BaseNotFoundException(EntityType.ChatMessage, id);
        }

        try {
            chatMessageRepository.deleteById(id);
            return true;
        } catch (Exception e) {
            logger.error(e.getMessage());
            return false;
        }
    }

    @Override
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

            // Create the composite key
            ChatMessageLikesId id = new ChatMessageLikesId(chatMessageId, userId);
            
            ChatMessageLikes chatMessageLikes = new ChatMessageLikes();
            chatMessageLikes.setId(id);
            chatMessageLikes.setChatMessage(chatMessage);
            chatMessageLikes.setUser(user);

            chatMessageLikesRepository.save(chatMessageLikes);
            return ChatMessageLikesMapper.toDTO(chatMessageLikes);

        } catch (Exception e) {
            logger.error(e.getMessage());
            throw new BaseSaveException("Like: chatMessageId: " + chatMessageId + " userId: "
                    + userId + ". Error: " + e.getMessage());
        }
    }

    @Override
    public List<BaseUserDTO> getChatMessageLikes(UUID chatMessageId) {
        ChatMessage chatMessage = chatMessageRepository.findById(chatMessageId)
                .orElseThrow(() -> new BaseNotFoundException(EntityType.ChatMessage, chatMessageId));

        List<ChatMessageLikes> likes = chatMessageLikesRepository.findByChatMessage(chatMessage);

        return likes.stream()
                .map(like -> {
                    List<UUID> friendsUserIds = userService.getFriendUserIdsByUserId(like.getUser().getId());
                    List<UUID> friendTagIds = ftService.getFriendTagIdsByOwnerUserId(like.getUser().getId());
                    return UserMapper.toDTO(like.getUser(), friendsUserIds, friendTagIds);
                })
                .collect(Collectors.toList());
    }


    @Override
    public void deleteChatMessageLike(UUID chatMessageId, UUID userId) {
        try {
            boolean exists = chatMessageLikesRepository.existsByChatMessage_IdAndUser_Id(chatMessageId, userId);
            if (!exists) {
                throw new BaseNotFoundException(EntityType.ChatMessage);
            }
            chatMessageLikesRepository.deleteByChatMessage_IdAndUser_Id(chatMessageId, userId);
        } catch (Exception e) {
            logger.error(e.getMessage());
            throw new BaseDeleteException("An error occurred while deleting the like for chatMessageId: "
                    + chatMessageId + " and userId: " + userId + ". Error: " + e.getMessage(), e);
        }
    }

    @Override
    public List<ChatMessageDTO> getChatMessagesByActivityId(UUID activityId) {
        try {
            List<ChatMessage> chatMessages = chatMessageRepository.getChatMessagesByActivityIdOrderByTimestampDesc(activityId);

            return chatMessages.stream()
                    .map(chatMessage -> {
                        List<UUID> likedByUserIds = getChatMessageLikeUserIds(chatMessage.getId());
                        return ChatMessageMapper.toDTO(chatMessage, likedByUserIds);
                    })
                    .collect(Collectors.toList());
        } catch (DataAccessException e) {
            logger.error(e.getMessage());
            throw new BasesNotFoundException(EntityType.ChatMessage);
        } catch (Exception e) {
            logger.error(e.getMessage());
            throw e;
        }
    }

    @Override
    public FullActivityChatMessageDTO getFullChatMessageByChatMessage(ChatMessageDTO chatMessage) {
        return new FullActivityChatMessageDTO(
                chatMessage.getId(),
                chatMessage.getContent(),
                chatMessage.getTimestamp(),
                userService.getBaseUserById(chatMessage.getSenderUserId()),
                chatMessage.getActivityId(),
                getChatMessageLikes(chatMessage.getId())
        );
    }

    @Override
    public List<FullActivityChatMessageDTO> convertChatMessagesToFullFeedActivityChatMessages(List<ChatMessageDTO> chatMessages) {
        return chatMessages.stream()
                .map(this::getFullChatMessageByChatMessage)
                .collect(Collectors.toList());
    }

}
