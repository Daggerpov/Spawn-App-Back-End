package com.danielagapov.spawn.shared.events.redis;

public final class RedisEventChannels {

    private RedisEventChannels() {}

    /** Published by chat-service when a new comment is created. Monolith subscribes to send notifications. */
    public static final String NEW_COMMENT = "events:new-comment";
}
