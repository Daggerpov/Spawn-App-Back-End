# JSON Serialization Fix - Global Cache Error Handler

## Issue Overview

After switching Redis cache serialization from JDK to JSON format (for RAM optimization), the application experienced multiple JSON deserialization errors caused by incompatible cached data and overly strict polymorphic type handling.

## Errors Resolved

### 1. Friend Request Errors
```
Could not read JSON:Unexpected token (END_ARRAY), expected VALUE_STRING: 
need String, Number of Boolean value that contains type id (for subtype of java.lang.Object)
```
- Affected: `getIncomingFriendRequestsByUserId`, `getSentFriendRequestsByUserId`

### 2. Activity Type Errors
```
Could not read JSON:Unexpected token (START_OBJECT), expected VALUE_STRING: 
need String, Number of Boolean value that contains type id (for subtype of java.lang.Object)
```
- Affected: `getOwnedActivityTypesForUser`, `CacheService.getLatestActivityTypeUpdate`

### 3. Phone Number Validation Errors
```
Invalid phone number format
```
- Occurred during user details updates, likely from corrupted cached user data

## Root Causes

1. **Polymorphic Type Handling Too Strict**: The `ObjectMapper` in `RedisCacheConfig` was using `DefaultTyping.NON_FINAL` which added type information to all objects including collections, causing issues with empty arrays and simple objects.

2. **Old Cached Data Incompatible**: Cache entries created with the old JDK serialization format couldn't be deserialized with the new JSON serializer.

3. **No Auto-Recovery**: When deserialization failed, the application would error out rather than clearing the corrupted cache and retrying.

## Solution: Global Cache Error Handler

Instead of adding domain-specific error handling in controllers and services, we implemented a **global cache error handler** at the configuration level. This is the proper Spring Cache pattern for handling cache errors.

### Changes Made

#### 1. Updated RedisCacheConfig.java - Polymorphic Type Handling

**Change**: Modified polymorphic type handling to be less aggressive

```java
// Before: Too aggressive
objectMapper.activateDefaultTyping(
    validator,
    ObjectMapper.DefaultTyping.NON_FINAL,  // Adds type info to all non-final classes
    JsonTypeInfo.As.PROPERTY
);

// After: More lenient
objectMapper.activateDefaultTyping(
    validator,
    ObjectMapper.DefaultTyping.OBJECT_AND_NON_CONCRETE,  // Only abstract types
    JsonTypeInfo.As.PROPERTY
);
```

**Why This Matters**:
- `NON_FINAL`: Adds type info to ArrayList, HashMap, concrete DTOs → breaks deserialization
- `OBJECT_AND_NON_CONCRETE`: Only adds type info to Object, Collection, List (interfaces) → works correctly

#### 2. Added Global CacheErrorHandler

**Implementation**: RedisCacheConfig now implements `CachingConfigurer` and provides a custom `CacheErrorHandler` bean.

```java
@Configuration
@EnableCaching
public class RedisCacheConfig implements CachingConfigurer {
    
    @Bean
    @Override
    public CacheErrorHandler errorHandler() {
        return new CacheErrorHandler() {
            @Override
            public void handleCacheGetError(RuntimeException exception, Cache cache, Object key) {
                // Check if this is a JSON deserialization error
                if (isJsonDeserializationError(exception)) {
                    logger.warn("Cache corruption detected in '{}' for key '{}'. Evicting...", 
                               cache.getName(), key);
                    cache.evict(key);  // Auto-evict corrupted entry
                }
                // Don't rethrow - allow application to fetch from database
            }
            // ... other error handlers
        };
    }
}
```

**How It Works**:
1. **Intercepts ALL cache errors globally** - no domain-specific logic needed
2. **Detects JSON deserialization errors** by checking exception cause chain
3. **Automatically evicts corrupted cache entries** 
4. **Doesn't rethrow exception** - allows Spring to retry via database
5. **Spring's @Cacheable automatically repopulates** cache with correct format

**Benefits**:
- ✅ Single source of truth for cache error handling
- ✅ No controller or service logic needed
- ✅ Works for ALL caches automatically (friend requests, activity types, etc.)
- ✅ Follows Spring Cache best practices
- ✅ Easy to maintain and test

#### 3. Improved Phone Number Validation

**File**: `AuthService.java`

Made phone number validation more lenient when the value hasn't actually changed:

```java
// Only validate and update if phone number is being changed
String currentPhone = user.getOptionalPhoneNumber().orElse("");
if (dto.getPhoneNumber() != null && !dto.getPhoneNumber().equals(currentPhone)) {
    String cleanedPhoneNumber = PhoneNumberValidator.cleanPhoneNumber(dto.getPhoneNumber());
    if (cleanedPhoneNumber == null || cleanedPhoneNumber.trim().isEmpty()) {
        logger.warn("Invalid phone number format for user...");
        throw new IllegalArgumentException("Invalid phone number format");
    }
    // ... rest of validation
}
```

This prevents errors when clients send unchanged (but possibly corrupted) phone numbers.

## How It Works - Example Flow

### Scenario: User requests friend list

