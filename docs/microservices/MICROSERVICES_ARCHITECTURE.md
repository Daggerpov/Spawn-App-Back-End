# Spawn App - Microservices Architecture

**Last Updated:** November 9, 2025

## Table of Contents

- [Executive Summary](#executive-summary)
- [Current Monolithic Architecture](#current-monolithic-architecture)
- [Proposed Microservices Architecture](#proposed-microservices-architecture)
  - [Service Boundaries](#service-boundaries)
  - [Service Descriptions](#service-descriptions)
- [Data Architecture](#data-architecture)
  - [Database Strategy](#database-strategy)
  - [Data Ownership](#data-ownership)
- [Inter-Service Communication](#inter-service-communication)
- [Shared Concerns](#shared-concerns)
- [Benefits and Trade-offs](#benefits-and-trade-offs)
- [Migration Strategy](#migration-strategy)

---

## Executive Summary

> **⚠️ Important:** Before diving into this architecture, read [MICROSERVICES_DECISION_GUIDE.md](./MICROSERVICES_DECISION_GUIDE.md) to understand if microservices is the right choice for your current stage, including cost implications and alternatives like modular monolith.

This document outlines a strategy to decompose the Spawn App monolithic back-end into a microservices architecture. The proposed design organizes services around business capabilities and domain-driven design principles, resulting in **8 core microservices** that communicate via REST APIs and asynchronous messaging.

**Key Goals:**
- Improve scalability and independent deployment
- Enable team autonomy and parallel development
- Optimize resource allocation based on service-specific needs
- Maintain data consistency while embracing eventual consistency where appropriate

---

## Current Monolithic Architecture

The current Spring Boot application is organized as a layered monolith with:

- **20 core entities** across multiple domains
- **60+ service classes** handling various business logic
- **Single PostgreSQL/MySQL database** with shared schema
- **Single deployment unit** with all features bundled together

### Current Domains (from Entity Analysis)

1. **Core User Management** - User, UserInfo, UserInterest, UserSocialMedia
2. **Activity Management** - Activity, ActivityType, ActivityUser, Location
3. **Social Features** - Friendship, FriendRequest, BlockedUser
4. **Communication** - ChatMessage, ChatMessageLikes
5. **Authentication** - EmailVerification, UserIdExternalIdMap, JWT handling
6. **Notifications** - DeviceToken, NotificationPreferences, Push notifications
7. **Support & Moderation** - ReportedContent, FeedbackSubmission
8. **Infrastructure** - ShareLink, BetaAccessSignUp, S3, Caching

---

## Proposed Microservices Architecture

### Service Boundaries

The decomposition follows the **Strangler Fig pattern** and domain-driven design principles:

```
┌─────────────────────────────────────────────────────────────┐
│                      API Gateway                             │
│          (Authentication, Routing, Rate Limiting)            │
└─────────────────────────────────────────────────────────────┘
                            │
        ┌───────────────────┼───────────────────┐
        │                   │                   │
        ▼                   ▼                   ▼
┌──────────────┐    ┌──────────────┐    ┌──────────────┐
│   User       │    │   Activity   │    │   Social     │
│   Service    │    │   Service    │    │   Service    │
└──────────────┘    └──────────────┘    └──────────────┘
        │                   │                   │
        ▼                   ▼                   ▼
┌──────────────┐    ┌──────────────┐    ┌──────────────┐
│     Auth     │    │    Chat      │    │ Notification │
│   Service    │    │   Service    │    │   Service    │
└──────────────┘    └──────────────┘    └──────────────┘
        │                   │
        ▼                   ▼
┌──────────────┐    ┌──────────────┐
│   Media      │    │  Analytics   │
│   Service    │    │  & Support   │
└──────────────┘    └──────────────┘
```

### Service Descriptions

#### 1. User Service
**Port:** 8081  
**Responsibility:** User profile and identity management

**Entities:**
- User
- UserInfo
- UserInterest
- UserSocialMedia

**Key Features:**
- User registration and profile management
- User search (fuzzy search with Jaro-Winkler)
- User statistics and preferences
- Profile picture management
- Phone number and email management

**Dependencies:**
- Auth Service (for authentication validation)
- Media Service (for profile pictures)
- Social Service (for friend count data)

**Database:** `user_db`

---

#### 2. Activity Service
**Port:** 8082  
**Responsibility:** Activity/event creation and management

**Entities:**
- Activity
- ActivityType
- ActivityUser (participation)
- Location
- CalendarActivity

**Key Features:**
- Create, update, delete activities
- Activity type templates with associated friends
- Activity participation management
- Location-based activity search
- Activity expiration and cleanup
- Calendar integration

**Dependencies:**
- User Service (to validate users and creators)
- Social Service (for friend associations in ActivityTypes)
- Chat Service (activity has chat)
- Notification Service (for activity invites/updates)

**Database:** `activity_db`

---

#### 3. Social Service
**Port:** 8083  
**Responsibility:** Social connections and relationships

**Entities:**
- Friendship
- FriendRequest
- BlockedUser

**Key Features:**
- Friend request management (send, accept, reject)
- Friendship management
- User blocking/unblocking
- Friend list retrieval
- Contact cross-reference for finding friends

**Dependencies:**
- User Service (to validate users)
- Notification Service (for friend request notifications)

**Database:** `social_db`

---

#### 4. Auth Service
**Port:** 8084  
**Responsibility:** Authentication and authorization

**Entities:**
- EmailVerification
- UserIdExternalIdMap (OAuth)

**Key Features:**
- Email/password authentication
- OAuth 2.0 (Google, Apple Sign-In)
- JWT token generation and validation
- Email verification
- Rate limiting for verification attempts

**Dependencies:**
- User Service (to create/validate users)
- Email Service (for verification emails)

**Database:** `auth_db`

**Note:** This service is **stateless** for JWT validation but stores OAuth mappings and verification codes.

---

#### 5. Chat Service
**Port:** 8085  
**Responsibility:** Activity-based messaging

**Entities:**
- ChatMessage
- ChatMessageLikes

**Key Features:**
- Send/receive messages in activity chats
- Message likes/reactions
- Message pagination and history
- Real-time messaging (WebSocket support)

**Dependencies:**
- Activity Service (to validate activity membership)
- User Service (to validate message senders)
- Notification Service (for message notifications)

**Database:** `chat_db`

---

#### 6. Notification Service
**Port:** 8086  
**Responsibility:** Push notifications and preferences

**Entities:**
- DeviceToken
- NotificationPreferences

**Key Features:**
- Push notification delivery (FCM for Android, APNS for iOS)
- Device token management
- Notification preferences management
- Event-driven notification handling (friend requests, activity invites, etc.)

**Dependencies:**
- User Service (for user preferences)
- External: Firebase Cloud Messaging (FCM), Apple Push Notification Service (APNS)

**Database:** `notification_db`

**Architecture:** Event-driven with message queue (RabbitMQ/Redis Pub/Sub)

---

#### 7. Media Service
**Port:** 8087  
**Responsibility:** File storage and retrieval

**Key Features:**
- Profile picture upload/download
- Image optimization and resizing
- S3/Cloud storage integration
- Pre-signed URL generation
- Cache management for media URLs

**Dependencies:**
- External: AWS S3 or Railway's Volumes
- Redis for caching signed URLs

**Database:** Minimal (metadata only in `media_db`)

---

#### 8. Analytics & Support Service
**Port:** 8088  
**Responsibility:** Reporting, feedback, and analytics

**Entities:**
- ReportedContent
- FeedbackSubmission
- BetaAccessSignUp
- ShareLink

**Key Features:**
- Content reporting (users, activities, messages)
- Feedback submission and management
- Beta signup tracking
- Share link generation and validation
- Search analytics
- Share link cleanup

**Dependencies:**
- User Service (for reporter information)
- Activity Service (for reported activities)
- Chat Service (for reported messages)

**Database:** `analytics_db`

---

## Data Architecture

### Database Strategy

**Approach:** Database-per-Service with Shared Cache

Each microservice has its own database to ensure:
- **Data isolation** - No shared schemas
- **Independent scaling** - Databases can be sized differently
- **Deployment independence** - Schema migrations don't require cross-service coordination

### Data Ownership

| Service | Owns | Read-Only Access To |
|---------|------|---------------------|
| User Service | User profiles, interests, social media | None |
| Activity Service | Activities, types, locations, participations | User IDs (validates via User Service API) |
| Social Service | Friendships, friend requests, blocks | User IDs (validates via User Service API) |
| Auth Service | Email verifications, OAuth mappings | User IDs (creates users via User Service API) |
| Chat Service | Messages, likes | User IDs, Activity IDs (validates via respective services) |
| Notification Service | Device tokens, preferences | User IDs (validates via User Service API) |
| Media Service | Media metadata and storage paths | User IDs (validates via User Service API) |
| Analytics & Support | Reports, feedback, share links | User IDs, Activity IDs (for reporting context) |

### Handling Relationships Across Services

#### Strategy 1: Store IDs Only + API Calls
For **infrequent access** or **strong consistency requirements**:
- Store only the foreign entity ID (e.g., `userId`, `activityId`)
- Call the owning service's API when full entity data is needed

**Example:** When Activity Service needs to display activity creator's name:
```java
// In Activity Service
Activity activity = activityRepository.findById(id);
UserDTO creator = userServiceClient.getUserById(activity.getCreatorId());
return new ActivityWithCreatorDTO(activity, creator);
```

#### Strategy 2: Event-Driven Data Synchronization
For **frequently accessed data** or **eventual consistency tolerance**:
- Replicate minimal data locally (e.g., username, profile picture URL)
- Subscribe to change events from the owning service
- Update local copies asynchronously

**Example:** Social Service maintains a denormalized `friend_cache` table:
```sql
-- In social_db
CREATE TABLE friend_cache (
    user_id UUID PRIMARY KEY,
    username VARCHAR(255),
    profile_picture_url VARCHAR(500),
    last_synced_at TIMESTAMP
);
```

When User Service updates a username, it publishes:
```json
{
  "event": "user.profile.updated",
  "userId": "uuid",
  "username": "new_username",
  "profilePictureUrl": "https://..."
}
```

Social Service consumes this event and updates its cache.

#### Strategy 3: API Composition at Gateway
For **read-heavy operations** that need data from multiple services:
- API Gateway aggregates data from multiple services
- Uses asynchronous/parallel calls to minimize latency
- Caches responses in Redis

**Example:** Get user profile with activity count and friend count:
```
Client → API Gateway
         ├─→ User Service: GET /users/{id}
         ├─→ Activity Service: GET /users/{id}/activity-count
         └─→ Social Service: GET /users/{id}/friend-count
         
API Gateway aggregates and returns combined response
```

---

## Inter-Service Communication

### Synchronous Communication (REST APIs)

**Use Cases:**
- User queries (GET requests)
- Commands requiring immediate feedback
- Health checks

**Implementation:**
- Spring Cloud OpenFeign for declarative REST clients
- Circuit breaker pattern with Resilience4j
- Timeouts: 3s for reads, 10s for writes

**Example:**
```java
@FeignClient(name = "user-service", url = "${user-service.url}")
public interface UserServiceClient {
    @GetMapping("/api/users/{id}")
    UserDTO getUserById(@PathVariable UUID id);
}
```

### Asynchronous Communication (Event-Driven)

**Use Cases:**
- Notifications
- Data synchronization
- Audit logging
- Background jobs

**Implementation Options:**

1. **Redis Pub/Sub** (Recommended for Railway)
   - Already in use for caching
   - Lightweight and fast
   - Good for simple event broadcasting

2. **RabbitMQ/CloudAMQP** (For production scaling)
   - Guaranteed delivery
   - Message persistence
   - Complex routing patterns

**Event Examples:**

```java
// User Service publishes
eventPublisher.publish("user.created", new UserCreatedEvent(userId, username));

// Social Service subscribes
@EventListener("user.created")
public void onUserCreated(UserCreatedEvent event) {
    // Initialize friend cache entry
}
```

```java
// Activity Service publishes
eventPublisher.publish("activity.invite", new ActivityInviteEvent(activityId, invitedUserIds));

// Notification Service subscribes
@EventListener("activity.invite")
public void onActivityInvite(ActivityInviteEvent event) {
    // Send push notifications
}
```

---

## Shared Concerns

### 1. API Gateway

**Responsibilities:**
- Routing requests to appropriate services
- JWT validation (shared with Auth Service)
- Rate limiting per user/IP
- Request logging and monitoring
- Response caching

**Technology:** Spring Cloud Gateway or Kong

### 2. Service Discovery

**Options:**
- **Hardcoded URLs** (for small deployments on Railway)
- **Spring Cloud Netflix Eureka** (for dynamic scaling)
- **Railway's internal DNS** (service-name.railway.internal)

### 3. Distributed Caching

**Current:** Redis is already in use

**Strategy:**
- Shared Redis cluster across all services
- Namespaced keys per service (e.g., `user:{userId}`, `activity:{activityId}`)
- TTL based on data volatility

### 4. Monitoring and Logging

**Distributed Tracing:**
- Spring Cloud Sleuth + Zipkin
- Trace IDs propagated across service calls

**Centralized Logging:**
- Logback with JSON formatting
- Ship logs to Railway's log aggregation
- Or use external: ELK stack (Elasticsearch, Logstash, Kibana)

**Metrics:**
- Spring Boot Actuator
- Prometheus + Grafana for visualization

### 5. Security

**Authentication Flow:**
1. Client → API Gateway with JWT
2. Gateway validates JWT with Auth Service (or caches public key)
3. Gateway forwards request with `X-User-Id` header
4. Downstream services trust the gateway (internal network)

**Service-to-Service Authentication:**
- Mutual TLS (mTLS) for production
- API keys for internal calls (Railway environment variables)

---

## Benefits and Trade-offs

### Benefits

✅ **Independent Scaling**
- Scale Activity Service during event creation peaks
- Scale Chat Service during high message volume
- Scale Notification Service independently for push notifications

✅ **Independent Deployment**
- Deploy User Service updates without touching Activity Service
- Reduce deployment risk and downtime

✅ **Technology Flexibility**
- Use WebSocket for real-time chat
- Use different databases (PostgreSQL for relational, MongoDB for messages if needed)

✅ **Team Autonomy**
- Different teams can own different services
- Clear boundaries reduce coordination overhead

✅ **Fault Isolation**
- If Chat Service goes down, activities can still be created
- Circuit breakers prevent cascading failures

### Trade-offs

❌ **Increased Complexity**
- More moving parts to monitor and debug
- Distributed tracing required
- Network latency between services

❌ **Data Consistency Challenges**
- No ACID transactions across services
- Eventual consistency requires careful design
- Compensating transactions for failures

❌ **Operational Overhead**
- More services to deploy and manage
- Need service discovery and load balancing
- More complex CI/CD pipelines

❌ **Development Complexity**
- Testing requires multiple services running
- Mock service clients for unit tests
- Integration tests become more complex

---

## Migration Strategy

See [MICROSERVICES_IMPLEMENTATION_PLAN.md](./MICROSERVICES_IMPLEMENTATION_PLAN.md) for the detailed step-by-step migration plan.

**High-Level Approach:**

1. **Extract vertical slices** - Start with least-coupled services (Analytics, Media)
2. **Implement service communication** - Add Feign clients and event publishing
3. **Migrate data** - Split database schemas per service
4. **Add gateway** - Route traffic through API Gateway
5. **Optimize** - Add caching, circuit breakers, monitoring

**Estimated Timeline:** 3-6 months for full migration with a small team

---

## Conclusion

The proposed microservices architecture balances the benefits of service independence with the pragmatic constraints of a small team and Railway's infrastructure. By following domain-driven design and the strangler fig pattern, we can gradually evolve from a monolith while maintaining system stability.

The architecture prioritizes:
- **Developer experience** - Clear boundaries and familiar technologies (Spring Boot, REST, Redis)
- **Operational simplicity** - Leverage Railway's deployment capabilities
- **Incremental adoption** - Migrate one service at a time without big-bang rewrites

---

**Next Steps:** Review [MICROSERVICES_IMPLEMENTATION_PLAN.md](./MICROSERVICES_IMPLEMENTATION_PLAN.md) for the concrete implementation roadmap.

