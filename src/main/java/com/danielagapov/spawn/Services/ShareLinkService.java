package com.danielagapov.spawn.Services;

import com.danielagapov.spawn.Enums.ShareLinkType;
import com.danielagapov.spawn.Models.ShareLink;
import com.danielagapov.spawn.Repositories.ShareLinkRepository;
import com.danielagapov.spawn.Services.Activity.ActivityExpirationService;
import com.danielagapov.spawn.Util.ShareCodeGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
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
    private final ActivityExpirationService expirationService;
    
    private static final int MAX_GENERATION_RETRIES = 10;
    
    /**
     * Generate a share link for an activity
     * @param activityId The activity ID to create a share link for
     * @param startTime The activity start time
     * @param endTime The activity end time
     * @param createdAt The activity creation time
     * @return The generated share code
     */
    @Transactional
    public String generateActivityShareLink(UUID activityId, java.time.OffsetDateTime startTime, java.time.OffsetDateTime endTime, Instant createdAt) {
        Instant shareExpiration = null;
        if (expirationService.calculateShareLinkExpiration(startTime, endTime, createdAt) != null) {
            shareExpiration = expirationService.calculateShareLinkExpiration(startTime, endTime, createdAt).toInstant();
        }
        return generateShareLink(activityId, ShareLinkType.ACTIVITY, shareExpiration);
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
    
} 