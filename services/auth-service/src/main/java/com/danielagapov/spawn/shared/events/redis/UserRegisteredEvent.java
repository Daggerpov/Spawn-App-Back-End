package com.danielagapov.spawn.shared.events.redis;

import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

/**
 * Event published via Redis Pub/Sub when a new user registers.
 * <p>
 * Consumed by the monolith to initialise default activity types,
 * send welcome notifications, etc.
 */
public record UserRegisteredEvent(
        UUID userId,
        String email,
        String username,
        String provider, // "email", "google", or "apple"
        Instant registeredAt
) implements Serializable {
}
