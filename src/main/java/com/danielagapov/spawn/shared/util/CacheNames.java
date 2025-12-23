package com.danielagapov.spawn.shared.util;

/**
 * Central repository for all cache names used throughout the application.
 * Using constants ensures consistency and prevents typos in cache name strings.
 * 
 * This class also provides cache groups for bulk operations.
 */
public final class CacheNames {
    
    private CacheNames() {
        // Prevent instantiation
    }
    
    // ========== User-related caches ==========
    public static final String FRIENDS_BY_USER_ID = "friendsByUserId";
    public static final String RECOMMENDED_FRIENDS = "recommendedFriends";
    public static final String USER_INTERESTS = "userInterests";
    public static final String USER_SOCIAL_MEDIA = "userSocialMedia";
    public static final String USER_SOCIAL_MEDIA_BY_USER_ID = "userSocialMediaByUserId";
    
    // ========== Friend request caches ==========
    public static final String INCOMING_FRIEND_REQUESTS = "incomingFetchFriendRequests";
    public static final String SENT_FRIEND_REQUESTS = "sentFetchFriendRequests";
    public static final String FRIEND_REQUESTS = "friendRequests";
    public static final String FRIEND_REQUESTS_BY_USER_ID = "friendRequestsByUserId";
    
    // ========== Activity type caches ==========
    public static final String ACTIVITY_TYPES = "activityTypes";
    public static final String ACTIVITY_TYPES_BY_USER_ID = "activityTypesByUserId";
    
    // ========== Location caches ==========
    public static final String LOCATIONS = "locations";
    public static final String LOCATION_BY_ID = "locationById";
    
    // ========== Stats caches ==========
    public static final String USER_STATS = "userStats";
    public static final String USER_STATS_BY_ID = "userStatsById";
    
    // ========== Activity caches ==========
    public static final String ACTIVITY_BY_ID = "ActivityById";
    public static final String FULL_ACTIVITY_BY_ID = "fullActivityById";
    public static final String ACTIVITY_INVITE_BY_ID = "ActivityInviteById";
    public static final String ACTIVITIES_BY_OWNER_ID = "ActivitiesByOwnerId";
    public static final String FEED_ACTIVITIES = "feedActivities";
    public static final String ACTIVITIES_INVITED_TO = "ActivitiesInvitedTo";
    public static final String FULL_ACTIVITIES_INVITED_TO = "fullActivitiesInvitedTo";
    public static final String FULL_ACTIVITIES_PARTICIPATING_IN = "fullActivitiesParticipatingIn";
    public static final String CALENDAR_ACTIVITIES = "calendarActivities";
    public static final String ALL_CALENDAR_ACTIVITIES = "allCalendarActivities";
    public static final String FILTERED_CALENDAR_ACTIVITIES = "filteredCalendarActivities";
    
    // ========== Blocked user caches ==========
    public static final String BLOCKED_USERS = "blockedUsers";
    public static final String BLOCKED_USER_IDS = "blockedUserIds";
    public static final String IS_BLOCKED = "isBlocked";
    
    // ========== Other caches ==========
    public static final String OTHER_PROFILES = "otherProfiles";
    public static final String FRIENDS_LIST = "friendsList";
    
    // ========== Cache groups for bulk operations ==========
    
    /**
     * All activity-related caches.
     * Used for clearing activity caches when activities expire or are updated.
     */
    public static final String[] ALL_ACTIVITY_CACHES = {
        FEED_ACTIVITIES,
        FULL_ACTIVITY_BY_ID,
        ACTIVITY_BY_ID,
        ACTIVITY_INVITE_BY_ID,
        ACTIVITIES_BY_OWNER_ID,
        ACTIVITIES_INVITED_TO,
        FULL_ACTIVITIES_INVITED_TO,
        FULL_ACTIVITIES_PARTICIPATING_IN,
        CALENDAR_ACTIVITIES,
        ALL_CALENDAR_ACTIVITIES,
        FILTERED_CALENDAR_ACTIVITIES
    };
    
    /**
     * All friend request related caches.
     * Used for clearing friend request caches when requests are created, accepted, or rejected.
     */
    public static final String[] ALL_FRIEND_REQUEST_CACHES = {
        INCOMING_FRIEND_REQUESTS,
        SENT_FRIEND_REQUESTS,
        FRIEND_REQUESTS,
        FRIEND_REQUESTS_BY_USER_ID
    };
    
    /**
     * All user profile related caches.
     * Used for clearing user caches when user data changes.
     */
    public static final String[] ALL_USER_CACHES = {
        FRIENDS_BY_USER_ID,
        RECOMMENDED_FRIENDS,
        USER_INTERESTS,
        USER_SOCIAL_MEDIA,
        USER_SOCIAL_MEDIA_BY_USER_ID,
        OTHER_PROFILES,
        FRIENDS_LIST
    };
    
    /**
     * All calendar related caches.
     * Used for clearing calendar caches when activities change.
     */
    public static final String[] ALL_CALENDAR_CACHES = {
        CALENDAR_ACTIVITIES,
        ALL_CALENDAR_ACTIVITIES,
        FILTERED_CALENDAR_ACTIVITIES
    };
    
    /**
     * All blocked user related caches.
     * Used for clearing blocked user caches when block status changes.
     */
    public static final String[] ALL_BLOCKED_USER_CACHES = {
        BLOCKED_USERS,
        BLOCKED_USER_IDS,
        IS_BLOCKED
    };
}


