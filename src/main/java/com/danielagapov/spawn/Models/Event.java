package com.danielagapov.spawn.Models;

import com.danielagapov.spawn.Models.User.User;
import com.danielagapov.spawn.Enums.EventCategory;
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
 * An event is the primary function of our app.
 * Upon creation, the creating user can invite many
 * friends directly, or by friend tags, that they've placed
 * friends into. Then, those invited users can choose to
 * participate in that event.
 */
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class Event implements Serializable {
    private @Id
    @GeneratedValue UUID id;

    private String title;
    private OffsetDateTime startTime;
    private OffsetDateTime endTime;
    private String icon;
    private String colorHexCode;
    
    @Enumerated(EnumType.STRING)
    private EventCategory category;

    @ManyToOne(cascade = CascadeType.REMOVE)
    @JoinColumn(name = "location_id", referencedColumnName = "id", nullable = false)
    private Location location;

    private String note;

    @ManyToOne
    @JoinColumn(name = "creator_id", referencedColumnName = "id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private User creator;
    
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
    
    public Event(UUID id, String title, OffsetDateTime startTime, OffsetDateTime endTime, Location location, String note, User creator, String icon, EventCategory category, String colorHexCode) {
        this.id = id;
        this.title = title;
        this.startTime = startTime;
        this.endTime = endTime;
        this.location = location;
        this.note = note;
        this.creator = creator;
        this.lastUpdated = Instant.now();
        this.icon = icon;
        this.category = category;
        this.colorHexCode = colorHexCode;
    }
}
