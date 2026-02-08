---
name: Microservices Infrastructure Setup
overview: Set up microservices infrastructure (API Gateway, shared patterns, single shared database, inter-service communication) before extracting the Activity Service as the next microservice.
todos:
  - id: api-gateway
    content: Create API Gateway (Spring Cloud Gateway) with JWT validation, routing, rate limiting, CORS
    status: completed
  - id: shared-lib
    content: Create shared library or copy shared DTOs/utils to services
    status: completed
  - id: shared-db
    content: Use single shared MySQL database for monolith and all services
    status: completed
  - id: inter-service-comm
    content: Set up Feign clients + Resilience4j for inter-service communication
    status: completed
  - id: redis-pubsub
    content: Implement Redis Pub/Sub for cross-service async events
    status: completed
  - id: extract-activity
    content: Extract Activity Service (entities, repos, services, controllers, DTOs)
    status: pending
  - id: gateway-routes
    content: Update gateway routes for activity-service
    status: pending
  - id: health-logging
    content: Add health checks, structured logging, and distributed tracing across services
    status: completed
isProject: false
---

# Microservices Infrastructure & Next Service Extraction Plan

## Current State

- Auth service extracted at `services/auth-service/` (port 8081)
- Main monolith at `src/main/java/` with modular package structure
- Both share the same MySQL database
- No API Gateway, no inter-service communication, no Docker
- Deploying on Railway

## Phase 1: Infrastructure Foundation (Weeks 1-3)

### 1A. API Gateway (Spring Cloud Gateway)

Create `gateway/api-gateway/` as a new Spring Boot application:

- **JWT validation** as a global filter -- the gateway validates tokens so downstream services don't each need to. It adds `X-User-Id` header to forwarded requests.
- **Route configuration** mapping `/api/v1/auth/**` to auth-service (port 8081), everything else to the monolith (port 8080) for now. As services are extracted, new routes get added.
- **Rate limiting** via Redis (reuse existing Bucket4j or use Spring Cloud Gateway's built-in Redis rate limiter).
- **CORS handling** centralized at the gateway level.
- **Health check** endpoint at `/actuator/health`.
- Gateway runs on port **8090** (or configurable).

Key files to create:

- `gateway/api-gateway/pom.xml` -- Spring Cloud Gateway + JWT dependencies
- `gateway/api-gateway/src/main/java/.../GatewayApplication.java`
- `gateway/api-gateway/src/main/java/.../filter/JwtAuthFilter.java`
- `gateway/api-gateway/src/main/resources/application.yml` -- route definitions

### 1B. Shared Library Pattern

Since services are independent Maven projects, create a lightweight shared module at `shared/spawn-common/`:

- **Common DTOs** for inter-service communication (e.g., `UserSummaryDTO` that services exchange)
- **JWT utilities** (token validation logic shared between gateway and services)
- **Common exception types**

Publish via `mvn install` locally, or as a GitHub Package for Railway builds. Each service adds it as a Maven dependency.

Alternatively, for maximum simplicity: just copy the ~5-10 shared classes into each service. Easier to start with, refactor to shared lib later.

### 1C. Single Shared Database

All backends (monolith and microservices) use **one MySQL database**:

- No new databases per service — monolith, auth-service, and future services (e.g. activity-service) all connect to the same MySQL instance/schema.
- Auth service and activity service use the same datasource URL as the monolith; each service only touches the tables it owns (e.g. auth: `email_verification`, `user_id_external_id_map`; activity: `activity`, `activity_type`, etc.).
- User data for login/registration can stay as direct DB access or REST to monolith, depending on preference; shared DB allows direct access if desired.
- Flyway migrations can live in the monolith (or a single migration module) so schema changes are applied once.

### 1D. Inter-Service Communication

Set up the pattern for services to call each other:

- **Synchronous (Feign/RestTemplate):** Auth service calls monolith to look up users. Activity service (later) calls monolith for user data.
- **Async events (Redis Pub/Sub):** Replace Spring `ApplicationEventPublisher` with Redis Pub/Sub for cross-service events (e.g., "user registered" event from auth -> monolith notification system). Start simple -- only add Redis Pub/Sub for events that actually need to cross service boundaries.
- **Circuit breakers (Resilience4j):** Wrap Feign clients so a downstream service outage doesn't cascade.

---

## Phase 2: Extract Activity Service (Weeks 4-8)

Once infrastructure is in place, extract the Activity domain:

### What moves to `services/activity-service/`:

- **Entities:** `Activity`, `ActivityType`, `ActivityUser`, `Location` (from [activity/internal/domain/](src/main/java/com/danielagapov/spawn/activity/internal/domain/))
- **Repositories:** Activity, ActivityType, ActivityUser, Location repositories
- **Services:** `ActivityService`, `ActivityTypeService`, `LocationService`, `CalendarService`, `ActivityParticipationService`
- **Controllers:** `ActivityController`, `ActivityTypeController`, `CalendarController`
- **DTOs:** All activity-related DTOs

### Cross-cutting concerns:

- Activity service needs **user data** (creator info, participant info) -- use Feign client to call monolith's `/api/v1/users/{id}` endpoint
- Activity service needs **friend data** (for invite validation) -- Feign client to monolith's social endpoints
- Activity creation triggers **notification events** -- publish to Redis Pub/Sub, monolith's notification module subscribes
- **Caching:** Activity service gets its own Redis namespace (`activity:*`)

### Database:

- **Shared database:** Activity service connects to the same MySQL as the monolith. Activity, ActivityType, ActivityUser, Location tables remain in that single schema; Flyway migrations (if used) stay in the monolith or a single place.

### Gateway update:

- Add route: `/api/v1/activities/**` and `/api/v1/activity-types/**` -> activity-service

---

## Phase 3: Polish & Prepare for Chat (Weeks 9-10)

- Add health checks to all services (Spring Boot Actuator)
- Set up structured logging (JSON format) across services
- Add distributed tracing headers (`X-Trace-Id` propagation)
- Document API contracts between services
- Monitor Railway metrics, tune resource allocation
- Evaluate: proceed to Chat Service extraction?

---

## Summary of Deliverables

- `gateway/api-gateway/` -- Spring Cloud Gateway with JWT validation, routing, rate limiting
- `shared/spawn-common/` (optional) -- shared DTOs and utilities
- `services/auth-service/` -- uses shared database; Feign client for user lookup as needed
- `services/activity-service/` -- new microservice for the activity domain (shared database)
- Updated monolith with removed activity code and new event subscribers for Redis Pub/Sub
- Updated gateway routes

## Key Technical Decisions

- **Independent Maven projects** per service (no parent POM)
- **No Docker** for local dev -- develop against Railway
- **Spring Cloud Gateway** for API Gateway
- **Feign + Resilience4j** for synchronous inter-service calls
- **Redis Pub/Sub** for async cross-service events
- **Single shared MySQL database** for monolith and all services on Railway

