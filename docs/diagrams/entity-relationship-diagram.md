# Spawn App - Entity Relationship Diagram

Generated: October 22, 2025

## Core Entities Overview

This diagram shows all database entities and their relationships in the Spawn App back-end.

## Entity Relationship Diagram (Mermaid)

```mermaid
erDiagram
    User ||--o{ Activity : creates
    User ||--o{ ActivityUser : "participates in"
    User ||--o{ ActivityType : creates
    User ||--o{ ChatMessage : sends
    User ||--o{ ChatMessageLikes : likes
    User ||--o{ FriendRequest : "sends/receives"
    User ||--o{ Friendship : "has friendship with"
    User ||--o| NotificationPreferences : has
    User ||--o{ DeviceToken : "has devices"
    User ||--o{ BlockedUser : "blocks/is blocked"
    User ||--o| EmailVerification : has
    User ||--o{ ReportedContent : "reports/owns"
    User ||--o| UserIdExternalIdMap : "maps to external"
    User ||--o{ FeedbackSubmission : submits
    User ||--o{ UserInterest : has
    User ||--o| UserSocialMedia : has
    
    Activity ||--|| Location : "has location"
    Activity ||--o| ActivityType : "based on"
    Activity ||--o{ ActivityUser : "has participants"
    Activity ||--o{ ChatMessage : "has messages"
    
    ActivityType ||--o{ ActivityType_AssociatedFriends : "has friends"
    ActivityType_AssociatedFriends }o--|| User : references
    
    ChatMessage ||--o{ ChatMessageLikes : "has likes"
    
    User {
        UUID id PK
        String username UK
        String profilePictureUrlString
        String phoneNumber UK
        String name
        String bio
        String email UK
        String password
        Date dateCreated
        Instant lastUpdated
        UserStatus status
        Boolean hasCompletedOnboarding
    }
    
    Activity {
        UUID id PK
        String title
        OffsetDateTime startTime
        OffsetDateTime endTime
        String icon
        String colorHexCode
        UUID activityTypeId FK
        UUID locationId FK
        String note
        Integer participantLimit
        UUID creatorId FK
        Instant createdAt
        Instant lastUpdated
        String clientTimezone
    }
    
    ActivityType {
        UUID id PK
        String title
        UUID creatorId FK
        Integer orderNum
        String icon
        Boolean isPinned
    }
    
    Location {
        UUID id PK
        String name
        Double latitude
        Double longitude
    }
    
    ActivityUser {
        UUID activityId PK_FK
        UUID userId PK_FK
        ParticipationStatus status
    }
    
    ChatMessage {
        UUID id PK
        String content
        Instant timestamp
        UUID userId FK
        UUID activityId FK
    }
    
    ChatMessageLikes {
        UUID chatMessageId PK_FK
        UUID userId PK_FK
    }
    
    Friendship {
        UUID id PK
        UUID userAId FK
        UUID userBId FK
        Instant createdAt
    }
    
    FriendRequest {
        UUID id PK
        UUID senderId FK
        UUID receiverId FK
        Instant createdAt
    }
    
    NotificationPreferences {
        Long id PK
        UUID userId FK_UK
        Boolean friendRequestsEnabled
        Boolean activityInvitesEnabled
        Boolean activityUpdatesEnabled
        Boolean chatMessagesEnabled
    }
    
    DeviceToken {
        UUID id PK
        String token UK
        DeviceType deviceType
        UUID userId FK
    }
    
    ShareLink {
        UUID id PK
        String shareCode UK
        ShareLinkType type
        UUID targetId
        Instant createdAt
        Instant expiresAt
    }
    
    BlockedUser {
        UUID id PK
        UUID blockerId FK
        UUID blockedId FK
        String reason
    }
    
    EmailVerification {
        UUID id PK
        Integer sendAttempts
        Instant lastSendAttemptAt
        Instant nextSendAttemptAt
        Integer checkAttempts
        Instant lastCheckAttemptAt
        Instant nextCheckAttemptAt
        String verificationCode UK
        String email UK
        Instant codeExpiresAt
        UUID userId FK
    }
    
    ReportedContent {
        UUID id PK
        UUID reporterId FK
        UUID contentId
        EntityType contentType
        OffsetDateTime timeReported
        ResolutionStatus resolution
        ReportType reportType
        String description
        UUID contentOwnerId FK
    }
    
    UserIdExternalIdMap {
        String id PK
        UUID userId FK_UK
        OAuthProvider provider
    }
    
    FeedbackSubmission {
        UUID id PK
        FeedbackType type
        UUID fromUserId FK
        String fromUserEmail
        OffsetDateTime submittedAt
        FeedbackStatus status
        String resolutionComment
        String message
        String imageUrl
    }
    
    BetaAccessSignUp {
        UUID id PK
        String email UK
        OffsetDateTime signedUpAt
        Boolean hasSubscribedToNewsletter
        Boolean hasBeenEmailed
    }
    
    UserInterest {
        UUID id PK
        UUID userId FK
        String interest
        Instant createdAt
    }
    
    UserSocialMedia {
        UUID id PK
        UUID userId FK_UK
        String whatsappNumber
        String instagramUsername
        Instant lastUpdated
    }
    
    ActivityType_AssociatedFriends {
        UUID activityTypeId PK_FK
        UUID userId PK_FK
    }
```

