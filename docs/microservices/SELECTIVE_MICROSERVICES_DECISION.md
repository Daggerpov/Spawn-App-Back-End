# Spawn App - Selective Microservices Decision Summary

**Date:** November 9, 2025  
**Decision:** Proceed with selective microservices for learning and scaling key services  
**Current Status:** ⏸️ **ON HOLD** - Spring Modulith refactoring in progress (prerequisite)  
**Prerequisite:** [Spring Modulith Refactoring](../refactoring/SPRING_MODULITH_REFACTORING_PLAN.md) must complete first

---

## ⚠️ Implementation Status

**This microservices extraction should begin AFTER Spring Modulith refactoring is complete.**

**Current Progress:**
- ✅ Spring Modulith Phase 1 Complete (Dec 8, 2025) - Package restructuring done
- 🔄 Spring Modulith Phase 2-6 In Progress (Dec 2025 - Jan 2026) - Fixing circular dependencies, adding Modulith, testing
- ⏸️ Microservices Extraction: Estimated to begin **February 2026** (after Phase 6)

**Why wait?** Spring Modulith validates service boundaries and fixes circular dependencies before the complexity of distributed systems. See [WHY_SPRING_MODULITH_FIRST.md](../refactoring/WHY_SPRING_MODULITH_FIRST.md) for detailed rationale.

---

## Executive Decision

After reviewing the comprehensive microservices analysis, the decision has been made to **implement a selective microservices architecture** focused on:

1. **Learning experience** as primary goal
2. **3-4 core services** instead of full 8-service decomposition
3. **MySQL as primary database** (familiar and reliable)
4. **WebSocket implementation** for Chat Service
5. **Optional Cassandra** for Chat if message volume justifies (>100k messages/day)

---

## Services to Extract

### Priority 1: Auth Service (Weeks 1-6)
- **Rationale:** Highest anticipated traffic, foundational for other services
- **Tech Stack:** Spring Boot, JWT, OAuth (Google, Apple)
- **Database:** MySQL `auth_db`
- **Resources:** 1GB RAM

### Priority 2: Activity Service (Weeks 7-12)
- **Rationale:** Highest anticipated traffic, core app feature
- **Tech Stack:** Spring Boot, REST APIs
- **Database:** MySQL `activity_db`
- **Resources:** 1.5GB RAM (highest allocation)

### Priority 3: Chat Service (Weeks 13-16)
- **Rationale:** Learning opportunity for WebSocket, can be scaled down
- **Tech Stack:** Spring Boot, Spring WebSocket (STOMP), SockJS fallback
- **Database:** MySQL `chat_db` (start), Cassandra (if volume grows)
- **Resources:** 512MB RAM (minimal)
- **Key Feature:** Real-time messaging via WebSocket

### Optional: User Service (Weeks 17-20)
- **Rationale:** Completes core extraction, enables full service independence
- **Tech Stack:** Spring Boot, fuzzy search (Jaro-Winkler)
- **Database:** MySQL `user_db`
- **Resources:** 512MB-1GB RAM
- **Decision Point:** Evaluate after Chat Service deployment

---

## Services to Keep in Monolith

The following remain in a **modular monolith** with clear boundaries for potential future extraction:

1. **Social Service** - Friendships, friend requests, blocking
2. **Notification Service** - Push notifications (FCM, APNS)
3. **Media Service** - S3 file storage and management
4. **Analytics Service** - Reporting, feedback, share links

**Rationale:** Lower traffic, less complexity, can be extracted later if needed

---

## Database Strategy

### Primary: MySQL
- **Why:** Familiar, reliable, well-understood by team
- **Approach:** Separate MySQL database per service
  - `auth_db` (1GB)
  - `activity_db` (2GB)
  - `chat_db` (1GB)
  - `user_db` (1GB, if User Service extracted)
  - `monolith_db` (2GB for remaining services)

