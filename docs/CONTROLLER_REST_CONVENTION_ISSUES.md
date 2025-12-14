# Controller REST Convention Issues

## Date: December 13, 2025

**Status: ✅ BACKEND IMPLEMENTATION COMPLETE** (December 13, 2025)

This document catalogs REST API convention violations found across all backend controllers during a comprehensive code review following the medium priority action from `API_DISCREPANCIES.md`.

All backend fixes have been implemented. Frontend changes are now required.

## Executive Summary

**Controllers Analyzed**: 18  
**Controllers with Issues**: 6  
**Total Issues Found**: 11

### Issue Breakdown
- **4 High Priority Issues**: camelCase in URL paths (should be kebab-case)
  - Affects 11 total endpoints
  - Violates web standards for URL formatting
  - Found in: ChatMessageController, BetaAccessSignUpController, ActivityController
  
- **7 Medium Priority Issues**: Redundant action verbs in paths
  - Affects 7 total endpoints
  - Violates REST naming conventions
  - Found in: UserController, BlockedUserController, NotificationController, FeedbackSubmissionController

### Recommendation
Fix **High Priority** (casing) issues first, then address **Medium Priority** (action verbs) issues. All changes require coordinated frontend updates across web and iOS applications.

---

## Overview

This review examined 18 controllers for the following REST convention violations:
- Redundant action verbs in endpoint paths (`/create`, `/fetch`, `/update`, `/delete`, `/register`, `/unregister`, etc.)
- Improper use of HTTP verbs
- Inconsistent path parameter naming
- **Inconsistent URL path casing (camelCase vs. kebab-case)**

**Summary**: Found **11 issues** across **6 controllers**
- **7 issues** with redundant action verbs
- **4 issues** with camelCase paths (should be kebab-case)

**Note**: URL paths should use kebab-case (e.g., `chat-messages`, not `chatMessages`). Parameter names in request bodies and query strings can remain camelCase as per JSON/Java conventions.

---

## Quick Reference Table

| Controller | Issue Type | Current Path | Suggested Path | Priority |
|------------|------------|--------------|----------------|----------|
| UserController | Action Verb | `PATCH /update-pfp/{id}` | `PATCH /{id}/profile-picture` | Medium |
| UserController | Action Verb | `PATCH /update/{id}` | `PATCH /{id}` | Medium |
| BlockedUserController | Action Verb | `POST /block` | `POST /blocked-users` | Medium |
| BlockedUserController | Action Verb + Params | `DELETE /unblock?...` | `DELETE /{blockerId}/{blockedId}` | Medium |
| NotificationController | Action Verb | `POST /device-tokens/register` | `POST /device-tokens` | Medium |
| NotificationController | Action Verb | `DELETE /device-tokens/unregister` | `DELETE /device-tokens/{token}` | Medium |
| FeedbackSubmissionController | Action Verb | `DELETE /delete/{id}` | `DELETE /{id}` | Medium |
| **ChatMessageController** | **camelCase Path** | `@RequestMapping("chatMessages")` | `@RequestMapping("chat-messages")` | **High** |
| **BetaAccessSignUpController** | **camelCase Path** | `@RequestMapping("betaAccessSignUp")` | `@RequestMapping("beta-access-sign-up")` | **High** |
| **ActivityController** | **camelCase Path** | `GET /feedActivities/{id}` | `GET /feed-activities/{id}` | **High** |
| **ActivityController** | **camelCase Path** | `PUT /{ActivityId}/toggleStatus/{id}` | `PUT /{activityId}/toggle-status/{id}` | **High** |

---

## Issues by Controller

### 1. UserController (`src/main/java/com/danielagapov/spawn/user/api/UserController.java`)

#### Issue 1.1: Redundant "update-pfp" action verb
- **Current**: `PATCH /api/v1/users/update-pfp/{id}`
- **Suggested**: `PATCH /api/v1/users/{id}/profile-picture`
- **Reasoning**: The HTTP verb PATCH already indicates an update operation. The endpoint should represent the resource being modified (profile picture) rather than the action.
- **Line**: 121