1. **Client calls** `GET /api/v1/friend-requests/incoming/{userId}`
2. **Spring Cache tries to fetch** from Redis cache
3. **Redis returns corrupted data** (old JDK serialization format)
4. **JSON deserialization fails** → Exception thrown
5. **Global CacheErrorHandler intercepts** the exception
6. **Checks if it's a JSON error** → Yes, it is
7. **Automatically evicts** the corrupted cache entry
8. **Returns control to Spring** without rethrowing exception
9. **Spring retry mechanism** calls the actual service method
10. **Service method queries database** → Gets fresh data
11. **Spring's @Cacheable** automatically caches the result with new JSON format
12. **Client receives correct data** ✅

### Next Request
- Cache now contains correctly formatted JSON
- No errors occur
- Normal cache hit performance

## Deployment

### Prerequisites
- Ensure you're on the `bug-fixes` branch

### Steps

1. **Deploy the updated code**
   ```bash
   git checkout bug-fixes
   git pull
   # Build and deploy as usual
   ```

2. **Monitor application logs** after deployment
   - Look for: `"Cache corruption detected in '...' for key '...'. Evicting..."`
   - Look for: `"Successfully evicted corrupted cache entry from '...' for key: ..."`
   - Should see automatic recovery happening

3. **Optional: Proactively clear all caches**
   ```bash
   ./scripts/clear-corrupted-caches.sh
   ```
   This is optional - the error handler will fix issues as they're encountered.

4. **Verify**
   - Test friend request endpoints
   - Test activity type endpoints
   - Test user details update
   - Confirm no recurring errors in logs

## Expected Behavior

### First Deployment (with old cached data)
- ⚠️ Cache GET errors detected for corrupted entries
- ✅ Automatically evicted and logged
- ✅ Requests succeed after retry
- ✅ New cache entries stored in correct JSON format

### After First Run (all caches refreshed)
- ✅ No errors
- ✅ Normal operation
- ✅ Full cache performance benefits maintained

## Files Modified

### Configuration
- `src/main/java/com/danielagapov/spawn/Config/RedisCacheConfig.java`
  - Changed polymorphic type handling
  - Added global `CacheErrorHandler`
  - Added `isJsonDeserializationError()` helper

### Service Layer
- `src/main/java/com/danielagapov/spawn/Services/Auth/AuthService.java`
  - Improved phone number validation logic

### Scripts
- `scripts/clear-corrupted-caches.sh` (optional manual cache clearing)

### Documentation
- `docs/fixes/JSON_SERIALIZATION_FIX.md` (this document)

## Technical Details

### Why Global Error Handler vs Controller Logic?

| Approach | Pros | Cons |
|----------|------|------|
| **Controller Logic** ❌ | - Direct control | - Duplicated across controllers<br>- Domain-specific<br>- Violates separation of concerns<br>- Hard to maintain |
| **Global Handler** ✅ | - Single source of truth<br>- Works for ALL caches<br>- Follows Spring patterns<br>- Clean separation of concerns | - None |

### CacheErrorHandler Methods

```java
public interface CacheErrorHandler {
    void handleCacheGetError(RuntimeException e, Cache cache, Object key);
    void handleCachePutError(RuntimeException e, Cache cache, Object key, Object value);
    void handleCacheEvictError(RuntimeException e, Cache cache, Object key);
    void handleCacheClearError(RuntimeException e, Cache cache);
}
```

Our implementation:
- **handleCacheGetError**: Detects and evicts corrupted entries, doesn't rethrow
- **handleCachePutError**: Logs error, allows app to continue without caching
- **handleCacheEvictError**: Logs error, not critical
- **handleCacheClearError**: Logs error, not critical

### Auto-Recovery Flow

```
Cache GET → Deserialization Error
    ↓
Error Handler Intercepts
    ↓
Check if JSON Error? → Yes
    ↓
Evict Corrupted Entry
    ↓
Don't Rethrow Exception
    ↓
Spring Retries via Database
    ↓
@Cacheable Repopulates Cache
    ↓
Success ✅
```

## Testing Checklist

- [ ] Deploy to production
- [ ] Monitor logs for automatic cache eviction messages
- [ ] Test GET `/api/v1/friend-requests/incoming/{userId}`
- [ ] Test GET `/api/v1/friend-requests/sent/{userId}`
- [ ] Test GET `/api/v1/users/{userId}/activity-types`
- [ ] Test POST `/api/v1/auth/user/details`
- [ ] Verify no recurring JSON errors
- [ ] Confirm cache entries are in JSON format (check Redis directly)
- [ ] Validate RAM optimization benefits maintained

## Related Documentation

- `docs/fixes/CACHE_ENCODING_FIX.md` - Original UTF-8 encoding fix
- `docs/optimization/RAM_OPTIMIZATION_SUMMARY.md` - Original RAM optimization
- `scripts/clear-activity-type-cache.sh` - Activity type specific cache clear

## Status

✅ **Implementation Complete**

The fix uses Spring Cache's proper error handling mechanism with a global `CacheErrorHandler`. This automatically detects and recovers from cache corruption for ALL cache types without requiring domain-specific logic in controllers or services.

---

**Date**: October 31, 2025  
**Author**: Development Team  
**Related Issues**: JSON serialization cache corruption after RAM optimization  
**Branch**: bug-fixes  
**Approach**: Global cache error handler (Spring Cache best practice)

