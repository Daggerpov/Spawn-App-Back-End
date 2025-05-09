package com.danielagapov.spawn.DTOs.Event;

import com.danielagapov.spawn.Enums.EventCategory;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.time.OffsetDateTime;
import java.util.UUID;

@AllArgsConstructor
@Getter
@Setter
// Groups events under an "abstract event"
public abstract class AbstractEventDTO implements Serializable {
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
    EventCategory category;
}