#### Issue 1.2: Redundant "update" action verb
- **Current**: `PATCH /api/v1/users/update/{id}`
- **Suggested**: `PATCH /api/v1/users/{id}`
- **Reasoning**: PATCH to `/users/{id}` already implies updating that user. The `/update` segment is redundant.
- **Line**: 177

---

### 2. BlockedUserController (`src/main/java/com/danielagapov/spawn/user/api/BlockedUserController.java`)

#### Issue 2.1: Redundant "block" action verb
- **Current**: `POST /api/v1/blocked-users/block`
- **Suggested**: `POST /api/v1/blocked-users`
- **Reasoning**: POST to a collection endpoint implicitly means "create". Creating a blocked user relationship is inherently a "block" action.
- **Alternative Consideration**: Could use nested resource: `POST /api/v1/users/{blockerId}/blocked-users` with `blockedId` in request body
- **Line**: 31

#### Issue 2.2: Non-RESTful "unblock" endpoint
- **Current**: `DELETE /api/v1/blocked-users/unblock?blockerId=...&blockedId=...`
- **Suggested**: `DELETE /api/v1/blocked-users/{blockerId}/{blockedId}` OR `DELETE /api/v1/users/{blockerId}/blocked-users/{blockedId}`
- **Reasoning**: DELETE should target a specific resource via path parameters, not query parameters. The `/unblock` action verb is redundant since DELETE already implies removal.
- **Line**: 44

---

### 3. NotificationController (`src/main/java/com/danielagapov/spawn/notification/api/NotificationController.java`)

#### Issue 3.1: Redundant "register" action verb
- **Current**: `POST /api/v1/notifications/device-tokens/register`
- **Suggested**: `POST /api/v1/notifications/device-tokens`
- **Reasoning**: POST to a collection endpoint already means "create/register". The `/register` suffix is redundant.
- **Line**: 34

#### Issue 3.2: Redundant "unregister" action verb  
- **Current**: `DELETE /api/v1/notifications/device-tokens/unregister`
- **Suggested**: `DELETE /api/v1/notifications/device-tokens/{token}` OR `DELETE /api/v1/notifications/device-tokens` (with token in request body)
- **Reasoning**: DELETE already implies removal/unregistration. The current endpoint uses the token from the request body, but a more RESTful approach would use it as a path parameter.
- **Line**: 47
- **Note**: May need to consider URL-encoding if token contains special characters

---

### 4. FeedbackSubmissionController (`src/main/java/com/danielagapov/spawn/analytics/api/FeedbackSubmissionController.java`)

#### Issue 4.1: Redundant "delete" action verb
- **Current**: `DELETE /api/v1/feedback/delete/{id}`
- **Suggested**: `DELETE /api/v1/feedback/{id}`
- **Reasoning**: The HTTP verb DELETE already conveys the action. Adding `/delete` in the path is redundant.
- **Line**: 153

---

### 5. ChatMessageController (`src/main/java/com/danielagapov/spawn/chat/api/ChatMessageController.java`)

#### Issue 5.1: camelCase in base path ❌
- **Current**: `@RequestMapping("api/v1/chatMessages")`
- **Suggested**: `@RequestMapping("api/v1/chat-messages")`
- **Reasoning**: URL paths should use kebab-case, not camelCase. This affects ALL endpoints in this controller:
  - `POST /api/v1/chatMessages` → `POST /api/v1/chat-messages`
  - `DELETE /api/v1/chatMessages/{id}` → `DELETE /api/v1/chat-messages/{id}`
  - `POST /api/v1/chatMessages/{chatMessageId}/likes/{userId}` → `POST /api/v1/chat-messages/{chatMessageId}/likes/{userId}`
  - `GET /api/v1/chatMessages/{chatMessageId}/likes` → `GET /api/v1/chat-messages/{chatMessageId}/likes`
  - `DELETE /api/v1/chatMessages/{chatMessageId}/likes/{userId}` → `DELETE /api/v1/chat-messages/{chatMessageId}/likes/{userId}`
