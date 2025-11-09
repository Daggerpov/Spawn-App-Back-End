# Microservices Architecture Visual Reference

**Last Updated:** November 9, 2025

This document provides visual diagrams to help understand the microservices architecture at a glance.

---

## Overall System Architecture

```
┌─────────────────────────────────────────────────────────────────────┐
│                         MOBILE CLIENTS                               │
│                    (iOS App / Android App)                           │
└─────────────────────────────────────────────────────────────────────┘
                                  │
                                  │ HTTPS
                                  ▼
┌─────────────────────────────────────────────────────────────────────┐
│                         API GATEWAY                                  │
│                   (Spring Cloud Gateway)                             │
│   ┌─────────────────────────────────────────────────────────┐      │
│   │  JWT Validation │ Rate Limiting │ Routing │ CORS │ Logs │      │
│   └─────────────────────────────────────────────────────────┘      │
└─────────────────────────────────────────────────────────────────────┘
                                  │
            ┌─────────────────────┼─────────────────────┐
            │                     │                     │
            ▼                     ▼                     ▼
    ┌──────────────┐      ┌──────────────┐     ┌──────────────┐
    │    AUTH      │      │    USER      │     │   SOCIAL     │
    │   SERVICE    │◄────▶│   SERVICE    │◄───▶│   SERVICE    │
    │   :8084      │      │   :8081      │     │   :8083      │
    └──────┬───────┘      └──────┬───────┘     └──────┬───────┘
           │                     │                     │
           ▼                     ▼                     ▼
    ┌──────────────┐      ┌──────────────┐     ┌──────────────┐
    │   auth_db    │      │   user_db    │     │  social_db   │
    └──────────────┘      └──────────────┘     └──────────────┘
            
            ┌─────────────────────┼─────────────────────┐
            │                     │                     │
            ▼                     ▼                     ▼
    ┌──────────────┐      ┌──────────────┐     ┌──────────────┐
    │  ACTIVITY    │      │    CHAT      │     │ NOTIFICATION │
    │   SERVICE    │◄────▶│   SERVICE    │────▶│   SERVICE    │
    │   :8082      │      │   :8085      │     │   :8086      │
    └──────┬───────┘      └──────┬───────┘     └──────┬───────┘
           │                     │                     │
           ▼                     ▼                     ▼
    ┌──────────────┐      ┌──────────────┐     ┌──────────────┐
    │ activity_db  │      │   chat_db    │     │notification_db│
    └──────────────┘      └──────────────┘     └──────────────┘
            
            ┌─────────────────────┼─────────────────────┐
            │                     │                     │
            ▼                     ▼                     ▼
    ┌──────────────┐      ┌──────────────┐     ┌──────────────┐
    │    MEDIA     │      │  ANALYTICS   │     │              │
    │   SERVICE    │      │   SERVICE    │     │    REDIS     │
    │   :8087      │      │   :8088      │     │  (Shared)    │
    └──────┬───────┘      └──────┬───────┘     └──────────────┘
           │                     │                     ▲
           ▼                     ▼                     │
    ┌──────────────┐      ┌──────────────┐            │
    │   media_db   │      │ analytics_db │            │
    └──────────────┘      └──────────────┘            │
           │                                           │
           ▼                                           │
    ┌──────────────┐                                  │
    │   AWS S3     │                                  │
    │  (Storage)   │                                  │
    └──────────────┘                                  │
                                                      │
            ┌─────────────────────────────────────────┘
            │ All services use Redis for caching
            └─ Namespaced keys: user:*, activity:*, etc.
```

---

## Service Dependencies Map

