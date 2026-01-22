# Refactoring Order Decision: Modulith vs Mediator First

**Decision Date:** December 8, 2025  
**Status:** ✅ **APPROVED & IN PROGRESS**  
**Current Phase:** Spring Modulith Phase 1 Complete, Phase 2 In Progress  
**Related Documents:**
- [Spring Modulith Refactoring Plan](./SPRING_MODULITH_REFACTORING_PLAN.md)
- [Why Spring Modulith First](./WHY_SPRING_MODULITH_FIRST.md)
- [Phase 1 Complete Summary](./PHASE_1_COMPLETE.md)
- [Mediator Pattern Refactoring](../mediator/MEDIATOR_PATTERN_REFACTORING.md)

---

## Executive Summary

**Decision: Do Spring Modulith refactoring first (6-8 weeks), then implement Mediator pattern (3-4 weeks) within stable module boundaries.**

**✅ Progress Update (Dec 23, 2025):**
- Phase 1 Package Restructuring: **COMPLETE** (Dec 8, 2025)
- Phase 2 Fix Circular Dependencies: **IN PROGRESS** (Current focus)
- See [PHASE_1_COMPLETE.md](./PHASE_1_COMPLETE.md) for details

### Quick Comparison

| Factor | Modulith First | Mediator First |
|--------|---------------|----------------|
| **Fixes circular dependencies** | ✅ Yes | ❌ No |
| **Microservices preparation** | ✅ Direct path | ⏸️ Indirect |
| **Rework required** | ✅ Minimal | ❌ Significant reorganization |
| **Production risk mitigation** | ✅ High | ⏸️ Low |
| **Can be done incrementally** | ⚠️ Harder | ✅ Easy |
| **Timeline** | 6-8 weeks | 3-4 weeks |

---

## Table of Contents

