package com.danielagapov.spawn.Config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;

import java.time.Duration;

@Configuration
@EnableCaching
@ConditionalOnClass(RedisConnectionFactory.class)
@ConditionalOnProperty(name = "spring.cache.type", havingValue = "redis", matchIfMissing = false)
@Profile("!test") // Don't load this configuration in test profile
public class RedisCacheConfig implements CachingConfigurer {
    
    private static final Logger logger = LoggerFactory.getLogger(RedisCacheConfig.class);

    @Bean
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        // RAM OPTIMIZATION: Use JSON serialization instead of JDK (saves ~60 MB, 40% reduction)
        // Configure ObjectMapper with proper UTF-8 support for emojis and special characters
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        
        // Configure polymorphic type handling for proper deserialization
        // Use OBJECT_AND_NON_CONCRETE instead of NON_FINAL to avoid issues with collections
        BasicPolymorphicTypeValidator validator = BasicPolymorphicTypeValidator.builder()
                .allowIfBaseType(Object.class)
                .build();
        
        objectMapper.activateDefaultTyping(
                validator,
                ObjectMapper.DefaultTyping.OBJECT_AND_NON_CONCRETE,
                JsonTypeInfo.As.PROPERTY
        );
        
        GenericJackson2JsonRedisSerializer serializer = 
            new GenericJackson2JsonRedisSerializer(objectMapper);
        
