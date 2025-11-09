# Spawn App - Microservices Implementation Plan for Railway

**Last Updated:** November 9, 2025

## Table of Contents

- [Overview](#overview)
- [Railway-Specific Considerations](#railway-specific-considerations)
- [Phase 1: Preparation & Infrastructure](#phase-1-preparation--infrastructure)
- [Phase 2: Extract First Microservice](#phase-2-extract-first-microservice)
- [Phase 3: Extract Core Services](#phase-3-extract-core-services)
- [Phase 4: API Gateway & Service Mesh](#phase-4-api-gateway--service-mesh)
- [Phase 5: Data Migration & Optimization](#phase-5-data-migration--optimization)
- [Phase 6: Production Cutover](#phase-6-production-cutover)
- [Rollback Strategy](#rollback-strategy)
- [Testing Strategy](#testing-strategy)
- [Cost Implications](#cost-implications)

---

## Overview

> **⚠️ Stop! Read This First:** Before implementing microservices, read [MICROSERVICES_DECISION_GUIDE.md](./MICROSERVICES_DECISION_GUIDE.md) to determine if microservices is the right choice for Spawn App at its current stage. The guide includes:
> - Comprehensive benefits vs drawbacks analysis
> - Railway-specific cost implications (6x increase)
> - Decision framework and scoring system
> - Alternative: Modular monolith (recommended for current scale)
> - **Current Recommendation: Start with modular monolith, not full microservices**

This document provides a step-by-step implementation plan for migrating the Spawn App monolith to a microservices architecture, specifically tailored for Railway's infrastructure. **Use this only if you've decided microservices is necessary after reading the decision guide.**

**Timeline:** 3-6 months  
**Approach:** Strangler Fig Pattern - Gradually replace monolith functionality  
**Risk:** Low to Medium (incremental migration with rollback capability)

---

## Railway-Specific Considerations

### Railway Platform Features

✅ **Built-in Service Discovery**
- Railway provides internal DNS: `service-name.railway.internal`
- Services can communicate without external service discovery tools

✅ **Environment Variables**
- Centralized configuration per service
- Secrets management built-in

✅ **Database Provisioning**
- Easy PostgreSQL/MySQL provisioning per service
- Built-in connection pooling

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
- Need to optimize for cost (see [Cost Implications](#cost-implications))

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

## Phase 2: Extract First Microservice

**Duration:** 2-3 weeks  
**Goal:** Extract Analytics & Support Service (least coupled)

**Why Start Here?**
- Minimal dependencies on other services
- Read-heavy, low write frequency
- Isolated domain (reporting, feedback)
- Low risk if something goes wrong

### 2.1 Create Analytics Service

**Database Setup on Railway:**

1. **Provision Database**
   - Railway Dashboard → Add PostgreSQL
   - Name: `analytics-db`
   - Link to `analytics-service`

2. **Migrate Tables**

```sql
-- Create these tables in analytics-db:
-- - reported_content
-- - feedback_submission
-- - beta_access_sign_up
-- - share_link
```

**Migration Script:**

```sql
-- Run on monolith DB to extract data
SELECT * INTO OUTFILE '/tmp/reported_content.csv' 
FIELDS TERMINATED BY ',' ENCLOSED BY '"'
FROM reported_content;

-- Import to analytics-db
COPY reported_content FROM '/tmp/reported_content.csv' 
DELIMITER ',' CSV HEADER;
```

**Tasks:**
- [ ] Create `analytics-service` Spring Boot project
- [ ] Copy entities: `ReportedContent`, `FeedbackSubmission`, `ShareLink`, `BetaAccessSignUp`
- [ ] Copy repositories: `ReportedContentRepository`, etc.
- [ ] Copy services: `ReportContentService`, `FeedbackSubmissionService`, `ShareLinkService`
- [ ] Copy controllers: All analytics/feedback/share link endpoints
- [ ] Update application.properties with Railway database URL
- [ ] Deploy to Railway as new service

**Railway Deployment:**

```bash
# In Railway Dashboard:
1. New Service → GitHub Repo
2. Root Directory: services/analytics-service
3. Build Command: cd services/analytics-service && mvn clean package
4. Start Command: java -Xmx512m -jar target/analytics-service.jar
5. Add Environment Variables:
   - DATABASE_URL (from analytics-db)
   - REDIS_URL (shared)
   - JWT_SECRET (shared)
```

### 2.2 Update Monolith to Call Analytics Service

**Add Feign Client in Monolith:**

```java
@FeignClient(name = "analytics-service", 
             url = "${analytics.service.url}")
public interface AnalyticsServiceClient {
    @PostMapping("/api/reports")
    ReportedContentDTO createReport(@RequestBody CreateReportDTO dto);
    
    @PostMapping("/api/feedback")
    FeedbackSubmissionDTO submitFeedback(@RequestBody CreateFeedbackSubmissionDTO dto);
}

// In ReportContentService (monolith)
@Service
public class ReportContentService {
    @Autowired
    private AnalyticsServiceClient analyticsClient;
    
    public ReportedContentDTO reportContent(CreateReportDTO dto) {
        // Delegate to analytics service
        return analyticsClient.createReport(dto);
    }
}
```

**Tasks:**
- [ ] Add Feign client for Analytics Service
- [ ] Update controllers to proxy requests to Analytics Service
- [ ] Test end-to-end flow
- [ ] Monitor latency (should be <100ms for Railway internal calls)

### 2.3 Validation & Cutover

**Tasks:**
- [ ] Run integration tests against Analytics Service
- [ ] Compare responses from monolith vs. microservice
- [ ] Load test Analytics Service
- [ ] Monitor for 1 week in production
- [ ] Remove analytics code from monolith (keep as fallback for 1 sprint)

---

## Phase 3: Extract Core Services

**Duration:** 8-12 weeks  
**Goal:** Extract User, Activity, Social, Auth, Chat, Notification services

### 3.1 Extract Auth Service

**Priority:** High (foundation for other services)

**Database:** `auth-db`

**Tables:**
- `email_verification`
- `user_id_external_id_map`

**Dependencies:**
- Calls User Service to create/validate users

**Tasks:**
- [ ] Create `auth-service` Spring Boot project
- [ ] Migrate JWT generation/validation logic
- [ ] Migrate OAuth (Google, Apple) logic
- [ ] Create endpoint: `POST /auth/login`
- [ ] Create endpoint: `POST /auth/register`
- [ ] Create endpoint: `POST /auth/oauth/google`
- [ ] Create endpoint: `POST /auth/oauth/apple`
- [ ] Create endpoint: `POST /auth/verify-email`
- [ ] Deploy to Railway
- [ ] Update monolith to use Auth Service

### 3.2 Extract User Service

**Priority:** High (most other services depend on it)

**Database:** `user-db`

**Tables:**
- `user`
- `user_info`
- `user_interest`
- `user_social_media`

**Events Published:**
- `user.created`
- `user.updated`
- `user.deleted`

**Tasks:**
- [ ] Create `user-service` Spring Boot project
- [ ] Migrate User entities and repositories
- [ ] Create REST API for user CRUD
- [ ] Create endpoint: `GET /users/{id}` (used by many services)
- [ ] Create endpoint: `GET /users/search` (fuzzy search)
- [ ] Create endpoint: `POST /users/contact-cross-reference`
- [ ] Add event publishing for user changes
- [ ] Deploy to Railway
- [ ] Update Auth Service to call User Service
- [ ] Update monolith to proxy user requests

### 3.3 Extract Social Service

**Database:** `social-db`

**Tables:**
- `friendship`
- `friend_request`
- `blocked_user`

**Events Published:**
- `friend_request.sent`
- `friend_request.accepted`
- `user.blocked`

**Tasks:**
- [ ] Create `social-service` Spring Boot project
- [ ] Migrate social entities
- [ ] Create REST API for friendships/friend requests/blocks
- [ ] Subscribe to `user.deleted` events (cascade delete friendships)
- [ ] Publish events for friend requests
- [ ] Deploy to Railway
- [ ] Update monolith to proxy social requests

### 3.4 Extract Activity Service

**Database:** `activity-db`

**Tables:**
- `activity`
- `activity_type`
- `activity_user`
- `location`

**Events Published:**
- `activity.created`
- `activity.updated`
- `activity.invite`

**Dependencies:**
- Calls User Service to validate creator
- Calls Social Service for ActivityType associated friends

**Tasks:**
- [ ] Create `activity-service` Spring Boot project
- [ ] Migrate activity entities
- [ ] Create REST API for activities and activity types
- [ ] Add scheduled jobs for activity expiration
- [ ] Publish events for activity changes
- [ ] Deploy to Railway
- [ ] Update monolith to proxy activity requests

### 3.5 Extract Chat Service

**Database:** `chat-db`

**Tables:**
- `chat_message`
- `chat_message_likes`

**Events Published:**
- `message.sent`

**Dependencies:**
- Calls Activity Service to validate activity membership

**Tasks:**
- [ ] Create `chat-service` Spring Boot project
- [ ] Migrate chat entities
- [ ] Create REST API for messages
- [ ] Add WebSocket support for real-time chat
- [ ] Publish events for new messages
- [ ] Deploy to Railway
- [ ] Update monolith to proxy chat requests

### 3.6 Extract Notification Service

**Database:** `notification-db`

**Tables:**
- `device_token`
- `notification_preferences`

**Events Subscribed:**
- `friend_request.sent`
- `friend_request.accepted`
- `activity.invite`
- `activity.updated`
- `message.sent`

**Dependencies:**
- Calls FCM/APNS for push notifications

**Tasks:**
- [ ] Create `notification-service` Spring Boot project
- [ ] Migrate notification entities
- [ ] Create REST API for device tokens
- [ ] Subscribe to all notification events
- [ ] Implement notification strategies (FCM, APNS)
- [ ] Deploy to Railway
- [ ] Update monolith to use Notification Service

### 3.7 Extract Media Service

**Database:** `media-db` (minimal, just metadata)

**Dependencies:**
- S3 or Railway Volumes for storage

**Tasks:**
- [ ] Create `media-service` Spring Boot project
- [ ] Migrate S3 integration
- [ ] Create endpoints for upload/download
- [ ] Generate pre-signed URLs
- [ ] Deploy to Railway
- [ ] Update User Service to call Media Service for profile pictures

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
| 1 App Server (2GB RAM) | $10/month |
| 1 PostgreSQL DB (2GB) | $10/month |
| 1 Redis (512MB) | $5/month |
| **Total** | **$25/month** |

### Microservices Architecture (Railway)

| Service | RAM | Cost/Month |
|---------|-----|------------|
| API Gateway | 512MB | $5 |
| User Service | 1GB | $7 |
| Activity Service | 1GB | $7 |
| Social Service | 512MB | $5 |
| Auth Service | 512MB | $5 |
| Chat Service | 1GB | $7 |
| Notification Service | 512MB | $5 |
| Media Service | 512MB | $5 |
| Analytics Service | 512MB | $5 |
| **Subtotal (Apps)** | | **$51** |
| PostgreSQL (8 databases) | | $80 |
| Redis (shared) | | $10 |
| **Total** | | **$141/month** |

### Cost Optimization Strategies

1. **Combine Low-Traffic Services**
   - Merge Analytics + Media into one service
   - Saves ~$10/month

2. **Use Smaller Instances**
   - Start with 512MB for all services
   - Scale up only when needed

3. **Share Databases**
   - Use schemas instead of separate databases
   - Saves ~$40/month (but loses isolation)

4. **Optimized Configuration:**

| Service | RAM | Cost/Month |
|---------|-----|------------|
| API Gateway | 512MB | $5 |
| User Service | 512MB | $5 |
| Activity Service | 1GB | $7 |
| Social Service | 512MB | $5 |
| Auth Service | 512MB | $5 |
| Chat Service | 1GB | $7 |
| Notification Service | 512MB | $5 |
| Media + Analytics | 512MB | $5 |
| **Subtotal (Apps)** | | **$44** |
| PostgreSQL (3 shared DBs) | | $30 |
| Redis (shared) | | $10 |
| **Total** | | **$84/month** |

**Recommendation:** Start with optimized configuration, scale up based on monitoring.

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

This implementation plan provides a pragmatic, phased approach to migrating Spawn App to microservices on Railway. By following the Strangler Fig pattern and starting with the least-coupled services, we minimize risk while maximizing learning.

**Key Success Factors:**
1. **Start Small:** Analytics Service first
2. **Measure Everything:** Latency, error rates, costs
3. **Automate Testing:** Integration and E2E tests
4. **Plan for Rollback:** Keep monolith as fallback
5. **Optimize Costs:** Right-size resources

**Next Steps:**
1. Review and approve architecture
2. Set up Railway project and infrastructure
3. Begin Phase 1: Preparation & Infrastructure
4. Extract Analytics Service as proof-of-concept

---

**Questions? Concerns?** Review the [MICROSERVICES_ARCHITECTURE.md](./MICROSERVICES_ARCHITECTURE.md) for the architectural context and rationale.

**Estimated Total Timeline:** 3-6 months (depending on team size and velocity)

**Recommended Team:** 2-3 developers working part-time (20-30 hours/week)

---

**Document Maintainer:** Backend Team  
**Last Updated:** November 9, 2025  
**Version:** 1.0