- **Line**: 25
- **Impact**: HIGH - All 5 endpoints in this controller need frontend updates

---

### 6. BetaAccessSignUpController (`src/main/java/com/danielagapov/spawn/analytics/api/BetaAccessSignUpController.java`)

#### Issue 6.1: camelCase in base path ❌
- **Current**: `@RequestMapping("api/v1/betaAccessSignUp")`
- **Suggested**: `@RequestMapping("api/v1/beta-access-sign-up")`
- **Reasoning**: URL paths should use kebab-case, not camelCase. This affects ALL endpoints in this controller:
  - `GET /api/v1/betaAccessSignUp/emails` → `GET /api/v1/beta-access-sign-up/emails`
  - `GET /api/v1/betaAccessSignUp/records` → `GET /api/v1/beta-access-sign-up/records`
  - `PUT /api/v1/betaAccessSignUp/{id}/emailed` → `PUT /api/v1/beta-access-sign-up/{id}/emailed`
  - `POST /api/v1/betaAccessSignUp` → `POST /api/v1/beta-access-sign-up`
- **Line**: 16
- **Impact**: HIGH - All 4 endpoints in this controller need frontend updates

---

### 7. ActivityController (`src/main/java/com/danielagapov/spawn/activity/api/ActivityController.java`)

#### Issue 7.1: camelCase in endpoint path ❌
- **Current**: `GET /api/v1/activities/feedActivities/{requestingUserId}`
- **Suggested**: `GET /api/v1/activities/feed-activities/{requestingUserId}`
- **Reasoning**: URL paths should use kebab-case, not camelCase.
- **Line**: 212

#### Issue 7.2: camelCase in endpoint path ❌
- **Current**: `PUT /api/v1/activities/{ActivityId}/toggleStatus/{userId}`
- **Suggested**: `PUT /api/v1/activities/{activityId}/toggle-status/{userId}`
- **Reasoning**: URL paths should use kebab-case, not camelCase. Also note the path parameter should be `{activityId}` (lowercase first letter) for consistency with other endpoints.
- **Line**: 175

---

## Controllers Following REST Conventions ✅

The following controllers properly follow REST conventions and require no changes:

1. **ShareLinkController** - Clean resource-based endpoints
2. **UserStatsController** - Proper use of nested resources
3. **UserSocialMediaController** - Clean CRUD operations
4. **UserInterestController** - Proper collection endpoints
5. **CalendarController** - Good use of query parameters for filtering
6. **FriendRequestController** - Well-designed friend request actions
7. **AuthController** - Appropriate auth-specific naming (auth endpoints often deviate from pure REST)
8. **SearchAnalyticsController** - Good analytics endpoint design
9. **ActivityTypeController** - Proper batch operations
10. **CacheController** - Acceptable for admin/utility endpoints
11. **ReportController** - Previously fixed (see API_DISCREPANCIES.md)

---

## REST Naming Conventions Reference

### Correct Patterns ✅

#### HTTP Verbs and Resource Paths
- `GET /resources` - List all resources (with optional query params for filtering)
- `POST /resources` - Create a new resource
- `GET /resources/{id}` - Get a specific resource
- `PUT /resources/{id}` - Update a resource (full replacement)
- `PATCH /resources/{id}` - Partial update of a resource
- `DELETE /resources/{id}` - Delete a resource

#### Nested Resources
- `GET /users/{userId}/friends` - Get all friends for a user
- `POST /users/{userId}/friends` - Add a friend for a user
- `DELETE /users/{userId}/friends/{friendId}` - Remove a specific friend

#### URL Path Casing (IMPORTANT)
- **URL paths**: Use **kebab-case** (lowercase with hyphens)
  - ✅ `/chat-messages`, `/beta-access-sign-up`, `/feed-activities`, `/device-tokens`
  - ❌ `/chatMessages`, `/betaAccessSignUp`, `/feedActivities`, `/deviceTokens`
