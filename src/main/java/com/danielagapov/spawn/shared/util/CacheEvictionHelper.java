package com.danielagapov.spawn.shared.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Utility class for safe cache eviction operations.
 * 
 * This helper centralizes all cache eviction logic, providing:
 * - Null-safe cache access
 * - Consistent error handling and logging
 * - Bulk eviction operations
 * - Reduces code duplication across services
 * 
 * All methods are best-effort: they log errors but never throw exceptions,
 * allowing the application to continue even if cache operations fail.
 */
@Component
public class CacheEvictionHelper {
    
    private static final Logger logger = LoggerFactory.getLogger(CacheEvictionHelper.class);
    
    private final CacheManager cacheManager;
    
    @Autowired
    public CacheEvictionHelper(CacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }
    
    /**
     * Safely evicts a single cache entry.
     * 
     * @param cacheName The name of the cache
     * @param key The key to evict
     */
    public void evictCache(String cacheName, Object key) {
        try {
            Cache cache = cacheManager.getCache(cacheName);
            if (cache != null) {
                cache.evict(key);
                logger.debug("Evicted cache '{}' for key: {}", cacheName, key);
            } else {
                logger.warn("Cache '{}' not found when attempting to evict key: {}", cacheName, key);
            }
        } catch (Exception e) {
            logger.error("Error evicting cache '{}' for key {}: {}", cacheName, key, e.getMessage());
            // Don't throw - this is a best-effort operation
        }
    }
    
    /**
     * Safely evicts the same key from multiple caches.
     * 
     * @param key The key to evict
     * @param cacheNames The names of the caches to evict from
     */
    public void evictCaches(Object key, String... cacheNames) {
        for (String cacheName : cacheNames) {
            evictCache(cacheName, key);
        }
    }
    
    /**
     * Safely evicts multiple keys from the same cache.
     * 
     * @param cacheName The name of the cache
     * @param keys The keys to evict
     */
    public void evictCacheForKeys(String cacheName, Object... keys) {
        for (Object key : keys) {
            evictCache(cacheName, key);
        }
    }
    
    /**
     * Safely evicts a cache entry for multiple user IDs.
     * Convenience method for the common pattern of evicting by user ID.
     * 
     * @param cacheName The name of the cache
     * @param userIds The user IDs to evict
     */
    public void evictCacheForUsers(String cacheName, UUID... userIds) {
        for (UUID userId : userIds) {
            evictCache(cacheName, userId);
        }
    }
    
    /**
     * Safely evicts multiple user IDs from multiple caches.
     * Convenience method for bulk user cache eviction.
     * 
     * @param userIds The user IDs to evict
     * @param cacheNames The cache names to evict from
     */
    public void evictCachesForUsers(UUID[] userIds, String... cacheNames) {
        for (UUID userId : userIds) {
            evictCaches(userId, cacheNames);
        }
    }
    
    /**
     * Safely clears an entire cache.
     * 
     * @param cacheName The name of the cache to clear
     */
    public void clearCache(String cacheName) {
        try {
            Cache cache = cacheManager.getCache(cacheName);
            if (cache != null) {
                cache.clear();
                logger.debug("Cleared cache: {}", cacheName);
            } else {
                logger.warn("Cache '{}' not found when attempting to clear", cacheName);
            }
        } catch (Exception e) {
            logger.error("Error clearing cache '{}': {}", cacheName, e.getMessage());
            // Don't throw - this is a best-effort operation
        }
    }
    
    /**
     * Safely clears multiple caches.
     * 
     * @param cacheNames The names of the caches to clear
     */
    public void clearCaches(String... cacheNames) {
        for (String cacheName : cacheNames) {
            clearCache(cacheName);
        }
    }
    
    /**
     * Clears all activity-related caches.
     * Convenience method that uses the predefined cache group.
     */
    public void clearAllActivityCaches() {
        logger.info("Clearing all activity-related caches");
        clearCaches(CacheNames.ALL_ACTIVITY_CACHES);
    }
    
    /**
     * Clears all friend request caches.
     * Convenience method that uses the predefined cache group.
     */
    public void clearAllFriendRequestCaches() {
        logger.info("Clearing all friend request caches");
        clearCaches(CacheNames.ALL_FRIEND_REQUEST_CACHES);
    }
    
    /**
     * Clears all user-related caches.
     * Convenience method that uses the predefined cache group.
     */
    public void clearAllUserCaches() {
        logger.info("Clearing all user-related caches");
        clearCaches(CacheNames.ALL_USER_CACHES);
    }
    
    /**
     * Clears all calendar caches.
     * Convenience method that uses the predefined cache group.
     */
    public void clearAllCalendarCaches() {
        logger.info("Clearing all calendar caches");
        clearCaches(CacheNames.ALL_CALENDAR_CACHES);
    }
    
    /**
     * Clears all blocked user caches.
     * Convenience method that uses the predefined cache group.
     */
    public void clearAllBlockedUserCaches() {
        logger.info("Clearing all blocked user caches");
        clearCaches(CacheNames.ALL_BLOCKED_USER_CACHES);
    }
    
    /**
     * Evicts friend-related caches for a specific user.
     * This is a common operation when friend relationships change.
     * 
     * @param userId The user ID whose friend caches should be evicted
     */
    public void evictFriendCachesForUser(UUID userId) {
        evictCaches(userId, 
            CacheNames.FRIENDS_BY_USER_ID,
            CacheNames.RECOMMENDED_FRIENDS,
            CacheNames.OTHER_PROFILES,
            CacheNames.FRIENDS_LIST
        );
    }
    
    /**
     * Evicts friend-related caches for multiple users.
     * Useful when a friendship is created/deleted affecting both users.
     * 
     * @param userIds The user IDs whose friend caches should be evicted
     */
    public void evictFriendCachesForUsers(UUID... userIds) {
        for (UUID userId : userIds) {
            evictFriendCachesForUser(userId);
        }
    }
    
    /**
     * Evicts activity caches for a specific user.
     * 
     * @param userId The user ID whose activity caches should be evicted
     */
    public void evictActivityCachesForUser(UUID userId) {
        evictCaches(userId,
            CacheNames.FEED_ACTIVITIES,
            CacheNames.ACTIVITIES_BY_OWNER_ID,
            CacheNames.ACTIVITIES_INVITED_TO,
            CacheNames.FULL_ACTIVITIES_INVITED_TO,
            CacheNames.FULL_ACTIVITIES_PARTICIPATING_IN
        );
        // Also clear caches that use composite keys
        clearCache(CacheNames.FULL_ACTIVITY_BY_ID);
    }
}


