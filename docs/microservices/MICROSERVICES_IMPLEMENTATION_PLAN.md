# Spawn App - Selective Microservices Implementation Plan for Railway

**Last Updated:** November 9, 2025

## Table of Contents

- [Overview](#overview)
- [Selective Microservices Approach](#selective-microservices-approach)
- [Railway-Specific Considerations](#railway-specific-considerations)
- [Phase 1: Preparation & Infrastructure](#phase-1-preparation--infrastructure)
- [Phase 2: Extract Core Services](#phase-2-extract-core-services)
  - [2.1: Auth Service](#21-auth-service)
  - [2.2: Activity Service](#22-activity-service)
  - [2.3: Chat Service (WebSocket)](#23-chat-service-websocket)
  - [2.4: User Service (Optional)](#24-user-service-optional)
- [Phase 3: API Gateway & Service Mesh](#phase-3-api-gateway--service-mesh)
- [Phase 4: Optimization & Monitoring](#phase-4-optimization--monitoring)
- [Rollback Strategy](#rollback-strategy)
- [Testing Strategy](#testing-strategy)
- [Cost Implications](#cost-implications)

---

## Overview

> **Updated Approach:** This plan now reflects a **selective microservices strategy** focused on extracting 3-4 core services for learning purposes while keeping other domains in a modular monolith.

**Key Decisions:**

1. ✅ **Extract for learning:** Auth, Activity, Chat (WebSocket), optionally User
2. ✅ **Keep in monolith:** Social, Notification, Media, Analytics
3. ✅ **Database:** MySQL for all services (familiar and reliable)
4. ⚠️ **Optional:** Cassandra for Chat if message volume >100k/day
5. ✅ **Focus:** WebSocket implementation for real-time chat

**Timeline:** 3-5 months  
**Approach:** Strangler Fig Pattern for selective services  
**Risk:** Low to Medium (smaller scope = lower risk than full migration)

---

## Selective Microservices Approach

### Services to Extract

#### Priority 1: Auth Service (Weeks 1-6)
**Why:** Highest anticipated traffic, clean domain boundaries, foundational for other services

**Scope:**
- OAuth (Google, Apple Sign-In)
- JWT generation and validation
- Email verification
- User authentication flows

**Database:** MySQL `auth_db`

#### Priority 2: Activity Service (Weeks 7-12)
**Why:** Highest anticipated traffic, core feature of the app

**Scope:**
- Activity CRUD operations
- Activity types and templates
- Location management
- Activity participation

**Database:** MySQL `activity_db`

#### Priority 3: Chat Service (Weeks 13-16)
**Why:** Learning opportunity for WebSockets, relatively isolated domain, can be scaled down

**Scope:**
- **WebSocket real-time messaging**
- REST API for message history
- Message reactions/likes
- Lightweight implementation

**Database:** MySQL `chat_db` (consider Cassandra if volume grows)

#### Optional: User Service (Weeks 17-20)
**Why:** Completes the core service extraction, enables full service independence

**Scope:**
- User profile management
- User search (fuzzy matching)
- User preferences
- Profile pictures

**Database:** MySQL `user_db`

### Services to Keep in Monolith

**Rationale:** Lower traffic, less complex, can be extracted later if needed

1. **Social Service** - Friendships, friend requests, blocking
2. **Notification Service** - Push notifications
3. **Media Service** - File storage (S3 integration)
4. **Analytics Service** - Reporting, feedback, share links

These remain in the monolith with clear module boundaries for potential future extraction.

---

## Railway-Specific Considerations

### Railway Platform Features

✅ **Built-in Service Discovery**
- Railway provides internal DNS: `service-name.railway.internal`
- Services can communicate without external service discovery tools

✅ **Environment Variables**
- Centralized configuration per service
- Secrets management built-in

✅ **MySQL Database Provisioning**
- **Using MySQL instead of PostgreSQL** (familiar and reliable for Spawn App)
- Easy MySQL provisioning per service
- Built-in connection pooling
- Shared instances possible with separate databases/schemas

✅ **Redis Support**
- Railway supports Redis Add-on
- Can be shared across services with namespacing

✅ **GitHub Integration**
- Automatic deployments from Git branches
- Monorepo support (multiple services in one repo)

### Railway Limitations

⚠️ **No Built-in API Gateway**
- Need to deploy Spring Cloud Gateway as a separate service

⚠️ **No Built-in Message Queue**
- Need to use Redis Pub/Sub or add CloudAMQP plugin

⚠️ **Regional Limitations**
- Services in same project should be in same region for low latency

⚠️ **Cost Per Service**
- Each service consumes resources independently
- Selective approach keeps costs manageable (~$73-85/month vs $105/month for full microservices)

---

## Phase 1: Preparation & Infrastructure

**Duration:** 2-3 weeks  
**Goal:** Set up foundation for microservices

### 1.1 Code Repository Restructuring

**Decision:** Monorepo vs. Multi-repo

**Recommended: Monorepo (for Railway)**

```
Spawn-App-Back-End/
├── services/
│   ├── user-service/
│   ├── activity-service/
│   ├── social-service/
│   ├── auth-service/
│   ├── chat-service/
│   ├── notification-service/
│   ├── media-service/
│   └── analytics-service/
├── shared/
│   ├── common-dtos/
│   ├── common-exceptions/
│   ├── common-utils/
│   └── event-schemas/
├── gateway/
│   └── api-gateway/
├── scripts/
│   └── deployment/
└── pom.xml (parent POM)
```

**Tasks:**
- [ ] Create parent `pom.xml` with shared dependencies
- [ ] Create `shared` module for common code (DTOs, exceptions, utils)
- [ ] Create skeleton Spring Boot projects for each service
- [ ] Set up Railway configuration files (`.railway` directory)

**Railway Configuration Example:**

Create `.railway/services.json`:
```json
{
  "services": [
    {
      "name": "user-service",
      "directory": "services/user-service",
      "startCommand": "java -jar target/user-service.jar"
    },
    {
      "name": "activity-service",
      "directory": "services/activity-service",
      "startCommand": "java -jar target/activity-service.jar"
    }
  ]
}
```

### 1.2 Set Up Shared Infrastructure on Railway

**Tasks:**

- [ ] **Provision Redis**
  - Railway Dashboard → Add Redis Plugin
  - Configure namespacing: `user:`, `activity:`, `social:`, etc.
  
- [ ] **Set Up Shared Environment Variables**
  - `JWT_SECRET` (shared across Auth Service and API Gateway)
  - `REDIS_URL`
  - `REDIS_PASSWORD`
  
- [ ] **Configure Logging**
  - Add structured JSON logging to all services
  - Railway automatically aggregates logs
  
- [ ] **Set Up Monitoring**
  - Enable Railway's built-in metrics
  - Consider adding: New Relic, Datadog, or Sentry for APM

### 1.3 Create Service Communication Layer

**Shared Library: `common-client` module**

```java
// services/shared/common-client/src/main/java/com/spawn/client/

@Configuration
public class FeignConfig {
    @Bean
    public RequestInterceptor requestInterceptor() {
        return requestTemplate -> {
            // Propagate trace IDs
            String traceId = MDC.get("traceId");
            if (traceId != null) {
                requestTemplate.header("X-Trace-Id", traceId);
            }
            
            // Propagate user context
            String userId = SecurityContextHolder.getContext()
                .getAuthentication().getName();
            if (userId != null) {
                requestTemplate.header("X-User-Id", userId);
            }
        };
    }
    
    @Bean
    public Retryer retryer() {
        return new Retryer.Default(100, 1000, 3);
    }
}

// Resilience4j Circuit Breaker
@Configuration
public class CircuitBreakerConfig {
    @Bean
    public CircuitBreakerRegistry circuitBreakerRegistry() {
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
            .failureRateThreshold(50)
            .waitDurationInOpenState(Duration.ofSeconds(30))
            .slidingWindowSize(10)
            .build();
        return CircuitBreakerRegistry.of(config);
    }
}
```

**Tasks:**
- [ ] Add Spring Cloud OpenFeign dependency
- [ ] Add Resilience4j for circuit breakers
- [ ] Create base Feign client configuration
- [ ] Create retry and timeout policies

### 1.4 Event Publishing Infrastructure

**Option 1: Redis Pub/Sub (Recommended for Start)**

```java
// services/shared/event-bus/src/main/java/com/spawn/events/

@Configuration
public class RedisPubSubConfig {
    @Bean
    RedisMessageListenerContainer container(RedisConnectionFactory factory) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(factory);
        return container;
    }
}

@Service
public class EventPublisher {
    private final RedisTemplate<String, Object> redisTemplate;
    
    public void publish(String channel, Object event) {
        redisTemplate.convertAndSend(channel, event);
    }
}

@Component
public class EventSubscriber {
    @Autowired
    private RedisMessageListenerContainer container;
    
    public void subscribe(String channel, MessageListener listener) {
        container.addMessageListener(listener, 
            new PatternTopic(channel));
    }
}
```

**Tasks:**
- [ ] Create event publishing library
- [ ] Define event schemas (JSON format)
- [ ] Create event subscribers
- [ ] Test pub/sub with Redis locally

---

## Phase 2: Extract Core Services

**Duration:** 12-16 weeks  
**Goal:** Extract Auth, Activity, Chat (WebSocket), and optionally User services

---

### 2.1: Auth Service

**Duration:** 4-6 weeks  
**Priority:** Highest (foundational for other services)

**Database Setup on Railway:**

1. **Provision MySQL Database**
   - Railway Dashboard → Add MySQL
   - Name: `auth-db`
   - Size: 1GB
   - Link to `auth-service`

2. **Migrate Tables**

```sql
-- Create these tables in auth-db:
CREATE DATABASE auth_db;
USE auth_db;

-- email_verification table
CREATE TABLE email_verification (
    id BINARY(16) PRIMARY KEY,
    user_id BINARY(16) NOT NULL,
    verification_code VARCHAR(255) NOT NULL,
    expiry_time TIMESTAMP NOT NULL,
    verified BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_user_id (user_id),
    INDEX idx_verification_code (verification_code)
);

-- user_id_external_id_map (OAuth mappings)
CREATE TABLE user_id_external_id_map (
    id BINARY(16) PRIMARY KEY,
    user_id BINARY(16) NOT NULL,
    external_id VARCHAR(255) NOT NULL,
    provider VARCHAR(50) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE INDEX idx_external_id_provider (external_id, provider),
    INDEX idx_user_id (user_id)
);
```

**Tasks:**
- [ ] Create `auth-service` Spring Boot project
- [ ] Copy entities: `EmailVerification`, `UserIdExternalIdMap`
- [ ] Copy repositories: `EmailVerificationRepository`, `UserIdExternalIdMapRepository`
- [ ] Copy services: `AuthenticationService`, `EmailVerificationService`, `OAuthService`
- [ ] Copy controllers: `/api/auth/login`, `/api/auth/register`, `/api/auth/oauth/*`
- [ ] Implement JWT generation and validation
- [ ] Add Feign client for User Service (to create/validate users)
- [ ] Configure application.properties with Railway MySQL URL
- [ ] Deploy to Railway as new service

**Railway Deployment:**

```bash
# In Railway Dashboard:
1. New Service → GitHub Repo
2. Root Directory: services/auth-service
3. Build Command: cd services/auth-service && mvn clean package
4. Start Command: java -Xmx1024m -jar target/auth-service.jar
5. Add Environment Variables:
   - DATABASE_URL (from auth-db MySQL)
   - REDIS_URL (shared)
   - JWT_SECRET (shared)
   - OAUTH_GOOGLE_CLIENT_ID
   - OAUTH_GOOGLE_CLIENT_SECRET
   - OAUTH_APPLE_CLIENT_ID
   - OAUTH_APPLE_CLIENT_SECRET
```

**Update Monolith:**

```java
@FeignClient(name = "auth-service", url = "${auth.service.url}")
public interface AuthServiceClient {
    @PostMapping("/api/auth/login")
    AuthResponseDTO login(@RequestBody LoginRequestDTO dto);
    
    @PostMapping("/api/auth/register")
    AuthResponseDTO register(@RequestBody RegisterRequestDTO dto);
    
    @PostMapping("/api/auth/validate")
    UserDTO validateToken(@RequestHeader("Authorization") String token);
}
```

---

### 2.2: Activity Service

**Duration:** 4-6 weeks  
**Priority:** High (core feature, high traffic expected)

**Database Setup on Railway:**

1. **Provision MySQL Database**
   - Railway Dashboard → Add MySQL
   - Name: `activity-db`
   - Size: 2GB
   - Link to `activity-service`

2. **Migrate Tables**

```sql
CREATE DATABASE activity_db;
USE activity_db;

-- activity table
CREATE TABLE activity (
    id BINARY(16) PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    creator_id BINARY(16) NOT NULL,
    start_time TIMESTAMP NOT NULL,
    end_time TIMESTAMP,
    location_id BINARY(16),
    activity_type_id BINARY(16),
    max_participants INT,
    is_public BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_updated TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_creator_id (creator_id),
    INDEX idx_start_time (start_time),
    INDEX idx_activity_type_id (activity_type_id)
);

-- activity_type table
CREATE TABLE activity_type (
    id BINARY(16) PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    icon VARCHAR(255),
    user_id BINARY(16) NOT NULL,
    associated_friend_ids TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_user_id (user_id)
);

-- activity_user (participation)
CREATE TABLE activity_user (
    id BINARY(16) PRIMARY KEY,
    activity_id BINARY(16) NOT NULL,
    user_id BINARY(16) NOT NULL,
    status VARCHAR(50) DEFAULT 'INVITED',
    joined_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE INDEX idx_activity_user (activity_id, user_id),
    INDEX idx_user_id (user_id)
);

-- location table
CREATE TABLE location (
    id BINARY(16) PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    address TEXT,
    latitude DECIMAL(10, 8),
    longitude DECIMAL(11, 8),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_coordinates (latitude, longitude)
);
```

**Tasks:**
- [ ] Create `activity-service` Spring Boot project
- [ ] Migrate activity entities: `Activity`, `ActivityType`, `ActivityUser`, `Location`
- [ ] Migrate repositories
- [ ] Migrate services: `ActivityService`, `ActivityTypeService`, `LocationService`
- [ ] Create REST API endpoints:
  - `POST /api/activities` (create)
  - `GET /api/activities/{id}` (get by ID)
  - `PUT /api/activities/{id}` (update)
  - `DELETE /api/activities/{id}` (delete)
  - `GET /api/activities/user/{userId}` (get user's activities)
  - `POST /api/activities/{id}/invite` (invite users)
- [ ] Add Feign clients for User Service and Social Service
- [ ] Implement scheduled jobs for activity expiration
- [ ] Deploy to Railway

**Railway Deployment:**

```bash
# In Railway Dashboard:
1. New Service → GitHub Repo
2. Root Directory: services/activity-service
3. Build Command: cd services/activity-service && mvn clean package
4. Start Command: java -Xmx1536m -jar target/activity-service.jar
5. Add Environment Variables:
   - DATABASE_URL (from activity-db MySQL)
   - REDIS_URL (shared)
   - USER_SERVICE_URL
   - SOCIAL_SERVICE_URL (still points to monolith)
```

---

### 2.3: Chat Service (WebSocket)

**Duration:** 3-4 weeks  
**Priority:** Medium (learning opportunity, can be scaled down)

**Database Setup on Railway:**

1. **Provision MySQL Database (Start)**
   - Railway Dashboard → Add MySQL
   - Name: `chat-db`
   - Size: 1GB
   - Link to `chat-service`

2. **Migrate Tables**

```sql
CREATE DATABASE chat_db;
USE chat_db;

-- chat_message table
CREATE TABLE chat_message (
    id BINARY(16) PRIMARY KEY,
    activity_id BINARY(16) NOT NULL,
    sender_id BINARY(16) NOT NULL,
    content TEXT NOT NULL,
    timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    edited_at TIMESTAMP NULL,
    deleted_at TIMESTAMP NULL,
    INDEX idx_activity_timestamp (activity_id, timestamp DESC),
    INDEX idx_sender_id (sender_id)
);

-- chat_message_likes table
CREATE TABLE chat_message_likes (
    id BINARY(16) PRIMARY KEY,
    message_id BINARY(16) NOT NULL,
    user_id BINARY(16) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE INDEX idx_message_user (message_id, user_id)
);
```

**WebSocket Implementation:**

```java
// WebSocket Configuration
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
    
    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // Enable simple broker for topic-based messaging
        config.enableSimpleBroker("/topic", "/queue");
        config.setApplicationDestinationPrefixes("/app");
        
        // Optional: Use external message broker (RabbitMQ) for production
        // config.enableStompBrokerRelay("/topic", "/queue")
        //     .setRelayHost("rabbitmq.railway.internal")
        //     .setRelayPort(61613);
    }
    
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws/chat")
            .setAllowedOriginPatterns("*")
            .withSockJS();  // Fallback for browsers without WebSocket support
    }
}

// WebSocket Controller
@Controller
public class ChatWebSocketController {
    
    @Autowired
    private ChatService chatService;
    
    @Autowired
    private SimpMessagingTemplate messagingTemplate;
    
    @MessageMapping("/chat/{activityId}/send")
    @SendTo("/topic/activity/{activityId}")
    public ChatMessageDTO sendMessage(
            @DestinationVariable UUID activityId,
            ChatMessageDTO message,
            Principal principal) {
        
        // Validate user is member of activity
        UUID userId = UUID.fromString(principal.getName());
        if (!chatService.isActivityMember(activityId, userId)) {
            throw new UnauthorizedException("User not member of activity");
        }
        
        // Save message to database
        ChatMessage savedMessage = chatService.saveMessage(activityId, message);
        
        // Broadcast to all subscribers of this activity's topic
        return ChatMessageDTO.from(savedMessage);
    }
    
    @MessageMapping("/chat/{activityId}/typing")
    @SendTo("/topic/activity/{activityId}/typing")
    public TypingNotificationDTO typing(
            @DestinationVariable UUID activityId,
            TypingNotificationDTO notification) {
        // Broadcast typing indicator (not saved to DB)
        return notification;
    }
}

// REST API for Message History
@RestController
@RequestMapping("/api/chat")
public class ChatRestController {
    
    @Autowired
    private ChatService chatService;
    
    @GetMapping("/activities/{activityId}/messages")
    public Page<ChatMessageDTO> getMessages(
            @PathVariable UUID activityId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        return chatService.getMessages(activityId, page, size);
    }
    
    @PostMapping("/messages/{messageId}/like")
    public ChatMessageDTO likeMessage(
            @PathVariable UUID messageId,
            @RequestHeader("X-User-Id") UUID userId) {
        return chatService.likeMessage(messageId, userId);
    }
}
```

**Tasks:**
- [ ] Create `chat-service` Spring Boot project
- [ ] Add WebSocket dependencies (spring-boot-starter-websocket)
- [ ] Migrate chat entities: `ChatMessage`, `ChatMessageLikes`
- [ ] Implement WebSocket configuration
- [ ] Implement WebSocket controllers for real-time messaging
- [ ] Implement REST API for message history
- [ ] Add Feign client for Activity Service (validate membership)
- [ ] Configure for minimal resource usage (512MB)
- [ ] Deploy to Railway

**Railway Deployment:**

```bash
# In Railway Dashboard:
1. New Service → GitHub Repo
2. Root Directory: services/chat-service
3. Build Command: cd services/chat-service && mvn clean package
4. Start Command: java -Xmx512m -jar target/chat-service.jar
5. Add Environment Variables:
   - DATABASE_URL (from chat-db MySQL)
   - REDIS_URL (shared, for WebSocket session management)
   - ACTIVITY_SERVICE_URL
6. Enable WebSocket support (Railway handles this automatically)
```

**Client Connection Example (Mobile/Web):**

```javascript
// Using SockJS + Stomp
const socket = new SockJS('https://chat-service.railway.app/ws/chat');
const stompClient = Stomp.over(socket);

stompClient.connect({}, function(frame) {
    // Subscribe to activity's message topic
    stompClient.subscribe('/topic/activity/' + activityId, function(message) {
        const chatMessage = JSON.parse(message.body);
        displayMessage(chatMessage);
    });
    
    // Send message
    stompClient.send('/app/chat/' + activityId + '/send', {}, 
        JSON.stringify({ content: 'Hello!', senderId: userId }));
});
```

**Optional: Cassandra Migration (If Message Volume >100k/day)**

```cql
-- Cassandra schema for high-volume chat
CREATE KEYSPACE chat_db WITH replication = {
    'class': 'SimpleStrategy',
    'replication_factor': 3
};

USE chat_db;

CREATE TABLE messages (
    activity_id uuid,
    timestamp timestamp,
    message_id uuid,
    sender_id uuid,
    content text,
    edited_at timestamp,
    deleted_at timestamp,
    PRIMARY KEY (activity_id, timestamp, message_id)
) WITH CLUSTERING ORDER BY (timestamp DESC);

CREATE TABLE message_likes (
    message_id uuid,
    user_id uuid,
    created_at timestamp,
    PRIMARY KEY (message_id, user_id)
);
```

---

### 2.4: User Service (Optional)

**Duration:** 3-4 weeks  
**Priority:** Optional (completes core service extraction)

**Database Setup on Railway:**

1. **Provision MySQL Database**
   - Railway Dashboard → Add MySQL
   - Name: `user-db`
   - Size: 1GB
   - Link to `user-service`

2. **Migrate Tables**

```sql
CREATE DATABASE user_db;
USE user_db;

-- user table
CREATE TABLE user (
    id BINARY(16) PRIMARY KEY,
    username VARCHAR(255) UNIQUE NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    phone_number VARCHAR(20) UNIQUE,
    password_hash VARCHAR(255),
    date_created TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_login TIMESTAMP,
    is_active BOOLEAN DEFAULT TRUE,
    INDEX idx_email (email),
    INDEX idx_username (username),
    INDEX idx_phone_number (phone_number)
);

-- user_info table
CREATE TABLE user_info (
    id BINARY(16) PRIMARY KEY,
    user_id BINARY(16) NOT NULL UNIQUE,
    full_name VARCHAR(255),
    bio TEXT,
    date_of_birth DATE,
    profile_picture_url VARCHAR(500),
    FOREIGN KEY (user_id) REFERENCES user(id) ON DELETE CASCADE
);

-- user_interest table
CREATE TABLE user_interest (
    id BINARY(16) PRIMARY KEY,
    user_id BINARY(16) NOT NULL,
    interest VARCHAR(255) NOT NULL,
    UNIQUE INDEX idx_user_interest (user_id, interest)
);

-- user_social_media table
CREATE TABLE user_social_media (
    id BINARY(16) PRIMARY KEY,
    user_id BINARY(16) NOT NULL UNIQUE,
    instagram_handle VARCHAR(255),
    twitter_handle VARCHAR(255),
    linkedin_profile VARCHAR(500),
    FOREIGN KEY (user_id) REFERENCES user(id) ON DELETE CASCADE
);
```

**Tasks:**
- [ ] Create `user-service` Spring Boot project
- [ ] Migrate user entities: `User`, `UserInfo`, `UserInterest`, `UserSocialMedia`
- [ ] Migrate repositories
- [ ] Migrate services: `UserService`, `UserSearchService`
- [ ] Implement fuzzy search (Jaro-Winkler)
- [ ] Create REST API endpoints
- [ ] Deploy to Railway

---

## Phase 4: API Gateway & Service Mesh

**Duration:** 2-3 weeks  
**Goal:** Centralize routing, authentication, rate limiting

### 4.1 Deploy API Gateway

**Technology:** Spring Cloud Gateway

**Responsibilities:**
- Route requests to appropriate services
- Validate JWT tokens
- Rate limiting
- Request logging
- CORS handling

**Gateway Routes Configuration:**

```yaml
# gateway/src/main/resources/application.yml

spring:
  cloud:
    gateway:
      routes:
        # User Service
        - id: user-service
          uri: http://user-service.railway.internal:8081
          predicates:
            - Path=/api/users/**
          filters:
            - name: CircuitBreaker
              args:
                name: user-service
                fallbackUri: forward:/fallback/user
        
        # Activity Service
        - id: activity-service
          uri: http://activity-service.railway.internal:8082
          predicates:
            - Path=/api/activities/**,/api/activity-types/**
          filters:
            - name: CircuitBreaker
              args:
                name: activity-service
                fallbackUri: forward:/fallback/activity
        
        # Social Service
        - id: social-service
          uri: http://social-service.railway.internal:8083
          predicates:
            - Path=/api/friends/**,/api/friend-requests/**,/api/blocks/**
        
        # Auth Service
        - id: auth-service
          uri: http://auth-service.railway.internal:8084
          predicates:
            - Path=/api/auth/**
        
        # Chat Service
        - id: chat-service
          uri: http://chat-service.railway.internal:8085
          predicates:
            - Path=/api/chat/**
        
        # Notification Service
        - id: notification-service
          uri: http://notification-service.railway.internal:8086
          predicates:
            - Path=/api/notifications/**
        
        # Media Service
        - id: media-service
          uri: http://media-service.railway.internal:8087
          predicates:
            - Path=/api/media/**
        
        # Analytics Service
        - id: analytics-service
          uri: http://analytics-service.railway.internal:8088
          predicates:
            - Path=/api/reports/**,/api/feedback/**,/api/share-links/**

      globalcors:
        corsConfigurations:
          '[/**]':
            allowedOrigins: "*"
            allowedMethods:
              - GET
              - POST
              - PUT
              - DELETE
            allowedHeaders: "*"
```

**JWT Validation Filter:**

```java
@Component
public class JWTAuthenticationFilter implements GlobalFilter, Ordered {
    
    @Autowired
    private JWTService jwtService;
    
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getPath().value();
        
        // Skip JWT validation for public endpoints
        if (path.startsWith("/api/auth/")) {
            return chain.filter(exchange);
        }
        
        String token = extractToken(exchange.getRequest());
        if (token == null) {
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }
        
        try {
            String userId = jwtService.validateToken(token);
            
            // Add user ID to request header for downstream services
            ServerHttpRequest modifiedRequest = exchange.getRequest()
                .mutate()
                .header("X-User-Id", userId)
                .build();
            
            return chain.filter(exchange.mutate().request(modifiedRequest).build());
        } catch (TokenExpiredException e) {
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }
    }
    
    @Override
    public int getOrder() {
        return -100; // Run before other filters
    }
}
```

**Tasks:**
- [ ] Create `api-gateway` Spring Boot project
- [ ] Configure routes for all services
- [ ] Add JWT validation filter
- [ ] Add rate limiting (Redis-based)
- [ ] Add request logging
- [ ] Deploy to Railway
- [ ] Update client apps to use Gateway URL

**Railway Setup:**
```bash
# In Railway Dashboard:
1. New Service → GitHub Repo
2. Root Directory: gateway
3. Environment Variables:
   - JWT_SECRET
   - REDIS_URL
4. Public Domain: Enable (this will be the public API endpoint)
5. Custom Domain (optional): api.spawnapp.com
```

### 4.2 Update Mobile Apps

**Tasks:**
- [ ] Update iOS app to use Gateway URL
- [ ] Update Android app to use Gateway URL
- [ ] Test all API calls through gateway
- [ ] Monitor latency (Gateway adds ~5-10ms)

---

## Phase 5: Data Migration & Optimization

**Duration:** 2-3 weeks  
**Goal:** Optimize performance and clean up monolith

### 5.1 Database Optimization

**Tasks per Service:**

- [ ] **Add Indexes**
  ```sql
  -- user-db
  CREATE INDEX idx_user_email ON user(email);
  CREATE INDEX idx_user_username ON user(username);
  CREATE INDEX idx_user_phone_number ON user(phone_number);
  
  -- activity-db
  CREATE INDEX idx_activity_creator_id ON activity(creator_id);
  CREATE INDEX idx_activity_start_time ON activity(start_time);
  
  -- social-db
  CREATE INDEX idx_friendship_user_a_id ON friendship(user_a_id);
  CREATE INDEX idx_friendship_user_b_id ON friendship(user_b_id);
  ```

- [ ] **Configure Connection Pooling**
  ```yaml
  # application.yml for each service
  spring:
    datasource:
      hikari:
        maximum-pool-size: 10
        minimum-idle: 5
        idle-timeout: 300000
        max-lifetime: 1200000
  ```

- [ ] **Enable Query Logging (dev only)**
  ```yaml
  spring:
    jpa:
      show-sql: true
      properties:
        hibernate:
          format_sql: true
          use_sql_comments: true
  ```

### 5.2 Caching Strategy

**Tasks:**

- [ ] **Cache User Profiles** (60 min TTL)
  ```java
  @Cacheable(value = "users", key = "#userId")
  public UserDTO getUserById(UUID userId) { ... }
  ```

- [ ] **Cache Activity Lists** (5 min TTL)
  ```java
  @Cacheable(value = "activities", key = "#userId + ':upcoming'")
  public List<ActivityDTO> getUpcomingActivities(UUID userId) { ... }
  ```

- [ ] **Cache Friend Lists** (10 min TTL)
  ```java
  @Cacheable(value = "friends", key = "#userId")
  public List<UserDTO> getFriends(UUID userId) { ... }
  ```

### 5.3 Implement Health Checks

**Add to Each Service:**

```java
@RestController
@RequestMapping("/actuator")
public class HealthController {
    
    @Autowired
    private DataSource dataSource;
    
    @GetMapping("/health")
    public ResponseEntity<HealthStatus> health() {
        boolean dbHealthy = checkDatabase();
        boolean redisHealthy = checkRedis();
        
        if (dbHealthy && redisHealthy) {
            return ResponseEntity.ok(new HealthStatus("UP"));
        } else {
            return ResponseEntity.status(503)
                .body(new HealthStatus("DOWN"));
        }
    }
}
```

**Railway Health Check Configuration:**

Railway automatically monitors `/` endpoint. Update to use `/actuator/health`:

```yaml
# railway.json
{
  "deploy": {
    "healthcheckPath": "/actuator/health",
    "healthcheckTimeout": 10
  }
}
```

---

## Phase 6: Production Cutover

**Duration:** 1-2 weeks  
**Goal:** Full migration to microservices

### 6.1 Traffic Migration Strategy

**Option 1: Blue-Green Deployment**

1. Keep monolith running (Blue)
2. Deploy all microservices behind API Gateway (Green)
3. Route 10% of traffic to Gateway
4. Monitor for errors/latency
5. Gradually increase to 50%, then 100%
6. Decommission monolith after 1 week

**Option 2: Feature Flagging**

Use feature flags to control which backend handles requests:

```java
@Service
public class ActivityService {
    @Value("${features.use-microservices}")
    private boolean useMicroservices;
    
    @Autowired
    private ActivityServiceClient activityClient;
    
    public ActivityDTO getActivity(UUID id) {
        if (useMicroservices) {
            return activityClient.getActivity(id);
        } else {
            // Use monolith logic
            return activityRepository.findById(id);
        }
    }
}
```

### 6.2 Monitoring & Alerting

**Railway Monitoring:**

Railway provides basic metrics. Add custom monitoring:

**Tasks:**
- [ ] Set up New Relic or Datadog APM
- [ ] Create dashboards for:
  - Request rate per service
  - Error rate per service
  - P95/P99 latency per service
  - Database connection pool usage
  - Redis hit rate
- [ ] Set up alerts:
  - Error rate > 5%
  - P99 latency > 500ms
  - Service health check failing
  - Database connection pool > 80%

### 6.3 Documentation

**Tasks:**
- [ ] Update API documentation (Swagger/OpenAPI)
- [ ] Document service endpoints and dependencies
- [ ] Create runbook for common issues
- [ ] Document rollback procedures

### 6.4 Decommission Monolith

**Tasks:**
- [ ] Ensure all traffic routed through Gateway
- [ ] Export final database backup from monolith DB
- [ ] Archive monolith codebase (Git tag: `v1-monolith-final`)
- [ ] Delete monolith Railway service
- [ ] Delete monolith database (after 1-month retention)

---

## Rollback Strategy

### Immediate Rollback (< 24 hours)

**If microservices have critical issues:**

1. **Update API Gateway routes to monolith**
   ```yaml
   # Revert all routes to monolith
   - id: fallback-to-monolith
     uri: http://monolith.railway.internal:8080
     predicates:
       - Path=/api/**
   ```

2. **Restore monolith database** (if data was migrated)
   ```bash
   psql -h monolith-db.railway.internal -U postgres < backup.sql
   ```

3. **Communicate with users** (via in-app message)

### Partial Rollback (Service-Specific)

**If one service has issues:**

1. **Reroute specific paths to monolith**
   ```yaml
   # Route only /api/users/** to monolith temporarily
   - id: user-service-fallback
     uri: http://monolith.railway.internal:8080
     predicates:
       - Path=/api/users/**
   ```

2. **Investigate and fix issue**

3. **Restore route to microservice**

---

## Testing Strategy

### Unit Tests

**Each Service:**
- [ ] Service layer tests (mock repositories)
- [ ] Controller tests (MockMvc)
- [ ] Repository tests (DataJpaTest)

### Integration Tests

**Per Service:**
- [ ] Test with real database (Testcontainers)
- [ ] Test Feign clients with WireMock

### End-to-End Tests

**Across Services:**
- [ ] Test complete user flows (registration → create activity → invite friends)
- [ ] Test event-driven workflows (activity invite → notification sent)

### Load Tests

**Tools:** JMeter, Gatling, or k6

**Scenarios:**
- [ ] 1000 concurrent users creating activities
- [ ] 5000 concurrent users sending chat messages
- [ ] 10,000 concurrent users fetching activity feeds

**Success Criteria:**
- P95 latency < 200ms
- Error rate < 0.1%
- No memory leaks over 1-hour test

### Chaos Engineering

**Test Resilience:**
- [ ] Kill one service instance (test circuit breakers)
- [ ] Simulate database connection timeout
- [ ] Simulate Redis failure (test cache fallback)

---

## Cost Implications

### Current Monolith (Railway)

| Resource | Cost |
|----------|------|
| 1 App Server (1GB RAM) | $7/month |
| 1 MySQL DB (2GB) | $10/month |
| 1 Redis (512MB) | $5/month |
| **Total** | **$22/month** |

### Selective Microservices Architecture (Railway)

**Without User Service:**

| Service | RAM | Cost/Month |
|---------|-----|------------|
| **Modular Monolith** (Social, Notification, Media, Analytics) | 1GB | $7 |
| Auth Service | 1GB | $7 |
| Activity Service | 1.5GB | $10 |
| Chat Service (WebSocket) | 512MB | $5 |
| API Gateway | 512MB | $5 |
| **Subtotal (Apps)** | | **$34** |
| MySQL (auth-db, 1GB) | | $7 |
| MySQL (activity-db, 2GB) | | $10 |
| MySQL (chat-db, 1GB) | | $7 |
| MySQL (monolith-db, 2GB) | | $10 |
| Redis (shared, 512MB) | | $5 |
| **Total** | | **$73/month** |

**With User Service (Optional):**

| Additional Service | RAM | Cost/Month |
|-------------------|-----|------------|
| User Service | 512MB | $5 |
| MySQL (user-db, 1GB) | | $7 |
| **Additional Cost** | | **$12** |
| **New Total** | | **$85/month** |

### Cost Increase Summary

| Configuration | Monthly Cost | Increase from Monolith |
|---------------|--------------|------------------------|
| **Current Monolith** | $22 | Baseline |
| **Selective (3 services)** | $73 | 3.3x |
| **Selective (4 services)** | $85 | 3.9x |
| **Full Microservices (8 services)** | $105+ | 4.8x |

### Cost Optimization Strategies

1. **MySQL Database Consolidation** (if cost becomes concern)
   - Use separate schemas instead of separate MySQL instances
   - Consolidate auth-db and chat-db into shared MySQL: Saves ~$7/month
   - Trade-off: Reduced isolation but still separate service deployments

2. **Cassandra Option for Chat**
   - If message volume >100k/day, replace MySQL chat-db with Cassandra
   - Options:
     - DataStax Astra (free tier up to certain limits)
     - Self-hosted Cassandra on Railway (~$5-10/month for single node)
   - Potential savings/neutral cost while gaining scalability

3. **Right-Size Resources**
   - Start with minimum RAM allocations
   - Monitor Railway metrics
   - Scale up only services that need it
   - Activity Service likely needs 1.5-2GB due to traffic
   - Chat Service can stay minimal (512MB) with efficient WebSocket impl

4. **Share Redis Across Services**
   - Already planned: one Redis instance with namespacing
   - Cost: $5/month total (vs $5 per service)
   - Namespacing strategy:
     ```
     auth:*
     activity:*
     chat:*
     monolith:*
     ```

### Learning Value per Dollar

| Approach | 5-Year Cost | Learning Value | Value/$ |
|----------|-------------|----------------|---------|
| Monolith | $1,320 | Low | - |
| Selective Microservices | $4,380 | **High** | **Best** |
| Full Microservices | $6,300 | Very High | Medium |

**Selective Microservices Justification:**
- Extra $3,060 over 5 years (~$612/year)
- Practical experience with microservices architecture
- WebSocket implementation in distributed systems
- Real-world service orchestration
- Resume/portfolio value
- Can scale down or consolidate if needed

---

## Success Metrics

### Technical Metrics

- [ ] **Deployment Frequency:** Increase from 1x/week to 3x/week (per service)
- [ ] **Lead Time:** Reduce from 2 days to 4 hours (code commit to production)
- [ ] **MTTR (Mean Time to Recovery):** Reduce from 2 hours to 15 minutes
- [ ] **Change Failure Rate:** Keep below 5%

### Performance Metrics

- [ ] **API Latency:** P95 < 200ms (currently ~150ms, expect +50ms for inter-service calls)
- [ ] **Throughput:** Support 10x current load without scaling
- [ ] **Availability:** 99.9% uptime per service

### Business Metrics

- [ ] **Feature Velocity:** Increase new feature delivery by 50%
- [ ] **Team Autonomy:** Enable 3 teams to work independently
- [ ] **Incident Impact:** Reduce blast radius (isolated service failures don't take down entire app)

---

## Conclusion

This selective microservices implementation plan provides a pragmatic approach to learning microservices architecture while maintaining manageable complexity and cost for Spawn App on Railway.

**Key Success Factors:**
1. **Focus on Learning:** 3-4 core services provide full microservices experience
2. **WebSocket Implementation:** Real-time chat as practical distributed systems learning
3. **MySQL Consistency:** Familiar database technology reduces operational burden
4. **Hybrid Approach:** Keep low-traffic services in modular monolith
5. **Flexibility:** Can scale up (extract more services) or scale down (consolidate)

**Timeline Summary:**
- **Week 0-3:** Infrastructure setup (API Gateway, shared libraries, Redis)
- **Week 4-9:** Auth Service extraction and deployment
- **Week 10-15:** Activity Service extraction and deployment
- **Week 16-19:** Chat Service with WebSocket implementation
- **Week 20-23:** (Optional) User Service extraction
- **Week 24+:** Monitoring, optimization, learning consolidation

**Next Steps:**
1. Review and approve selective architecture
2. Set up Railway project infrastructure
3. Begin Phase 1: Preparation & Infrastructure
4. Extract Auth Service as first real service
5. Iterate and learn from each service extraction

---

**Questions? Concerns?** 

- Review [MICROSERVICES_ARCHITECTURE.md](./MICROSERVICES_ARCHITECTURE.md) for architectural details
- Review [MICROSERVICES_DECISION_GUIDE.md](./MICROSERVICES_DECISION_GUIDE.md) for decision rationale

**Estimated Total Timeline:** 3-5 months (depending on pace and optional User Service)

**Recommended Approach:** Part-time implementation (10-15 hours/week) while maintaining existing features

---

**Document Maintainer:** Backend Team  
**Last Updated:** November 9, 2025  
**Version:** 2.0 (Updated for Selective Microservices Approach)

---

## Appendix: Technology Stack

### Core Technologies
- **Java 17+** with Spring Boot 3.x
- **Spring Cloud OpenFeign** for REST client communication
- **Spring Cloud Gateway** for API Gateway
- **Spring WebSocket** (STOMP) for real-time chat
- **Resilience4j** for circuit breakers and retries
- **Spring Cloud Sleuth** for distributed tracing

### Databases
- **MySQL 8.0** for all services (primary choice)
- **Cassandra** (optional for chat if volume justifies)

### Infrastructure
- **Railway** for hosting and deployment
- **Redis** for distributed caching and session management
- **Docker** (Railway handles automatically)

### Monitoring & Logging
- **Spring Boot Actuator** for health checks and metrics
- **Railway logs** for centralized logging
- **Optional:** Zipkin for trace visualization

### Testing
- **JUnit 5** for unit tests
- **TestContainers** for integration tests with real databases
- **WireMock** for mocking service calls

---

