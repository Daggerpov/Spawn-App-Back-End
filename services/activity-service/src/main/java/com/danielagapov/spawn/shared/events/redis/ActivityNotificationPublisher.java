package com.danielagapov.spawn.shared.events.redis;

import com.danielagapov.spawn.shared.events.ActivityInviteNotificationEvent;
import com.danielagapov.spawn.shared.events.ActivityParticipationNotificationEvent;
import com.danielagapov.spawn.shared.events.ActivityUpdateNotificationEvent;
import com.danielagapov.spawn.shared.events.NotificationEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Bridges in-process notification events to Redis Pub/Sub.
 * <p>
 * The ActivityService publishes notification events via Spring's ApplicationEventPublisher.
 * This listener intercepts them and re-publishes to Redis so the monolith's
 * notification module (which subscribes to these channels) can send push notifications.
 */
@Component
public class ActivityNotificationPublisher {

    private static final Logger log = LoggerFactory.getLogger(ActivityNotificationPublisher.class);
    private final RedisEventPublisher redisEventPublisher;

    public ActivityNotificationPublisher(RedisEventPublisher redisEventPublisher) {
        this.redisEventPublisher = redisEventPublisher;
    }

    @EventListener
    public void handleActivityInvite(ActivityInviteNotificationEvent event) {
        publishToRedis(RedisEventChannels.ACTIVITY_INVITE, event);
    }

    @EventListener
    public void handleActivityUpdate(ActivityUpdateNotificationEvent event) {
        publishToRedis(RedisEventChannels.ACTIVITY_UPDATED, event);
    }

    @EventListener
    public void handleActivityParticipation(ActivityParticipationNotificationEvent event) {
        publishToRedis(RedisEventChannels.ACTIVITY_PARTICIPATION_CHANGED, event);
    }

    private void publishToRedis(String channel, NotificationEvent event) {
        try {
            // Publish a serializable representation of the notification
            var payload = new java.util.HashMap<String, Object>();
            payload.put("type", event.getType().name());
            payload.put("title", event.getTitle());
            payload.put("message", event.getMessage());
            payload.put("targetUserIds", event.getTargetUserIds());
            payload.put("data", event.getData());

            redisEventPublisher.publish(channel, payload);
        } catch (Exception e) {
            log.error("Failed to publish notification event to Redis channel '{}': {}", channel, e.getMessage());
            // Non-critical — activity operations should not fail because of notification issues
        }
    }
}
