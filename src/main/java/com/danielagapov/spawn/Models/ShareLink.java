package com.danielagapov.spawn.Models;

import com.danielagapov.spawn.Enums.ShareLinkType;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

/**
 * ShareLink entity for managing human-readable share codes for activities and profiles.
 * Instead of exposing UUIDs in sharing links, we use randomly generated two-word combinations.
 * 
 * The database table will be automatically created by Hibernate when the application starts
 * due to spring.jpa.hibernate.ddl-auto=update in application.properties.
 */
@Entity
@Table(name = "share_link", 
       indexes = {
           @Index(name = "idx_share_code", columnList = "shareCode", unique = true),
           @Index(name = "idx_target_id_type", columnList = "targetId, type"),
           @Index(name = "idx_expires_at", columnList = "expiresAt")
       })
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class ShareLink implements Serializable {
    
    @Id
    @GeneratedValue
    private UUID id;
    
    @Column(unique = true, nullable = false, length = 50)
    private String shareCode;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ShareLinkType type;
    
    @Column(nullable = false)
    private UUID targetId;
    
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    
    @Column(name = "expires_at")
    private Instant expiresAt;
    
    @PrePersist
    public void prePersist() {
        if (this.createdAt == null) {
            this.createdAt = Instant.now();
        }
    }
    
    public ShareLink(String shareCode, ShareLinkType type, UUID targetId, Instant expiresAt) {
        this.shareCode = shareCode;
        this.type = type;
        this.targetId = targetId;
        this.expiresAt = expiresAt;
        this.createdAt = Instant.now();
    }
    
    /**
     * Check if this share link has expired
     */
    public boolean isExpired() {
        return expiresAt != null && Instant.now().isAfter(expiresAt);
    }
} 