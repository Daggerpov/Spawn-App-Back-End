package com.danielagapov.spawn.shared.events.redis;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;

/**
 * Configures Redis Pub/Sub message listener container for the monolith.
 * <p>
 * Subscribes to channels published by other microservices (e.g. auth-service)
 * and routes messages to the appropriate handler methods.
 */
@Configuration
public class RedisSubscriberConfig {

    @Bean
    public RedisMessageListenerContainer redisMessageListenerContainer(
            RedisConnectionFactory connectionFactory,
            UserRegisteredEventSubscriber userRegisteredSubscriber,
            NewCommentEventSubscriber newCommentEventSubscriber) {

        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);

        container.addMessageListener(
                new MessageListenerAdapter(userRegisteredSubscriber, "onMessage"),
                new ChannelTopic(RedisEventChannels.USER_REGISTERED)
        );

        container.addMessageListener(
                new MessageListenerAdapter(newCommentEventSubscriber, "onMessage"),
                new ChannelTopic(RedisEventChannels.NEW_COMMENT)
        );

        return container;
    }
}
