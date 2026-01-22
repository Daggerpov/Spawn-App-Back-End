# DRY Refactoring Analysis

**Date:** October 31, 2025  
**Purpose:** Identify and document opportunities to apply the DRY (Don't Repeat Yourself) principle across the codebase

## Executive Summary

This document identifies key areas where code duplication exists and provides actionable refactoring recommendations. The main areas of concern are:

1. **Cache configuration and management** (HIGH PRIORITY)
2. **Controller exception handling patterns** (MEDIUM PRIORITY)
3. **Cache validation logic** (MEDIUM PRIORITY)
4. **Mapper utility methods** (LOW PRIORITY)

---

## 1. Cache Configuration (HIGH PRIORITY)

### Location: `RedisCacheConfig.java` (Lines 71-95)

### Problem
Four nearly identical `RedisCacheConfiguration` objects are created with only the TTL duration differing:

```java
// Lines 71-75: User data config
RedisCacheConfiguration userDataConfig = RedisCacheConfiguration.defaultCacheConfig()
        .entryTtl(Duration.ofMinutes(30))
        .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(serializer))
        .disableCachingNullValues();

// Lines 77-81: Static data config
RedisCacheConfiguration staticDataConfig = RedisCacheConfiguration.defaultCacheConfig()
        .entryTtl(Duration.ofHours(4))
        .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(serializer))
        .disableCachingNullValues();

// Lines 83-87: Stats config
RedisCacheConfiguration statsConfig = RedisCacheConfiguration.defaultCacheConfig()
        .entryTtl(Duration.ofMinutes(15))
        .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(serializer))
        .disableCachingNullValues();

// Lines 91-95: Activity config
RedisCacheConfiguration activityConfig = RedisCacheConfiguration.defaultCacheConfig()
        .entryTtl(Duration.ofMinutes(5))
        .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(serializer))
        .disableCachingNullValues();
```

### Recommendation
Extract a helper method to create cache configurations:

```java
private RedisCacheConfiguration createCacheConfig(Duration ttl, GenericJackson2JsonRedisSerializer serializer) {
    return RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(ttl)
            .serializeValuesWith(
                RedisSerializationContext.SerializationPair.fromSerializer(serializer))
            .disableCachingNullValues();
}
```

Then use it:
```java
RedisCacheConfiguration userDataConfig = createCacheConfig(Duration.ofMinutes(30), serializer);
RedisCacheConfiguration staticDataConfig = createCacheConfig(Duration.ofHours(4), serializer);
RedisCacheConfiguration statsConfig = createCacheConfig(Duration.ofMinutes(15), serializer);
RedisCacheConfiguration activityConfig = createCacheConfig(Duration.ofMinutes(5), serializer);
```

**Impact:** Reduces ~40 lines of duplicated code to ~4 lines + 1 helper method

---

## 2. Cache Eviction Patterns (HIGH PRIORITY)

### Locations
- `BlockedUserService.java` (lines 231-254)
- `CalendarService.java` (lines 244-276)
- `CacheService.java` (lines 592-624)
- `FriendRequestService.java` (lines 298-309, 340-343)
- `UserService.java` (lines 206-210)
- `ActivityCacheCleanupService.java` (lines 39-59)

### Problem
Repetitive pattern across 67 instances in 10 files:

```java
if (cacheManager.getCache("cacheName") != null) {
    cacheManager.getCache("cacheName").evict(key);
}
```

**Example 1:** `BlockedUserService.java` (lines 235-248)
```java
Cache recommendedFriendsCache = cacheManager.getCache("recommendedFriends");
if (recommendedFriendsCache != null) {
    recommendedFriendsCache.evict(userId);
}

Cache otherProfilesCache = cacheManager.getCache("otherProfiles");
if (otherProfilesCache != null) {
    otherProfilesCache.evict(userId);
}

Cache friendsListCache = cacheManager.getCache("friendsList");
if (friendsListCache != null) {
    friendsListCache.evict(userId);
}
```

**Example 2:** `FriendRequestService.java` (lines 298-309)
```java
if (cacheManager.getCache("incomingFetchFriendRequests") != null) {
    cacheManager.getCache("incomingFetchFriendRequests").evict(receiver.getId());
}
if (cacheManager.getCache("sentFetchFriendRequests") != null) {
    cacheManager.getCache("sentFetchFriendRequests").evict(sender.getId());
}
if (cacheManager.getCache("recommendedFriends") != null) {
    cacheManager.getCache("recommendedFriends").evict(sender.getId());
    cacheManager.getCache("recommendedFriends").evict(receiver.getId());
}
```

### Recommendation
Create a centralized `CacheEvictionHelper` or `CacheUtility` class:

```java
@Component
public class CacheEvictionHelper {
    private static final Logger logger = LoggerFactory.getLogger(CacheEvictionHelper.class);
    
    private final CacheManager cacheManager;
    
    @Autowired
    public CacheEvictionHelper(CacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }
    
    /**
     * Safely evicts a single cache entry
     */
    public void evictCache(String cacheName, Object key) {
        try {
            Cache cache = cacheManager.getCache(cacheName);
            if (cache != null) {
                cache.evict(key);
                logger.debug("Evicted cache '{}' for key: {}", cacheName, key);
            } else {
                logger.warn("Cache '{}' not found when attempting to evict key: {}", cacheName, key);
            }
        } catch (Exception e) {
            logger.error("Error evicting cache '{}' for key {}: {}", cacheName, key, e.getMessage());
            // Don't throw - this is a best-effort operation
        }
    }
    
    /**
     * Safely evicts multiple cache entries with the same key
     */
    public void evictCaches(Object key, String... cacheNames) {
        for (String cacheName : cacheNames) {
            evictCache(cacheName, key);
        }
    }
    
    /**
     * Safely evicts a single key from a cache for multiple user IDs
     */
    public void evictCacheForUsers(String cacheName, UUID... userIds) {
        for (UUID userId : userIds) {
            evictCache(cacheName, userId);
        }
    }
    
    /**
     * Safely clears an entire cache
     */
    public void clearCache(String cacheName) {
        try {
            Cache cache = cacheManager.getCache(cacheName);
            if (cache != null) {
                cache.clear();
                logger.debug("Cleared cache: {}", cacheName);
            } else {
                logger.warn("Cache '{}' not found when attempting to clear", cacheName);
            }
        } catch (Exception e) {
            logger.error("Error clearing cache '{}': {}", cacheName, e.getMessage());
            // Don't throw - this is a best-effort operation
        }
    }
    
    /**
     * Safely clears multiple caches
     */
    public void clearCaches(String... cacheNames) {
        for (String cacheName : cacheNames) {
            clearCache(cacheName);
        }
    }
}
```

**Usage Examples:**

Before:
```java
if (cacheManager.getCache("incomingFetchFriendRequests") != null) {
    cacheManager.getCache("incomingFetchFriendRequests").evict(receiver.getId());
}
if (cacheManager.getCache("sentFetchFriendRequests") != null) {
    cacheManager.getCache("sentFetchFriendRequests").evict(sender.getId());
}
```

After:
```java
cacheEvictionHelper.evictCache("incomingFetchFriendRequests", receiver.getId());
cacheEvictionHelper.evictCache("sentFetchFriendRequests", sender.getId());
```

Or even better:
```java
cacheEvictionHelper.evictCaches(userId, "recommendedFriends", "otherProfiles", "friendsList");
```

**Impact:** 
- Reduces 67 instances of 3-5 lines each to single-line calls
- Centralizes error handling and logging
- Estimated reduction: ~200-300 lines of duplicated code
- Improves maintainability and consistency

---

## 3. Activity Cache Cleanup List (MEDIUM PRIORITY)

### Locations
- `ActivityCacheCleanupService.java` (lines 39-51)
- `CacheService.java` (lines 597-617)

### Problem
The same list of activity cache names is defined in multiple places:

**ActivityCacheCleanupService.java:**
```java
String[] activityCaches = {
    "feedActivities",
    "fullActivityById",
    "ActivityById",
    "ActivityInviteById",
    "ActivitiesByOwnerId",
    "ActivitiesInvitedTo",
    "fullActivitiesInvitedTo",
    "fullActivitiesParticipatingIn",
    "calendarActivities",
    "allCalendarActivities",
    "filteredCalendarActivities"
};
```

**CacheService.java:**
```java
if (cacheManager.getCache("feedActivities") != null) {
    cacheManager.getCache("feedActivities").evict(userId);
}
if (cacheManager.getCache("fullActivityById") != null) {
    cacheManager.getCache("fullActivityById").clear();
}
// ... more individual checks
```

### Recommendation
Create a `CacheNames` constants class:

```java
public final class CacheNames {
    private CacheNames() {} // Prevent instantiation
    
    // User-related caches
    public static final String FRIENDS_BY_USER_ID = "friendsByUserId";
    public static final String RECOMMENDED_FRIENDS = "recommendedFriends";
    public static final String USER_INTERESTS = "userInterests";
    public static final String USER_SOCIAL_MEDIA = "userSocialMedia";
    public static final String USER_SOCIAL_MEDIA_BY_USER_ID = "userSocialMediaByUserId";
    
    // Friend request caches
    public static final String INCOMING_FRIEND_REQUESTS = "incomingFetchFriendRequests";
    public static final String SENT_FRIEND_REQUESTS = "sentFetchFriendRequests";
    public static final String FRIEND_REQUESTS = "friendRequests";
    public static final String FRIEND_REQUESTS_BY_USER_ID = "friendRequestsByUserId";
    
    // Activity type caches
    public static final String ACTIVITY_TYPES = "activityTypes";
    public static final String ACTIVITY_TYPES_BY_USER_ID = "activityTypesByUserId";
    
    // Location caches
    public static final String LOCATIONS = "locations";
    public static final String LOCATION_BY_ID = "locationById";
    
    // Stats caches
    public static final String USER_STATS = "userStats";
    public static final String USER_STATS_BY_ID = "userStatsById";
    
    // Activity caches
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
    
    // Blocked user caches
    public static final String BLOCKED_USERS = "blockedUsers";
    public static final String BLOCKED_USER_IDS = "blockedUserIds";
    public static final String IS_BLOCKED = "isBlocked";
    
    // Cache groups for bulk operations
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
    
    public static final String[] ALL_FRIEND_REQUEST_CACHES = {
        INCOMING_FRIEND_REQUESTS,
        SENT_FRIEND_REQUESTS,
        FRIEND_REQUESTS,
        FRIEND_REQUESTS_BY_USER_ID
    };
    
    public static final String[] ALL_USER_CACHES = {
        FRIENDS_BY_USER_ID,
        RECOMMENDED_FRIENDS,
        USER_INTERESTS,
        USER_SOCIAL_MEDIA,
        USER_SOCIAL_MEDIA_BY_USER_ID
    };
}
```

**Usage:**
```java
// In ActivityCacheCleanupService
for (String cacheName : CacheNames.ALL_ACTIVITY_CACHES) {
    if (cacheManager.getCache(cacheName) != null) {
        cacheManager.getCache(cacheName).clear();
        clearedCaches++;
    }
}

// In services with @Cacheable annotation
@Cacheable(value = CacheNames.FRIENDS_BY_USER_ID, key = "#userId")
public List<UserDTO> getFullFriendUsersByUserId(UUID userId) {
    // ...
}

// With the helper
cacheEvictionHelper.clearCaches(CacheNames.ALL_ACTIVITY_CACHES);
```

**Impact:**
- Ensures consistency across cache names
- Makes it easy to update cache names in one place
- Reduces typo-related bugs
- Provides clear documentation of all caches in the system

---

## 4. Controller Exception Handling (MEDIUM PRIORITY)

### Locations
All controllers exhibit this pattern:
- `ActivityController.java`
- `FriendRequestController.java`
- `ChatMessageController.java`
- `ReportController.java`
- `FeedbackSubmissionController.java`
- `AuthController.java`
- etc.

### Problem
Every endpoint has nearly identical try-catch blocks:

```java
@PostMapping
public ResponseEntity<FullFeedActivityDTO> createActivity(@RequestBody ActivityDTO activityDTO) {
    try {
        FullFeedActivityDTO response = activityService.createActivityWithSuggestions(activityDTO);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    } catch (IllegalArgumentException e) {
        logger.error("Invalid request for activity creation: " + e.getMessage());
        return new ResponseEntity<FullFeedActivityDTO>(HttpStatus.BAD_REQUEST);
    } catch (BaseNotFoundException e) {
        logger.error("Entity not found during activity creation: " + e.getMessage());
        return new ResponseEntity<FullFeedActivityDTO>(HttpStatus.NOT_FOUND);
    } catch (Exception e) {
        logger.error("Error creating activity: " + e.getMessage());
        return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
```

### Recommendation
Use `@ControllerAdvice` with `@ExceptionHandler` for global exception handling:

```java
@ControllerAdvice
@RestController
public class GlobalExceptionHandler {
    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    
    @ExceptionHandler(BaseNotFoundException.class)
    public ResponseEntity<?> handleNotFoundException(BaseNotFoundException e, WebRequest request) {
        logger.error("Entity not found: {} with ID: {}", e.entityType, e.getMessage());
        ErrorResponse errorResponse = new ErrorResponse(
            HttpStatus.NOT_FOUND.value(),
            e.getMessage(),
            request.getDescription(false)
        );
        return new ResponseEntity<>(errorResponse, HttpStatus.NOT_FOUND);
    }
    
    @ExceptionHandler(BasesNotFoundException.class)
    public ResponseEntity<?> handleMultipleNotFoundException(BasesNotFoundException e, WebRequest request) {
        logger.warn("Multiple entities not found: {}", e.entityType);
        ErrorResponse errorResponse = new ErrorResponse(
            HttpStatus.NOT_FOUND.value(),
            e.getMessage(),
            request.getDescription(false)
        );
        return new ResponseEntity<>(errorResponse, HttpStatus.NOT_FOUND);
    }
    
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<?> handleIllegalArgumentException(IllegalArgumentException e, WebRequest request) {
        logger.error("Invalid argument: {}", e.getMessage());
        ErrorResponse errorResponse = new ErrorResponse(
            HttpStatus.BAD_REQUEST.value(),
            e.getMessage(),
            request.getDescription(false)
        );
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }
    
    @ExceptionHandler(FieldAlreadyExistsException.class)
    public ResponseEntity<?> handleFieldAlreadyExistsException(FieldAlreadyExistsException e, WebRequest request) {
        logger.warn("Field already exists: {}", e.getMessage());
        ErrorResponse errorResponse = new ErrorResponse(
            HttpStatus.CONFLICT.value(),
            e.getMessage(),
            request.getDescription(false)
        );
        return new ResponseEntity<>(errorResponse, HttpStatus.CONFLICT);
    }
    
    @ExceptionHandler(BaseSaveException.class)
    public ResponseEntity<?> handleSaveException(BaseSaveException e, WebRequest request) {
        logger.error("Error saving entity: {}", e.getMessage());
        ErrorResponse errorResponse = new ErrorResponse(
            HttpStatus.BAD_REQUEST.value(),
            e.getMessage(),
            request.getDescription(false)
        );
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }
    
    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> handleGlobalException(Exception e, WebRequest request) {
        logger.error("Unexpected error: {}", e.getMessage(), e);
        ErrorResponse errorResponse = new ErrorResponse(
            HttpStatus.INTERNAL_SERVER_ERROR.value(),
            "An unexpected error occurred",
            request.getDescription(false)
        );
        return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}

// Simple error response DTO
public class ErrorResponse {
    private int status;
    private String message;
    private String path;
    private LocalDateTime timestamp;
    
    public ErrorResponse(int status, String message, String path) {
        this.status = status;
        this.message = message;
        this.path = path;
        this.timestamp = LocalDateTime.now();
    }
    
    // Getters and setters...
}
```

**After refactoring, controllers become much simpler:**

```java
@PostMapping
public ResponseEntity<FullFeedActivityDTO> createActivity(@RequestBody ActivityDTO activityDTO) {
    FullFeedActivityDTO response = activityService.createActivityWithSuggestions(activityDTO);
    return new ResponseEntity<>(response, HttpStatus.CREATED);
}
```

**Impact:**
- Removes try-catch blocks from ~50+ controller methods
- Centralizes exception handling logic
- Provides consistent error responses
- Estimated reduction: 500+ lines of duplicated exception handling code

**Note:** Some endpoints may need specific error handling logic. In those cases, handle specific exceptions locally and let others propagate to the global handler.

---

## 5. Cache Validation Pattern (MEDIUM PRIORITY)

### Location: `CacheService.java` (Lines 120-183)

### Problem
Repetitive if-containsKey-put pattern:

```java
if (clientCacheTimestamps.containsKey(CacheType.FRIENDS.getKey())) {
    response.put(CacheType.FRIENDS.getKey(), validateFriendsCache(user, clientCacheTimestamps.get(CacheType.FRIENDS.getKey())));
}

if (clientCacheTimestamps.containsKey(CacheType.EVENTS.getKey())) {
    response.put(CacheType.EVENTS.getKey(), validateEventsCache(user, clientCacheTimestamps.get(CacheType.EVENTS.getKey())));
}

// ... repeated 11 more times
```

### Recommendation
Use a more functional approach with a map of validators:

```java
@FunctionalInterface
private interface CacheValidator {
    CacheValidationResponseDTO validate(User user, String clientTimestamp);
}

@Override
public Map<String, CacheValidationResponseDTO> validateCache(
        UUID userId, Map<String, String> clientCacheTimestamps) {
    
    Map<String, CacheValidationResponseDTO> response = new HashMap<>();
    User user = userRepository.findById(userId).orElse(null);
    
    if (user == null) {
        logger.warn("Cache validation requested for non-existent user: {}", userId);
        return response;
    }
    
    // Handle null clientCacheTimestamps
    if (clientCacheTimestamps == null) {
        logger.warn("Client cache timestamps is null for user: {}", userId);
        return createInvalidationResponseForAllCaches();
    }
    
    // Map of cache types to their validators
    Map<String, CacheValidator> validators = new HashMap<>();
    validators.put(CacheType.FRIENDS.getKey(), this::validateFriendsCache);
    validators.put(CacheType.EVENTS.getKey(), this::validateEventsCache);
    validators.put(CacheType.ACTIVITY_TYPES.getKey(), this::validateActivityTypesCache);
    validators.put(CacheType.PROFILE_PICTURE.getKey(), this::validateProfilePictureCache);
    validators.put(CacheType.OTHER_PROFILES.getKey(), this::validateOtherProfilesCache);
    validators.put(CacheType.RECOMMENDED_FRIENDS.getKey(), this::validateRecommendedFriendsCache);
    validators.put(CacheType.FRIEND_REQUESTS.getKey(), this::validateFriendRequestsCache);
    validators.put(CacheType.SENT_FRIEND_REQUESTS.getKey(), this::validateSentFriendRequestsCache);
    validators.put(CacheType.RECENTLY_SPAWNED.getKey(), this::validateRecentlySpawnedCache);
    validators.put(CacheType.PROFILE_STATS.getKey(), this::validateProfileStatsCache);
    validators.put(CacheType.PROFILE_INTERESTS.getKey(), this::validateProfileInterestsCache);
    validators.put(CacheType.PROFILE_SOCIAL_MEDIA.getKey(), this::validateProfileSocialMediaCache);
    validators.put(CacheType.PROFILE_EVENTS.getKey(), this::validateProfileEventsCache);
    
    // Process all cache types that exist in the client request
    clientCacheTimestamps.forEach((cacheKey, timestamp) -> {
        CacheValidator validator = validators.get(cacheKey);
        if (validator != null) {
            response.put(cacheKey, validator.validate(user, timestamp));
        } else {
            logger.warn("Unknown cache type requested: {}", cacheKey);
        }
    });
    
    return response;
}

private Map<String, CacheValidationResponseDTO> createInvalidationResponseForAllCaches() {
    Map<String, CacheValidationResponseDTO> response = new HashMap<>();
    for (CacheType cacheType : CacheType.values()) {
        response.put(cacheType.getKey(), new CacheValidationResponseDTO(true, null));
    }
    return response;
}
```

**Impact:**
- Reduces ~60 lines of repetitive if-statements to a clean map-based approach
- Makes it easier to add new cache types
- More functional and maintainable

---

## 6. Mapper Stream Operations (LOW PRIORITY)

### Locations
- `ActivityMapper.java`
- `ChatMessageMapper.java`
- `UserMapper.java`
- `ActivityTypeMapper.java`
- All other mapper classes

### Problem
Similar `toDTOList` and `toEntityList` patterns across all mappers:

```java
public static List<ActivityDTO> toDTOList(
        List<Activity> entities,
        Map<UUID, UUID> creatorUserIdMap,
        Map<UUID, List<UUID>> participantUserIdsMap,
        Map<UUID, List<UUID>> invitedUserIdsMap,
        Map<UUID, List<UUID>> chatMessageIdsMap,
        Map<UUID, Boolean> isExpiredMap
) {
    return entities.stream()
            .map(entity -> toDTO(
                    entity,
                    creatorUserIdMap.get(entity.getId()),
                    participantUserIdsMap.getOrDefault(entity.getId(), List.of()),
                    invitedUserIdsMap.getOrDefault(entity.getId(), List.of()),
                    chatMessageIdsMap.getOrDefault(entity.getId(), List.of()),
                    isExpiredMap.getOrDefault(entity.getId(), false)
            ))
            .collect(Collectors.toList());
}
```

### Recommendation
This is a lower priority issue because:
1. The complexity varies significantly between mappers (ActivityMapper needs many parameters, others need few)
2. Creating a generic solution might reduce readability
3. The current approach is explicit and type-safe

However, if you want to reduce duplication, consider:

```java
// Base utility class
public abstract class BaseMapper<E, D> {
    protected List<D> toDTOList(List<E> entities, Function<E, D> mapper) {
        return entities.stream()
                .map(mapper)
                .collect(Collectors.toList());
    }
    
    protected List<E> toEntityList(List<D> dtos, Function<D, E> mapper) {
        return dtos.stream()
                .map(mapper)
                .collect(Collectors.toList());
    }
}

// Usage in UserMapper
public final class UserMapper extends BaseMapper<User, BaseUserDTO> {
    public static List<BaseUserDTO> toDTOList(List<User> users) {
        return new UserMapper().toDTOList(users, UserMapper::toDTO);
    }
}
```

**Impact:** Minor - reduces boilerplate but may impact readability. Consider on a case-by-case basis.

---

## Implementation Priority

### High Priority (Should implement immediately)
1. **Cache Eviction Helper** - Affects 67 instances across 10 files
2. **Cache Configuration Helper** in RedisCacheConfig
3. **Cache Names Constants** - Prevents bugs and improves maintainability

### Medium Priority (Should implement soon)
4. **Global Exception Handler** - Significantly simplifies controllers
5. **Cache Validation Refactoring** - Improves CacheService maintainability

### Low Priority (Nice to have)
6. **Mapper Utility Base Class** - Only if team agrees it improves readability

---

## Estimated Impact

### Code Reduction
- **High Priority items:** ~400-500 lines of duplicated code removed
- **Medium Priority items:** ~600-700 lines of duplicated code removed
- **Total:** ~1000-1200 lines of duplicated code eliminated

### Maintainability Benefits
- Centralized cache management logic
- Consistent error handling across all endpoints
- Single source of truth for cache names
- Easier to add new features without copy-pasting code
- Reduced risk of bugs from inconsistent implementations

### Testing Benefits
- Fewer places to test the same logic
- Cache helper can be unit tested independently
- Exception handler can be integration tested once

---

## Implementation Steps

### Phase 1: Cache Infrastructure (Week 1)
1. Create `CacheNames` constants class
2. Create `CacheEvictionHelper` utility class
3. Refactor `RedisCacheConfig` to use helper method
4. Update all `@Cacheable` annotations to use `CacheNames` constants

### Phase 2: Update Services (Week 2)
5. Inject `CacheEvictionHelper` into all services that use cache eviction
6. Replace all manual cache eviction code with helper methods
7. Test each service after refactoring

### Phase 3: Exception Handling (Week 3)
8. Create `GlobalExceptionHandler` with `@ControllerAdvice`
9. Create `ErrorResponse` DTO
10. Gradually remove try-catch blocks from controllers (one controller at a time)
11. Test each controller after refactoring

### Phase 4: Cache Validation (Week 4)
12. Refactor `CacheService.validateCache()` to use functional approach
13. Test cache validation thoroughly

### Phase 5: Polish (Optional)
14. Consider mapper refactoring if team agrees
15. Document new utility classes
16. Update onboarding documentation

---

## Testing Strategy

### Unit Tests Required
- `CacheEvictionHelper` - test all methods with mocked CacheManager
- `GlobalExceptionHandler` - test each exception type
- Refactored `RedisCacheConfig` helper methods

### Integration Tests Required
- Cache eviction in various service scenarios
- Exception handling end-to-end
- Cache validation with real Redis (in test environment)

### Regression Testing
- Run full test suite after each phase
- Manually test critical user flows
- Monitor production logs for unexpected errors

---

## Risks and Mitigation

### Risk 1: Breaking Changes
**Mitigation:** 
- Implement incrementally (one service/controller at a time)
- Keep old code temporarily with `@Deprecated` annotation
- Comprehensive testing after each change

### Risk 2: Performance Impact
**Mitigation:**
- Helper methods should have minimal overhead (simple delegations)
- Benchmark critical paths if needed
- Monitor production performance metrics

### Risk 3: Team Adoption
**Mitigation:**
- Document new patterns clearly
- Code review sessions to explain changes
- Update contribution guidelines

---

## Conclusion

The codebase has significant opportunities for DRY refactoring, particularly in cache management and exception handling. Implementing these recommendations will:

1. **Reduce code volume by ~1000-1200 lines**
2. **Improve maintainability** through centralized logic
3. **Reduce bugs** from inconsistent implementations
4. **Make onboarding easier** with clearer patterns

The highest priority items (cache eviction helper and cache names constants) can be implemented quickly and will have immediate benefits. The medium priority items (exception handling) require more careful planning but will significantly improve code quality.

I recommend starting with Phase 1 (Cache Infrastructure) as it has the highest impact-to-effort ratio.


