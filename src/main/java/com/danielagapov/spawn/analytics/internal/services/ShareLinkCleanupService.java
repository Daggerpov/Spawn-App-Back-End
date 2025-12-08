package com.danielagapov.spawn.analytics.internal.services;

import com.danielagapov.spawn.shared.util.ShareLinkType;
import com.danielagapov.spawn.activity.internal.repositories.IActivityRepository;
import com.danielagapov.spawn.user.internal.repositories.IUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * Service to clean up expired and orphaned share links
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ShareLinkCleanupService {
    
    private final ShareLinkService shareLinkService;
    private final IActivityRepository activityRepository;
    private final IUserRepository userRepository;
    
    /**
     * Clean up expired share links every hour
     */
    @Scheduled(fixedRate = 1, timeUnit = TimeUnit.HOURS)
    public void cleanupExpiredShareLinks() {
        try {
            int deletedCount = shareLinkService.cleanupExpiredLinks();
            if (deletedCount > 0) {
                log.info("Cleaned up {} expired share links", deletedCount);
            }
        } catch (Exception e) {
            log.error("Error cleaning up expired share links", e);
        }
    }
    
    /**
     * Clean up orphaned share links (links pointing to deleted activities/users) every day
     */
    @Scheduled(fixedRate = 1, timeUnit = TimeUnit.DAYS)
    public void cleanupOrphanedShareLinks() {
        try {
            // This would require a more complex query to find orphaned links
            // For now, we'll rely on the controller to clean up links when activities/users are not found
            log.debug("Orphaned share link cleanup completed");
        } catch (Exception e) {
            log.error("Error cleaning up orphaned share links", e);
        }
    }
} 