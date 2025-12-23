package com.danielagapov.spawn.analytics.internal.repositories;

import com.danielagapov.spawn.shared.util.ShareLinkType;
import com.danielagapov.spawn.analytics.internal.domain.ShareLink;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ShareLinkRepository extends JpaRepository<ShareLink, UUID> {
    
    /**
     * Find a share link by its share code
     */
    Optional<ShareLink> findByShareCode(String shareCode);
    
    /**
     * Find all share links for a specific target and type
     */
    List<ShareLink> findByTargetIdAndType(UUID targetId, ShareLinkType type);
    
    /**
     * Find an active (non-expired) share link by share code
     */
    @Query("SELECT s FROM ShareLink s WHERE s.shareCode = :shareCode AND (s.expiresAt IS NULL OR s.expiresAt > :now)")
    Optional<ShareLink> findActiveByShareCode(@Param("shareCode") String shareCode, @Param("now") Instant now);
    
    /**
     * Delete all share links for a specific target
     */
    @Modifying
    @Transactional
    @Query("DELETE FROM ShareLink s WHERE s.targetId = :targetId AND s.type = :type")
    void deleteByTargetIdAndType(@Param("targetId") UUID targetId, @Param("type") ShareLinkType type);
    
    /**
     * Delete all expired share links
     */
    @Modifying
    @Transactional
    @Query("DELETE FROM ShareLink s WHERE s.expiresAt IS NOT NULL AND s.expiresAt < :now")
    int deleteExpiredLinks(@Param("now") Instant now);
    
    /**
     * Check if a share code already exists
     */
    boolean existsByShareCode(String shareCode);
} 