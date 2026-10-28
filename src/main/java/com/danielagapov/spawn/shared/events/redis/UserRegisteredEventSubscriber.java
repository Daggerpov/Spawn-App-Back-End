package com.danielagapov.spawn.shared.events.redis;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Component;

/**
 * Subscribes to the {@code events:user-registered} Redis channel and
 * re-publishes the event as a Spring ApplicationEvent so existing
 * in-process listeners (e.g. ActivityTypeEventListener) keep working
 * without modification.
 * <p>
 * This acts as a bridge: Redis Pub/Sub -> Spring ApplicationEventPublisher.
 */
@Component
public class UserRegisteredEventSubscriber implements MessageListener {

    private static final Logger log = LoggerFactory.getLogger(UserRegisteredEventSubscriber.class);
    private final ApplicationEventPublisher springEventPublisher;
    private final ObjectMapper objectMapper;

    public UserRegisteredEventSubscriber(ApplicationEventPublisher springEventPublisher) {
        this.springEventPublisher = springEventPublisher;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            String json = new String(message.getBody());
            UserRegisteredEvent event = objectMapper.readValue(json, UserRegisteredEvent.class);
            log.info("Received user-registered event for userId={}, email={}", event.userId(), event.email());

            // Re-publish as a Spring event so existing @EventListeners are triggered.
            // This keeps backward compatibility with the monolith's in-process event system.
            springEventPublisher.publishEvent(event);
        } catch (Exception e) {
            log.error("Failed to process user-registered event: {}", e.getMessage(), e);
        }
    }
}
