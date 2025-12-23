package com.danielagapov.spawn.chat.internal.services;

import com.danielagapov.spawn.chat.api.dto.ChatMessageDTO;
import com.danielagapov.spawn.chat.api.dto.ChatMessageLikesDTO;
import com.danielagapov.spawn.chat.api.dto.CreateChatMessageDTO;
import com.danielagapov.spawn.chat.api.dto.FullActivityChatMessageDTO;
import com.danielagapov.spawn.user.api.dto.BaseUserDTO;
import com.danielagapov.spawn.shared.util.EntityType;
import com.danielagapov.spawn.shared.events.NewCommentNotificationEvent;
import com.danielagapov.spawn.shared.exceptions.Base.BaseDeleteException;
import com.danielagapov.spawn.shared.exceptions.Base.BaseNotFoundException;
import com.danielagapov.spawn.shared.exceptions.Base.BaseSaveException;
import com.danielagapov.spawn.shared.exceptions.Base.BasesNotFoundException;
import com.danielagapov.spawn.shared.exceptions.EntityAlreadyExistsException;
import com.danielagapov.spawn.shared.exceptions.Logger.ILogger;
import com.danielagapov.spawn.shared.util.ChatMessageLikesMapper;
import com.danielagapov.spawn.shared.util.ChatMessageMapper;
import com.danielagapov.spawn.shared.util.ParticipationStatus;
import com.danielagapov.spawn.shared.util.UserMapper;
import com.danielagapov.spawn.activity.api.IActivityService;
import com.danielagapov.spawn.activity.internal.domain.Activity;
import com.danielagapov.spawn.chat.internal.domain.ChatMessage;
import com.danielagapov.spawn.chat.internal.domain.ChatMessageLikes;
import com.danielagapov.spawn.chat.internal.domain.ChatMessageLikesId;
import com.danielagapov.spawn.user.internal.domain.User;
import com.danielagapov.spawn.activity.internal.repositories.IActivityRepository;
import com.danielagapov.spawn.chat.internal.repositories.IChatMessageLikesRepository;
import com.danielagapov.spawn.chat.internal.repositories.IChatMessageRepository;
import com.danielagapov.spawn.user.internal.repositories.IUserRepository;
import com.danielagapov.spawn.user.internal.services.IUserService;
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
    private final IUserRepository userRepository;
    private final IChatMessageLikesRepository chatMessageLikesRepository;
    private final ILogger logger;
    private final IActivityService activityService;
    private final ApplicationEventPublisher eventPublisher;

    public ChatMessageService(IChatMessageRepository chatMessageRepository, IUserService userService,
                              IActivityRepository ActivityRepository, IChatMessageLikesRepository chatMessageLikesRepository,
                              IUserRepository userRepository, ILogger logger,
                              IActivityService activityService,
                              ApplicationEventPublisher eventPublisher) {
        this.chatMessageRepository = chatMessageRepository;
        this.userService = userService;
        this.ActivityRepository = ActivityRepository;
        this.chatMessageLikesRepository = chatMessageLikesRepository;
        this.userRepository = userRepository;
        this.logger = logger;
        this.activityService = activityService;
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
            @CacheEvict(value = "feedActivities", allEntries = true)
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

        // Get participant IDs using the public API (maintains module boundaries)
        List<UUID> participantIds = activityService.getParticipantUserIdsByActivityIdAndStatus(
                activity.getId(), ParticipationStatus.participating);

        // Create and publish notification event with participant IDs
        eventPublisher.publishEvent(new NewCommentNotificationEvent(
                sender.getId(),
                sender.getUsername(),
                activity.getId(),
                activity.getTitle(),
                activity.getCreator().getId(),
                savedMessage,
                participantIds));

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
    public List<Object[]> getChatMessageIdsByActivityIds(List<UUID> activityIds) {
        try {
            if (activityIds.isEmpty()) {
                return List.of();
            }
            // Use the batch query from repository
            return chatMessageRepository.findChatMessageIdsByActivityIds(activityIds);
        } catch (DataAccessException e) {
            logger.error("Error fetching chat message IDs for activities: " + e.getMessage());
            throw new BasesNotFoundException(EntityType.ChatMessage);
        } catch (Exception e) {
            logger.error("Error fetching chat message IDs for activities: " + e.getMessage());
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
                    return UserMapper.toDTO(like.getUser(), friendsUserIds);
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
