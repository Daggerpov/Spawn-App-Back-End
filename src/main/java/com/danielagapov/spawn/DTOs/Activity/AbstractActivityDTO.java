package com.danielagapov.spawn.DTOs.Activity;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.UUID;
import com.fasterxml.jackson.annotation.JsonProperty;

@NoArgsConstructor
@Getter
@Setter
// Groups activities under an "abstract activity"
public abstract class AbstractActivityDTO implements Serializable {
    UUID id;
    String title;
    OffsetDateTime startTime;
    OffsetDateTime endTime;
    String note;
    /**
     * The icon is stored as a Unicode emoji character string (e.g. "⭐️", "🎉", "🏀").
     * This is the literal emoji character as a String, not a shortcode or description.
     * It's rendered directly in the UI and stored as a String in the database.
     * Java String supports full UTF-8 emoji characters.
     */
    String icon;
    Integer participantLimit; // null means unlimited participants
    Instant createdAt;
    
    /**
     * Indicates whether this activity is expired based on server-side logic.
     * This field is computed by the back-end and serves as the single source of truth
     * for expiration status across all clients.
     */
    @JsonProperty("isExpired")
    boolean isExpired;

    // Custom constructor with all fields including isExpired
    public AbstractActivityDTO(UUID id, String title, OffsetDateTime startTime, OffsetDateTime endTime, 
                              String note, String icon, Integer participantLimit, Instant createdAt, boolean isExpired) {
        this.id = id;
        this.title = title;
        this.startTime = startTime;
        this.endTime = endTime;
        this.note = note;
        this.icon = icon;
        this.participantLimit = participantLimit;
        this.createdAt = createdAt;
        this.isExpired = isExpired;
    }
}