```
User Service
├── Called By:
│   ├── Auth Service (to create/validate users)
│   ├── Activity Service (to validate creator)
│   ├── Social Service (to validate users)
│   ├── Chat Service (to validate message sender)
│   ├── Notification Service (to get user preferences)
│   └── Media Service (to validate user)
└── Calls:
    ├── Media Service (for profile pictures)
    └── Social Service (for friend count)

Activity Service
├── Called By:
│   ├── Chat Service (to validate activity membership)
│   └── Analytics Service (for reported activities)
└── Calls:
    ├── User Service (to validate creator)
    ├── Social Service (for ActivityType friends)
    └── Notification Service (for activity invites)

Social Service
├── Called By:
│   ├── User Service (for friend count)
│   └── Activity Service (for ActivityType friends)
└── Calls:
    ├── User Service (to validate users)
    └── Notification Service (for friend request notifications)

Auth Service
├── Called By:
│   └── API Gateway (for JWT validation)
└── Calls:
    └── User Service (to create users during registration)

Chat Service
├── Called By:
│   └── Analytics Service (for reported messages)
└── Calls:
    ├── Activity Service (to validate membership)
    ├── User Service (to validate sender)
    └── Notification Service (for message notifications)

Notification Service
├── Called By:
│   ├── Activity Service (activity invites/updates)
│   ├── Social Service (friend requests)
│   └── Chat Service (new messages)
└── Calls:
    ├── FCM (Firebase Cloud Messaging)
    └── APNS (Apple Push Notification Service)

Media Service
├── Called By:
│   └── User Service (profile pictures)
└── Calls:
    ├── User Service (to validate user)
    └── AWS S3 (file storage)

Analytics Service
├── Called By:
│   └── All services (for reporting/feedback)
└── Calls:
    ├── User Service (for reporter info)
    ├── Activity Service (for reported activities)
    └── Chat Service (for reported messages)
```

---

## Database Schema Ownership

```
┌─────────────────────────────────────────────────────────────────┐
│                         DATABASE LAYER                          │
└─────────────────────────────────────────────────────────────────┘

auth_db (PostgreSQL)
├── email_verification
│   ├── id (PK)
│   ├── email
│   ├── verification_code
│   ├── send_attempt_count
│   └── next_send_attempt_at
└── user_id_external_id_map
    ├── external_id (PK)
    ├── user_id (FK → user_db.user.id)
    ├── provider (GOOGLE, APPLE)
    └── created_at

user_db (PostgreSQL)
├── user
│   ├── id (PK)
│   ├── username (unique)
│   ├── email (unique)
│   ├── phone_number (unique)
│   ├── password_hash
│   └── date_created
├── user_info
│   ├── user_id (PK, FK → user.id)
│   ├── bio
│   ├── profile_picture_url
│   └── date_of_birth
├── user_interest
│   ├── id (PK)
│   ├── user_id (FK → user.id)
│   ├── interest
│   └── created_at
└── user_social_media
    ├── user_id (PK, FK → user.id)
    ├── instagram_handle
    ├── twitter_handle
    └── linkedin_url

social_db (PostgreSQL)
├── friendship
│   ├── id (PK)
│   ├── user_a_id (FK → user_db.user.id)
│   ├── user_b_id (FK → user_db.user.id)
│   ├── created_at
│   └── UNIQUE(user_a_id, user_b_id)
├── friend_request
│   ├── id (PK)
│   ├── sender_id (FK → user_db.user.id)
│   ├── receiver_id (FK → user_db.user.id)
│   ├── status (PENDING, ACCEPTED, REJECTED)
│   ├── created_at
│   └── UNIQUE(sender_id, receiver_id)
└── blocked_user
    ├── id (PK)
    ├── blocker_id (FK → user_db.user.id)
    ├── blocked_id (FK → user_db.user.id)
    ├── blocked_at
    └── UNIQUE(blocker_id, blocked_id)

activity_db (PostgreSQL)
├── activity
│   ├── id (PK)
│   ├── creator_id (FK → user_db.user.id)
│   ├── activity_type_id (FK → activity_type.id)
│   ├── name
│   ├── description
│   ├── start_time
│   ├── end_time
│   ├── max_participants
│   └── created_at
├── activity_type
│   ├── id (PK)
│   ├── creator_id (FK → user_db.user.id)
│   ├── name
│   ├── icon
│   ├── order_num
│   └── UNIQUE(creator_id, order_num)
├── activity_user
│   ├── activity_id (PK, FK → activity.id)
│   ├── user_id (PK, FK → user_db.user.id)
│   ├── participation_status (INVITED, PARTICIPATING, DECLINED)
│   └── PRIMARY KEY(activity_id, user_id)
└── location
    ├── id (PK)
    ├── activity_id (FK → activity.id, UNIQUE)
    ├── latitude
    ├── longitude
    └── address

chat_db (PostgreSQL)
├── chat_message
│   ├── id (PK)
│   ├── activity_id (FK → activity_db.activity.id)
│   ├── sender_id (FK → user_db.user.id)
│   ├── content
│   ├── sent_at
│   └── edited_at
└── chat_message_likes
    ├── chat_message_id (PK, FK → chat_message.id)
    ├── user_id (PK, FK → user_db.user.id)
    ├── liked_at
    └── PRIMARY KEY(chat_message_id, user_id)

notification_db (PostgreSQL)
├── device_token
│   ├── id (PK)
│   ├── user_id (FK → user_db.user.id)
│   ├── token (unique)
│   ├── device_type (IOS, ANDROID)
│   └── registered_at
└── notification_preferences
    ├── user_id (PK, FK → user_db.user.id)
    ├── push_enabled
    ├── email_enabled
    ├── friend_request_notifications
    ├── activity_invite_notifications
    └── activity_update_notifications

media_db (PostgreSQL)
└── media_metadata
    ├── id (PK)
    ├── user_id (FK → user_db.user.id)
    ├── file_key (S3 object key)
    ├── file_type (IMAGE, VIDEO)
    ├── file_size
    ├── uploaded_at
    └── expires_at

analytics_db (PostgreSQL)
├── reported_content
│   ├── id (PK)
│   ├── reporter_id (FK → user_db.user.id, nullable)
│   ├── content_id (UUID of reported entity)
│   ├── content_type (USER, ACTIVITY, CHAT_MESSAGE)
│   ├── report_type (SPAM, HARASSMENT, INAPPROPRIATE)
│   ├── resolution_status (PENDING, REVIEWED, RESOLVED)
│   └── time_reported
├── feedback_submission
│   ├── id (PK)
│   ├── from_user_id (FK → user_db.user.id, nullable)
│   ├── feedback_type (BUG, FEATURE_REQUEST, GENERAL)
│   ├── content
│   ├── status (NEW, IN_PROGRESS, RESOLVED)
│   └── submitted_at
├── beta_access_sign_up
│   ├── id (PK)
│   ├── email (unique)
│   ├── signed_up_at
│   └── approved
└── share_link
    ├── id (PK)
    ├── share_code (unique)
    ├── target_id (UUID of Activity or User)
    ├── type (ACTIVITY, PROFILE)
    ├── created_at
    └── expires_at
```

