package com.danielagapov.spawn.Models.User.Profile;

import com.danielagapov.spawn.Models.User.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

/**
 * Represents a user's social media links.
 */
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Table(name = "user_social_media")
public class UserSocialMedia implements Serializable {
    @Id
    @GeneratedValue
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private User user;

    @Column(name = "whatsapp_number")
    private String whatsappNumber;

    @Column(name = "instagram_username")
    private String instagramUsername;

    @Column(name = "last_updated")
    private Instant lastUpdated;

    @PrePersist
    public void prePersist() {
        if (this.lastUpdated == null) {
            this.lastUpdated = Instant.now();
        }
    }

    @PreUpdate
    public void preUpdate() {
        this.lastUpdated = Instant.now();
    }
    
    public UserSocialMedia(User user) {
        this.user = user;
        this.lastUpdated = Instant.now();
    }
} 