- **Path parameters**: Use **camelCase** (consistent with Java naming)
  - ✅ `{userId}`, `{activityId}`, `{requestingUserId}`
- **Query parameters**: Use **camelCase** (consistent with JSON/Java conventions)
  - ✅ `?searchQuery=...`, `?requestingUserId=...`
- **Request/Response bodies**: Use **camelCase** (JSON standard)
  - ✅ `{ "userName": "...", "profilePicture": "..." }`

### Anti-Patterns to Avoid ❌

#### Redundant Action Verbs
- ❌ `POST /resources/create`
- ❌ `GET /resources/fetch`
- ❌ `GET /resources/getAll`
- ❌ `PUT /resources/update/{id}`
- ❌ `PATCH /resources/update/{id}`
- ❌ `DELETE /resources/delete/{id}`
- ❌ `POST /resources/register`
- ❌ `DELETE /resources/unregister`

The HTTP verb already conveys the action; redundant path segments add verbosity without value.

#### Incorrect Casing
- ❌ `GET /chatMessages` (use `/chat-messages`)
- ❌ `GET /betaAccessSignUp` (use `/beta-access-sign-up`)
- ❌ `GET /feedActivities` (use `/feed-activities`)
- ❌ `POST /deviceTokens/register` (use `/device-tokens`)

URL paths should be readable and predictable. kebab-case is the web standard.

---

## Priority Assessment

### High Priority
**camelCase Issues (4 issues)**: These are inconsistencies that violate standard web conventions:
- `ChatMessageController` - All 5 endpoints affected
- `BetaAccessSignUpController` - All 4 endpoints affected  
- `ActivityController` - 2 endpoints affected

**Rationale**: kebab-case is the standard for URL paths across the web. Mixed casing creates:
- Confusion for frontend developers
- Inconsistency across the API
- Potential URL-encoding issues
- Harder-to-read URLs

### Medium Priority (Recommended)
**Redundant Action Verbs (7 issues)**: These are REST convention violations:
- `UserController` - 2 endpoints
- `BlockedUserController` - 2 endpoints
- `NotificationController` - 2 endpoints
- `FeedbackSubmissionController` - 1 endpoint

**Rationale**: 
- Maintain consistency with the fixed `ReportController` 
- Improve API documentation clarity
- Follow industry-standard REST practices
- Reduce confusion for frontend developers

### Considerations Before Implementing
1. **Breaking Changes**: All 11 endpoint changes will require coordinated frontend updates
2. **Versioning**: Consider if these changes warrant a new API version (v2)
3. **Deprecation Period**: May want to support old endpoints temporarily with deprecation warnings
4. **Documentation**: Update OpenAPI/Swagger specs, frontend configs, and mobile apps
5. **Testing**: Ensure integration tests cover both old and new endpoints during transition
6. **Migration Strategy**: 
   - High priority (casing) issues could be fixed first as they're more fundamental
   - Medium priority (action verbs) could follow in a second phase

---

## Implementation Checklist

When fixing these issues, ensure:

- [x] Backend controller endpoint paths updated
- [x] Service layer methods remain unchanged (internal implementation details)
- [ ] Integration tests updated
- [ ] Frontend code updated:
  - [ ] Web app (`spawn-app/src/`)
  - [ ] iOS app (`Spawn-App-iOS-SwiftUI/`)
- [ ] API documentation updated
- [ ] Deprecation warnings added to old endpoints (if using gradual migration)
- [x] Database queries remain unaffected (these are API-level changes only)

---

## Backend Implementation Progress

### ✅ Completed (December 13, 2025)

All backend REST convention issues have been fixed:

#### High Priority (camelCase → kebab-case) - COMPLETE
1. ✅ **ChatMessageController** - Changed base path from `chatMessages` to `chat-messages`
   - 5 endpoints updated
   - File: `src/main/java/com/danielagapov/spawn/chat/api/ChatMessageController.java`

