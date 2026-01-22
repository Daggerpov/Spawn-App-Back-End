package com.danielagapov.spawn.analytics.internal.services;

/**
 * Enumeration of cache types used in the mobile cache validation system.
 * Each cache type represents a specific category of data that can be cached on the client.
 */
public enum CacheType {
    FRIENDS("friends"),
    EVENTS("events"),
    ACTIVITY_TYPES("activityTypes"),
    PROFILE_PICTURE("profilePicture"),
    OTHER_PROFILES("otherProfiles"),
    RECOMMENDED_FRIENDS("recommendedFriends"),
    FRIEND_REQUESTS("friendRequests"),
    SENT_FRIEND_REQUESTS("sentFriendRequests"),
    RECENTLY_SPAWNED("recentlySpawned"),
    PROFILE_STATS("profileStats"),
    PROFILE_INTERESTS("profileInterests"),
    PROFILE_SOCIAL_MEDIA("profileSocialMedia"),
    PROFILE_EVENTS("profileEvents");

    private final String key;

    CacheType(String key) {
        this.key = key;
    }

    /**
     * Gets the string key used for the cache type.
     * This is used for logging and as the key in cache validation requests.
     */
    public String getKey() {
        return key;
    }

    /**
     * Gets a human-readable name for logging purposes.
     */
    public String getDisplayName() {
        switch (this) {
            case FRIENDS:
                return "friends";
            case EVENTS:
                return "activities";
            case ACTIVITY_TYPES:
                return "activity types";
            case PROFILE_PICTURE:
                return "profile picture";
            case OTHER_PROFILES:
                return "other profiles";
            case RECOMMENDED_FRIENDS:
                return "recommended friends";
            case FRIEND_REQUESTS:
                return "friend requests";
            case SENT_FRIEND_REQUESTS:
                return "sent friend requests";
            case RECENTLY_SPAWNED:
                return "recently spawned";
            case PROFILE_STATS:
                return "profile stats";
            case PROFILE_INTERESTS:
                return "profile interests";
            case PROFILE_SOCIAL_MEDIA:
                return "profile social media";
            case PROFILE_EVENTS:
                return "profile activities";
            default:
                return key;
        }
    }

    @Override
    public String toString() {
        return key;
    }
}

