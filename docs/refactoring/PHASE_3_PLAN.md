# Spring Modulith Refactoring - Phase 3 Plan

**Phase:** Shared Data Resolution  
**Status:** 🔄 In Progress  
**Started:** December 23, 2025  
**Target Completion:** End of Week 5

---

## Overview

Phase 3 focuses on establishing clear data ownership boundaries and creating public APIs to replace direct cross-module repository access. This ensures that each module's internal data is only accessed through well-defined interfaces.

---

## Identified Issues

### Cross-Module Repository Access Violations

**Repository:** `IActivityUserRepository`  
**Location:** `activity/internal/repositories/IActivityUserRepository.java`  
**Owner:** Activity Module

| File | Module | Violation | Required Action |
|------|--------|-----------|-----------------|
| `ActivityService.java` | Activity | ✅ No | Owner - keep as-is |
| `CalendarService.java` | Activity | ✅ No | Owner - keep as-is |
| `UserService.java` | User | ❌ Yes | Use `ActivityPublicApi` |
| `UserSearchService.java` | User | ❌ Yes | Use `ActivityPublicApi` |
| `UserStatsService.java` | User | ❌ Yes | Use `ActivityPublicApi` |
| `ChatMessageService.java` | Chat | ❌ Yes | Use `ActivityPublicApi` |

### Events with Internal Type References

| Event | Issue | Resolution |
|-------|-------|------------|
| `NewCommentNotificationEvent.java` | Imports `IActivityUserRepository` | Use DTO or UUID list |
| `ActivityUpdateNotificationEvent.java` | Imports `IActivityUserRepository` | Use DTO or UUID list |

---

## Data Ownership Matrix

| Entity | Module | Reason | External Access Pattern |
|--------|--------|--------|------------------------|
| `User` | User | Core user identity | `UserPublicApi` |
| `Activity` | Activity | Core activity data | `ActivityPublicApi` |
| `ActivityType` | Activity | Activity categorization | `ActivityPublicApi` |
| `ActivityUser` | Activity | Participation relationship | `ActivityPublicApi` |
| `Location` | Activity | Activity context | Embedded in Activity |
| `ChatMessage` | Chat | Message data | Events (already done) |
| `ChatMessageLike` | Chat | Engagement data | Internal only |
| `Friendship` | Social | Social relationship | `SocialPublicApi` (future) |
| `FriendRequest` | Social | Social interaction | `SocialPublicApi` (future) |
| `DeviceToken` | Notification | Push notification | Internal only |
| `EmailVerification` | Auth | Auth flow | Internal only |
| `UserIdExternalIdMap` | Auth | OAuth mapping | Internal only |
| `ReportedContent` | Analytics | Reporting | Internal only |
| `FeedbackSubmission` | Analytics | Feedback | Internal only |
| `ShareLink` | Analytics | Share tracking | Public API |
| `BetaAccessSignUp` | Analytics | Beta access | Internal only |
| `UserInterest` | User | User preferences | `UserPublicApi` |
| `UserSocialMedia` | User | Social links | `UserPublicApi` |
| `BlockedUser` | User | Block list | `UserPublicApi` |

---

## Implementation Tasks

### Task 1: Create ActivityPublicApi Interface

**Priority:** High  
**Location:** `activity/api/ActivityPublicApi.java`

```java
package com.danielagapov.spawn.activity.api;

import java.util.List;
import java.util.UUID;

/**
 * Public API for Activity module - exposes read-only operations
 * for other modules to access activity data without direct repository access.
 */
public interface ActivityPublicApi {
    
    /**
     * Get all activity IDs a user is participating in
     */
    List<UUID> getActivityIdsByUserId(UUID userId);
    
    /**
     * Get all participant user IDs for an activity
     */
    List<UUID> getParticipantIdsByActivityId(UUID activityId);
    
    /**
     * Check if a user is a participant in an activity
     */
    boolean isUserParticipant(UUID activityId, UUID userId);
    
    /**
     * Get participant count for an activity
     */
    int getParticipantCount(UUID activityId);
    
    /**
     * Get activities by IDs (for batch lookups)
     */
    List<UUID> getActivityIdsByUserIds(List<UUID> userIds);
}
```

### Task 2: Create ActivityPublicApiImpl

**Location:** `activity/internal/services/ActivityPublicApiImpl.java`

```java
package com.danielagapov.spawn.activity.internal.services;

import com.danielagapov.spawn.activity.api.ActivityPublicApi;
import com.danielagapov.spawn.activity.internal.repositories.IActivityUserRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class ActivityPublicApiImpl implements ActivityPublicApi {
    
    private final IActivityUserRepository activityUserRepository;
    
    public ActivityPublicApiImpl(IActivityUserRepository activityUserRepository) {
        this.activityUserRepository = activityUserRepository;
    }
    
    @Override
    public List<UUID> getActivityIdsByUserId(UUID userId) {
        return activityUserRepository.findActivityIdsByUserId(userId);
    }
    
    @Override
    public List<UUID> getParticipantIdsByActivityId(UUID activityId) {
        return activityUserRepository.findUserIdsByActivityId(activityId);
    }
    
    @Override
    public boolean isUserParticipant(UUID activityId, UUID userId) {
        return activityUserRepository.existsByActivityIdAndUserId(activityId, userId);
    }
    
    @Override
    public int getParticipantCount(UUID activityId) {
        return activityUserRepository.countByActivityId(activityId);
    }
    
    @Override
    public List<UUID> getActivityIdsByUserIds(List<UUID> userIds) {
        // Implementation depends on repository methods available
        return activityUserRepository.findActivityIdsByUserIdIn(userIds);
    }
}
```