2. ✅ **BetaAccessSignUpController** - Changed base path from `betaAccessSignUp` to `beta-access-sign-up`
   - 4 endpoints updated
   - File: `src/main/java/com/danielagapov/spawn/analytics/api/BetaAccessSignUpController.java`

3. ✅ **ActivityController** - Fixed endpoint paths
   - `feedActivities` → `feed-activities`
   - `{ActivityId}/toggleStatus` → `{activityId}/toggle-status`
   - File: `src/main/java/com/danielagapov/spawn/activity/api/ActivityController.java`

#### Medium Priority (Redundant Action Verbs) - COMPLETE
4. ✅ **UserController** - Removed redundant update prefixes
   - `PATCH /update-pfp/{id}` → `PATCH /{id}/profile-picture`
   - `PATCH /update/{id}` → `PATCH /{id}`
   - File: `src/main/java/com/danielagapov/spawn/user/api/UserController.java`

5. ✅ **BlockedUserController** - Removed block/unblock actions, switched to path params
   - `POST /block` → `POST /`
   - `DELETE /unblock?blockerId=...&blockedId=...` → `DELETE /{blockerId}/{blockedId}`
   - File: `src/main/java/com/danielagapov/spawn/user/api/BlockedUserController.java`

6. ✅ **NotificationController** - Removed register/unregister actions
   - `POST /device-tokens/register` → `POST /device-tokens`
   - `DELETE /device-tokens/unregister` → `DELETE /device-tokens` (kept token in body to avoid URL encoding issues)
   - File: `src/main/java/com/danielagapov/spawn/notification/api/NotificationController.java`

7. ✅ **FeedbackSubmissionController** - Removed delete prefix
   - `DELETE /delete/{id}` → `DELETE /{id}`
   - File: `src/main/java/com/danielagapov/spawn/analytics/api/FeedbackSubmissionController.java`

---

## Frontend Changes Required

### Overview
All backend endpoints have been updated to follow REST conventions. The following frontend changes are now required in both the iOS and Web applications.

### iOS App Changes (`Spawn-App-iOS-SwiftUI/`)

#### 1. Chat Messages API (5 endpoints) - HIGH PRIORITY
**File(s) to update**: Search for `chatMessages` in API service files
- `POST /api/v1/chatMessages` → `POST /api/v1/chat-messages`
- `DELETE /api/v1/chatMessages/{id}` → `DELETE /api/v1/chat-messages/{id}`
- `POST /api/v1/chatMessages/{chatMessageId}/likes/{userId}` → `POST /api/v1/chat-messages/{chatMessageId}/likes/{userId}`
- `GET /api/v1/chatMessages/{chatMessageId}/likes` → `GET /api/v1/chat-messages/{chatMessageId}/likes`
- `DELETE /api/v1/chatMessages/{chatMessageId}/likes/{userId}` → `DELETE /api/v1/chat-messages/{chatMessageId}/likes/{userId}`

**Note**: These endpoints are marked as deprecated/not currently used on mobile. Update them for future implementation.

#### 2. Activity Feed API (2 endpoints) - HIGH PRIORITY (CRITICAL)
**File(s) to update**: Main feed view, activity participation toggle
- `GET /api/v1/activities/feedActivities/{requestingUserId}` → `GET /api/v1/activities/feed-activities/{requestingUserId}`
- `PUT /api/v1/activities/{ActivityId}/toggleStatus/{userId}` → `PUT /api/v1/activities/{activityId}/toggle-status/{userId}`

**Impact**: These are core features heavily used in the app.

#### 3. User Profile API (2 endpoints) - MEDIUM PRIORITY
**File(s) to update**: Profile update, profile picture upload
- `PATCH /api/v1/users/update-pfp/{id}` → `PATCH /api/v1/users/{id}/profile-picture`
- `PATCH /api/v1/users/update/{id}` → `PATCH /api/v1/users/{id}`

