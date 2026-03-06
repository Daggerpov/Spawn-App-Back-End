package com.danielagapov.spawn.shared.events.redis;

import java.io.Serializable;
import java.util.UUID;

/**
 * Event received from Redis when chat-service publishes a new comment.
 * Matches the payload sent by chat-service.
 */
public record NewCommentRedisEvent(
        UUID senderUserId,
        String senderUsername,
        UUID activityId,
        UUID messageId,
        String content
) implements Serializable {}