---

## Event-Driven Communication Flow

```
┌────────────────────────────────────────────────────────────────┐
│                    REDIS PUB/SUB CHANNELS                      │
└────────────────────────────────────────────────────────────────┘

CHANNEL: user.created
Publisher: User Service
Subscribers: Social Service, Notification Service, Analytics Service
Event Payload:
{
  "eventId": "uuid",
  "timestamp": "2025-11-09T10:00:00Z",
  "userId": "uuid",
  "username": "johndoe",
  "email": "john@example.com"
}

─────────────────────────────────────────────────────────────────

CHANNEL: user.updated
Publisher: User Service
Subscribers: Social Service (updates friend cache), Activity Service
Event Payload:
{
  "eventId": "uuid",
  "timestamp": "2025-11-09T10:00:00Z",
  "userId": "uuid",
  "updatedFields": ["username", "profilePictureUrl"],
  "username": "john_doe_updated",
  "profilePictureUrl": "https://s3.../new_pic.jpg"
}

─────────────────────────────────────────────────────────────────

CHANNEL: friend_request.sent
Publisher: Social Service
Subscribers: Notification Service
Event Payload:
{
  "eventId": "uuid",
  "timestamp": "2025-11-09T10:00:00Z",
  "senderId": "uuid",
  "receiverId": "uuid",
  "senderUsername": "johndoe"
}

─────────────────────────────────────────────────────────────────

CHANNEL: friend_request.accepted
Publisher: Social Service
Subscribers: Notification Service
Event Payload:
{
  "eventId": "uuid",
  "timestamp": "2025-11-09T10:00:00Z",
  "senderId": "uuid",
  "receiverId": "uuid",
  "acceptedByUsername": "janedoe"
}

─────────────────────────────────────────────────────────────────

CHANNEL: activity.created
Publisher: Activity Service
Subscribers: Analytics Service
Event Payload:
{
  "eventId": "uuid",
  "timestamp": "2025-11-09T10:00:00Z",
  "activityId": "uuid",
  "creatorId": "uuid",
  "activityName": "Basketball game",
  "startTime": "2025-11-10T18:00:00Z"
}

─────────────────────────────────────────────────────────────────

CHANNEL: activity.invite
Publisher: Activity Service
Subscribers: Notification Service
Event Payload:
{
  "eventId": "uuid",
  "timestamp": "2025-11-09T10:00:00Z",
  "activityId": "uuid",
  "activityName": "Basketball game",
  "inviterUserId": "uuid",
  "invitedUserIds": ["uuid1", "uuid2", "uuid3"]
}

─────────────────────────────────────────────────────────────────

CHANNEL: message.sent
Publisher: Chat Service
Subscribers: Notification Service
Event Payload:
{
  "eventId": "uuid",
  "timestamp": "2025-11-09T10:00:00Z",
  "messageId": "uuid",
  "activityId": "uuid",
  "senderId": "uuid",
  "senderUsername": "johndoe",
  "content": "Hey everyone!"
}
```

