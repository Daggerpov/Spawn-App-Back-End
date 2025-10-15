package com.danielagapov.spawn.Services.Activity;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * Service that periodically cleans up expired activities from cache.
 * This prevents stale activity data from being served to clients when activities
 * have naturally expired over time.
 */
@Service
public class ActivityCacheCleanupService {
    
    private static final Logger logger = LoggerFactory.getLogger(ActivityCacheCleanupService.class);
    
    @Autowired
    private CacheManager cacheManager;
    
    /**
     * Periodically evicts all activity-related caches to ensure fresh expiration data.
     * Runs every 15 minutes to prevent serving stale activities that have expired.
     * 
     * Note: This is a preventive measure in addition to the 5-minute TTL.
     * Activities can expire at any time, so we proactively clear caches to ensure
     * the isExpired field is recalculated based on current time.
     */
    @Scheduled(fixedRate = 300000) // 5 minutes = 600,000 milliseconds
    public void cleanupExpiredActivityCaches() {
        try {
            logger.info("🧹 Starting scheduled activity cache cleanup");
            
            int clearedCaches = 0;
            
            // Clear all activity-related caches
            String[] activityCaches = {
                "feedActivities",
                "fullActivityById",
                "ActivityById",
                "ActivityInviteById",
                "ActivitiesByOwnerId",
                "ActivitiesInvitedTo",
                "fullActivitiesInvitedTo",
                "fullActivitiesParticipatingIn",
                "calendarActivities",
                "allCalendarActivities",
                "filteredCalendarActivities"
            };
            
            for (String cacheName : activityCaches) {
                if (cacheManager.getCache(cacheName) != null) {
                    cacheManager.getCache(cacheName).clear();
                    clearedCaches++;
                    logger.debug("✅ Cleared cache: {}", cacheName);
                }
            }
            
            logger.info("✅ Completed activity cache cleanup - cleared {} caches", clearedCaches);
            
        } catch (Exception e) {
            logger.error("❌ Error during activity cache cleanup: {}", e.getMessage(), e);
            // Don't throw - this is a best-effort background task
        }
    }
}