#### 4. Block User API (2 endpoints) - MEDIUM PRIORITY
**File(s) to update**: Block/unblock user functionality
- `POST /api/v1/blocked-users/block` → `POST /api/v1/blocked-users`
- `DELETE /api/v1/blocked-users/unblock?blockerId=...&blockedId=...` → `DELETE /api/v1/blocked-users/{blockerId}/{blockedId}`

**Note**: The unblock endpoint now uses path parameters instead of query parameters.

#### 5. Push Notifications API (2 endpoints) - MEDIUM PRIORITY
**File(s) to update**: Device token registration
- `POST /api/v1/notifications/device-tokens/register` → `POST /api/v1/notifications/device-tokens`
- `DELETE /api/v1/notifications/device-tokens/unregister` → `DELETE /api/v1/notifications/device-tokens`

**Note**: Both endpoints still accept the token in the request body.

---

### Web App Changes (`spawn-app/src/`)

#### 1. Beta Access Sign Up (4 endpoints) - HIGH PRIORITY
**File(s) to update**: Landing page beta sign-up form, Admin panel
- `GET /api/v1/betaAccessSignUp/emails` → `GET /api/v1/beta-access-sign-up/emails`
- `GET /api/v1/betaAccessSignUp/records` → `GET /api/v1/beta-access-sign-up/records`
- `PUT /api/v1/betaAccessSignUp/{id}/emailed` → `PUT /api/v1/beta-access-sign-up/{id}/emailed`
- `POST /api/v1/betaAccessSignUp` → `POST /api/v1/beta-access-sign-up`

**Impact**: Affects landing page and admin panel functionality.

#### 2. Chat Messages API (5 endpoints) - MEDIUM PRIORITY
Same changes as iOS app (see above). If web app uses chat functionality.

#### 3. Activity Feed API (2 endpoints) - MEDIUM PRIORITY
Same changes as iOS app (see above). If web app displays activities.

#### 4. User Profile API (2 endpoints) - LOW PRIORITY
Same changes as iOS app (see above). If web app has profile editing.

#### 5. Admin Panel - Feedback Management (1 endpoint)
**File(s) to update**: Admin feedback management
- `DELETE /api/v1/feedback/delete/{id}` → `DELETE /api/v1/feedback/{id}`

---

### Frontend Implementation Checklist

#### iOS App
- [ ] Update chat message API calls (5 endpoints)
- [ ] Update feed activities endpoint (CRITICAL)
- [ ] Update participation toggle endpoint (CRITICAL)
- [ ] Update profile picture upload endpoint
- [ ] Update user profile update endpoint
- [ ] Update block user endpoint
- [ ] Update unblock user endpoint (switch from query params to path params)
- [ ] Update device token registration endpoint
- [ ] Update device token unregistration endpoint

#### Web App
- [ ] Update beta access sign-up endpoints (4 endpoints)
- [ ] Update admin panel beta access management
- [ ] Update feedback deletion endpoint (admin panel)
- [ ] Update chat/activity endpoints if used in web app

#### Testing
- [ ] Test all affected features on iOS
- [ ] Test all affected features on web
- [ ] Verify admin panel functionality
- [ ] Test error handling for all updated endpoints
- [ ] Verify backward compatibility if using deprecation period

---

### Migration Notes

1. **Breaking Changes**: All 18 endpoint changes are breaking changes requiring coordinated frontend updates.

2. **Priority Order**:
   - **Phase 1 (CRITICAL)**: Activity feed endpoints on iOS (most used feature)
   - **Phase 2 (HIGH)**: Beta access sign-up on Web, user profile endpoints
   - **Phase 3 (MEDIUM)**: Block user, notifications, feedback deletion

3. **Testing Strategy**:
   - Update and test iOS feed functionality first (highest impact)
   - Then update web beta sign-up functionality
   - Finally update remaining features

4. **URL Encoding Note**: The notification device token endpoints keep the token in the request body to avoid potential URL encoding issues with special characters.

