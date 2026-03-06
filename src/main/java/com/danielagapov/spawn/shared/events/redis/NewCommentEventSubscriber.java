package com.danielagapov.spawn.shared.events.redis;

import com.danielagapov.spawn.shared.feign.ActivityServiceClient;
import com.danielagapov.spawn.chat.api.dto.ChatMessageDTO;
import com.danielagapov.spawn.shared.events.NewCommentNotificationEvent;
import com.danielagapov.spawn.shared.util.ParticipationStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

/**
 * Subscribes to the {@code events:new-comment} Redis channel (published by chat-service)
 * and re-publishes as NewCommentNotificationEvent so the monolith's notification flow runs.
 */
@Component
public class NewCommentEventSubscriber implements MessageListener {

    private static final Logger log = LoggerFactory.getLogger(NewCommentEventSubscriber.class);
    private final ApplicationEventPublisher springEventPublisher;
    private final ActivityServiceClient activityServiceClient;
    private final ObjectMapper objectMapper;

    public NewCommentEventSubscriber(ApplicationEventPublisher springEventPublisher,
                                    ActivityServiceClient activityServiceClient) {
        this.springEventPublisher = springEventPublisher;
        this.activityServiceClient = activityServiceClient;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            String json = new String(message.getBody());
            NewCommentRedisEvent event = objectMapper.readValue(json, NewCommentRedisEvent.class);
            log.info("Received new-comment event for activityId={}, messageId={}", event.activityId(), event.messageId());

            String activityTitle = activityServiceClient.getActivityTitle(event.activityId());
            UUID creatorId = activityServiceClient.getCreatorId(event.activityId());
            List<UUID> participantIds = activityServiceClient.getParticipantUserIds(
                    event.activityId(), ParticipationStatus.participating);

            ChatMessageDTO messageDTO = new ChatMessageDTO(
                    event.messageId(),
                    event.content(),
                    null,
                    event.senderUserId(),
                    event.activityId(),
                    null
            );

            springEventPublisher.publishEvent(new NewCommentNotificationEvent(
                    event.senderUserId(),
                    event.senderUsername(),
                    event.activityId(),
                    activityTitle,
                    creatorId,
                    messageDTO,
                    participantIds
            ));
        } catch (Exception e) {
            log.error("Failed to process new-comment event: {}", e.getMessage(), e);
        }
    }
}
