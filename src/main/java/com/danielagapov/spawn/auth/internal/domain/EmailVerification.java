package com.danielagapov.spawn.auth.internal.domain;

import com.danielagapov.spawn.user.internal.domain.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Getter
@Setter
public class EmailVerification {
    @Id
    @GeneratedValue
    private UUID id;
    
    private int sendAttempts = 0;
    private Instant lastSendAttemptAt;
    private Instant nextSendAttemptAt;
    
    private int checkAttempts = 0;
    private Instant lastCheckAttemptAt;
    private Instant nextCheckAttemptAt;

    @Column(unique = true, nullable = false)
    private String verificationCode;
    @Column(unique = true)
    private String email;
    private Instant codeExpiresAt;

    @OneToOne(fetch = FetchType.LAZY)
    private User user;

    @PrePersist
    public void onCreate() {
        if (lastSendAttemptAt == null) {
            lastSendAttemptAt = Instant.now();
        }
        if (nextSendAttemptAt == null) {
            nextSendAttemptAt = Instant.now().plusSeconds(30);
        }
    }

    @PreUpdate
    public void onUpdate() {
        lastSendAttemptAt = Instant.now();
        lastCheckAttemptAt = Instant.now();
    }
} 