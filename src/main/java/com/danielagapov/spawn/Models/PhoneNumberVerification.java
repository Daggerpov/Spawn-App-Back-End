package com.danielagapov.spawn.Models;

import com.danielagapov.spawn.Models.User.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Getter
@Setter
public class PhoneNumberVerification {
    @Id
    @GeneratedValue
    private UUID id;
    private int sendAttempts = 0;

    private Instant lastSendAttemptAt;
    private Instant nextSendAttemptAt;

    private List<String> attemptedPhoneNumbers = new ArrayList<>();


    private int checkAttempts = 0;
    private Instant lastCheckAttemptAt;
    private Instant nextCheckAttemptAt;


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
        if (attemptedPhoneNumbers == null) {
            attemptedPhoneNumbers = new ArrayList<>();
        }
    }

    @PreUpdate
    public void onUpdate() {
        lastSendAttemptAt = Instant.now();
        lastCheckAttemptAt = Instant.now();
    }
}
