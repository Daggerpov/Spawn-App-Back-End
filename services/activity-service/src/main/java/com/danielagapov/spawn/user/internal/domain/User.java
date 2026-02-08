package com.danielagapov.spawn.user.internal.domain;

import com.danielagapov.spawn.shared.util.UserStatus;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@Table(name = "`user`")
public class User implements Serializable {
    @Id @GeneratedValue
    private UUID id;

    @Column(nullable = true, unique = true)
    private String username;
    private String profilePictureUrlString;

    @Column(unique = true, nullable = true)
    private String phoneNumber;

    @Column(nullable = true)
    private String name;
    private String bio;
    
    @Column(nullable = true, unique = true)
    private String email;
    private String password;
    private Date dateCreated;
    
    @Column(name = "last_updated")
    private Instant lastUpdated;

    @Column(nullable = false)
    private UserStatus status;

    @Column(nullable = false, columnDefinition = "BOOLEAN DEFAULT FALSE")
    private Boolean hasCompletedOnboarding = false;

    @PrePersist
    public void prePersist() {
        if (this.lastUpdated == null) this.lastUpdated = Instant.now();
        if (this.dateCreated == null) this.dateCreated = new Date();
    }

    @PreUpdate
    public void preUpdate() {
        this.lastUpdated = Instant.now();
    }

    public User(UUID id, String username, String profilePictureUrlString, String name, String bio, String email) {
        this.id = id; this.username = username; this.profilePictureUrlString = profilePictureUrlString;
        this.name = name; this.bio = bio; this.email = email;
        this.lastUpdated = Instant.now(); this.hasCompletedOnboarding = false;
    }

    public void markOnboardingCompleted() {
        this.hasCompletedOnboarding = true;
    }
}
