# API Discrepancies and REST Convention Issues

## Date: December 13, 2025

This document tracks API endpoint discrepancies and deviations from REST conventions discovered during comprehensive code review.

---

## Executive Summary

Following initial fixes to ReportController, a comprehensive review of all 18 backend controllers was conducted (see [CONTROLLER_REST_CONVENTION_ISSUES.md](./CONTROLLER_REST_CONVENTION_ISSUES.md) for full details).

**Key Findings:**
- **4 High Priority Issues**: camelCase in URL paths affecting 11 endpoints
- **7 Medium Priority Issues**: Redundant action verbs affecting 7 endpoints  
- **6 Controllers** require updates
- **12 Controllers** already follow REST conventions

---

## ReportController Issues

### ✅ Fixed Issues

1. **Non-RESTful endpoint naming**
   - **OLD**: `POST /api/v1/reports/create`
   - **NEW**: `POST /api/v1/reports`
   - **Reasoning**: In REST conventions, POST to a collection endpoint implicitly means "create"

2. **Non-RESTful endpoint naming**
   - **OLD**: `GET /api/v1/reports/fetch`
   - **NEW**: `GET /api/v1/reports`
   - **Reasoning**: GET to a collection endpoint implicitly means "fetch/retrieve"

3. **Redundant "fetch" prefix in sub-resources**
   - **OLD**: `GET /api/v1/reports/fetch/reporter/{reporterId}`
   - **NEW**: `GET /api/v1/reports/reporter/{reporterId}`
   - **Reasoning**: The HTTP verb (GET) already indicates fetch/retrieval

4. **Redundant "fetch" prefix in sub-resources**
   - **OLD**: `GET /api/v1/reports/fetch/content-owner/{contentOwnerId}`
   - **NEW**: `GET /api/v1/reports/content-owner/{contentOwnerId}`
   - **Reasoning**: The HTTP verb (GET) already indicates fetch/retrieval

5. **Missing Update Report Status endpoint**
   - **NEW**: `PUT /api/v1/reports/{reportId}?resolution={resolution}`
   - **Status**: ✅ **IMPLEMENTED**
   - **Implementation Details**:
     - Accepts resolution as a query parameter
     - Validates resolution against enum values: PENDING, FALSE, BAN, SUSPENSION, WARN
     - Returns 400 Bad Request for invalid resolution values
     - Returns 404 Not Found if report doesn't exist
     - Returns updated report DTO on success

6. **Missing Delete Report endpoint**
   - **NEW**: `DELETE /api/v1/reports/{reportId}`
   - **Status**: ✅ **IMPLEMENTED**
   - **Implementation Details**:
     - Deletes report by ID
     - Returns 404 Not Found if report doesn't exist
     - Returns 204 No Content on successful deletion

---

## System-Wide REST Convention Issues

See [CONTROLLER_REST_CONVENTION_ISSUES.md](./CONTROLLER_REST_CONVENTION_ISSUES.md) for complete analysis.

### High Priority Issues - URL Path Casing ❌

**Issue**: camelCase used in URL paths instead of kebab-case (web standard)  
**Impact**: 11 endpoints across 3 controllers

| Controller | Current | Correct | Endpoints Affected |
|------------|---------|---------|-------------------|
| ChatMessageController | `/chatMessages` | `/chat-messages` | 5 |
| BetaAccessSignUpController | `/betaAccessSignUp` | `/beta-access-sign-up` | 4 |
| ActivityController | `/feedActivities`, `/toggleStatus` | `/feed-activities`, `/toggle-status` | 2 |

### Medium Priority Issues - Redundant Action Verbs

**Issue**: Action verbs in paths when HTTP method already conveys the action  
**Impact**: 7 endpoints across 4 controllers

| Controller | Current | Correct |
|------------|---------|---------|
| UserController | `PATCH /update-pfp/{id}` | `PATCH /{id}/profile-picture` |
| UserController | `PATCH /update/{id}` | `PATCH /{id}` |
| BlockedUserController | `POST /block` | `POST /blocked-users` |
| BlockedUserController | `DELETE /unblock?...` | `DELETE /{blockerId}/{blockedId}` |
| NotificationController | `POST /device-tokens/register` | `POST /device-tokens` |
| NotificationController | `DELETE /device-tokens/unregister` | `DELETE /device-tokens/{token}` |
| FeedbackSubmissionController | `DELETE /delete/{id}` | `DELETE /{id}` |

---

## Recommended Actions

### ✅ Completed Actions
1. **✅ Implemented missing endpoints**:
   - `PUT /api/v1/reports/{reportId}` - Update report resolution status
   - `DELETE /api/v1/reports/{reportId}` - Delete a report
   - Added corresponding service layer methods to IReportContentService and ReportContentService
   - Fixed import statements for Logger and exception classes
2. **✅ Comprehensive controller review completed** (18 controllers analyzed)
3. **✅ Updated frontend to use actual ResolutionStatus enum values** (no custom mappings)

### 🔴 High Priority - TODO
Fix camelCase issues (violates web standards):
1. **ChatMessageController** - Change base path to `/chat-messages` (5 endpoints)
2. **BetaAccessSignUpController** - Change base path to `/beta-access-sign-up` (4 endpoints)  
3. **ActivityController** - Fix `/feedActivities` → `/feed-activities` and `/toggleStatus` → `/toggle-status` (2 endpoints)