## Relationship Types Legend

- `||--o{` : One to many
- `||--||` : One to one (required on both sides)
- `||--o|` : One to zero or one
- `}o--||` : Many to one

## Key Constraints

- **PK**: Primary Key
- **FK**: Foreign Key
- **UK**: Unique Key
- **PK_FK**: Composite Primary Key that is also a Foreign Key

## Enums Reference

### UserStatus
- `EMAIL_VERIFIED`
- `USERNAME_AND_PHONE_NUMBER`
- `NAME_AND_PHOTO`
- `CONTACT_IMPORT`
- `ACTIVE`

### ParticipationStatus
- `INVITED`
- `PARTICIPATING`
- `DECLINED`

### DeviceType
- `IOS`
- `ANDROID`

### ShareLinkType
- `ACTIVITY`
- `PROFILE`

### EntityType
- `USER`
- `ACTIVITY`
- `CHAT_MESSAGE`

### ResolutionStatus
- `PENDING`
- `UNDER_REVIEW`
- `RESOLVED`
- `DISMISSED`

### ReportType
- `SPAM`
- `HARASSMENT`
- `BULLYING`
- `HATE_SPEECH`
- `NUDITY`
- `VIOLENCE`
- `OTHER`

### FeedbackType
- `BUG`
- `FEATURE_REQUEST`
- `GENERAL`

### FeedbackStatus
- `PENDING`
- `UNDER_REVIEW`
- `RESOLVED`
- `DISMISSED`

### OAuthProvider
- `GOOGLE`
- `APPLE`

## Entity Descriptions

### Core Entities

- **User**: Represents a Spawn app user with authentication and profile information
- **Activity**: The primary entity representing an event/activity that users can create and participate in
- **ActivityType**: Templates for activities with associated friends and custom icons
- **Location**: Geographic location for activities with coordinates and display name

### Participation & Social

- **ActivityUser**: Junction table managing activity participation status (invited/participating/declined)
- **Friendship**: Symmetric friendship relationship between two users
- **FriendRequest**: One-way friend request pending acceptance
- **BlockedUser**: Tracks blocked users for privacy and safety

### Communication

- **ChatMessage**: Messages within activity chats
- **ChatMessageLikes**: Tracks likes on chat messages
- **NotificationPreferences**: User-specific notification settings

### System & Support

- **DeviceToken**: Push notification tokens for mobile devices
- **ShareLink**: Human-readable share codes for activities and profiles
- **EmailVerification**: Email verification workflow tracking
- **UserIdExternalIdMap**: OAuth provider mapping (Google, Apple)
- **ReportedContent**: Content moderation and reporting system
- **FeedbackSubmission**: User feedback and bug reports
- **BetaAccessSignUp**: Beta program signup tracking

### User Profile Extensions

- **UserInterest**: User interests (one-to-many)
- **UserSocialMedia**: Social media links (WhatsApp, Instagram)

## Database Cascade Rules

### ON DELETE CASCADE
- Deleting a User cascades to: Activities, ActivityUsers, ChatMessages, FriendRequests, Friendships, DeviceTokens, BlockedUser records, UserInterests, UserSocialMedia, EmailVerification, UserIdExternalIdMap
- Deleting an Activity cascades to: ActivityUsers, ChatMessages
- Deleting a ChatMessage cascades to: ChatMessageLikes
- Deleting an ActivityType cascades to its AssociatedFriends relationships

### ON DELETE SET NULL
- Deleting an ActivityType sets Activity.activityTypeId to NULL
- Deleting a User (reporter) sets ReportedContent.reporterId to NULL
- Deleting a User sets FeedbackSubmission.fromUserId to NULL

### Notes on Cascade Behavior
- Activity-Location has CASCADE on Location side (location is owned by activity)
- NotificationPreferences, EmailVerification, UserSocialMedia, and UserIdExternalIdMap have CASCADE (owned by user)
- ReportedContent preserves reports even if reporter deletes account (SET NULL)

