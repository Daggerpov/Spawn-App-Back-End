# Spawn App - Database Entities Summary

Generated: October 22, 2025

## Quick Reference Table

| Entity | Type | Purpose | Key Relationships |
|--------|------|---------|------------------|
| **User** | Core | Main user account with authentication and profile | Creator of Activities, ActivityTypes; Participant in Activities; Has Friendships |
| **Activity** | Core | Event/activity that users create and participate in | Created by User; Has Location; Based on ActivityType; Has ActivityUsers, ChatMessages |
| **ActivityType** | Core | Template for activities with associated friends | Created by User; Has many AssociatedFriends (Users) |
| **Location** | Core | Geographic location for activities | Owned by Activity (one-to-one) |
| **ActivityUser** | Junction | Tracks participation status in activities | Links Activity ↔ User with status (invited/participating/declined) |
| **Friendship** | Social | Symmetric friendship between two users | Links User ↔ User (bidirectional) |
| **FriendRequest** | Social | Pending friend request | Links sender User → receiver User (one-way) |
| **BlockedUser** | Social | User blocking for privacy/safety | Links blocker User → blocked User |
| **ChatMessage** | Communication | Messages in activity chats | Sent by User; Belongs to Activity; Can have ChatMessageLikes |
| **ChatMessageLikes** | Junction | Likes on chat messages | Links User ↔ ChatMessage |
| **NotificationPreferences** | Settings | User notification settings | One-to-one with User |
| **DeviceToken** | System | Push notification tokens | Many-to-one with User |
| **ShareLink** | System | Human-readable share codes | References Activity or User profile via targetId |
| **EmailVerification** | Auth | Email verification workflow | One-to-one with User |
| **UserIdExternalIdMap** | Auth | OAuth provider mapping | One-to-one with User; Maps to Google/Apple |
| **ReportedContent** | Moderation | Content/user reports | Reported by User; Targets content owned by User |
| **FeedbackSubmission** | Support | User feedback and bug reports | Optional many-to-one with User |
| **BetaAccessSignUp** | Marketing | Beta program signups | Standalone (no FK to User) |
| **UserInterest** | Profile | User interests | Many-to-one with User |
| **UserSocialMedia** | Profile | Social media links | One-to-one with User |

## Entity Count by Category

### Core Entities (4)
- User
- Activity
- ActivityType
- Location

### Junction Tables (3)
- ActivityUser
- ChatMessageLikes
- ActivityType_AssociatedFriends

### Social Features (3)
- Friendship
- FriendRequest
- BlockedUser

### Communication (2)
- ChatMessage
- ChatMessageLikes (also junction)

### Authentication & Security (2)
- EmailVerification
- UserIdExternalIdMap

### User Profile (2)
- UserInterest
- UserSocialMedia

### System & Infrastructure (3)
- DeviceToken
- ShareLink
- NotificationPreferences

### Moderation & Support (3)
- ReportedContent
- FeedbackSubmission
- BetaAccessSignUp

**Total: 20 entities** (including junction tables)

## Relationship Cardinality Summary

### One-to-One Relationships
- User ↔ NotificationPreferences
- User ↔ EmailVerification
- User ↔ UserIdExternalIdMap
- User ↔ UserSocialMedia
- Activity → Location (cascade delete on location)

### One-to-Many Relationships
- User → Activities (as creator)
- User → ActivityTypes (as creator)
- User → ChatMessages (as sender)
- User → FriendRequests (as sender/receiver)
- User → DeviceTokens
- User → BlockedUser records (as blocker/blocked)
- User → UserInterests
- User → FeedbackSubmissions
- Activity → ChatMessages
- ActivityType → Activity (optional, can be null)

### Many-to-Many Relationships
- User ↔ Activity (via ActivityUser junction table)
- User ↔ ChatMessage (via ChatMessageLikes junction table)
- User ↔ User (via Friendship - symmetric)
- ActivityType ↔ User (via ActivityType_AssociatedFriends - for friend associations)

## Composite Keys

### ActivityUser
- Primary Key: `(activityId, userId)`
- Both are foreign keys

### ChatMessageLikes
- Primary Key: `(chatMessageId, userId)`
- Both are foreign keys

### ActivityType_AssociatedFriends
- Primary Key: `(activityTypeId, userId)`
- Both are foreign keys

## Unique Constraints

