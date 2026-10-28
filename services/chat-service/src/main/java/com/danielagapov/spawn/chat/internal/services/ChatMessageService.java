package com.danielagapov.spawn.chat.internal.services;

import com.danielagapov.spawn.chat.api.dto.ChatMessageDTO;
import com.danielagapov.spawn.chat.api.dto.ChatMessageLikesDTO;
import com.danielagapov.spawn.chat.api.dto.CreateChatMessageDTO;
import com.danielagapov.spawn.chat.api.dto.FullActivityChatMessageDTO;
import com.danielagapov.spawn.chat.internal.domain.ChatMessage;
import com.danielagapov.spawn.chat.internal.domain.ChatMessageLikes;
import com.danielagapov.spawn.chat.internal.domain.ChatMessageLikesId;
import com.danielagapov.spawn.chat.internal.repositories.IChatMessageLikesRepository;
import com.danielagapov.spawn.chat.internal.repositories.IChatMessageRepository;
import com.danielagapov.spawn.chat.internal.util.ChatMessageMapper;
import com.danielagapov.spawn.shared.events.redis.NewCommentRedisEvent;
import com.danielagapov.spawn.shared.events.redis.RedisEventChannels;
import com.danielagapov.spawn.shared.events.redis.RedisEventPublisher;
import com.danielagapov.spawn.shared.exceptions.Base.BaseNotFoundException;
import com.danielagapov.spawn.shared.exceptions.Base.BaseSaveException;
import com.danielagapov.spawn.shared.exceptions.EntityAlreadyExistsException;
import com.danielagapov.spawn.shared.util.EntityType;
import com.danielagapov.spawn.shared.exceptions.Logger.ILogger;
import com.danielagapov.spawn.user.api.dto.BaseUserDTO;
import com.danielagapov.spawn.shared.feign.MonolithUserClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ChatMessageService {

    private final IChatMessageRepository chatMessageRepository;
    private final IChatMessageLikesRepository chatMessageLikesRepository;
    private final MonolithUserClient monolithUserClient;
    private final RedisEventPublisher redisEventPublisher;
    private final ILogger logger;

    public ChatMessageService(IChatMessageRepository chatMessageRepository,
                              IChatMessageLikesRepository chatMessageLikesRepository,
                              MonolithUserClient monolithUserClient,
                              RedisEventPublisher redisEventPublisher,
                              ILogger logger) {
        this.chatMessageRepository = chatMessageRepository;
        this.chatMessageLikesRepository = chatMessageLikesRepository;
        this.monolithUserClient = monolithUserClient;
        this.redisEventPublisher = redisEventPublisher;
        this.logger = logger;
    }

    public ChatMessageDTO getChatMessageById(UUID id) {
        ChatMessage cm = chatMessageRepository.findById(id)
                .orElseThrow(() -> new BaseNotFoundException(EntityType.ChatMessage, id));
        List<UUID> likedBy = getChatMessageLikeUserIds(cm.getId());
        return ChatMessageMapper.toDTO(cm, likedBy);
    }

    public List<FullActivityChatMessageDTO> getFullChatMessagesByActivityId(UUID activityId) {
        List<ChatMessageDTO> list = getChatMessagesByActivityId(activityId);
        List<FullActivityChatMessageDTO> result = new ArrayList<>();
        for (ChatMessageDTO cm : list) {
            result.add(getFullChatMessageByChatMessage(cm));
        }
        return result;
    }

    @Transactional
    public FullActivityChatMessageDTO createChatMessage(CreateChatMessageDTO dto) {
        ChatMessageDTO toSave = new ChatMessageDTO(null, dto.getContent(), Instant.now(),
                dto.getSenderUserId(), dto.getActivityId(), List.of());
        ChatMessageDTO saved = saveChatMessage(toSave);

        String senderUsername;
        try {
            BaseUserDTO sender = monolithUserClient.getUserById(dto.getSenderUserId());
            senderUsername = sender != null && sender.getUsername() != null ? sender.getUsername() : "unknown";
        } catch (Exception e) {
            logger.warn("Could not fetch sender username for new-comment event: " + e.getMessage());
            senderUsername = "unknown";
        }

        redisEventPublisher.publish(RedisEventChannels.NEW_COMMENT, new NewCommentRedisEvent(
                dto.getSenderUserId(),
                senderUsername,
                dto.getActivityId(),
                saved.getId(),
                saved.getContent()
        ));

        return getFullChatMessageByChatMessage(saved);
    }

    public ChatMessageDTO saveChatMessage(ChatMessageDTO dto) {
        ChatMessage entity = ChatMessageMapper.toEntity(dto, dto.getSenderUserId(), dto.getActivityId());
        ChatMessage saved = chatMessageRepository.save(entity);
        return ChatMessageMapper.toDTO(saved, List.of());
    }

    public List<UUID> getChatMessageIdsByActivityId(UUID activityId) {
        return chatMessageRepository.getChatMessagesByActivityIdOrderByTimestampDesc(activityId)
                .stream().map(ChatMessage::getId).collect(Collectors.toList());
    }

    public List<ChatMessageDTO> getChatMessagesByActivityId(UUID activityId) {
        List<ChatMessage> messages = chatMessageRepository.getChatMessagesByActivityIdOrderByTimestampDesc(activityId);
        return messages.stream()
                .map(cm -> ChatMessageMapper.toDTO(cm, getChatMessageLikeUserIds(cm.getId())))
                .collect(Collectors.toList());
    }

    public boolean deleteChatMessageById(UUID id) {
        if (!chatMessageRepository.existsById(id)) {
            throw new BaseNotFoundException(EntityType.ChatMessage, id);
        }
        chatMessageRepository.deleteById(id);
        return true;
    }

    public ChatMessageLikesDTO createChatMessageLike(UUID chatMessageId, UUID userId) {
        if (chatMessageLikesRepository.existsByChatMessage_IdAndUser_Id(chatMessageId, userId)) {
            throw new EntityAlreadyExistsException(EntityType.ChatMessage, chatMessageId);
        }
        ChatMessage chatMessage = chatMessageRepository.findById(chatMessageId)
                .orElseThrow(() -> new BaseNotFoundException(EntityType.ChatMessage, chatMessageId));
        ChatMessageLikesId id = new ChatMessageLikesId(chatMessageId, userId);
        ChatMessageLikes like = new ChatMessageLikes();
        like.setId(id);
        like.setChatMessage(chatMessage);
        like.setUser(new com.danielagapov.spawn.chat.internal.domain.UserRef(userId));
        chatMessageLikesRepository.save(like);
        return new ChatMessageLikesDTO(chatMessageId, userId);
    }

    public List<BaseUserDTO> getChatMessageLikes(UUID chatMessageId) {
        ChatMessage cm = chatMessageRepository.findById(chatMessageId)
                .orElseThrow(() -> new BaseNotFoundException(EntityType.ChatMessage, chatMessageId));
        List<ChatMessageLikes> likes = chatMessageLikesRepository.findByChatMessage(cm);
        List<BaseUserDTO> result = new ArrayList<>();
        for (ChatMessageLikes like : likes) {
            try {
                result.add(monolithUserClient.getUserById(like.getUser().getId()));
            } catch (Exception e) {
                logger.warn("Could not fetch user " + like.getUser().getId() + " for like: " + e.getMessage());
            }
        }
        return result;
    }

    public void deleteChatMessageLike(UUID chatMessageId, UUID userId) {
        if (!chatMessageLikesRepository.existsByChatMessage_IdAndUser_Id(chatMessageId, userId)) {
            throw new BaseNotFoundException(EntityType.ChatMessage);
        }
        chatMessageLikesRepository.deleteByChatMessage_IdAndUser_Id(chatMessageId, userId);
    }

    private List<UUID> getChatMessageLikeUserIds(UUID chatMessageId) {
        ChatMessage cm = chatMessageRepository.findById(chatMessageId)
                .orElseThrow(() -> new BaseNotFoundException(EntityType.ChatMessage, chatMessageId));
        return chatMessageLikesRepository.findByChatMessage(cm).stream()
                .map(l -> l.getUser().getId())
                .collect(Collectors.toList());
    }

    private FullActivityChatMessageDTO getFullChatMessageByChatMessage(ChatMessageDTO chatMessage) {
        BaseUserDTO sender;
        try {
            sender = monolithUserClient.getUserById(chatMessage.getSenderUserId());
        } catch (Exception e) {
            logger.warn("Could not fetch sender for chat message: " + e.getMessage());
            throw new BaseSaveException("User lookup failed: " + e.getMessage());
        }
        List<BaseUserDTO> likedByUsers = getChatMessageLikes(chatMessage.getId());
        return new FullActivityChatMessageDTO(
                chatMessage.getId(),
                chatMessage.getContent(),
                chatMessage.getTimestamp(),
                sender,
                chatMessage.getActivityId(),
                likedByUsers
        );
    }
}
