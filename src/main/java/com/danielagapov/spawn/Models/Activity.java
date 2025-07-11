package com.danielagapov.spawn.Models;

import com.danielagapov.spawn.Models.User.User;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.io.Serializable;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * An activity is the primary function of our app.
 * Upon creation, the creating user can invite many
 * friends directly, or by friend tags, that they've placed
 * friends into. Then, those invited users can choose to
 * participate in that activity.
 */
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Activity implements Serializable {
    private @Id
    @GeneratedValue UUID id;

    private String title;
    private OffsetDateTime startTime;
    private OffsetDateTime endTime;
    private String icon;
    private String colorHexCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "activity_type_id", referencedColumnName = "id", nullable = true)
    @OnDelete(action = OnDeleteAction.SET_NULL)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private ActivityType activityType;

    @ManyToOne(cascade = CascadeType.REMOVE)
    @JoinColumn(name = "location_id", referencedColumnName = "id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Location location;

    private String note;

    @ManyToOne
    @JoinColumn(name = "creator_id", referencedColumnName = "id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private User creator;
    
    @Column(name = "created_at")
    private Instant createdAt;
    
    @Column(name = "last_updated")
    private Instant lastUpdated;
    
    @PrePersist
    public void prePersist() {
        if (this.createdAt == null) {
            this.createdAt = Instant.now();
        }
        if (this.lastUpdated == null) {
            this.lastUpdated = Instant.now();
        }
    }

    @PreUpdate
    public void preUpdate() {
        this.lastUpdated = Instant.now();
    }
    
    public Activity(UUID id, String title, OffsetDateTime startTime, OffsetDateTime endTime, Location location, String note, User creator, String icon) {
        this.id = id;
        this.title = title;
        this.startTime = startTime;
        this.endTime = endTime;
        this.location = location;
        this.note = note;
        this.creator = creator;
        this.createdAt = Instant.now();
        this.lastUpdated = Instant.now();
        this.icon = icon;
    }
}