### Optional: Cassandra for Chat
- **Trigger:** Message volume exceeds 100k messages/day
- **Benefits:** 
  - Optimized for time-series data
  - Horizontal scalability
  - Learning opportunity for NoSQL in microservices
- **Options:**
  - DataStax Astra (free tier available)
  - Self-hosted on Railway (~$5-10/month)

---

## WebSocket Chat Implementation

### Why WebSocket?
- Real-time message delivery without polling
- Lower latency for chat experience
- Reduced server load compared to HTTP polling
- Excellent learning opportunity for distributed real-time systems

### Tech Stack
- **Spring WebSocket** with STOMP protocol
- **SockJS** fallback for browsers without WebSocket support
- **Redis** for WebSocket session management across instances
- **REST API** for message history and likes

### Architecture
```
Mobile/Web Client
    |
    | WebSocket (STOMP)
    v
API Gateway
    |
    v
Chat Service (Spring WebSocket)
    |
    +-- MySQL (message persistence)
    +-- Redis (session management)
    +-- Activity Service (membership validation via Feign)
```

### Message Flow
1. Client connects via WebSocket (`/ws/chat`)
2. Client subscribes to activity topic (`/topic/activity/{activityId}`)
3. Client sends message to `/app/chat/{activityId}/send`
4. Chat Service validates sender is activity member
5. Chat Service saves to database
6. Chat Service broadcasts to all subscribers of topic

---

## Cost Analysis

### Current State
- **Monolith:** $22/month
  - App Server (1GB): $7
  - MySQL (2GB): $10
  - Redis (512MB): $5

### Selective Microservices (3 services)
- **Total:** $73/month (3.3x increase)
  - Modular Monolith (1GB): $7
  - Auth Service (1GB): $7
  - Activity Service (1.5GB): $10
  - Chat Service (512MB): $5
  - API Gateway (512MB): $5
  - MySQL databases: $34 total
  - Redis (shared, 512MB): $5

### Selective Microservices (4 services, with User)
- **Total:** $85/month (3.9x increase)
  - Add User Service (512MB): $5
  - Add MySQL user_db: $7

### 5-Year Total Cost
- **Monolith:** $1,320
- **Selective (3 services):** $4,380 (+$3,060 learning investment)
- **Selective (4 services):** $5,100 (+$3,780 learning investment)

**Justification:** $600-750/year for practical microservices experience and portfolio value

---

## Timeline

**Total Duration:** 3-5 months (depending on optional User Service)

| Phase | Duration | Activities |
|-------|----------|-----------|
| **Phase 1** | Weeks 1-3 | Infrastructure setup (API Gateway, Redis, shared libraries) |
| **Phase 2a** | Weeks 4-9 | Auth Service extraction and deployment |
| **Phase 2b** | Weeks 10-15 | Activity Service extraction and deployment |
| **Phase 2c** | Weeks 16-19 | Chat Service with WebSocket implementation |
| **Phase 2d** | Weeks 20-23 | (Optional) User Service extraction |
| **Phase 3** | Weeks 24+ | Monitoring, optimization, learning consolidation |

**Recommended Pace:** 10-15 hours/week part-time implementation

---

## Learning Objectives

### Primary Goals
1. **Microservices Architecture Patterns**
   - Service boundaries and domain-driven design
   - Inter-service communication (REST, events)
   - Data management in distributed systems

2. **WebSocket in Distributed Systems**
   - Real-time messaging implementation
   - Session management across instances
   - Fallback strategies (SockJS)

3. **Service Orchestration**
   - API Gateway routing and authentication
   - Circuit breakers and resilience patterns
   - Distributed tracing

4. **Database Strategies**
   - MySQL per service
   - Optional NoSQL (Cassandra) for specific use cases
   - Data consistency in microservices

5. **DevOps and Deployment**
   - Independent service deployment
   - Railway platform expertise
   - Monitoring and observability

### Secondary Goals
- Resume/portfolio enhancement
- Real-world distributed systems experience
- Understanding trade-offs (complexity vs. benefits)
- Team scalability patterns