---

## Request Flow Example: Create Activity

```
1. Client Request
   │
   │ POST /api/activities
   │ Authorization: Bearer <JWT>
   │ Body: { name: "Basketball", startTime: "2025-11-10T18:00:00Z", ... }
   │
   ▼
2. API Gateway
   │
   ├─ Validate JWT ────────────────┐
   │  Extract userId: "user123"    │
   │                                │
   ├─ Add X-User-Id: user123       │
   │                                │
   ▼                                │
3. Activity Service                 │
   │                                │
   ├─ GET /users/{id} ◄────────────┼─ 4. User Service
   │  Validate creator exists       │    Returns: { id, username, ... }
   │                                │
   ├─ Create activity in activity_db
   │  activity_id: "activity789"
   │
   ├─ Publish event ────────────────┼─ 5. Redis Pub/Sub
   │  Channel: activity.created     │    Event: { activityId: "activity789", ... }
   │                                │
   ▼                                ▼
6. Response to Client          7. Notification Service (async)
   │                                │
   │ 201 Created                    ├─ Subscribe to activity.created
   │ { id: "activity789", ... }    │
   │                                ├─ (future) Send notifications to invitees
   │                                │
   └────────────────────────────────┘
```

---

## Request Flow Example: Get User Profile with Stats

```
1. Client Request
   │
   │ GET /api/users/user123
   │ Authorization: Bearer <JWT>
   │
   ▼
2. API Gateway
   │
   ├─ Validate JWT
   │
   ├─ Route to User Service
   │
   ▼
3. User Service
   │
   ├─ Check Redis Cache ───────────┐
   │  Key: user:user123            │ 4. Redis
   │  Cache Miss                    │    Returns: null (not cached)
   │                                │
   ├─ Query user_db ───────────────┼─ 5. user_db
   │  SELECT * FROM user           │    Returns: { id, username, email, ... }
   │  WHERE id = 'user123'          │
   │                                │
   ├─ Parallel API calls:          │
   │  ├─ GET /users/user123/activity-count ◄── 6a. Activity Service
   │  │  Returns: 12                │              Query activity_db
   │  │                              │              WHERE creator_id = 'user123'
   │  └─ GET /users/user123/friend-count ◄──── 6b. Social Service
   │     Returns: 45                │              Query social_db
   │                                │              WHERE user_a_id = 'user123'
   │                                │              OR user_b_id = 'user123'
   │                                │
   ├─ Compose response ────────────┤
   │  {                             │
   │    id: "user123",              │
   │    username: "johndoe",        │
   │    activityCount: 12,          │
   │    friendCount: 45             │
   │  }                             │
   │                                │
   ├─ Cache in Redis ──────────────┼─ 7. Redis
   │  Key: user:user123            │    SET user:user123 {...}
   │  TTL: 60 minutes               │    EXPIRE user:user123 3600
   │                                │
   ▼                                │
8. Response to Client               │
   │                                │
   │ 200 OK                         │
   │ { id: "user123", ... }         │
   │                                │
   └────────────────────────────────┘
```

