package com.danielagapov.spawn.shared.events.redis;

import java.io.Serializable;
import java.util.UUID;

/**
 * Event published to Redis when a new chat message is created.
 * Monolith subscribes and builds NewCommentNotificationEvent (with activity title, participants from its own services).
 */
public record NewCommentRedisEvent(
        UUID senderUserId,
        String senderUsername,
        UUID activityId,
        UUID messageId,
        String content
) implements Serializable {}