---

## Flexibility and Exit Strategy

### Scale Up (if needed)
- Extract Social Service for independent scaling
- Extract Notification Service for higher throughput
- Extract Media Service for storage optimization
- Move to full 8-service architecture

### Scale Down (if desired)
- Consolidate services back into modular monolith
- Keep learnings and architecture patterns
- Reduce costs while maintaining code quality
- Re-extract specific services if traffic justifies

### Pivot Points
- After Auth Service: Evaluate complexity vs. benefit
- After Activity Service: Assess operational overhead
- After Chat Service: Decide on User Service
- After 6 months: Comprehensive cost/benefit review

---

## Success Metrics

### Technical
- [ ] Auth Service handles all authentication traffic independently
- [ ] Activity Service scales to handle 2x current load
- [ ] Chat WebSocket maintains <100ms message delivery latency
- [ ] API Gateway routes requests with <20ms overhead
- [ ] Circuit breakers prevent cascading failures

### Learning
- [ ] Team understands microservices trade-offs through experience
- [ ] WebSocket implementation works reliably in production
- [ ] Monitoring and observability provide clear insights
- [ ] Can deploy services independently without coordination

### Business
- [ ] User experience remains unchanged or improves
- [ ] Deployment frequency increases (more confidence)
- [ ] Time to recover from failures decreases
- [ ] Cost increase justified by learnings and capabilities

---

## Key Decisions Summary

| Decision | Choice | Rationale |
|----------|--------|-----------|
| **Approach** | Selective Microservices | Learning value + manageable complexity |
| **Service Count** | 3-4 core services | Focused learning without overwhelming operational burden |
| **Primary Services** | Auth, Activity, Chat | Highest traffic + learning opportunities |
| **Database** | MySQL | Familiar, reliable, team expertise |
| **Chat Technology** | WebSocket (STOMP) | Real-time capability + learning objective |
| **NoSQL Option** | Cassandra (optional) | Only if chat volume justifies (>100k msg/day) |
| **Remaining Services** | Modular Monolith | Keep complexity manageable, extract later if needed |
| **Cost Acceptance** | 3.3-3.9x increase | Investment in learning and experience |
| **Timeline** | 3-5 months | Realistic for part-time implementation |

---

## Next Steps

1. **Immediate (Week 0)**
   - [ ] Review and finalize architecture diagrams
   - [ ] Set up Railway project structure
   - [ ] Create GitHub repository structure (monorepo)

2. **Short-term (Weeks 1-3)**
   - [ ] Implement API Gateway
   - [ ] Set up shared Redis instance
   - [ ] Create shared library modules (DTOs, utilities)
   - [ ] Configure CI/CD pipelines

3. **Medium-term (Weeks 4-16)**
   - [ ] Extract and deploy Auth Service
   - [ ] Extract and deploy Activity Service
   - [ ] Extract and deploy Chat Service with WebSocket

4. **Long-term (Weeks 17+)**
   - [ ] (Optional) Extract User Service
   - [ ] Monitor and optimize
   - [ ] Document learnings
   - [ ] Evaluate next steps (expand, maintain, or consolidate)

---

## Documentation References

- **[MICROSERVICES_DECISION_GUIDE.md](./MICROSERVICES_DECISION_GUIDE.md)** - Comprehensive analysis and decision framework
- **[MICROSERVICES_IMPLEMENTATION_PLAN.md](./MICROSERVICES_IMPLEMENTATION_PLAN.md)** - Step-by-step migration roadmap
- **[MICROSERVICES_ARCHITECTURE.md](./MICROSERVICES_ARCHITECTURE.md)** - Architectural details and patterns

---

## Approval

**Decision Approved By:** Project Owner  
**Date:** November 9, 2025  
**Review Date:** June 9, 2026 (6 months post-implementation)

---

**Document Maintainer:** Backend Team  
**Last Updated:** November 9, 2025  
**Version:** 1.0