        // Set default TTL of 100 minutes with JSON serialization
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(100))
                .serializeValuesWith(
                    RedisSerializationContext.SerializationPair.fromSerializer(serializer))
                .disableCachingNullValues()  // Don't cache null values
                .computePrefixWith(cacheName -> "spawn:" + cacheName + ":");

        // Configure different TTL values for different cache types
        RedisCacheConfiguration userDataConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(30)) // User data changes more frequently
                .serializeValuesWith(
                    RedisSerializationContext.SerializationPair.fromSerializer(serializer))
                .disableCachingNullValues();
        
        RedisCacheConfiguration staticDataConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofHours(4)) // Static data like activity types, locations
                .serializeValuesWith(
                    RedisSerializationContext.SerializationPair.fromSerializer(serializer))
                .disableCachingNullValues();
        
        RedisCacheConfiguration statsConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(15)) // Stats change frequently
                .serializeValuesWith(
                    RedisSerializationContext.SerializationPair.fromSerializer(serializer))
                .disableCachingNullValues();
        
        // Activity caches with shorter TTL to prevent stale expiration status
        // Activities can expire naturally over time, so we use a shorter cache duration
        RedisCacheConfiguration activityConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(5)) // 5 minutes to ensure fresh expiration data
                .serializeValuesWith(
                    RedisSerializationContext.SerializationPair.fromSerializer(serializer))
                .disableCachingNullValues();

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfig)
                // User-related caches
                .withCacheConfiguration("friendsByUserId", userDataConfig)
                .withCacheConfiguration("recommendedFriends", userDataConfig)
                .withCacheConfiguration("userInterests", userDataConfig)
                .withCacheConfiguration("userSocialMedia", userDataConfig)
                .withCacheConfiguration("userSocialMediaByUserId", userDataConfig)
                
                // Friend request caches
                .withCacheConfiguration("incomingFetchFriendRequests", userDataConfig)
                .withCacheConfiguration("sentFetchFriendRequests", userDataConfig)
                .withCacheConfiguration("friendRequests", userDataConfig)
                .withCacheConfiguration("friendRequestsByUserId", userDataConfig)
                
                // Activity type caches
                .withCacheConfiguration("activityTypes", staticDataConfig)
                .withCacheConfiguration("activityTypesByUserId", staticDataConfig)
                
                // Location caches
                .withCacheConfiguration("locations", staticDataConfig)
                .withCacheConfiguration("locationById", staticDataConfig)
                
                // Stats caches
                .withCacheConfiguration("userStats", statsConfig)
                .withCacheConfiguration("userStatsById", statsConfig)
                
                // Activity caches - shorter TTL to prevent stale expiration status
                .withCacheConfiguration("ActivityById", activityConfig)
                .withCacheConfiguration("fullActivityById", activityConfig)
                .withCacheConfiguration("ActivityInviteById", activityConfig)
                .withCacheConfiguration("ActivitiesByOwnerId", activityConfig)
                .withCacheConfiguration("feedActivities", activityConfig)
                .withCacheConfiguration("ActivitiesInvitedTo", activityConfig)
                .withCacheConfiguration("fullActivitiesInvitedTo", activityConfig)
                .withCacheConfiguration("fullActivitiesParticipatingIn", activityConfig)
                .withCacheConfiguration("calendarActivities", activityConfig)
                .withCacheConfiguration("allCalendarActivities", activityConfig)
                .withCacheConfiguration("filteredCalendarActivities", activityConfig)
                
                // Blocked user caches
                .withCacheConfiguration("blockedUsers", userDataConfig)
                .withCacheConfiguration("blockedUserIds", userDataConfig)
                .withCacheConfiguration("isBlocked", userDataConfig)
                .build();
    }

    /**
     * Global cache error handler that automatically evicts corrupted cache entries
     * when JSON deserialization fails (typically from old JDK serialization format).
     * This prevents domain-specific error handling in controllers and services.
     */
    @Bean
    @Override
    public CacheErrorHandler errorHandler() {
        return new CacheErrorHandler() {
            @Override
            public void handleCacheGetError(RuntimeException exception, Cache cache, Object key) {
                // Check if this is a JSON deserialization error
                if (isJsonDeserializationError(exception)) {
                    logger.warn("Cache corruption detected in '{}' for key '{}'. Evicting and allowing retry...", 
                               cache.getName(), key);
                    try {
                        cache.evict(key);
                        logger.info("Successfully evicted corrupted cache entry from '{}' for key: {}", 
                                   cache.getName(), key);
                    } catch (Exception e) {
                        logger.error("Failed to evict corrupted cache entry from '{}': {}", 
                                    cache.getName(), e.getMessage());
                    }
                } else {
                    // For non-deserialization errors, log but don't break the application
                    logger.error("Cache GET error in '{}' for key '{}': {}", 
                                cache.getName(), key, exception.getMessage());
                }
                // Don't rethrow - allow application to continue by fetching from database
            }

            @Override
            public void handleCachePutError(RuntimeException exception, Cache cache, Object key, Object value) {
                logger.error("Cache PUT error in '{}' for key '{}': {}", 
                            cache.getName(), key, exception.getMessage());
                // Don't rethrow - allow application to continue without caching
            }

            @Override
            public void handleCacheEvictError(RuntimeException exception, Cache cache, Object key) {
                logger.error("Cache EVICT error in '{}' for key '{}': {}", 
                            cache.getName(), key, exception.getMessage());
                // Don't rethrow - not critical if eviction fails
            }

            @Override
            public void handleCacheClearError(RuntimeException exception, Cache cache) {
                logger.error("Cache CLEAR error in '{}': {}", 
                            cache.getName(), exception.getMessage());
                // Don't rethrow - not critical if clear fails
            }
        };
    }

    /**
     * Checks if an exception is a JSON deserialization error, typically caused by
     * corrupted cache data from the old JDK serialization format.
     */
    private boolean isJsonDeserializationError(RuntimeException exception) {
        Throwable cause = exception;
        while (cause != null) {
            if (cause instanceof JsonParseException || cause instanceof JsonMappingException) {
                return true;
            }
            // Also check for the specific error message patterns from logs
            if (cause.getMessage() != null && 
                (cause.getMessage().contains("Could not read JSON") ||
                 cause.getMessage().contains("Unexpected token"))) {
                return true;
            }
            cause = cause.getCause();
        }
        return false;
    }
}
