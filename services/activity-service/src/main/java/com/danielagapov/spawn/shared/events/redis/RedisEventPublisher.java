package com.danielagapov.spawn.shared.events.redis;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * Publishes domain events to Redis Pub/Sub channels.
 * <p>
 * Events are serialised to JSON so any subscriber (regardless of language or
 * framework) can consume them. Use {@link RedisEventChannels} for channel names.
 */
@Component
public class RedisEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(RedisEventPublisher.class);
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public RedisEventPublisher(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    /**
     * Publish an event to the given Redis channel.
     *
     * @param channel one of {@link RedisEventChannels} constants
     * @param event   the event object (will be serialised to JSON)
     */
    public void publish(String channel, Object event) {
        try {
            String json = objectMapper.writeValueAsString(event);
            redisTemplate.convertAndSend(channel, json);
            log.info("Published event to channel '{}': {}", channel, json);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialise event for channel '{}': {}", channel, e.getMessage());
        } catch (Exception e) {
            log.error("Failed to publish event to channel '{}': {}", channel, e.getMessage());
        }
    }
}