### User
- `username` (nullable unique)
- `email` (nullable unique)
- `phoneNumber` (nullable unique)

### ActivityType
- `(creatorId, orderNum)` - ensures ordering per creator

### Friendship
- `(userAId, userBId)` - prevents duplicate friendships

### FriendRequest
- `(senderId, receiverId)` - prevents duplicate requests

### BlockedUser
- `(blockerId, blockedId)` - prevents duplicate blocks

### ShareLink
- `shareCode` - ensures unique share codes

### EmailVerification
- `verificationCode` - unique verification codes
- `email` - unique email per verification

### BetaAccessSignUp
- `email` - unique email signups

### DeviceToken
- `token` - unique device tokens

### NotificationPreferences
- `userId` - one preference set per user

### UserSocialMedia
- `userId` - one social media record per user

### UserIdExternalIdMap
- `userId` - one external ID per user

## Indexes

### User
- `idx_name` on `name` column

### Friendship
- `idx_user_a_id` on `userAId`
- `idx_user_b_id` on `userBId`

### ShareLink
- `idx_share_code` on `shareCode` (unique)
- `idx_target_id_type` on `(targetId, type)`
- `idx_expires_at` on `expiresAt`

## Cascade Delete Behavior

### User Deletion Cascades To:
- All Activities created by the user
- All ActivityTypes created by the user
- All ActivityUser records (participations)
- All ChatMessages sent by the user
- All ChatMessageLikes by the user
- All FriendRequests (sent or received)
- All Friendships
- All DeviceTokens
- All BlockedUser records (as blocker or blocked)
- NotificationPreferences
- EmailVerification
- UserIdExternalIdMap
- UserInterests
- UserSocialMedia

### Activity Deletion Cascades To:
- All ActivityUser records (participations)
- All ChatMessages in the activity
- The associated Location

### ChatMessage Deletion Cascades To:
- All ChatMessageLikes on that message

### ActivityType Deletion:
- Sets `activityTypeId` to NULL in Activity (does not delete activities)
- Cascades to ActivityType_AssociatedFriends relationships

## Nullable Foreign Keys

These FKs can be null:
- `Activity.activityTypeId` - Activity can exist without a type
- `EmailVerification.userId` - Verification can exist before user assignment
- `ReportedContent.reporterId` - Preserves report even if reporter deletes account
- `FeedbackSubmission.fromUserId` - Preserves feedback even if user deletes account

## Timestamp Fields

### createdAt (Instant/OffsetDateTime)
- Activity
- Friendship
- FriendRequest
- ShareLink
- UserInterest
- EmailVerification (lastSendAttemptAt, nextSendAttemptAt, etc.)
- ReportedContent (timeReported)
- FeedbackSubmission (submittedAt)
- BetaAccessSignUp (signedUpAt)

### lastUpdated (Instant)
- User
- Activity
- UserSocialMedia

### Combined (createdAt + lastUpdated)
- Activity: createdAt, lastUpdated
- User: dateCreated, lastUpdated

## Special Features

### Rate Limiting (EmailVerification)
- Tracks send attempts and check attempts
- Has `nextSendAttemptAt` and `nextCheckAttemptAt` for rate limiting

### Soft References (ShareLink)
- Uses `targetId` (UUID) instead of FK to reference Activity or User
- `type` enum indicates whether it's an ACTIVITY or PROFILE link
- Can have expiration via `expiresAt`

### OAuth Integration (UserIdExternalIdMap)
- Maps external provider IDs (Google, Apple) to internal User IDs
- Uses String as PK for the external provider's ID format

### Reporting System (ReportedContent)
- Generic reporting system using `contentId` + `contentType` enum
- Can report USER, ACTIVITY, or CHAT_MESSAGE
- Tracks resolution status and report type

## Enum Reference

All enum types used across entities:

1. **UserStatus** (User)
2. **ParticipationStatus** (ActivityUser)
3. **DeviceType** (DeviceToken)
4. **ShareLinkType** (ShareLink)
5. **EntityType** (ReportedContent)
6. **ResolutionStatus** (ReportedContent)
7. **ReportType** (ReportedContent)
8. **FeedbackType** (FeedbackSubmission)
9. **FeedbackStatus** (FeedbackSubmission)
10. **OAuthProvider** (UserIdExternalIdMap)

See the main ERD diagram for enum value details.

