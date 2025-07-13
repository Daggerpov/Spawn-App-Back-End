package com.danielagapov.spawn.Services;

import com.danielagapov.spawn.Enums.ShareLinkType;
import com.danielagapov.spawn.Models.Activity;
import com.danielagapov.spawn.Models.ShareLink;
import com.danielagapov.spawn.Models.User.User;
import com.danielagapov.spawn.Repositories.ShareLinkRepository;
import com.danielagapov.spawn.Util.ShareCodeGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

/**
 * Service for managing share links with human-readable codes instead of UUIDs.
 * Handles generation, resolution, and cleanup of share links.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ShareLinkService {
    
    private final ShareLinkRepository shareLinkRepository;
    private final ShareCodeGenerator shareCodeGenerator;
    
    private static final int MAX_GENERATION_RETRIES = 10;
    
    /**
     * Generate a share link for an activity
     * @param activityId The activity ID to create a share link for
     * @param startTime The activity start time
     * @param endTime The activity end time
     * @return The generated share code
     */
    @Transactional
    public String generateActivityShareLink(UUID activityId, java.time.OffsetDateTime startTime, java.time.OffsetDateTime endTime) {
        return generateShareLink(activityId, ShareLinkType.ACTIVITY, calculateActivityExpiration(startTime, endTime));
    }
    
    /**
     * Generate a share link for a user profile
     * @param userId The user ID to create a share link for
     * @return The generated share code
     */
    @Transactional
    public String generateProfileShareLink(UUID userId) {
        return generateShareLink(userId, ShareLinkType.PROFILE, null); // Profile links don't expire
    }
    
    /**
     * Generate a share link for a given target ID and type
     * @param targetId The ID of the target (activity or user)
     * @param type The type of share link
     * @param expiresAt When the link should expire (null for no expiration)
     * @return The generated share code
     */
    @Transactional
    public String generateShareLink(UUID targetId, ShareLinkType type, Instant expiresAt) {
        // First, check if a valid link already exists
        Optional<ShareLink> existingLink = shareLinkRepository.findByTargetIdAndType(targetId, type)
            .stream()
            .filter(link -> !link.isExpired())
            .findFirst();
            
        if (existingLink.isPresent()) {
            return existingLink.get().getShareCode();
        }
        
        // Generate a unique share code
        String shareCode = shareCodeGenerator.generateUniqueShareCode(
            MAX_GENERATION_RETRIES, 
            shareLinkRepository::existsByShareCode
        );
        
        // Create and save the share link
        ShareLink shareLink = new ShareLink(shareCode, type, targetId, expiresAt);
        shareLinkRepository.save(shareLink);
        
        log.info("Generated share link: {} for {} with ID: {} (expires: {})", 
                shareCode, type, targetId, 
                expiresAt != null ? expiresAt.toString() : "never");
        return shareCode;
    }
    
    /**
     * Resolve a share code to its target ID and type
     * @param shareCode The share code to resolve
     * @return Optional containing the ShareLink if found and not expired
     */
    @Transactional(readOnly = true)
    public Optional<ShareLink> resolveShareCode(String shareCode) {
        return shareLinkRepository.findActiveByShareCode(shareCode, Instant.now());
    }
    
    /**
     * Delete all share links for a specific target
     * @param targetId The target ID to delete links for
     * @param type The type of links to delete
     */
    @Transactional
    public void deleteShareLinksForTarget(UUID targetId, ShareLinkType type) {
        shareLinkRepository.deleteByTargetIdAndType(targetId, type);
        log.info("Deleted share links for {} with ID: {}", type, targetId);
    }
    
    /**
     * Clean up expired share links
     * @return Number of expired links deleted
     */
    @Transactional
    public int cleanupExpiredLinks() {
        int deletedCount = shareLinkRepository.deleteExpiredLinks(Instant.now());
        if (deletedCount > 0) {
            log.info("Cleaned up {} expired share links", deletedCount);
        }
        return deletedCount;
    }
    
    /**
     * Calculate when an activity share link should expire
     * @param startTime The activity start time
     * @param endTime The activity end time
     * @return Expiration time (activity end time + 1 day, or start time + 2 days if no end time)
     */
    private Instant calculateActivityExpiration(java.time.OffsetDateTime startTime, java.time.OffsetDateTime endTime) {
        if (endTime != null) {
            // Link expires 1 day after activity ends
            return endTime.toInstant().plus(1, ChronoUnit.DAYS);
        } else if (startTime != null) {
            // Link expires 2 days after activity starts if no end time
            return startTime.toInstant().plus(2, ChronoUnit.DAYS);
        } else {
            // Default to 2 days from now if no times are set
            return Instant.now().plus(2, ChronoUnit.DAYS);
        }
    }
} 