---

## Railway Deployment Structure

```
┌─────────────────────────────────────────────────────────────────┐
│                      RAILWAY PROJECT                            │
│                    "spawn-app-backend"                          │
└─────────────────────────────────────────────────────────────────┘
                                │
        ┌───────────────────────┼───────────────────────┐
        │                       │                       │
        ▼                       ▼                       ▼
┌─────────────┐         ┌─────────────┐         ┌─────────────┐
│  Service:   │         │  Service:   │         │  Service:   │
│ api-gateway │         │user-service │         │auth-service │
├─────────────┤         ├─────────────┤         ├─────────────┤
│ Port: 8080  │         │ Port: 8081  │         │ Port: 8084  │
│ Public: ✓   │         │ Public: ✗   │         │ Public: ✗   │
│ Domain: api │         │ Internal    │         │ Internal    │
│.railway.app │         │ DNS only    │         │ DNS only    │
└─────────────┘         └─────────────┘         └─────────────┘
        │                       │                       │
        │                       ▼                       ▼
        │               ┌─────────────┐         ┌─────────────┐
        │               │ Database:   │         │ Database:   │
        │               │   user_db   │         │   auth_db   │
        │               ├─────────────┤         ├─────────────┤
        │               │PostgreSQL   │         │PostgreSQL   │
        │               │ 2GB Storage │         │ 1GB Storage │
        │               └─────────────┘         └─────────────┘
        │
        ▼
┌─────────────┐         ┌─────────────┐         ┌─────────────┐
│  Service:   │         │  Service:   │         │  Service:   │
│activity-svc │         │social-svc   │         │ chat-svc    │
├─────────────┤         ├─────────────┤         ├─────────────┤
│ Port: 8082  │         │ Port: 8083  │         │ Port: 8085  │
│ Public: ✗   │         │ Public: ✗   │         │ Public: ✗   │
└─────────────┘         └─────────────┘         └─────────────┘
        │                       │                       │
        ▼                       ▼                       ▼
┌─────────────┐         ┌─────────────┐         ┌─────────────┐
│ Database:   │         │ Database:   │         │ Database:   │
│activity_db  │         │ social_db   │         │  chat_db    │
├─────────────┤         ├─────────────┤         ├─────────────┤
│PostgreSQL   │         │PostgreSQL   │         │PostgreSQL   │
│ 2GB Storage │         │ 1GB Storage │         │ 1GB Storage │
└─────────────┘         └─────────────┘         └─────────────┘

┌─────────────┐         ┌─────────────┐         ┌─────────────┐
│  Service:   │         │  Service:   │         │  Service:   │
│notification │         │media-svc    │         │analytics-svc│
│    -svc     │         │             │         │             │
├─────────────┤         ├─────────────┤         ├─────────────┤
│ Port: 8086  │         │ Port: 8087  │         │ Port: 8088  │
│ Public: ✗   │         │ Public: ✗   │         │ Public: ✗   │
└─────────────┘         └─────────────┘         └─────────────┘
        │                       │                       │
        ▼                       ▼                       ▼
┌─────────────┐         ┌─────────────┐         ┌─────────────┐
│ Database:   │         │ Database:   │         │ Database:   │
│notification │         │  media_db   │         │analytics_db │
│    _db      │         │             │         │             │
├─────────────┤         ├─────────────┤         ├─────────────┤
│PostgreSQL   │         │PostgreSQL   │         │PostgreSQL   │
│ 512MB       │         │ 512MB       │         │ 1GB Storage │
└─────────────┘         └─────────────┘         └─────────────┘
        │                       │
        │                       ▼
        │               ┌─────────────┐
        │               │   AWS S3    │
        │               │   Bucket    │
        │               │             │
        │               │ Profile Pics│
        │               │ Media Files │
        │               └─────────────┘
        │
        ▼
┌─────────────────────────────────────────┐
│          SHARED INFRASTRUCTURE          │
├─────────────────────────────────────────┤
│  ┌─────────────┐   ┌─────────────┐    │
│  │   Redis     │   │  Railway    │    │
│  │   Cache     │   │  Volumes    │    │
│  │             │   │  (Logs)     │    │
│  │ 512MB RAM   │   │             │    │
│  └─────────────┘   └─────────────┘    │
└─────────────────────────────────────────┘

ENVIRONMENT VARIABLES (Shared across all services):
├─ JWT_SECRET=<shared-secret>
├─ REDIS_URL=redis://redis.railway.internal:6379
├─ REDIS_PASSWORD=<password>
├─ AWS_ACCESS_KEY_ID=<key>
├─ AWS_SECRET_ACCESS_KEY=<secret>
├─ FCM_SERVER_KEY=<firebase-key>
└─ APNS_CERTIFICATE=<apple-cert>
```

