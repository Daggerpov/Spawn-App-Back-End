package com.danielagapov.spawn.Services.ChatMessage;

import com.danielagapov.spawn.DTOs.ChatMessage.ChatMessageDTO;
import com.danielagapov.spawn.DTOs.ChatMessage.ChatMessageLikesDTO;
import com.danielagapov.spawn.DTOs.ChatMessage.CreateChatMessageDTO;
import com.danielagapov.spawn.DTOs.ChatMessage.FullEventChatMessageDTO;
import com.danielagapov.spawn.DTOs.User.BaseUserDTO;
import com.danielagapov.spawn.Enums.EntityType;
import com.danielagapov.spawn.Enums.ParticipationStatus;
import com.danielagapov.spawn.Exceptions.Base.BaseDeleteException;
import com.danielagapov.spawn.Exceptions.Base.BaseNotFoundException;
import com.danielagapov.spawn.Exceptions.Base.BaseSaveException;
import com.danielagapov.spawn.Exceptions.Base.BasesNotFoundException;
import com.danielagapov.spawn.Exceptions.EntityAlreadyExistsException;
import com.danielagapov.spawn.Exceptions.Logger.ILogger;
import com.danielagapov.spawn.Mappers.ChatMessageLikesMapper;
import com.danielagapov.spawn.Mappers.ChatMessageMapper;
import com.danielagapov.spawn.Mappers.UserMapper;
import com.danielagapov.spawn.Models.*;
import com.danielagapov.spawn.Repositories.*;
import com.danielagapov.spawn.Services.FriendTag.IFriendTagService;
import com.danielagapov.spawn.Services.PushNotification.PushNotificationService;
import com.danielagapov.spawn.Services.User.IUserService;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
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
    private final PushNotificationService pushNotificationService;
    private final IEventUserRepository eventUserRepository;

    public ChatMessageService(IChatMessageRepository chatMessageRepository, IUserService userService,
                              IEventRepository eventRepository, IChatMessageLikesRepository chatMessageLikesRepository,
                              IFriendTagService ftService, IUserRepository userRepository, ILogger logger,
                              PushNotificationService pushNotificationService, IEventUserRepository eventUserRepository) {
        this.chatMessageRepository = chatMessageRepository;
        this.userService = userService;
        this.eventRepository = eventRepository;
        this.chatMessageLikesRepository = chatMessageLikesRepository;
        this.ftService = ftService;
        this.userRepository = userRepository;
        this.logger = logger;
        this.pushNotificationService = pushNotificationService;
        this.eventUserRepository = eventUserRepository;
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
    public ChatMessageDTO createChatMessage(CreateChatMessageDTO newChatMessageDTO) {
        ChatMessageDTO chatMessageDTO = new ChatMessageDTO(
                UUID.randomUUID(),
                newChatMessageDTO.getContent(),
                Instant.now(),
                newChatMessageDTO.getSenderUserId(),
                newChatMessageDTO.getEventId(),
                List.of()
        );

        ChatMessageDTO savedMessage = saveChatMessage(chatMessageDTO);

        // Send notifications after saving the message
        sendChatMessageNotifications(savedMessage);

        return savedMessage;
    }

    @Override
    public FullEventChatMessageDTO getFullChatMessageById(UUID id) {
        return getFullChatMessageByChatMessage(getChatMessageById(id));
    }

    @Override
    public List<FullEventChatMessageDTO> getFullChatMessagesByEventId(UUID eventId) {
        ArrayList<FullEventChatMessageDTO> fullChatMessages = new ArrayList<>();
        for (ChatMessageDTO cm : getChatMessagesByEventId(eventId)) {
            fullChatMessages.add(getFullChatMessageByChatMessage(cm));
        }
        return fullChatMessages;
    }


    // Other methods remain mostly the same but updated to work with mappings
    @Override
    public ChatMessageDTO saveChatMessage(ChatMessageDTO chatMessageDTO) {
        try {
            User userSender = userRepository.findById(chatMessageDTO.getSenderUserId())
                    .orElseThrow(() -> new BaseNotFoundException(EntityType.ChatMessage, chatMessageDTO.getSenderUserId()));
            Event event = eventRepository.findById(chatMessageDTO.getEventId())
                    .orElseThrow(() -> new BaseNotFoundException(EntityType.ChatMessage, chatMessageDTO.getEventId()));

            ChatMessage chatMessageEntity = ChatMessageMapper.toEntity(chatMessageDTO, userSender, event);

            chatMessageRepository.save(chatMessageEntity);

            return ChatMessageMapper.toDTO(chatMessageEntity, List.of()); // Empty likedByUserIds list
        } catch (DataAccessException e) {
            logger.error(e.getMessage());
            throw new BaseSaveException("Failed to save chatMessage: " + e.getMessage());
        } catch (Exception e) {
            logger.error(e.getMessage());
            throw e;
        }
    }

    @Override
    public List<UUID> getChatMessageIdsByEventId(UUID eventId) {
        try {
            // Find the event by its ID
            Event event = eventRepository.findById(eventId)
                    .orElseThrow(() -> new BaseNotFoundException(EntityType.Event, eventId));

            // Retrieve all chat messages for the specified event
            List<ChatMessage> chatMessages = chatMessageRepository.getChatMessagesByEventId(eventId);

            // Extract the IDs of the chat messages and return them as a list
            return chatMessages.stream()
                    .map(ChatMessage::getId)
                    .collect(Collectors.toList());
        } catch (DataAccessException e) {
            logger.error(e.getMessage());
            throw new BaseNotFoundException(EntityType.ChatMessage, eventId);
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

            ChatMessageLikes chatMessageLikes = new ChatMessageLikes();
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


    @Override
    public void deleteChatMessageLike(UUID chatMessageId, UUID userId) {
        try {
            boolean exists = chatMessageLikesRepository.existsByChatMessage_IdAndUser_Id(chatMessageId, userId);
            if (!exists) {
                throw new BasesNotFoundException(EntityType.ChatMessage);
            }
            chatMessageLikesRepository.deleteByChatMessage_IdAndUser_Id(chatMessageId, userId);
        } catch (Exception e) {
            logger.error(e.getMessage());
            throw new BaseDeleteException("An error occurred while deleting the like for chatMessageId: "
                    + chatMessageId + " and userId: " + userId + ". Error: " + e.getMessage(), e);
        }
    }

    @Override
    public List<ChatMessageDTO> getChatMessagesByEventId(UUID eventId) {
        try {
            List<ChatMessage> chatMessages = chatMessageRepository.getChatMessagesByEventId(eventId);

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
    public FullEventChatMessageDTO getFullChatMessageByChatMessage(ChatMessageDTO chatMessage) {
        return new FullEventChatMessageDTO(
                chatMessage.getId(),
                chatMessage.getContent(),
                chatMessage.getTimestamp(),
                userService.getUserById(chatMessage.getSenderUserId()),
                chatMessage.getEventId(),
                getChatMessageLikes(chatMessage.getId())
        );
    }

    @Override
    public List<FullEventChatMessageDTO> convertChatMessagesToFullFeedEventChatMessages(List<ChatMessageDTO> chatMessages) {
        return chatMessages.stream()
                .map(this::getFullChatMessageByChatMessage)
                .collect(Collectors.toList());
    }

    /**
     * Send push notifications for a new chat message
     *
     * @param chatMessageDTO The saved chat message
     */
    private void sendChatMessageNotifications(ChatMessageDTO chatMessageDTO) {
        try {
            UUID eventId = chatMessageDTO.getEventId();
            UUID senderUserId = chatMessageDTO.getSenderUserId();

            // Get event and sender details
            Event event = eventRepository.findById(eventId)
                    .orElseThrow(() -> new BaseNotFoundException(EntityType.Event, eventId));
            User sender = userRepository.findById(senderUserId)
                    .orElseThrow(() -> new BaseNotFoundException(EntityType.User, senderUserId));

            // Prepare notification data
            Map<String, String> data = new HashMap<>();
            data.put("type", "chatMessage");
            data.put("eventId", eventId.toString());
            data.put("messageId", chatMessageDTO.getId().toString());
            data.put("senderId", senderUserId.toString());

            // 1. Notify event creator if they're not the sender
            if (!event.getCreator().getId().equals(senderUserId)) {
                pushNotificationService.sendNotificationToUser(
                        event.getCreator().getId(),
                        "New Comment on Your Event",
                        sender.getUsername() + " commented on " + event.getTitle() + ": " + chatMessageDTO.getContent(),
                        data
                );
            }

            // 2. Notify participating users (except the sender)
            List<EventUser> participants = eventUserRepository.findEventsByEvent_IdAndStatus(eventId, ParticipationStatus.participating);
            for (EventUser participant : participants) {
                UUID participantId = participant.getUser().getId();
                // Skip if participant is the sender or the event creator (already notified)
                if (!participantId.equals(senderUserId) && !participantId.equals(event.getCreator().getId())) {
                    pushNotificationService.sendNotificationToUser(
                            participantId,
                            "New Comment on Event",
                            sender.getUsername() + " commented on an event you're participating in: " + event.getTitle(),
                            data
                    );
                }

            }
        } catch (Exception e) {
            // Log error but don't prevent message creation
            logger.error("Error sending chat message notifications: " + e.getMessage());
        }
    }

}