- [Key Reasons to Prioritize Modulith](#key-reasons-to-prioritize-modulith)
- [Why Not Mediator First](#why-not-mediator-first)
- [How They Work Together](#how-they-work-together)
- [Detailed Analysis](#detailed-analysis)
- [Implementation Sequence](#implementation-sequence)
- [Alternative: Hybrid Approach](#alternative-hybrid-approach)
- [Final Recommendation](#final-recommendation)

---

## Key Reasons to Prioritize Modulith

### 1. Addresses Critical Architectural Issues

The Modulith refactoring fixes **hidden circular dependencies** that are currently masked by `@Lazy` annotations:

**Current Problems:**
```java
// Services/Activity/ActivityService.java (line 68)
@Autowired
@Lazy // avoid circular dependency problems with ChatMessageService
public ActivityService(..., IChatMessageService chatMessageService, ...) {
    this.chatMessageService = chatMessageService;
}

// Services/User/UserService.java (line 64)
@Autowired
@Lazy // Avoid circular dependency issues with ActivityTypeService
public UserService(..., IActivityTypeService activityTypeService, ...) {
    this.activityTypeService = activityTypeService;
}
```

**Why This Matters:**
- These are **blocking issues** for microservices
- Can cause production deadlocks and startup failures
- The Mediator pattern **doesn't solve these** - it only changes how controllers call services

**Modulith Solution:**
- Forces you to break circular dependencies using events
- Validates boundaries at compile time
- Prevents these issues before microservices extraction

### 2. Avoids Duplicate Refactoring Work

**If you do Mediator first:**

1. You create this structure:
```
Mediator/
├── commands/
│   ├── auth/
│   │   ├── RegisterUserCommand.java
│   │   └── LoginUserCommand.java
│   ├── friendrequest/
│   └── blockeduser/
├── queries/
│   ├── auth/
│   └── friendrequest/
└── handlers/
    ├── auth/
    └── friendrequest/
```

2. Then during Modulith refactoring, you need to **reorganize all that code**:
```
auth/internal/
├── mediator/
│   ├── commands/
│   │   ├── RegisterUserCommand.java    ← Moved from Mediator/commands/auth/
│   │   └── LoginUserCommand.java       ← Moved from Mediator/commands/auth/
│   └── handlers/
│       └── RegisterUserHandler.java    ← Moved from Mediator/handlers/auth/
```

3. **Result:** Rework all Mediator imports, package paths, and handler registrations

**If you do Modulith first:**

1. Establish clean module boundaries:
```
auth/
activity/
chat/
user/
```

2. Then implement Mediator **within each module** - no reorganization needed!

### 3. Better Architectural Layering

The patterns complement each other at **different architectural levels**:

```
┌─────────────────────────────────────────────────┐
│         Controllers (API Layer)                  │
│         - Thin, REST-focused                     │
└─────────────────────────────────────────────────┘
                    ↓
┌─────────────────────────────────────────────────┐
│    Mediator Pattern (INTERNAL to each module)   │  ← Mediator lives HERE
│    - Commands/Queries/Handlers                   │
│    - Decouples controllers from services         │
└─────────────────────────────────────────────────┘
                    ↓
┌─────────────────────────────────────────────────┐
│    Module Boundaries (BETWEEN modules)          │  ← Modulith defines THIS
│    - auth/ activity/ chat/ user/                 │
│    - Event-driven communication                  │
│    - No direct cross-module dependencies         │
└─────────────────────────────────────────────────┘
```

**Key Insight:**
- **Modulith** = Macro architecture (how modules interact)
- **Mediator** = Micro architecture (how classes within a module interact)
- You need the macro structure (Modulith) before optimizing micro structure (Mediator)

### 4. Timeline Efficiency

**Sequential Approach:**
- Modulith first: 6-8 weeks
- Then Mediator: 3-4 weeks
- **Total:** 9-12 weeks with minimal rework

**Mediator First (Hidden Costs):**
- Mediator: 3-4 weeks
- Modulith: 6-8 weeks
- Reorganizing Mediator code: 1-2 weeks (fixing imports, paths, tests)
- **Total:** 10-14 weeks with frustrating rework

**Net Savings:** 1-2 weeks + avoiding rework frustration

### 5. Aligns with Strategic Goal

Your documentation shows you're planning for **microservices**:
- [MICROSERVICES_IMPLEMENTATION_PLAN.md](../microservices/MICROSERVICES_IMPLEMENTATION_PLAN.md)
- [WHY_SPRING_MODULITH_FIRST.md](./WHY_SPRING_MODULITH_FIRST.md)

**Modulith** is a direct stepping stone to microservices:
```
Monolith → Modulith → Microservices
```

**Mediator** is an internal code organization pattern:
```
Fat Controllers → Mediator → Thin Controllers
```

While both are valuable, **Modulith directly enables your strategic goal**.

---

## Why Not Mediator First?

### Doesn't Address Circular Dependencies

**Current Issue:**
```java
ActivityService → ChatMessageService → ActivityService  // Circular!
UserService → ActivityTypeService → UserService         // Circular!
```

**After Mediator Pattern:**
```java
ActivityController → Mediator → ActivityHandler → ActivityService → ChatMessageService
ChatMessageController → Mediator → ChatHandler → ChatMessageService → ActivityService
// Still circular! Just with more layers.
```

The Mediator pattern adds a layer between controllers and services, but **doesn't change service-to-service dependencies**.

### Doesn't Prepare for Microservices

**Mediator Pattern:**
- Decouples controllers from services ✅
- Makes testing easier ✅
- Doesn't enforce module boundaries ❌
- Doesn't validate data ownership ❌
- Doesn't require event-driven communication ❌

**Modulith:**
- Enforces module boundaries ✅
- Validates data ownership ✅
- Forces event-driven communication ✅
- Tests module isolation ✅
- Direct path to microservices ✅

### Creates Reorganization Work

If you implement 30+ commands, 15+ queries, and 45+ handlers in a `Mediator/` package, then decide to do Modulith:

**Files to reorganize:** 90+ files  
**Imports to fix:** 200+ import statements  
**Tests to update:** 50+ test files  
**Build issues to debug:** Numerous

This is **avoidable** by establishing module boundaries first.

---

## How They Work Together

After completing Modulith refactoring, you can implement Mediator **within each module** for a powerful combination:

### Integrated Structure

```
com.danielagapov.spawn/
├── auth/                              # Module boundary (Modulith)
│   ├── api/
│   │   ├── AuthController.java        # Thin controller using mediator
│   │   └── dto/
│   ├── internal/
│   │   ├── mediator/                  # Mediator pattern (internal)
│   │   │   ├── Mediator.java
│   │   │   ├── commands/
│   │   │   │   ├── RegisterUserCommand.java
│   │   │   │   └── LoginUserCommand.java
│   │   │   ├── queries/
│   │   │   │   └── GetUserByTokenQuery.java
│   │   │   └── handlers/
│   │   │       ├── RegisterUserHandler.java
│   │   │       └── LoginUserHandler.java
│   │   ├── services/
│   │   │   ├── AuthService.java
│   │   │   └── OAuthService.java
│   │   └── repositories/
│   └── AuthModule.java
│
├── activity/                          # Module boundary (Modulith)
│   ├── api/
│   │   └── ActivityController.java    # Uses mediator
│   ├── internal/
│   │   ├── mediator/                  # Mediator pattern (internal)
│   │   │   ├── commands/
│   │   │   ├── queries/
│   │   │   └── handlers/
│   │   └── services/
│   └── ActivityModule.java
│
└── shared/
    └── events/                        # Cross-module events (Modulith)
        ├── ActivityEvents.java
        └── UserEvents.java
```

### Communication Patterns

**Within a Module (Mediator):**
```java
// Controller → Mediator → Handler → Service
AuthController
  → mediator.send(new RegisterUserCommand(...))
    → RegisterUserHandler.handle()
      → AuthService.registerUser()
```

**Between Modules (Modulith Events):**
```java
// Activity module publishes event
activityService.createActivity(...);
eventPublisher.publishEvent(new ActivityCreatedEvent(...));

// Notification module listens
@EventListener
void handleActivityCreated(ActivityCreatedEvent event) {
    notificationService.sendInviteNotifications(event);
}
```

### Combined Benefits

| Benefit | Modulith | Mediator | Together |
|---------|----------|----------|----------|
| **Decouple controllers from services** | ❌ | ✅ | ✅ |
| **Enforce module boundaries** | ✅ | ❌ | ✅ |
| **Prevent circular dependencies** | ✅ | ❌ | ✅ |
| **Testable command handlers** | ❌ | ✅ | ✅ |
| **Event-driven cross-module comm** | ✅ | ❌ | ✅ |
| **Single Responsibility Principle** | ❌ | ✅ | ✅ |
| **Microservices readiness** | ✅ | ❌ | ✅ |
| **Clean internal architecture** | ❌ | ✅ | ✅ |

**Result:** Best of both worlds!

---

## Detailed Analysis

### Circular Dependency Impact

**Current State:**
```
ActivityService (has @Lazy ChatMessageService)
    ↓ needs
ChatMessageService
    ↓ needs
ActivityService (to validate activities exist)
```

**After Mediator Only:**
```
ActivityHandler → ActivityService (still has @Lazy ChatMessageService)
    ↓ needs
ChatMessageHandler → ChatMessageService
    ↓ needs
ActivityService (to validate activities exist)
```
**Problem:** Circular dependency persists at service layer!

**After Modulith:**
```
ActivityService
    → publishes GetChatMessageCountQuery event
    
ChatMessageService
    → listens to GetChatMessageCountQuery
    → publishes ChatMessageCountResponse event
```
**Solution:** Cycle broken with event-driven communication!

**After Modulith + Mediator:**
```
ActivityHandler → ActivityService
    → publishes query event
    
ChatMessageHandler → ChatMessageService
    → listens to query event
```
**Result:** Clean architecture at all layers!

### Shared Repository Impact

**Current Problem:**
```java
// IActivityUserRepository is used by:
ActivityService.java   (line 57)  // Who owns this?
UserService.java       (line 50)  // Both services access it!
```

**After Mediator Only:**
```java
// Still used by:
ActivityHandler → ActivityService (uses IActivityUserRepository)
UserHandler → UserService (uses IActivityUserRepository)
// Problem: Shared data access still unclear!
```

**After Modulith:**
```java
// Clear ownership established:
activity/internal/repositories/IActivityUserRepository.java
    └─ Only ActivityService can access

// UserService queries via events:
eventPublisher.publishEvent(new GetUserActivitiesQuery(userId));
```
**Solution:** Data ownership enforced!

### Microservices Transition

**Modulith → Microservices (Smooth):**
```java
// In Modulith (event-driven):
eventPublisher.publishEvent(new ActivityCreatedEvent(...));

// In Microservices (just change transport):
kafkaTemplate.send("activity-events", new ActivityCreatedEvent(...));
// ✅ Pattern already proven in monolith!
```

**Mediator → Microservices (No Direct Help):**
```java
// Mediator pattern stays within each microservice
// Doesn't help with service-to-service communication
// Still need to design inter-service patterns from scratch
```

---

## Implementation Sequence

### Recommended: Modulith → Mediator

**✅ Months 1-2: Spring Modulith Refactoring (STARTED Dec 2025)**
- ✅ Week 1-2: Package restructuring (COMPLETE Dec 8)
  - All 266 files moved
  - Build successful
- 🔄 Week 3-4: Fix circular dependencies with events (IN PROGRESS)
- Week 5: Resolve shared data ownership
- Week 6-7: Add Modulith dependencies, testing
- Week 8: Documentation and validation

**Expected Result:** Module boundaries enforced, circular dependencies fixed

**Months 3-4: Mediator Pattern Implementation (Optional)**
- Week 1: Core mediator infrastructure in `shared/`
- Week 2: Auth module mediator (largest)
- Week 3: Activity module mediator
- Week 4: Social modules mediator (friend requests, blocked users)
- Week 5: Testing and optimization

**Expected Result:** Clean internal architecture within modules

**Month 5+: Microservices Extraction**
- Follow [MICROSERVICES_IMPLEMENTATION_PLAN.md](../microservices/MICROSERVICES_IMPLEMENTATION_PLAN.md)
- Event patterns already proven
- Module boundaries already validated

### Not Recommended: Mediator → Modulith

**Months 1-2: Mediator Pattern**
- Week 1: Core mediator infrastructure
- Week 2-5: Implement 90+ commands/queries/handlers

**Problem:** All code in `Mediator/` package with no module boundaries

**Months 3-4: Modulith Refactoring**
- Week 1-2: Create module structure
- Week 3-4: **Reorganize all Mediator code into modules**
- Week 5-6: Fix circular dependencies
- Week 7-8: Fix imports, tests, build issues

**Result:** 2+ weeks of frustrating rework

---

## Alternative: Hybrid Approach

If you **must** start Mediator work early, consider this compromise:

### Phase 1: Modulith Foundation (Weeks 1-3)
- Complete package restructuring (Phase 1 of Modulith plan)
- Fix circular dependencies (Phase 2 of Modulith plan)
- **Don't** add full Modulith dependencies yet

### Phase 2: Mediator in Parallel (Weeks 3-5)
- While doing Modulith Phase 3-4, implement Mediator
- But **respect the module boundaries** already established
- Put Mediator code in `{module}/internal/mediator/`

### Phase 3: Complete Modulith (Weeks 6-8)
- Add Modulith dependencies
- Run module boundary tests
- Fix any violations

**Pros:**
- Gets Mediator benefits sooner
- Teams can work in parallel

**Cons:**
- More complex coordination
- Risk of boundary violations
- Requires discipline to not shortcut module boundaries

**Verdict:** Only do this if you have 2+ developers working independently. For solo development, stick with sequential approach.

---

## Final Recommendation

### Do Spring Modulith First

**Reasons:**
1. ✅ **Fixes production-risk circular dependencies** (must be done anyway)
2. ✅ **Required prerequisite for microservices** (strategic goal)
3. ✅ **Establishes boundaries that Mediator can respect** (prevents rework)
4. ✅ **Validates data ownership** (critical for distributed systems)
5. ✅ **More urgent** (@Lazy is a band-aid, not a solution)

**Timeline:** 6-8 weeks

### Then Consider Mediator Pattern

**Reasons:**
1. ✅ **Improves internal module architecture** (valuable but not urgent)
2. ✅ **Can be done incrementally** (module by module)
3. ✅ **Won't require reorganization** (boundaries already clear)
4. ✅ **Easier to implement** when modules are isolated

**Timeline:** 3-4 weeks (optional enhancement)

### Total Time Investment

**Sequential Approach:** 9-12 weeks (clean, no rework)

**Comparison to Direct Microservices:**
- Direct to microservices: 5-6 months + major production issues
- Modulith → Microservices: 4-5 months (stable)
- Modulith → Mediator → Microservices: 5-6 months (most robust)

---

## Decision Checklist

**Choose Modulith First if:**
- ✅ You're planning microservices within 6-12 months
- ✅ You have circular dependencies (you do - 3 found)
- ✅ You have shared repository access (you do - ActivityUserRepository)
- ✅ You want to minimize rework
- ✅ You value production stability over internal code organization

**Choose Mediator First if:**
- ⏸️ Microservices are 2+ years away or not planned
- ⏸️ Controllers are extremely fat and unmaintainable (yours are reasonable)
- ⏸️ Team is already familiar with CQRS/Mediator patterns
- ⏸️ You don't mind reorganizing code later

**For Spawn App:** All signs point to **Modulith First** ✅

**✅ DECISION IMPLEMENTED - Phase 1 complete, Phase 2 in progress**

---

## Next Steps

### ✅ Completed
1. ~~Review and approve this decision~~ - **APPROVED Dec 8, 2025**
2. ~~Phase 1: Package restructuring~~ - **COMPLETE Dec 8, 2025**
   - All 266 files moved to modular structure
   - Build successful
   - See [PHASE_1_COMPLETE.md](./PHASE_1_COMPLETE.md)

### 🔄 Current (Week 3-4)
**Phase 2: Fix Circular Dependencies** - IN PROGRESS

1. **This Week: Activity ↔ Chat Circular Dependency**
   - Create event contracts in `shared/events/`
   - Update ActivityService to use events instead of direct calls
   - Update ChatMessageService to respond to events
   - Remove `@Lazy` annotation

2. **Next Week: User ↔ ActivityType Circular Dependency**
   - Create preference update events
   - Remove direct ActivityTypeService dependency
   - Remove `@Lazy` annotation

3. **Phase 3 (Week 5):**
   - Resolve shared ActivityUserRepository
   - Add Spring Modulith dependencies

### ⏭️ Upcoming

4. **After Phase 6 Complete (Week 8+):**
   - Assess need for Mediator pattern
   - If valuable, implement incrementally within modules

5. **Month 5+:**
   - Begin microservices extraction
   - Follow [MICROSERVICES_IMPLEMENTATION_PLAN.md](../microservices/MICROSERVICES_IMPLEMENTATION_PLAN.md)

---

## References

- [Spring Modulith Refactoring Plan](./SPRING_MODULITH_REFACTORING_PLAN.md)
- [Why Spring Modulith First](./WHY_SPRING_MODULITH_FIRST.md)
- [Mediator Pattern Refactoring](../mediator/MEDIATOR_PATTERN_REFACTORING.md)
- [Microservices Implementation Plan](../microservices/MICROSERVICES_IMPLEMENTATION_PLAN.md)

---

**Document Status:** Decision Approved & In Progress  
**Last Updated:** December 23, 2025  
**Version:** 1.1  
**Decision Owner:** Development Team  
**Next Review:** After Modulith Phase 2 completion (Week 4)



