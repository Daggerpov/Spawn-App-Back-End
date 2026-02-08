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
import java.util.Optional;
import java.util.UUID;

/*
 * Represents a unique Spawn User.
 * This is the auth-service version of the User entity (simplified, no notification preferences).
 */
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@Table(
        name = "`user`",
        indexes = {
                @Index(name = "idx_name", columnList = "name"),
                @Index(name = "idx_email", columnList = "email"),
                @Index(name = "idx_username", columnList = "username"),
                @Index(name = "idx_phone_number", columnList = "phoneNumber")
        }
)
public class User implements Serializable {
    @Id
    @GeneratedValue
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
        if (this.lastUpdated == null) {
            this.lastUpdated = Instant.now();
        }
        if (this.dateCreated == null) {
            this.dateCreated = new Date();
        }
    }

    @PreUpdate
    public void preUpdate() {
        this.lastUpdated = Instant.now();
    }

    public User(UUID id, String username, String profilePictureUrlString, String name, String bio, String email) {
        this.id = id;
        this.username = username;
        this.profilePictureUrlString = profilePictureUrlString;
        this.name = name;
        this.bio = bio;
        this.email = email;
        this.lastUpdated = Instant.now();
        this.hasCompletedOnboarding = false;
    }

    public Optional<String> getOptionalUsername() {
        return Optional.ofNullable(username).filter(s -> !s.trim().isEmpty());
    }

    public Optional<String> getOptionalPhoneNumber() {
        return Optional.ofNullable(phoneNumber).filter(s -> !s.trim().isEmpty());
    }

    public Optional<String> getOptionalName() {
        return Optional.ofNullable(name).filter(s -> !s.trim().isEmpty());
    }

    public Optional<String> getOptionalEmail() {
        return Optional.ofNullable(email).filter(s -> !s.trim().isEmpty());
    }

    public Optional<String> getOptionalBio() {
        return Optional.ofNullable(bio).filter(s -> !s.trim().isEmpty());
    }

    public Optional<String> getOptionalPassword() {
        return Optional.ofNullable(password).filter(s -> !s.trim().isEmpty());
    }

    public boolean hasRequiredFieldsForStatus() {
        if (!getOptionalEmail().isPresent()) {
            return false;
        }
        switch (status) {
            case EMAIL_VERIFIED:
                return true;
            case USERNAME_AND_PHONE_NUMBER:
                return getOptionalUsername().isPresent() && getOptionalPhoneNumber().isPresent();
            case NAME_AND_PHOTO:
            case CONTACT_IMPORT:
            case ACTIVE:
                return getOptionalUsername().isPresent() && 
                       getOptionalPhoneNumber().isPresent() && 
                       getOptionalName().isPresent();
            default:
                return false;
        }
    }

    public String getDisplayName() {
        return getOptionalName()
            .or(() -> getOptionalUsername())
            .or(() -> getOptionalEmail().map(email -> email.split("@")[0]))
            .orElse("User");
    }

    public void markOnboardingCompleted() {
        this.hasCompletedOnboarding = true;
    }
}