**Rationale**: kebab-case is the standard for URL paths. Mixed casing creates confusion and potential URL-encoding issues.

### 🟡 Medium Priority - Recommended
Remove redundant action verbs (7 endpoints):
- Maintain consistency with fixed ReportController
- Follow industry-standard REST practices
- Improve API documentation clarity

### Low Priority
3. **Consider query parameter consolidation**:
   - The endpoints `/reporter/{reporterId}` and `/content-owner/{contentOwnerId}` could potentially be consolidated into the main GET endpoint with query parameters:
     - `GET /api/v1/reports?reporterId={id}`
     - `GET /api/v1/reports?contentOwnerId={id}`
   - However, the current approach is also valid and may be more semantically clear

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

#### URL Path Casing
- **URL paths**: Use **kebab-case** (lowercase with hyphens)
  - ✅ `/chat-messages`, `/beta-access-sign-up`, `/feed-activities`
  - ❌ `/chatMessages`, `/betaAccessSignUp`, `/feedActivities`
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
- ❌ `DELETE /resources/delete/{id}`
- ❌ `POST /resources/register`
- ❌ `DELETE /resources/unregister`

The HTTP verb already conveys the action; redundant path segments add verbosity without value.

#### Incorrect Casing
- ❌ `GET /chatMessages` (use `/chat-messages`)
- ❌ `GET /betaAccessSignUp` (use `/beta-access-sign-up`)
- ❌ `GET /feedActivities` (use `/feed-activities`)

---

## Resolution Status Implementation

### Backend ResolutionStatus Enum

The backend uses the following enum values (defined in `com.danielagapov.spawn.shared.util.ResolutionStatus`):

| Status | Description |
|--------|-------------|
| `PENDING` | Report is awaiting review (default for new reports) |
| `FALSE` | Report was determined to be false/invalid |
| `BAN` | Report resulted in banning the content/user |
| `SUSPENSION` | Report resulted in temporary suspension |
| `WARN` | Report resulted in a warning |

### Frontend Implementation

Both frontends now use the actual backend enum values directly:

#### Web Frontend (`spawn/src/components/admin/ReportsTab.jsx`)
- ✅ Dropdown for selecting specific action: FALSE, BAN, SUSPENSION, WARN
- ✅ No custom mapping (sends actual enum values to backend)
- ✅ Displays actual backend resolution status values

#### iOS Frontend (`Spawn-App-iOS-SwiftUI/Models/DTOs/ReportedContentDTO.swift`)
- ✅ Updated `ResolutionStatus` enum to match backend exactly
- ✅ Removed custom statuses (RESOLVED, DISMISSED)
- ✅ Added all backend statuses (FALSE, BAN, SUSPENSION, WARN)
- ✅ UI updated to display and color-code all resolution types

### Migration Notes

**Previous Implementation (REMOVED):**
- Custom frontend statuses 'RESOLVED' and 'REJECTED'
- Backend mapping: 'REJECTED' → `ResolutionStatus.FALSE`
- Generic "Approve"/"Reject" buttons without specific action selection

**Current Implementation:**
- Direct use of backend enum values
- Admin selects specific moderation action (FALSE, BAN, SUSPENSION, WARN)
- No mapping layer needed
- Consistent status values across all systems

---

## Additional Notes

- The frontend updates have been applied to match the normalized backend endpoints:
  - **Web Frontend** (`spawn/src/components/admin/ReportsTab.jsx`) ✅
  - **iOS Frontend** (`Spawn-App-iOS-SwiftUI/...Config/ReadOperationConfig.swift` and `WriteOperationConfig.swift`) ✅
- All missing endpoints have been implemented ✅
- Service layer methods (e.g., `fileReport`, `getFetchReportsByFilters`) maintain their original names as they are internal implementation details
- Frontend now uses actual ResolutionStatus enum values without custom mappings ✅

---

## Implementation Checklist

For future REST convention fixes:

- [ ] Backend controller endpoint paths updated
- [ ] Service layer methods remain unchanged (internal implementation details)
- [ ] Integration tests updated
- [ ] Frontend code updated:
  - [ ] Web app (`spawn-app/src/`)
  - [ ] iOS app (`Spawn-App-iOS-SwiftUI/`)
- [ ] API documentation updated
- [ ] Deprecation warnings added to old endpoints (if using gradual migration)
- [ ] Database queries remain unaffected (these are API-level changes only)

---

## References

- Comprehensive Controller Review: [CONTROLLER_REST_CONVENTION_ISSUES.md](./CONTROLLER_REST_CONVENTION_ISSUES.md)
- REST API Best Practices: [Microsoft REST API Guidelines](https://github.com/microsoft/api-guidelines/blob/vNext/Guidelines.md)
- HTTP Method Definitions: [RFC 9110](https://www.rfc-editor.org/rfc/rfc9110.html#name-methods)
- URL Naming Conventions: [Google API Design Guide](https://cloud.google.com/apis/design/naming_convention)
- kebab-case Standard: [RFC 3986 URI Generic Syntax](https://www.rfc-editor.org/rfc/rfc3986)