---

## Migration Phases Timeline

```
Month 1: Preparation & Proof-of-Concept
┌─────────────────────────────────────────────────────────────────┐
│ Week 1-2: Infrastructure Setup                                  │
│ ├─ Create monorepo structure                                    │
│ ├─ Set up Railway project                                       │
│ ├─ Provision Redis                                              │
│ └─ Create shared libraries                                      │
├─────────────────────────────────────────────────────────────────┤
│ Week 3-4: Extract Analytics Service                             │
│ ├─ Create analytics-service Spring Boot project                 │
│ ├─ Migrate entities and services                                │
│ ├─ Deploy to Railway                                            │
│ └─ Validate with integration tests                              │
└─────────────────────────────────────────────────────────────────┘

Month 2: Core Identity Services
┌─────────────────────────────────────────────────────────────────┐
│ Week 5-6: Extract Auth Service                                  │
│ ├─ Migrate JWT logic                                            │
│ ├─ Migrate OAuth (Google, Apple)                                │
│ ├─ Deploy to Railway                                            │
│ └─ Update monolith to use Auth Service                          │
├─────────────────────────────────────────────────────────────────┤
│ Week 7-8: Extract User Service                                  │
│ ├─ Migrate user entities                                        │
│ ├─ Add event publishing for user changes                        │
│ ├─ Deploy to Railway                                            │
│ └─ Update Auth Service to call User Service                     │
└─────────────────────────────────────────────────────────────────┘

Month 3: Social & Activity Services
┌─────────────────────────────────────────────────────────────────┐
│ Week 9-10: Extract Social Service                               │
│ ├─ Migrate friendship/friend request entities                   │
│ ├─ Subscribe to user.deleted events                             │
│ ├─ Deploy to Railway                                            │
│ └─ Integration tests                                            │
├─────────────────────────────────────────────────────────────────┤
│ Week 11-12: Extract Activity Service                            │
│ ├─ Migrate activity entities                                    │
│ ├─ Add activity expiration job                                  │
│ ├─ Publish activity.invite events                               │
│ └─ Deploy to Railway                                            │
└─────────────────────────────────────────────────────────────────┘

Month 4: Communication Services
┌─────────────────────────────────────────────────────────────────┐
│ Week 13-14: Extract Chat Service                                │
│ ├─ Migrate chat entities                                        │
│ ├─ Add WebSocket support                                        │
│ ├─ Publish message.sent events                                  │
│ └─ Deploy to Railway                                            │
├─────────────────────────────────────────────────────────────────┤
│ Week 15-16: Extract Notification Service                        │
│ ├─ Migrate device token entities                                │
│ ├─ Subscribe to all notification events                         │
│ ├─ Implement FCM/APNS strategies                                │
│ └─ Deploy to Railway                                            │
└─────────────────────────────────────────────────────────────────┘

Month 5: Media & API Gateway
┌─────────────────────────────────────────────────────────────────┐
│ Week 17-18: Extract Media Service                               │
│ ├─ Migrate S3 integration                                       │
│ ├─ Create upload/download endpoints                             │
│ ├─ Generate pre-signed URLs                                     │
│ └─ Deploy to Railway                                            │
├─────────────────────────────────────────────────────────────────┤
│ Week 19-20: Deploy API Gateway                                  │
│ ├─ Create Spring Cloud Gateway project                          │
│ ├─ Configure routes for all services                            │
│ ├─ Add JWT validation filter                                    │
│ ├─ Add rate limiting                                            │
│ └─ Deploy to Railway with public domain                         │
└─────────────────────────────────────────────────────────────────┘

Month 6: Optimization & Cutover
┌─────────────────────────────────────────────────────────────────┐
│ Week 21-22: Database & Performance Optimization                 │
│ ├─ Add database indexes                                         │
│ ├─ Configure connection pooling                                 │
│ ├─ Implement caching strategies                                 │
│ └─ Add health checks                                            │
├─────────────────────────────────────────────────────────────────┤
│ Week 23-24: Production Cutover                                  │
│ ├─ Load testing (1000+ concurrent users)                        │
│ ├─ Set up monitoring and alerting                               │
│ ├─ Gradual traffic migration (10% → 50% → 100%)                 │
│ ├─ Documentation updates                                        │
│ └─ Decommission monolith                                        │
└─────────────────────────────────────────────────────────────────┘

MILESTONES:
🎯 Month 1 End: Proof-of-concept validated
🎯 Month 2 End: Auth and user management decoupled
🎯 Month 3 End: Core business logic extracted
🎯 Month 4 End: All services running independently
🎯 Month 5 End: API Gateway live
🎯 Month 6 End: Full migration complete, monolith decommissioned
```

