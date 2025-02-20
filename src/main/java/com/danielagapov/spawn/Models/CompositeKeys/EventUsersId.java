package com.danielagapov.spawn.Models.CompositeKeys;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

@Embeddable
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class EventUsersId implements Serializable {
    @Column(name = "event_id", nullable = false)
    private UUID eventId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof EventUsersId that)) return false;
        return Objects.equals(eventId, that.eventId) &&
                Objects.equals(userId, that.userId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(eventId, userId);
    }
}