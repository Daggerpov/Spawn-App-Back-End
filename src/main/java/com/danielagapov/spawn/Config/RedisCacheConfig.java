package com.danielagapov.spawn.Config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.interceptor.SimpleCacheErrorHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;

import java.time.Duration;

@Configuration
@EnableCaching
@ConditionalOnClass(RedisConnectionFactory.class)
@ConditionalOnProperty(name = "spring.cache.type", havingValue = "redis", matchIfMissing = false)
@Profile("!test") // Don't load this configuration in test profile
public class RedisCacheConfig {

    @Bean
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        // Set default TTL of 100 minutes
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(100));

        // Configure different TTL values for different cache types
        RedisCacheConfiguration userDataConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(30)); // User data changes more frequently
        
        RedisCacheConfiguration staticDataConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofHours(4)); // Static data like activity types, locations
        
        RedisCacheConfiguration statsConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(15)); // Stats change frequently

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfig)
                .withCacheConfiguration("userInterests", userDataConfig)
                .withCacheConfiguration("userSocialMedia", userDataConfig)
                .withCacheConfiguration("userSocialMediaByUserId", userDataConfig)
                .withCacheConfiguration("friendRequests", userDataConfig)
                .withCacheConfiguration("friendRequestsByUserId", userDataConfig)
                .withCacheConfiguration("activityTypes", staticDataConfig)
                .withCacheConfiguration("activityTypesByUserId", staticDataConfig)
                .withCacheConfiguration("locations", staticDataConfig)
                .withCacheConfiguration("locationById", staticDataConfig)
                .withCacheConfiguration("userStats", statsConfig)
                .withCacheConfiguration("userStatsById", statsConfig)
                .build();
    }

    @Bean
    public SimpleCacheErrorHandler errorHandler() {
        return new SimpleCacheErrorHandler();
    }
}