5. **Path Parameter Change**: The unblock user endpoint now uses path parameters (`/{blockerId}/{blockedId}`) instead of query parameters. Update both the URL construction and parameter passing in frontend code.

---

## Impact Analysis

### Breaking Changes by Issue Type

#### camelCase to kebab-case Changes (High Priority)
**Total Endpoints Affected**: 11 endpoints across 3 controllers

- **ChatMessageController**: 5 endpoints
  - All chat message operations in iOS and Web apps
  - Includes message likes functionality
- **BetaAccessSignUpController**: 4 endpoints
  - Beta sign-up form on website
  - Admin panel beta access management
- **ActivityController**: 2 endpoints
  - Main feed activities endpoint (heavily used)
  - Participation toggle (core feature)

**Frontend Impact**: HIGH - These are core features used throughout the application

#### Action Verb Removals (Medium Priority)
**Total Endpoints Affected**: 7 endpoints across 4 controllers

- **UserController**: 2 endpoints
  - Profile picture updates
  - User profile updates
- **BlockedUserController**: 2 endpoints
  - Block/unblock user functionality
- **NotificationController**: 2 endpoints
  - Push notification device registration
- **FeedbackSubmissionController**: 1 endpoint
  - Admin feedback deletion

**Frontend Impact**: MEDIUM - Important features but more isolated

---

## Additional Notes

- **AuthController** was excluded from violations as authentication/authorization endpoints often use action verbs by convention (e.g., `/login`, `/register`, `/refresh-token`, `/verify-email`). These are industry-standard naming patterns for auth operations.
- **CacheController's** `/clear-calendar-caches` endpoint could be considered a special case as it's an admin utility endpoint rather than a resource CRUD operation. Admin utility endpoints may justify action verbs.
- The `BlockedUserController` issues are the most impactful of the action verb issues as they affect both naming and parameter passing conventions (query params vs path params)
- **FriendRequestController's** use of `?friendRequestAction=accept/reject` with PUT is acceptable as it's updating the state of the friend request resource
- **camelCase violations** are more critical than action verb violations because they violate fundamental web standards, not just REST best practices
- Path parameter names like `{userId}` and `{activityId}` correctly use camelCase - only the URL path segments themselves should use kebab-case

---

## Recommended Implementation Order

### Phase 1: High Priority - URL Path Casing (4 issues, 11 endpoints)
1. **ChatMessageController** - Change base path from `chatMessages` to `chat-messages`
   - Update: 5 endpoints
   - Frontend Files: iOS message views, Web chat components
   
2. **BetaAccessSignUpController** - Change base path from `betaAccessSignUp` to `beta-access-sign-up`
   - Update: 4 endpoints
   - Frontend Files: Web landing page, Admin panel
   
3. **ActivityController** - Fix 2 endpoint paths
   - `feedActivities` → `feed-activities`
   - `{ActivityId}/toggleStatus` → `{activityId}/toggle-status`
   - Frontend Files: iOS feed view (critical), Web activity components

### Phase 2: Medium Priority - Action Verb Removal (7 issues)
4. **UserController** - Remove redundant update prefixes
5. **BlockedUserController** - Remove block/unblock actions, use path params
6. **NotificationController** - Remove register/unregister actions
7. **FeedbackSubmissionController** - Remove delete prefix

### Estimated Frontend Changes
- **iOS App**: ~15 API call sites
- **Web App**: ~10 API call sites
- **Admin Panel**: ~5 API call sites

---

## References

- Original Issue Discovery: `API_DISCREPANCIES.md`
- REST API Best Practices: [Microsoft REST API Guidelines](https://github.com/microsoft/api-guidelines/blob/vNext/Guidelines.md)
- HTTP Method Definitions: [RFC 9110](https://www.rfc-editor.org/rfc/rfc9110.html#name-methods)
- URL Naming Conventions: [Google API Design Guide](https://cloud.google.com/apis/design/naming_convention)
- kebab-case Standard: [RFC 3986 URI Generic Syntax](https://www.rfc-editor.org/rfc/rfc3986)
