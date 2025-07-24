package com.danielagapov.spawn.DTOs.Activity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.UUID;

@NoArgsConstructor
@AllArgsConstructor
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
}