### Task 3: Update UserService

**Location:** `user/internal/services/UserService.java`

**Change:**
```java
// BEFORE
import com.danielagapov.spawn.activity.internal.repositories.IActivityUserRepository;

// AFTER
import com.danielagapov.spawn.activity.api.ActivityPublicApi;
```

**Update dependency:**
```java
// BEFORE
private final IActivityUserRepository activityUserRepository;

// AFTER
private final ActivityPublicApi activityApi;
```

**Update usage:**
```java
// BEFORE
activityUserRepository.findActivityIdsByUserId(userId);

// AFTER
activityApi.getActivityIdsByUserId(userId);
```

### Task 4: Update UserSearchService

**Location:** `user/internal/services/UserSearchService.java`

Same pattern as Task 3 - replace `IActivityUserRepository` with `ActivityPublicApi`.

### Task 5: Update UserStatsService

**Location:** `user/internal/services/UserStatsService.java`

Same pattern as Task 3 - replace `IActivityUserRepository` with `ActivityPublicApi`.

### Task 6: Update ChatMessageService

**Location:** `chat/internal/services/ChatMessageService.java`

Same pattern as Task 3 - replace `IActivityUserRepository` with `ActivityPublicApi`.

### Task 7: Update Notification Events

**Files:**
- `shared/events/NewCommentNotificationEvent.java`
- `shared/events/ActivityUpdateNotificationEvent.java`

**Change:** Replace `IActivityUserRepository` parameter with `List<UUID> participantIds`

```java
// BEFORE
public record NewCommentNotificationEvent(
    UUID activityId,
    IActivityUserRepository activityUserRepository,
    // ...
) {}

// AFTER
public record NewCommentNotificationEvent(
    UUID activityId,
    List<UUID> participantIds,
    // ...
) {}
```

### Task 8: Verify Repository Methods Exist

Ensure `IActivityUserRepository` has all needed methods for the public API:

```java
public interface IActivityUserRepository extends JpaRepository<ActivityUser, UUID> {
    
    // Needed for ActivityPublicApi
    List<UUID> findActivityIdsByUserId(UUID userId);
    List<UUID> findUserIdsByActivityId(UUID activityId);
    boolean existsByActivityIdAndUserId(UUID activityId, UUID userId);
    int countByActivityId(UUID activityId);
    List<UUID> findActivityIdsByUserIdIn(List<UUID> userIds);
}
```

---

## Verification Steps

### After Each Task

```bash
# Compile to verify no errors
JAVA_HOME=$(/usr/libexec/java_home -v 17) ./mvnw clean compile -DskipTests

# Verify no direct repository imports from other modules
grep -r "import com.danielagapov.spawn.activity.internal.repositories" \
  src/main/java/com/danielagapov/spawn/user \
  src/main/java/com/danielagapov/spawn/chat \
  src/main/java/com/danielagapov/spawn/social

# Should only find imports in activity module after Phase 3
```

### Final Verification

```bash
# Full build
JAVA_HOME=$(/usr/libexec/java_home -v 17) ./mvnw clean compile

# Run tests
JAVA_HOME=$(/usr/libexec/java_home -v 17) ./mvnw test
```

---

## Success Criteria

- [ ] `ActivityPublicApi` interface created
- [ ] `ActivityPublicApiImpl` implementation created
- [ ] `UserService` updated to use `ActivityPublicApi`
- [ ] `UserSearchService` updated to use `ActivityPublicApi`
- [ ] `UserStatsService` updated to use `ActivityPublicApi`
- [ ] `ChatMessageService` updated to use `ActivityPublicApi`
- [ ] Notification events updated to use DTOs/UUIDs
- [ ] No cross-module repository imports remain
- [ ] Build successful
- [ ] Tests pass (excluding pre-existing failures)

---

## Estimated Effort

| Task | Time Estimate |
|------|---------------|
| Task 1: Create ActivityPublicApi | 15 min |
| Task 2: Create ActivityPublicApiImpl | 30 min |
| Task 3: Update UserService | 20 min |
| Task 4: Update UserSearchService | 15 min |
| Task 5: Update UserStatsService | 15 min |
| Task 6: Update ChatMessageService | 15 min |
| Task 7: Update Notification Events | 20 min |
| Task 8: Verify Repository Methods | 15 min |
| Testing & Verification | 30 min |
| **Total** | **~3 hours** |

---

## Next Phase Preview

After Phase 3 completion, **Phase 4** will add Spring Modulith dependencies:

1. Update `pom.xml` with Spring Modulith BOM
2. Create `package-info.java` for each module
3. Add `@Modulith` annotation to main application
4. Configure module boundary enforcement

---

## References

- [SPRING_MODULITH_REFACTORING_PLAN.md](./SPRING_MODULITH_REFACTORING_PLAN.md) - Full plan
- [PHASE_2_COMPLETE.md](./PHASE_2_COMPLETE.md) - Previous phase
- [Spring Modulith Docs](https://spring.io/projects/spring-modulith)

---

**Document Version:** 1.0  
**Created:** December 23, 2025  
**Author:** Modulith Refactoring Team

