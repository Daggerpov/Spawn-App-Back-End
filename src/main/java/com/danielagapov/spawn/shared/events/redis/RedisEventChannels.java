package com.danielagapov.spawn.shared.events.redis;

/**
 * Redis Pub/Sub channel names for cross-service events.
 * <p>
 * Centralised here so publishers and subscribers reference the same channel names.
 * As more services are extracted, add new channels here.
 */
public final class RedisEventChannels {

    private RedisEventChannels() {
        // utility class
    }

    /** Published by auth-service when a new user completes registration. */
    public static final String USER_REGISTERED = "events:user-registered";

    /** Published by auth-service when a user accepts Terms of Service. */
    public static final String USER_TOS_ACCEPTED = "events:user-tos-accepted";

    /** Published by activity-service when a new activity is created. */
    public static final String ACTIVITY_CREATED = "events:activity-created";

    /** Published by activity-service when users are invited to an activity. */
    public static final String ACTIVITY_INVITE = "events:activity-invite";

    /** Published by activity-service when an activity is updated. */
    public static final String ACTIVITY_UPDATED = "events:activity-updated";

    /** Published by activity-service when a user joins/leaves an activity. */
    public static final String ACTIVITY_PARTICIPATION_CHANGED = "events:activity-participation-changed";

    /** Published by chat-service when a new comment is created. Monolith subscribes to send notifications. */
    public static final String NEW_COMMENT = "events:new-comment";
}
