package com.danielagapov.spawn.shared.events;

import java.util.UUID;

/**
 * Domain events for User-ActivityType module inter-module communication.
 * Used to break circular dependencies between User and Activity modules.
 */
public final class UserActivityTypeEvents {
    
    private UserActivityTypeEvents() {
        // Utility class - prevent instantiation
    }
    
    /**
     * Event published when a new user is created and needs default activity types initialized.
     * Published by User module, consumed by Activity module.
     */
    public record UserCreatedEvent(
        UUID userId,
        String username
    ) {}
    
    /**
     * Event published when default activity types have been initialized for a user.
     * Published by Activity module in response to UserCreatedEvent.
     */
    public record DefaultActivityTypesInitializedEvent(
        UUID userId,
        int activityTypeCount
    ) {}
}
