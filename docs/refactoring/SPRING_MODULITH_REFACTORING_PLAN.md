# Spring Modulith Refactoring Plan

**Project:** Spawn App Back-End  
**Goal:** Refactor monolith to Spring Modulith with validated module boundaries  
**Timeline:** 6-8 weeks (Started Dec 2025)  
**Current Status:** ✅ Phase 1 Complete | ✅ Phase 2 Complete | 🔄 Phase 3 In Progress  
**Next Step:** Microservices extraction (see [MICROSERVICES_IMPLEMENTATION_PLAN.md](../microservices/MICROSERVICES_IMPLEMENTATION_PLAN.md))

---

## Table of Contents

- [Current Progress](#current-progress)
- [Overview](#overview)
- [Target Architecture](#target-architecture)
- [Phase 2: Fix Circular Dependencies](#phase-2-fix-circular-dependencies-week-3-4) ✅ **COMPLETE**
- [Phase 3: Shared Data Resolution](#phase-3-shared-data-resolution-week-5) ⬅️ **YOU ARE HERE**
- [Phase 4: Add Spring Modulith](#phase-4-add-spring-modulith-dependencies-week-5)
- [Phase 5: Module Boundary Testing](#phase-5-module-boundary-testing-week-6-7)
- [Phase 6: Documentation & Validation](#phase-6-documentation--validation-week-8)
- [Appendix](#appendix)

---

## Current Progress

### ✅ Phase 1: Package Restructuring (COMPLETE - Dec 8, 2025)

**Accomplishments:**
- ✅ Created 8 module directories with `api/` and `internal/` separation
- ✅ Moved all 266 Java files to new module structure
- ✅ Updated all package declarations
- ✅ Fixed ~1,500+ import statements
- ✅ Upgraded Lombok to 1.18.34
- ✅ **Compilation successful** - project builds cleanly

**Modules Created:**
- `auth/` - Authentication and OAuth (17 files)
- `activity/` - Activities, types, locations (48 files)
- `chat/` - Messaging and chat (10 files)
- `user/` - User management, profiles, search (72 files)
- `social/` - Friend requests, friendships, blocking (8 files)
- `notification/` - Push notifications (10 files)
- `media/` - S3 file storage (2 files)
- `analytics/` - Reporting, feedback, share links (18 files)
- `shared/` - Events, exceptions, config, utilities (81 files)

**See:** [PHASE_1_COMPLETE.md](./PHASE_1_COMPLETE.md) for detailed summary

### ✅ Phase 2: Fix Circular Dependencies (COMPLETE - Dec 23, 2025)

**Accomplishments:**
- ✅ Created event contracts in `shared/events/` for Chat and ActivityType modules
- ✅ Fixed Activity ↔ Chat circular dependency with `ChatQueryService` and `ChatEventListener`
- ✅ Fixed User ↔ ActivityType circular dependency with `ActivityTypeEventListener`
- ✅ Removed all `@Lazy` annotations (4 total removed)
- ✅ **Build successful** - no circular dependency warnings

**New Files Created:**
- `shared/events/ChatEvents.java` - Chat query/response events
- `shared/events/UserActivityTypeEvents.java` - User creation events
- `activity/internal/services/ChatQueryService.java` - Event-driven chat queries
- `chat/internal/services/ChatEventListener.java` - Chat event handler
- `activity/internal/services/ActivityTypeEventListener.java` - ActivityType event handler

### 🔄 Next: Phase 3 (Current Focus)

Resolve shared data ownership and create public APIs for cross-module access.

---

## Overview

### Why This Refactoring?

Spring Modulith provides a structured approach to validate service boundaries BEFORE the complexity of distributed systems. This refactoring will:

1. **Identify and fix circular dependencies** currently hidden by `@Lazy` annotations
2. **Enforce bounded contexts** through compile-time module boundaries
3. **Validate event-driven architecture** that will become inter-service communication
4. **Reduce microservices migration risk** by proving boundaries work in-process first

### Success Criteria

- [x] Zero circular dependencies between modules ✅ *(Complete - Phase 2)*
- [x] Zero `@Lazy` annotations in cross-module dependencies ✅ *(Complete - Phase 2)*
- [ ] All inter-module communication via events or public APIs *(In Progress - Phase 3)*
- [ ] No direct cross-module repository access *(In Progress - Phase 3)*
- [ ] Module boundary tests passing *(Phase 5)*
- [ ] No performance regression *(Phase 5)*
- [ ] Clear ownership of all database entities *(In Progress - Phase 3)*
- [ ] Documentation of module contracts *(Phase 6)*

---

## Critical Issues to Fix

### Identified in Phase 1 - Must Be Resolved

#### 1. Circular Dependency: Activity ↔ Chat ⚠️

**Location:** `activity/internal/services/ActivityService.java` (still has `@Lazy`)

```java
@Autowired
@Lazy // avoid circular dependency problems with ChatMessageService
public ActivityService(..., IChatMessageService chatMessageService, ...) {
    this.chatMessageService = chatMessageService;
}
```

**Impact:** ChatMessageService depends on ActivityService, creating a cycle.  
**Must fix in Phase 2** - See solution below.

#### 2. Circular Dependency: User ↔ ActivityType ⚠️

**Location:** `user/internal/services/UserService.java` (still has `@Lazy`)

```java
@Autowired
@Lazy // Avoid circular dependency issues with ActivityTypeService
public UserService(..., IActivityTypeService activityTypeService, ...) {
    this.activityTypeService = activityTypeService;
}
```

**Impact:** Creates tight coupling between User and Activity domains.  
**Must fix in Phase 2** - See solution below.

#### 3. Shared Repository: ActivityUser ⚠️

**Used by:**
- `activity/internal/services/ActivityService.java`
- `user/internal/services/UserService.java`

**Problem:** No clear ownership - which module owns the Activity-User participation relationship?  
**Must resolve in Phase 3** - Assign to Activity module.

---

## Target Architecture

### Module Boundaries

```
com.danielagapov.spawn/
├── auth/                           # Authentication & Authorization Module
│   ├── api/                        # Public API
│   │   ├── AuthController.java
│   │   └── dto/                    # Public DTOs
│   ├── internal/                   # Private implementation
│   │   ├── services/
│   │   │   ├── AuthService.java
│   │   │   ├── OAuthService.java
│   │   │   └── EmailVerificationService.java
│   │   ├── repositories/
│   │   └── domain/                 # Domain models
│   └── AuthModule.java             # Module config
│
├── activity/                       # Activity Management Module
│   ├── api/
│   │   ├── ActivityController.java
│   │   ├── ActivityTypeController.java
│   │   └── dto/
│   ├── internal/
│   │   ├── services/
│   │   │   ├── ActivityService.java
│   │   │   ├── ActivityTypeService.java
│   │   │   ├── LocationService.java
│   │   │   └── ActivityExpirationService.java
│   │   ├── repositories/
│   │   └── domain/
│   └── ActivityModule.java
│
├── chat/                           # Chat & Messaging Module
│   ├── api/
│   │   ├── ChatMessageController.java
│   │   └── dto/
│   ├── internal/
│   │   ├── services/
│   │   │   └── ChatMessageService.java
│   │   ├── repositories/
│   │   └── domain/
│   └── ChatModule.java
│
├── user/                           # User Management Module
│   ├── api/
│   │   ├── UserController.java
│   │   └── dto/
│   ├── internal/
│   │   ├── services/
│   │   │   ├── UserService.java
│   │   │   ├── UserSearchService.java
│   │   │   ├── UserStatsService.java
│   │   │   ├── UserInterestService.java
│   │   │   └── UserSocialMediaService.java
│   │   ├── repositories/
│   │   └── domain/
│   └── UserModule.java
│
├── social/                         # Social Features Module
│   ├── api/
│   │   ├── FriendRequestController.java
│   │   └── dto/
│   ├── internal/
│   │   ├── services/
│   │   │   ├── FriendRequestService.java
│   │   │   └── BlockedUserService.java
│   │   ├── repositories/
│   │   └── domain/
│   └── SocialModule.java
│
├── notification/                   # Notification Module
│   ├── api/
│   │   ├── NotificationController.java
│   │   └── dto/
│   ├── internal/
│   │   ├── services/
│   │   │   ├── NotificationService.java
│   │   │   ├── FCMService.java
│   │   │   └── APNSNotificationStrategy.java
│   │   ├── repositories/
│   │   └── domain/
│   └── NotificationModule.java
│
├── media/                          # Media Storage Module
│   ├── api/
│   │   └── dto/
│   ├── internal/
│   │   ├── services/
│   │   │   └── S3Service.java
│   │   └── domain/
│   └── MediaModule.java
│
├── analytics/                      # Analytics & Reporting Module
│   ├── api/
│   │   ├── FeedbackSubmissionController.java
│   │   ├── ShareLinkController.java
│   │   └── dto/
│   ├── internal/
│   │   ├── services/
│   │   │   ├── ReportContentService.java
│   │   │   ├── FeedbackSubmissionService.java
│   │   │   ├── ShareLinkService.java
│   │   │   └── SearchAnalyticsService.java
│   │   ├── repositories/
│   │   └── domain/
│   └── AnalyticsModule.java
│
└── shared/                         # Shared Kernel
    ├── events/                     # Domain events (cross-module)
    │   ├── ActivityEvents.java
    │   ├── UserEvents.java
    │   ├── NotificationEvents.java
    │   └── SocialEvents.java
    ├── exceptions/                 # Common exceptions
    ├── config/                     # Shared configuration
    └── util/                       # Common utilities
```

### Module Dependencies (Target)

```
┌─────────────────────────────────────────────────────────────┐
│                       API Gateway/Controllers                 │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌──────────┐   ┌──────────┐   ┌──────────┐   ┌──────────┐
│   Auth   │   │ Activity │   │   Chat   │   │   User   │
│  Module  │   │  Module  │   │  Module  │   │  Module  │
└──────────┘   └──────────┘   └──────────┘   └──────────┘
      │             │              │              │
      └─────────────┴──────────────┴──────────────┘
                    │
                    ▼
            ┌──────────────┐
            │ Shared Events│
            └──────────────┘
```

**Rules:**
1. Modules only communicate via events or public APIs
2. No direct repository access across modules
3. DTOs in `api/dto` can be shared; `internal/domain` cannot
4. `@ApplicationModuleListener` for event subscriptions

---

---

## Phase 2: Fix Circular Dependencies (Week 3-4)

### Goal

Remove `@Lazy` annotations by replacing direct service calls with event-driven communication.

### Issue 1: Activity ↔ Chat Circular Dependency

#### Current Problem

**ActivityService.java** (line 68):
```java
@Autowired
@Lazy // avoid circular dependency problems with ChatMessageService
public ActivityService(..., IChatMessageService chatMessageService, ...) {
    this.chatMessageService = chatMessageService;
}
```

**Root Cause:** ActivityService needs to check chat messages when building activity DTOs, and ChatMessageService needs to validate activities exist before posting messages.

#### Solution: Replace with Events

**Step 1:** Create domain events in `shared/events/ChatEvents.java`

```java
package com.danielagapov.spawn.shared.events;

import java.time.Instant;
import java.util.UUID;

/**
 * Event published when a chat message is created
 */
public record ChatMessageCreatedEvent(
    UUID messageId,
    UUID activityId,
    UUID senderId,
    String content,
    Instant timestamp
) {}

/**
 * Query event to request chat message count for an activity
 */
public record GetActivityChatMessageCountQuery(
    UUID activityId,
    UUID requestId  // Correlation ID
) {}

/**
 * Response event for chat message count query
 */
public record ActivityChatMessageCountResponse(
    UUID activityId,
    UUID requestId,  // Correlation ID
    int messageCount
) {}
```

**Step 2:** Update ActivityService to use events

```java
package com.danielagapov.spawn.activity.internal.services;

import com.danielagapov.spawn.shared.events.GetActivityChatMessageCountQuery;
import com.danielagapov.spawn.shared.events.ActivityChatMessageCountResponse;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CompletableFuture;

@Service
public class ActivityService implements IActivityService {
    private final ApplicationEventPublisher eventPublisher;
    
    // Store for async query responses
    private final ConcurrentHashMap<UUID, CompletableFuture<Integer>> pendingQueries = new ConcurrentHashMap<>();
    
    // REMOVE: private final IChatMessageService chatMessageService;
    
    @Autowired
    public ActivityService(
        IActivityRepository repository,
        ApplicationEventPublisher eventPublisher,
        // ... other dependencies, EXCLUDING IChatMessageService
    ) {
        this.repository = repository;
        this.eventPublisher = eventPublisher;
        // ... other assignments
    }
    
    /**
     * Get chat message count via event query
     */
    private int getChatMessageCount(UUID activityId) {
        UUID requestId = UUID.randomUUID();
        CompletableFuture<Integer> future = new CompletableFuture<>();
        
        // Register pending query
        pendingQueries.put(requestId, future);
        
        // Publish query event
        eventPublisher.publishEvent(
            new GetActivityChatMessageCountQuery(activityId, requestId)
        );
        
        try {
            // Wait for response (with timeout)
            return future.get(500, TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            logger.warn("Timeout waiting for chat message count for activity " + activityId);
            return 0; // Fallback value
        } finally {
            pendingQueries.remove(requestId);
        }
    }
    
    /**
     * Listen for chat message count responses
     */
    @EventListener
    public void handleChatMessageCountResponse(ActivityChatMessageCountResponse response) {
        CompletableFuture<Integer> future = pendingQueries.get(response.requestId());
        if (future != null) {
            future.complete(response.messageCount());
        }
    }
}
```

**Step 3:** Update ChatMessageService to respond to queries

```java
package com.danielagapov.spawn.chat.internal.services;

import com.danielagapov.spawn.shared.events.GetActivityChatMessageCountQuery;
import com.danielagapov.spawn.shared.events.ActivityChatMessageCountResponse;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;

@Service
public class ChatMessageService implements IChatMessageService {
    private final IChatMessageRepository repository;
    private final ApplicationEventPublisher eventPublisher;
    
    @Autowired
    public ChatMessageService(
        IChatMessageRepository repository,
        ApplicationEventPublisher eventPublisher
    ) {
        this.repository = repository;
        this.eventPublisher = eventPublisher;
    }
    
    /**
     * Respond to chat message count queries
     */
    @EventListener
    public void handleChatMessageCountQuery(GetActivityChatMessageCountQuery query) {
        int count = repository.countByActivityId(query.activityId());
        
        // Publish response
        eventPublisher.publishEvent(
            new ActivityChatMessageCountResponse(
                query.activityId(),
                query.requestId(),
                count
            )
        );
    }
}
```

**Step 4:** Remove `@Lazy` annotation

Delete the `@Lazy` annotation from ActivityService constructor - no longer needed!

### Issue 2: User ↔ ActivityType Circular Dependency

#### Current Problem

**UserService.java** (line 64):
```java
@Autowired
@Lazy // Avoid circular dependency issues
public UserService(..., IActivityTypeService activityTypeService, ...) {
    this.activityTypeService = activityTypeService;
}
```

**Root Cause:** UserService needs ActivityTypeService to manage user's activity type preferences, and ActivityTypeService may need user info.

#### Solution: Move Activity Types to Activity Module

**Step 1:** Refactor UserService to use events for activity type operations

```java
package com.danielagapov.spawn.user.internal.services;

import com.danielagapov.spawn.shared.events.UserActivityTypePreferencesUpdatedEvent;
import org.springframework.context.ApplicationEventPublisher;

@Service
public class UserService implements IUserService {
    private final ApplicationEventPublisher eventPublisher;
    
    // REMOVE: private final IActivityTypeService activityTypeService;
    
    @Autowired
    public UserService(
        IUserRepository repository,
        ApplicationEventPublisher eventPublisher,
        // ... other dependencies, EXCLUDING IActivityTypeService
    ) {
        this.repository = repository;
        this.eventPublisher = eventPublisher;
        // ... other assignments
    }
    
    /**
     * Update user's activity type preferences
     */
    public void updateActivityTypePreferences(UUID userId, List<UUID> activityTypeIds) {
        // Update user record
        User user = repository.findById(userId)
            .orElseThrow(() -> new UserNotFoundException(userId));
        
        // Publish event for activity module to handle
        eventPublisher.publishEvent(
            new UserActivityTypePreferencesUpdatedEvent(userId, activityTypeIds)
        );
    }
}
```

**Step 2:** Activity module listens to user preference events

```java
package com.danielagapov.spawn.activity.internal.services;

import com.danielagapov.spawn.shared.events.UserActivityTypePreferencesUpdatedEvent;
import org.springframework.context.event.EventListener;

@Service
public class ActivityTypeService implements IActivityTypeService {
    
    @EventListener
    public void handleUserActivityTypePreferencesUpdate(
        UserActivityTypePreferencesUpdatedEvent event
    ) {
        // Update activity type associations for this user
        List<ActivityType> activityTypes = repository.findAllById(event.activityTypeIds());
        // ... handle preferences
    }
}
```

**Step 3:** Remove `@Lazy` annotation from UserService

### Issue 3: Shared ActivityUserRepository

#### Current Problem

Both `ActivityService` and `UserService` directly access `IActivityUserRepository`.

**Decision:** Activity module owns the Activity-User participation relationship.

#### Solution: User module queries via events

**Step 1:** Create query events in `shared/events/ActivityEvents.java`

```java
package com.danielagapov.spawn.shared.events;

import java.util.UUID;
import java.util.List;

/**
 * Query to get activities for a user
 */
public record GetUserActivitiesQuery(
    UUID userId,
    UUID requestId  // Correlation ID
) {}

/**
 * Response with user's activities
 */
public record UserActivitiesResponse(
    UUID userId,
    UUID requestId,
    List<UUID> activityIds
) {}
```

**Step 2:** UserService queries via events instead of direct repository access

```java
package com.danielagapov.spawn.user.internal.services;

@Service
public class UserService implements IUserService {
    // REMOVE: private final IActivityUserRepository activityUserRepository;
    
    /**
     * Get user's activities via event query
     */
    private List<UUID> getUserActivityIds(UUID userId) {
        UUID requestId = UUID.randomUUID();
        CompletableFuture<List<UUID>> future = new CompletableFuture<>();
        
        pendingActivityQueries.put(requestId, future);
        
        eventPublisher.publishEvent(
            new GetUserActivitiesQuery(userId, requestId)
        );
        
        try {
            return future.get(1, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            logger.warn("Timeout waiting for user activities");
            return Collections.emptyList();
        }
    }
    
    @EventListener
    public void handleUserActivitiesResponse(UserActivitiesResponse response) {
        CompletableFuture<List<UUID>> future = pendingActivityQueries.get(response.requestId());
        if (future != null) {
            future.complete(response.activityIds());
        }
    }
}
```

**Step 3:** ActivityService owns the repository and responds to queries

```java
package com.danielagapov.spawn.activity.internal.services;

@Service
public class ActivityService implements IActivityService {
    private final IActivityUserRepository activityUserRepository;
    
    @EventListener
    public void handleGetUserActivitiesQuery(GetUserActivitiesQuery query) {
        List<UUID> activityIds = activityUserRepository
            .findActivityIdsByUserId(query.userId());
        
        eventPublisher.publishEvent(
            new UserActivitiesResponse(
                query.userId(),
                query.requestId(),
                activityIds
            )
        );
    }
}
```

### Testing Circular Dependency Fixes

After each fix, run tests:

```bash
mvn test -Dtest=ActivityServiceTest
mvn test -Dtest=UserServiceTest
mvn test -Dtest=ChatMessageServiceTest
```

Verify:
- [ ] No `@Lazy` annotations remain
- [ ] All tests pass
- [ ] Event publishing/receiving works
- [ ] No direct cross-module service calls

---

## Phase 3: Shared Data Resolution (Week 5)

### Goal

Establish clear data ownership for all entities and resolve shared repository access.

### Data Ownership Matrix

| Entity | Owned By | Reason | Access Pattern for Other Modules |
|--------|----------|--------|----------------------------------|
| `User` | User Module | Core user data | Event query or public API |
| `Activity` | Activity Module | Core activity data | Event query or public API |
| `ActivityType` | Activity Module | Part of activity domain | Event query for preferences |
| `ActivityUser` | Activity Module | Participation is activity concept | Event query for user's activities |
| `ChatMessage` | Chat Module | Message data | Event query for counts/history |
| `Friendship` | Social Module | Social relationship | Event query for friend lists |
| `FriendRequest` | Social Module | Social interaction | Event query or notification |
| `Location` | Activity Module | Part of activity context | Embedded in activity data |
| `EmailVerification` | Auth Module | Authentication flow | Not accessed externally |
| `UserIdExternalIdMap` | Auth Module | OAuth mapping | Not accessed externally |
| `DeviceToken` | Notification Module | Push notification data | Not accessed externally |
| `ReportedContent` | Analytics Module | Reporting data | Admin queries only |
| `FeedbackSubmission` | Analytics Module | Feedback data | Admin queries only |
| `ShareLink` | Analytics Module | Share tracking | Public API for creation |

### Repository Migration Rules

1. **Move repository to owning module's `internal/repositories/` package**
2. **Remove repository access from other modules**
3. **Replace with event queries or public API calls**

### Example: ActivityUserRepository

**Before:**
```
Repositories/IActivityUserRepository.java
└─ Used by:
   ├─ ActivityService (owner)
   └─ UserService (violation!)
```

**After:**
```
activity/internal/repositories/IActivityUserRepository.java
└─ Used by:
   └─ ActivityService only

user/internal/services/UserService.java
└─ Uses: GetUserActivitiesQuery event
```

### Public API vs Events

**Use Public API when:**
- Synchronous read-only query
- Frequently needed data
- Low latency requirement
- Simple data structure

**Use Events when:**
- Asynchronous operation acceptable
- Complex query with multiple steps
- State changes/side effects
- Decoupling is priority

### Creating Public APIs

For frequently accessed data, create public API in module's `api/` package:

```java
// activity/api/ActivityPublicApi.java
package com.danielagapov.spawn.activity.api;

import com.danielagapov.spawn.activity.api.dto.ActivityDTO;
import java.util.UUID;
import java.util.List;

/**
 * Public API for Activity module
 * Can be called directly by other modules
 */
public interface ActivityPublicApi {
    
    /**
     * Get activity by ID (read-only)
     */
    ActivityDTO getActivityById(UUID activityId);
    
    /**
     * Get user's activity IDs (read-only)
     */
    List<UUID> getUserActivityIds(UUID userId);
    
    /**
     * Check if user is participant in activity (read-only)
     */
    boolean isUserParticipant(UUID activityId, UUID userId);
}
```

Implementation in internal services:

```java
// activity/internal/services/ActivityPublicApiImpl.java
package com.danielagapov.spawn.activity.internal.services;

import com.danielagapov.spawn.activity.api.ActivityPublicApi;

@Service
class ActivityPublicApiImpl implements ActivityPublicApi {
    private final IActivityService activityService;
    private final IActivityUserRepository activityUserRepository;
    
    @Override
    public ActivityDTO getActivityById(UUID activityId) {
        return activityService.getActivityById(activityId);
    }
    
    @Override
    public List<UUID> getUserActivityIds(UUID userId) {
        return activityUserRepository.findActivityIdsByUserId(userId);
    }
    
    @Override
    public boolean isUserParticipant(UUID activityId, UUID userId) {
        return activityUserRepository.existsByActivityIdAndUserId(activityId, userId);
    }
}
```

Other modules can inject public API:

```java
// user/internal/services/UserService.java
@Service
public class UserService implements IUserService {
    private final ActivityPublicApi activityApi;  // Direct API call OK for reads
    
    @Autowired
    public UserService(ActivityPublicApi activityApi) {
        this.activityApi = activityApi;
    }
    
    public UserProfileDTO getUserProfile(UUID userId) {
        // ...
        List<UUID> activityIds = activityApi.getUserActivityIds(userId);
        // ...
    }
}
```

---

## Phase 4: Add Spring Modulith Dependencies (Week 5)

### Goal

Add Spring Modulith to project and configure module boundaries.

### Step 4.1: Update pom.xml

Add Spring Modulith dependencies:

```xml
<properties>
    <java.version>17</java.version>
    <spring-boot.version>3.3.5</spring-boot.version>
    <spring-modulith.version>1.1.0</spring-modulith.version>
</properties>

<dependencies>
    <!-- Spring Modulith Core -->
    <dependency>
        <groupId>org.springframework.modulith</groupId>
        <artifactId>spring-modulith-starter-core</artifactId>
        <version>${spring-modulith.version}</version>
    </dependency>
    
    <!-- Spring Modulith Testing -->
    <dependency>
        <groupId>org.springframework.modulith</groupId>
        <artifactId>spring-modulith-starter-test</artifactId>
        <version>${spring-modulith.version}</version>
        <scope>test</scope>
    </dependency>
    
    <!-- Spring Modulith Events (Kafka/AMQP future support) -->
    <dependency>
        <groupId>org.springframework.modulith</groupId>
        <artifactId>spring-modulith-events-api</artifactId>
        <version>${spring-modulith.version}</version>
    </dependency>
    
    <!-- Spring Modulith Observability -->
    <dependency>
        <groupId>org.springframework.modulith</groupId>
        <artifactId>spring-modulith-observability</artifactId>
        <version>${spring-modulith.version}</version>
    </dependency>
</dependencies>
```

### Step 4.2: Create Module Configuration Files

Create `package-info.java` for each module to document module boundaries:

**auth/package-info.java:**
```java
/**
 * Authentication and Authorization Module
 * 
 * Public API:
 * - AuthController (REST endpoints)
 * - api.dto.* (public DTOs)
 * 
 * Internal:
 * - OAuth services (Google, Apple Sign-In)
 * - JWT token generation/validation
 * - Email verification flow
 * 
 * Dependencies:
 * - User module (via events) for user creation
 * 
 * Events Published:
 * - UserRegisteredEvent
 * - EmailVerificationSentEvent
 * - OAuthLoginSuccessEvent
 * 
 * Events Consumed:
 * - None
 */
@org.springframework.modulith.ApplicationModule(
    allowedDependencies = {"shared"}
)
package com.danielagapov.spawn.auth;
```

**activity/package-info.java:**
```java
/**
 * Activity Management Module
 * 
 * Public API:
 * - ActivityController, ActivityTypeController (REST endpoints)
 * - ActivityPublicApi (programmatic access)
 * - api.dto.* (public DTOs)
 * 
 * Internal:
 * - Activity CRUD operations
 * - Activity type management
 * - Location management
 * - Participation management (ActivityUser)
 * - Activity expiration logic
 * 
 * Dependencies:
 * - User module (via UserPublicApi) for user validation
 * - Chat module (via events) for message counts
 * - Notification module (via events) for invites
 * 
 * Data Ownership:
 * - Activity, ActivityType, Location, ActivityUser
 * 
 * Events Published:
 * - ActivityCreatedEvent
 * - ActivityUpdatedEvent
 * - ActivityDeletedEvent
 * - ActivityInviteRequestedEvent
 * - ActivityParticipationChangedEvent
 * 
 * Events Consumed:
 * - GetUserActivitiesQuery
 * - GetActivityChatMessageCountQuery
 */
@org.springframework.modulith.ApplicationModule(
    allowedDependencies = {"shared", "user"}
)
package com.danielagapov.spawn.activity;
```

**chat/package-info.java:**
```java
/**
 * Chat and Messaging Module
 * 
 * Public API:
 * - ChatMessageController (REST endpoints)
 * - api.dto.* (public DTOs)
 * 
 * Internal:
 * - Chat message CRUD
 * - Message likes/reactions
 * - Message history
 * 
 * Dependencies:
 * - Activity module (via ActivityPublicApi) to validate activity exists
 * - User module (via UserPublicApi) to validate sender
 * 
 * Data Ownership:
 * - ChatMessage, ChatMessageLikes
 * 
 * Events Published:
 * - ChatMessageCreatedEvent
 * - ChatMessageLikedEvent
 * - ActivityChatMessageCountResponse
 * 
 * Events Consumed:
 * - GetActivityChatMessageCountQuery
 * - ActivityDeletedEvent (to cleanup messages)
 */
@org.springframework.modulith.ApplicationModule(
    allowedDependencies = {"shared", "activity", "user"}
)
package com.danielagapov.spawn.chat;
```

Create similar files for: user, social, notification, media, analytics modules.

### Step 4.3: Configure Application for Modulith

Update main application class:

```java
package com.danielagapov.spawn;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.modulith.Modulith;

@Modulith(
    systemName = "Spawn App",
    sharedModules = {"shared"}
)
@SpringBootApplication
public class SpawnApplication {
    public static void main(String[] args) {
        SpringApplication.run(SpawnApplication.class, args);
    }
}
```

---

## Phase 5: Module Boundary Testing (Week 6-7)

### Goal

Validate that modules respect boundaries and cannot access each other's internals.

### Step 5.1: Create Module Structure Tests

Create test class to verify module structure:

```java
package com.danielagapov.spawn;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;
import org.springframework.modulith.docs.Documenter;

class ModuleStructureTests {
    
    ApplicationModules modules = ApplicationModules.of(SpawnApplication.class);
    
    @Test
    void verifiesModularStructure() {
        // Verify no cyclic dependencies
        modules.verify();
    }
    
    @Test
    void createModuleDocumentation() throws Exception {
        // Generate module documentation
        new Documenter(modules)
            .writeDocumentation()
            .writeModulesAsPlantUml();
    }
    
    @Test
    void ensureModuleEncapsulation() {
        // Verify that internal packages are not accessed from outside
        modules.forEach(module -> {
            module.verifyDependencies();
        });
    }
}
```

Run test:
```bash
mvn test -Dtest=ModuleStructureTests
```

This will:
- ✅ Verify no circular dependencies
- ✅ Ensure modules only depend on allowed modules
- ✅ Generate module dependency diagrams
- ❌ Fail if any module accesses another's `internal` package

### Step 5.2: Individual Module Tests

Create boundary test for each module:

**Activity Module Test:**
```java
package com.danielagapov.spawn.activity;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.test.ApplicationModuleTest;
import org.springframework.modulith.test.Scenario;
import org.springframework.beans.factory.annotation.Autowired;
import com.danielagapov.spawn.shared.events.ActivityCreatedEvent;

@ApplicationModuleTest
class ActivityModuleTests {
    
    @Test
    void activityModuleIsIndependent(Scenario scenario) {
        // Test that activity module can function independently
        scenario
            .stimulate(() -> activityService.createActivity(testActivityDTO))
            .andWaitForEventOfType(ActivityCreatedEvent.class)
            .toArrive();
    }
    
    @Test
    void doesNotAccessUserInternals() {
        // This test will fail if ActivityService directly imports from
        // com.danielagapov.spawn.user.internal.*
    }
    
    @Test
    void respondsToQueryEvents(Scenario scenario) {
        // Test event-driven query
        scenario
            .publish(new GetActivityChatMessageCountQuery(activityId, requestId))
            .andWaitForEventOfType(ActivityChatMessageCountResponse.class)
            .matching(response -> response.requestId().equals(requestId))
            .toArrive();
    }
}
```

**User Module Test:**
```java
package com.danielagapov.spawn.user;

import org.springframework.modulith.test.ApplicationModuleTest;

@ApplicationModuleTest
class UserModuleTests {
    
    @Test
    void userModuleIsIndependent() {
        // Verify user module has no direct dependencies on other module internals
    }
    
    @Test
    void publishesUserRegisteredEvent(Scenario scenario) {
        scenario
            .stimulate(() -> userService.registerUser(testUserDTO))
            .andWaitForEventOfType(UserRegisteredEvent.class)
            .toArrive();
    }
}
```

### Step 5.3: Event Integration Tests

Test event-driven communication between modules:

```java
package com.danielagapov.spawn.integration;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.modulith.test.Scenario;
import org.junit.jupiter.api.Test;

@SpringBootTest
class EventIntegrationTests {
    
    @Test
    void activityCreationTriggersNotification(Scenario scenario) {
        // Test complete flow: Activity created → Notification sent
        scenario
            .stimulate(() -> activityService.createActivity(activityDTO))
            .andWaitForEventOfType(ActivityCreatedEvent.class)
            .toArrive()
            .andWaitForEventOfType(NotificationSentEvent.class)
            .matching(event -> event.activityId().equals(activityId))
            .toArrive();
    }
    
    @Test
    void userRegistrationCreatesDefaultActivityTypes(Scenario scenario) {
        // Test: User registered → Activity types initialized
        scenario
            .stimulate(() -> authService.registerUser(registrationDTO))
            .andWaitForEventOfType(UserRegisteredEvent.class)
            .toArrive()
            .andWaitForEventOfType(UserActivityTypePreferencesUpdatedEvent.class)
            .toArrive();
    }
}
```

### Step 5.4: Performance Testing

Ensure event-driven refactoring doesn't degrade performance:

```java
package com.danielagapov.spawn.performance;

import org.junit.jupiter.api.Test;
import org.springframework.util.StopWatch;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class PerformanceTests {
    
    @Test
    void activityFeedLoadTimeUnder500ms() {
        StopWatch watch = new StopWatch();
        watch.start();
        
        List<ActivityDTO> activities = activityService.getUserFeed(userId);
        
        watch.stop();
        
        assertThat(watch.getTotalTimeMillis())
            .as("Activity feed should load in under 500ms")
            .isLessThan(500);
    }
    
    @Test
    void eventDrivenChatMessageCountUnder100ms() {
        StopWatch watch = new StopWatch();
        watch.start();
        
        int count = activityService.getChatMessageCountForActivity(activityId);
        
        watch.stop();
        
        assertThat(watch.getTotalTimeMillis())
            .as("Event-driven query should complete in under 100ms")
            .isLessThan(100);
    }
}
```

Run performance tests:
```bash
mvn test -Dtest=PerformanceTests
```

### Step 5.5: Test Coverage Requirements

Ensure adequate test coverage:

```bash
mvn clean test jacoco:report
```

**Minimum Coverage Targets:**
- Module public APIs: 90%
- Event handlers: 85%
- Internal services: 80%
- Overall: 75%

---

## Phase 6: Documentation & Validation (Week 8)

### Goal

Document the modular architecture and validate readiness for microservices.

### Step 6.1: Generate Module Documentation

Spring Modulith can auto-generate documentation:

```java
@Test
void generateModuleDocumentation() throws Exception {
    ApplicationModules modules = ApplicationModules.of(SpawnApplication.class);
    
    new Documenter(modules)
        .writeDocumentation()                    // Generates Asciidoc
        .writeModulesAsPlantUml()                // Generates PlantUML diagrams
        .writeIndividualModulesAsPlantUml();     // Per-module diagrams
}
```

Output location: `target/spring-modulith-docs/`

### Step 6.2: Create Module Dependency Diagram

Generate visual representation:

```plantuml
@startuml
package "Spawn App Modules" {
    
    [Auth Module] --> [User Module] : UserRegisteredEvent
    [Auth Module] --> [Shared]
    
    [Activity Module] --> [User Module] : UserPublicApi
    [Activity Module] --> [Chat Module] : GetChatMessageCountQuery
    [Activity Module] --> [Notification Module] : ActivityInviteEvent
    [Activity Module] --> [Shared]
    
    [Chat Module] --> [Activity Module] : ActivityPublicApi
    [Chat Module] --> [User Module] : UserPublicApi
    [Chat Module] --> [Shared]
    
    [User Module] --> [Activity Module] : GetUserActivitiesQuery
    [User Module] --> [Shared]
    
    [Social Module] --> [User Module] : UserPublicApi
    [Social Module] --> [Notification Module] : FriendRequestEvent
    [Social Module] --> [Shared]
    
    [Notification Module] --> [User Module] : UserPublicApi
    [Notification Module] --> [Shared]
    
    [Media Module] --> [User Module] : UserPublicApi
    [Media Module] --> [Shared]
    
    [Analytics Module] --> [User Module] : UserPublicApi
    [Analytics Module] --> [Activity Module] : ActivityPublicApi
    [Analytics Module] --> [Shared]
}
@enduml
```

Save to: `docs/diagrams/modulith-architecture.puml`

### Step 6.3: Document Module APIs

Create API documentation for each module in `docs/refactoring/module-apis/`:

**docs/refactoring/module-apis/ACTIVITY_MODULE_API.md:**
```markdown
# Activity Module API

## Public API Interface

### ActivityPublicApi

\`\`\`java
public interface ActivityPublicApi {
    ActivityDTO getActivityById(UUID activityId);
    List<UUID> getUserActivityIds(UUID userId);
    boolean isUserParticipant(UUID activityId, UUID userId);
}
\`\`\`

## Events Published

### ActivityCreatedEvent
- **When:** New activity is created
- **Payload:** activityId, creatorId, invitedUserIds, timestamp
- **Consumers:** Notification module, Analytics module

### ActivityUpdatedEvent
- **When:** Activity details are modified
- **Payload:** activityId, updatedFields, timestamp
- **Consumers:** Notification module (notify participants)

### ActivityInviteRequestedEvent
- **When:** Users are invited to an activity
- **Payload:** activityId, invitedUserIds, invitedBy
- **Consumers:** Notification module

## Events Consumed

### GetUserActivitiesQuery
- **From:** User module
- **Response:** UserActivitiesResponse with activity IDs

### GetActivityChatMessageCountQuery
- **From:** Activity module (internal query to Chat module)
- **Response:** ActivityChatMessageCountResponse

## REST Endpoints

- POST /api/activities - Create activity
- GET /api/activities/{id} - Get activity
- PUT /api/activities/{id} - Update activity
- DELETE /api/activities/{id} - Delete activity
- GET /api/activities/feed - Get user feed

## Data Ownership

- Activity
- ActivityType
- Location
- ActivityUser (participation)
```

Create similar docs for all modules.

### Step 6.4: Validation Checklist

Before proceeding to microservices, verify:

**Architecture:**
- [ ] All modules have `package-info.java` with boundaries documented
- [ ] Module structure tests passing (`ModuleStructureTests.verify()`)
- [ ] No circular dependencies
- [ ] No direct access to other modules' `internal` packages

**Dependencies:**
- [ ] Zero `@Lazy` annotations for cross-module dependencies
- [ ] All cross-module calls via events or public APIs
- [ ] Clear data ownership for all entities
- [ ] Shared repositories eliminated or moved to owning module

**Events:**
- [ ] All domain events documented
- [ ] Event handlers tested
- [ ] Event-driven queries working (with timeouts)
- [ ] Event correlation IDs for async queries

**Testing:**
- [ ] Module boundary tests passing for all modules
- [ ] Event integration tests passing
- [ ] Performance tests show no regression
- [ ] Test coverage meets minimums (75%+)

**Documentation:**
- [ ] Module dependency diagrams generated
- [ ] API documentation for each module
- [ ] Event catalog documented
- [ ] Migration guide written

**Deployment:**
- [ ] Application still runs as monolith
- [ ] All features functional
- [ ] No new bugs introduced
- [ ] Performance metrics same or better

### Step 6.5: Microservices Readiness Assessment

**Ready to proceed to microservices if:**

1. **Technical Validation:**
   - All tests passing
   - Module boundaries enforced
   - Event-driven communication proven

2. **Team Validation:**
   - Team understands module boundaries
   - Event patterns are clear
   - Comfortable with async communication

3. **Business Validation:**
   - No feature regressions
   - Users see no difference
   - Performance acceptable

**If not ready:**
- Iterate on module boundaries
- Fix remaining circular dependencies
- Improve event-driven patterns
- Add more tests

### Step 6.6: Next Steps Document

Create `docs/refactoring/MICROSERVICES_TRANSITION_PLAN.md`:

```markdown
# Transition from Modulith to Microservices

## Prerequisites (Completed)
- ✅ Spring Modulith refactoring complete
- ✅ Module boundaries validated
- ✅ Event-driven communication working

## Conversion Mapping

### Module → Microservice

Each Spring Modulith module becomes one microservice:

| Module | Microservice Name | Port | Database |
|--------|------------------|------|----------|
| auth | auth-service | 8084 | auth_db (MySQL) |
| activity | activity-service | 8082 | activity_db (MySQL) |
| chat | chat-service | 8085 | chat_db (MySQL) |
| user | user-service | 8081 | user_db (MySQL) |
| social | social-service | 8083 | social_db (MySQL) |
| notification | notification-service | 8086 | monolith_db (shared) |
| media | media-service | 8087 | monolith_db (shared) |
| analytics | analytics-service | 8088 | monolith_db (shared) |

### Event Bus Transition

| Current | Microservices |
|---------|---------------|
| ApplicationEventPublisher | Kafka Producer |
| @EventListener | Kafka Consumer |
| Synchronous (in-memory) | Asynchronous (network) |

### API Transition

| Current | Microservices |
|---------|---------------|
| ModulePublicApi (direct call) | Feign Client (HTTP) |
| No retry logic needed | Resilience4j circuit breakers |
| No timeout handling | Timeout + fallback strategies |

## Timeline

See [MICROSERVICES_IMPLEMENTATION_PLAN.md](../microservices/MICROSERVICES_IMPLEMENTATION_PLAN.md) for detailed timeline.

Estimated: 3-4 months after Modulith completion.
```

---

## Appendix

### A. Spring Modulith Dependencies

Full POM configuration:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0">
    <modelVersion>4.0.0</modelVersion>
    
    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.3.5</version>
    </parent>
    
    <groupId>com.danielagapov</groupId>
    <artifactId>spawn</artifactId>
    <version>0.0.1-SNAPSHOT</version>
    
    <properties>
        <java.version>17</java.version>
        <spring-modulith.version>1.1.0</spring-modulith.version>
    </properties>
    
    <dependencies>
        <!-- Existing Spring Boot dependencies -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        
        <!-- Spring Modulith Core -->
        <dependency>
            <groupId>org.springframework.modulith</groupId>
            <artifactId>spring-modulith-starter-core</artifactId>
            <version>${spring-modulith.version}</version>
        </dependency>
        
        <!-- Spring Modulith Events -->
        <dependency>
            <groupId>org.springframework.modulith</groupId>
            <artifactId>spring-modulith-events-api</artifactId>
            <version>${spring-modulith.version}</version>
        </dependency>
        
        <!-- Spring Modulith Testing -->
        <dependency>
            <groupId>org.springframework.modulith</groupId>
            <artifactId>spring-modulith-starter-test</artifactId>
            <version>${spring-modulith.version}</version>
            <scope>test</scope>
        </dependency>
        
        <!-- Spring Modulith Observability -->
        <dependency>
            <groupId>org.springframework.modulith</groupId>
            <artifactId>spring-modulith-observability</artifactId>
            <version>${spring-modulith.version}</version>
        </dependency>
        
        <!-- Spring Modulith Documentation -->
        <dependency>
            <groupId>org.springframework.modulith</groupId>
            <artifactId>spring-modulith-docs</artifactId>
            <version>${spring-modulith.version}</version>
            <scope>test</scope>
        </dependency>
    </dependencies>
</project>
```

### B. Module Naming Conventions

**Package Naming:**
- Module root: `com.danielagapov.spawn.{module}`
- Public API: `com.danielagapov.spawn.{module}.api`
- Internal implementation: `com.danielagapov.spawn.{module}.internal`
- Shared code: `com.danielagapov.spawn.shared`

**Event Naming:**
- Past tense for facts: `UserRegisteredEvent`, `ActivityCreatedEvent`
- Query suffix for requests: `GetUserActivitiesQuery`
- Response suffix for answers: `UserActivitiesResponse`

**Module Names:**
- Singular: `activity` not `activities`
- Lowercase: `user` not `User`
- Domain-focused: `auth` not `security`

### C. Event-Driven Refactoring Best Practices

1. **Always Use Correlation IDs**
   ```java
   UUID requestId = UUID.randomUUID();
   publish(new QueryEvent(requestId, ...));
   // Later: match response by requestId
   ```

2. **Set Timeouts on Event Queries**
   ```java
   future.get(500, TimeUnit.MILLISECONDS);
   ```

3. **Provide Fallback Values**
   ```java
   catch (TimeoutException e) {
       return Collections.emptyList(); // Graceful degradation
   }
   ```

4. **Log Event Publishing/Consumption**
   ```java
   logger.debug("Publishing GetUserActivitiesQuery for user {}", userId);
   logger.debug("Received UserActivitiesResponse with {} activities", count);
   ```

5. **Test Events in Isolation**
   ```java
   @Test
   void publishesCorrectEvent() {
       scenario.stimulate(() -> service.doAction())
           .andWaitForEventOfType(ExpectedEvent.class)
           .toArrive();
   }
   ```

### D. Migration Checklist

Track your progress:

**✅ Week 1-2: Package Restructuring (COMPLETED Dec 8, 2025)**
- [x] Day 1: Create module skeleton directories
- [x] Day 2: Move Auth module files
- [x] Day 3: Move Activity module files
- [x] Day 4: Move Chat module files
- [x] Day 5: Move User module files
- [x] Day 6-7: Move remaining modules
- [x] Day 8: Move shared code
- [x] Day 9-10: Update imports and test
- [x] **Build successful** - See [PHASE_1_COMPLETE.md](./PHASE_1_COMPLETE.md)

**✅ Week 3-4: Fix Circular Dependencies (COMPLETE - Dec 23, 2025)**
- [x] Identify all `@Lazy` annotations (4 found)
- [x] Create event contracts in `shared/events`
- [x] Fix Activity ↔ Chat circular dependency
- [x] Fix User ↔ ActivityType circular dependency
- [x] Remove all `@Lazy` annotations
- [x] Test each fix independently (build successful)

**Week 5: Shared Data Resolution (Phase 3)** ⬅️ Current
- [ ] Document data ownership matrix (see [PHASE_3_PLAN.md](./PHASE_3_PLAN.md))
- [ ] Create `ActivityPublicApi` interface
- [ ] Create `ActivityPublicApiImpl` implementation
- [ ] Update User module services to use public API
- [ ] Update Chat module services to use public API
- [ ] Update notification events to use DTOs
- [ ] Verify no cross-module repository imports remain

**Week 5-6: Add Spring Modulith (Phase 4)**
- [ ] Update POM with Modulith dependencies
- [ ] Create `package-info.java` for each module
- [ ] Update main application class with `@Modulith`

**Week 6-7: Module Testing**
- [ ] Create `ModuleStructureTests`
- [ ] Create boundary tests for each module
- [ ] Create event integration tests
- [ ] Run performance regression tests
- [ ] Verify test coverage meets minimums
- [ ] Fix any boundary violations

**Week 8: Documentation & Validation**
- [ ] Generate module documentation
- [ ] Create module dependency diagrams
- [ ] Document each module's API
- [ ] Complete validation checklist
- [ ] Assess microservices readiness
- [ ] Write transition plan

### E. Troubleshooting Guide

**Problem: Module structure test fails with "Cycle detected"**

*Solution:*
1. Run test to see which modules have cycle
2. Check for direct service injection between those modules
3. Replace with event or public API
4. Re-run test

**Problem: Performance regression after event refactoring**

*Solution:*
1. Check event query timeouts (should be < 100ms)
2. Add caching for frequently queried data
3. Consider creating public API for hot paths
4. Profile with JProfiler to find bottleneck

**Problem: Test fails: "Module accesses internal package"**

*Solution:*
1. Search codebase for imports from `{module}.internal`
2. Replace with import from `{module}.api`
3. If needed, expose class in public API

**Problem: Events not arriving in tests**

*Solution:*
1. Verify `@EventListener` annotation present
2. Check event class matches exactly (not subclass)
3. Increase timeout in test: `.toArrive(Duration.ofSeconds(5))`
4. Add logging to see if event published

### F. Resources

**Spring Modulith Documentation:**
- Official Docs: https://spring.io/projects/spring-modulith
- Reference: https://docs.spring.io/spring-modulith/reference/
- Samples: https://github.com/spring-projects/spring-modulith/tree/main/spring-modulith-examples

**Related Spawn App Documentation:**
- [WHY_SPRING_MODULITH_FIRST.md](./WHY_SPRING_MODULITH_FIRST.md) - Rationale for this approach
- [MICROSERVICES_IMPLEMENTATION_PLAN.md](../microservices/MICROSERVICES_IMPLEMENTATION_PLAN.md) - Next steps after Modulith
- [MICROSERVICES_DECISION_GUIDE.md](../microservices/MICROSERVICES_DECISION_GUIDE.md) - Strategic decision context

**Books & Articles:**
- *Learning Domain-Driven Design* by Vlad Khononov
- *Building Microservices* by Sam Newman (Chapter on Incremental Migration)
- Spring Modulith Blog: https://spring.io/blog/category/spring-modulith

---

**Document Status:** In Progress - Phase 3  
**Last Updated:** December 23, 2025  
**Version:** 1.2  
**Next Review:** After Phase 3 completion

---

**✅ Phase 1 Complete!** See [PHASE_1_COMPLETE.md](./PHASE_1_COMPLETE.md)

**✅ Phase 2 Complete!** See [PHASE_2_COMPLETE.md](./PHASE_2_COMPLETE.md)

**🔄 Current Focus: Phase 3 - Shared Data Resolution**

**Quick Start Phase 3:**
```bash
# Verify current build status
JAVA_HOME=$(/usr/libexec/java_home -v 17) ./mvnw clean compile -DskipTests

# Check for cross-module repository violations
grep -r "import com.danielagapov.spawn.activity.internal.repositories" \
  src/main/java/com/danielagapov/spawn/user \
  src/main/java/com/danielagapov/spawn/chat

# See Phase 3 detailed plan
# docs/refactoring/PHASE_3_PLAN.md
```

**Phase 3 Key Tasks:**
1. Create `ActivityPublicApi` interface and implementation
2. Update User module services to use public API instead of direct repository access
3. Update Chat module services similarly
4. Update notification events to use DTOs instead of repository types

**Need Help?**
- Review [PHASE_3_PLAN.md](./PHASE_3_PLAN.md) for detailed tasks
- Check [WHY_SPRING_MODULITH_FIRST.md](./WHY_SPRING_MODULITH_FIRST.md) for context
- Check troubleshooting section in this doc
- Refer to Spring Modulith samples repository

