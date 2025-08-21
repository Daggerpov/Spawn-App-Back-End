package com.danielagapov.spawn.Models.User;

import com.danielagapov.spawn.Enums.UserStatus;
import com.danielagapov.spawn.Models.NotificationPreferences;
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
 * Friends are now managed through the Friendship model directly.
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
                @Index(name = "idx_name", columnList = "name")
        }
)
public class User implements Serializable {
    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = true, unique = true) // Username is optional until user provides it
    private String username;
    private String profilePictureUrlString;

    @Column(unique = true, nullable = true) // Phone number is optional until user provides it
    private String phoneNumber;

    @Column(nullable = true) // Name is optional until user provides it
    private String name;
    private String bio;
    
    @Column(nullable = true, unique = true)
    private String email;
    private String password;
    private Date dateCreated;
    
    @Column(name = "last_updated")
    private Instant lastUpdated;
    
    @OneToOne(mappedBy = "user", cascade = CascadeType.REMOVE, orphanRemoval = true)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private NotificationPreferences notificationPreferences;

    @Column(nullable = false)
    private UserStatus status;

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
    }

    // Optional-based helper methods for safe access to nullable fields
    
    /**
     * Returns the username as an Optional for safe handling of null values
     * @return Optional containing username if present, empty otherwise
     */
    public Optional<String> getOptionalUsername() {
        return Optional.ofNullable(username).filter(s -> !s.trim().isEmpty());
    }
    
    /**
     * Returns the phone number as an Optional for safe handling of null values
     * @return Optional containing phone number if present, empty otherwise
     */
    public Optional<String> getOptionalPhoneNumber() {
        return Optional.ofNullable(phoneNumber).filter(s -> !s.trim().isEmpty());
    }
    
    /**
     * Returns the name as an Optional for safe handling of null values
     * @return Optional containing name if present, empty otherwise
     */
    public Optional<String> getOptionalName() {
        return Optional.ofNullable(name).filter(s -> !s.trim().isEmpty());
    }
    
    /**
     * Returns the email as an Optional for safe handling of null values
     * @return Optional containing email if present, empty otherwise
     */
    public Optional<String> getOptionalEmail() {
        return Optional.ofNullable(email).filter(s -> !s.trim().isEmpty());
    }
    
    /**
     * Returns the bio as an Optional for safe handling of null values
     * @return Optional containing bio if present, empty otherwise
     */
    public Optional<String> getOptionalBio() {
        return Optional.ofNullable(bio).filter(s -> !s.trim().isEmpty());
    }
    
    /**
     * Returns the password as an Optional for safe handling of null values
     * @return Optional containing password if present, empty otherwise
     */
    public Optional<String> getOptionalPassword() {
        return Optional.ofNullable(password).filter(s -> !s.trim().isEmpty());
    }
    
    /**
     * Checks if the user has completed the basic required fields for their current status
     * @return true if user has the minimum required fields for their status
     */
    public boolean hasRequiredFieldsForStatus() {
        // Email is always required
        if (!getOptionalEmail().isPresent()) {
            return false;
        }
        
        // Check requirements based on status
        switch (status) {
            case EMAIL_VERIFIED:
                return true; // Only email required
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
    
    /**
     * Returns a display name for the user, falling back through available options
     * @return Display name (name -> username -> email prefix -> "User")
     */
    public String getDisplayName() {
        return getOptionalName()
            .or(() -> getOptionalUsername())
            .or(() -> getOptionalEmail().map(email -> email.split("@")[0]))
            .orElse("User");
    }
}