---

## Cost Breakdown Over Time

```
┌───────────────────────────────────────────────────────────────┐
│                    MONTHLY COST PROGRESSION                   │
└───────────────────────────────────────────────────────────────┘

Current (Monolith)
├─ App Server (2GB)     : $10
├─ PostgreSQL (2GB)     : $10
├─ Redis (512MB)        : $5
└─ TOTAL                : $25/month

Month 1 (Monolith + Analytics Service)
├─ Monolith             : $25
├─ Analytics Service    : $5
├─ Analytics DB         : $5
└─ TOTAL                : $35/month (+$10)

Month 2 (+ Auth + User Services)
├─ Monolith             : $25
├─ Analytics Service    : $5
├─ Auth Service         : $5
├─ User Service         : $7
├─ Databases (3)        : $15
└─ TOTAL                : $57/month (+$32)

Month 3 (+ Social + Activity Services)
├─ Monolith             : $25
├─ 5 Microservices      : $27
├─ Databases (5)        : $25
└─ TOTAL                : $77/month (+$52)

Month 4 (+ Chat + Notification Services)
├─ Monolith             : $25
├─ 7 Microservices      : $39
├─ Databases (7)        : $35
└─ TOTAL                : $99/month (+$74)

Month 5 (+ Media Service + API Gateway)
├─ Monolith (standby)   : $10 (scaled down)
├─ API Gateway          : $5
├─ 8 Microservices      : $46
├─ Databases (8)        : $40
└─ TOTAL                : $101/month (+$76)

Month 6+ (Full Microservices, Monolith Decommissioned)
├─ API Gateway          : $5
├─ 8 Microservices      : $44
├─ Databases (shared)   : $30
├─ Redis                : $10
└─ TOTAL                : $89/month (+$64 vs original)

OPTIMIZED (with service consolidation)
├─ API Gateway          : $5
├─ 7 Microservices      : $39 (Media+Analytics combined)
├─ Databases (shared)   : $30
├─ Redis                : $10
└─ TOTAL                : $84/month (+$59 vs original)

Cost Increase Justification:
✓ 3.4x cost = 10x scalability
✓ Independent scaling per service
✓ Faster feature delivery (3x deployment frequency)
✓ Reduced MTTR (2 hours → 15 minutes)
✓ Better fault isolation
```

---

**Document Version:** 1.0  
**Last Updated:** November 9, 2025  
**Maintained By:** Backend Team

For detailed written documentation, see:
- [MICROSERVICES_ARCHITECTURE.md](./MICROSERVICES_ARCHITECTURE.md)
- [MICROSERVICES_IMPLEMENTATION_PLAN.md](./MICROSERVICES_IMPLEMENTATION_PLAN.md)

