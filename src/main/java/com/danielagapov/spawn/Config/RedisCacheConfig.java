package com.danielagapov.spawn.Config;

import com.danielagapov.spawn.Utils.Cache.CacheNames;
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
        RedisCacheConfiguration userDataConfig = createCacheConfig(Duration.ofMinutes(30), serializer); // User data changes more frequently
        RedisCacheConfiguration staticDataConfig = createCacheConfig(Duration.ofHours(4), serializer); // Static data like activity types, locations
        RedisCacheConfiguration statsConfig = createCacheConfig(Duration.ofMinutes(15), serializer); // Stats change frequently
        
        // Activity caches with shorter TTL to prevent stale expiration status
        // Activities can expire naturally over time, so we use a shorter cache duration
        RedisCacheConfiguration activityConfig = createCacheConfig(Duration.ofMinutes(5), serializer); // 5 minutes to ensure fresh expiration data

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfig)
                // User-related caches
                .withCacheConfiguration(CacheNames.FRIENDS_BY_USER_ID, userDataConfig)
                .withCacheConfiguration(CacheNames.RECOMMENDED_FRIENDS, userDataConfig)
                .withCacheConfiguration(CacheNames.USER_INTERESTS, userDataConfig)
                .withCacheConfiguration(CacheNames.USER_SOCIAL_MEDIA, userDataConfig)
                .withCacheConfiguration(CacheNames.USER_SOCIAL_MEDIA_BY_USER_ID, userDataConfig)
                
                // Friend request caches
                .withCacheConfiguration(CacheNames.INCOMING_FRIEND_REQUESTS, userDataConfig)
                .withCacheConfiguration(CacheNames.SENT_FRIEND_REQUESTS, userDataConfig)
                .withCacheConfiguration(CacheNames.FRIEND_REQUESTS, userDataConfig)
                .withCacheConfiguration(CacheNames.FRIEND_REQUESTS_BY_USER_ID, userDataConfig)
                
                // Activity type caches
                .withCacheConfiguration(CacheNames.ACTIVITY_TYPES, staticDataConfig)
                .withCacheConfiguration(CacheNames.ACTIVITY_TYPES_BY_USER_ID, staticDataConfig)
                
                // Location caches
                .withCacheConfiguration(CacheNames.LOCATIONS, staticDataConfig)
                .withCacheConfiguration(CacheNames.LOCATION_BY_ID, staticDataConfig)
                
                // Stats caches
                .withCacheConfiguration(CacheNames.USER_STATS, statsConfig)
                .withCacheConfiguration(CacheNames.USER_STATS_BY_ID, statsConfig)
                
                // Activity caches - shorter TTL to prevent stale expiration status
                .withCacheConfiguration(CacheNames.ACTIVITY_BY_ID, activityConfig)
                .withCacheConfiguration(CacheNames.FULL_ACTIVITY_BY_ID, activityConfig)
                .withCacheConfiguration(CacheNames.ACTIVITY_INVITE_BY_ID, activityConfig)
                .withCacheConfiguration(CacheNames.ACTIVITIES_BY_OWNER_ID, activityConfig)
                .withCacheConfiguration(CacheNames.FEED_ACTIVITIES, activityConfig)
                .withCacheConfiguration(CacheNames.ACTIVITIES_INVITED_TO, activityConfig)
                .withCacheConfiguration(CacheNames.FULL_ACTIVITIES_INVITED_TO, activityConfig)
                .withCacheConfiguration(CacheNames.FULL_ACTIVITIES_PARTICIPATING_IN, activityConfig)
                .withCacheConfiguration(CacheNames.CALENDAR_ACTIVITIES, activityConfig)
                .withCacheConfiguration(CacheNames.ALL_CALENDAR_ACTIVITIES, activityConfig)
                .withCacheConfiguration(CacheNames.FILTERED_CALENDAR_ACTIVITIES, activityConfig)
                
                // Blocked user caches
                .withCacheConfiguration(CacheNames.BLOCKED_USERS, userDataConfig)
                .withCacheConfiguration(CacheNames.BLOCKED_USER_IDS, userDataConfig)
                .withCacheConfiguration(CacheNames.IS_BLOCKED, userDataConfig)
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
    
    /**
     * Helper method to create cache configurations with consistent settings.
     * This reduces code duplication for cache configurations that only differ in TTL.
     * 
     * @param ttl The time-to-live duration for the cache
     * @param serializer The JSON serializer to use
     * @return A configured RedisCacheConfiguration
     */
    private RedisCacheConfiguration createCacheConfig(Duration ttl, GenericJackson2JsonRedisSerializer serializer) {
        return RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(ttl)
                .serializeValuesWith(
                    RedisSerializationContext.SerializationPair.fromSerializer(serializer))
                .disableCachingNullValues();
    }
}
