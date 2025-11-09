# Microservices Architecture Documentation

**Navigation Guide for Spawn App Microservices Implementation**

---

## 🚦 Decision Made: Selective Microservices for Learning

**Date:** November 9, 2025  
**Approach:** Extract 3-4 core services while keeping other domains in modular monolith

```
┌─────────────────────────────────────────────────────────────┐
│  ✅ DECISION: Selective Microservices for Learning          │
│                                                              │
│  Extract:                                                    │
│  ├─ Auth Service (highest traffic)                          │
│  ├─ Activity Service (highest traffic)                      │
│  ├─ Chat Service (WebSocket implementation)                 │
│  └─ User Service (optional)                                 │
│                                                              │
│  Keep in Monolith:                                          │
│  ├─ Social Service                                          │
│  ├─ Notification Service                                    │
│  ├─ Media Service                                           │
│  └─ Analytics Service                                       │
│                                                              │
│  Database: MySQL (all services)                             │
│  Optional: Cassandra for Chat (if volume >100k msg/day)     │
│  Cost: $73-85/month (3.3-3.9x increase)                     │
│  Timeline: 3-5 months                                       │
└─────────────────────────────────────────────────────────────┘
```

---

## 📚 Document Index

### 1. [SELECTIVE_MICROSERVICES_DECISION.md](./SELECTIVE_MICROSERVICES_DECISION.md) ⭐ **START HERE**

**Purpose:** Summary of selective microservices decision and implementation approach

**Contents:**
- ✅ Final decision rationale (learning + selective scaling)
- 🎯 Services to extract (Auth, Activity, Chat, optionally User)
- 🗄️ Database strategy (MySQL primary, Cassandra optional)
- 🔌 WebSocket implementation details for Chat Service
- 💰 Cost analysis ($73-85/month for selective approach)
- 📅 Timeline and learning objectives

**Key Takeaway:** 
> Implementing selective microservices (3-4 services) for **learning experience** while keeping complexity and costs manageable. Focus on Auth (highest traffic), Activity (highest traffic), and Chat (WebSocket learning opportunity).

**Time to Read:** 15-20 minutes

---

### 2. [MICROSERVICES_DECISION_GUIDE.md](./MICROSERVICES_DECISION_GUIDE.md) 

**Purpose:** Comprehensive analysis leading to selective microservices decision

**Contents:**
- ✅ Updated recommendation: Selective microservices for learning
- 📊 Benefits vs drawbacks analysis
- 💰 Railway cost analysis (updated for selective approach)
- 🏗️ Comparison: Monolith vs Modular Monolith vs Selective vs Full Microservices
- 📖 Action plan for selective implementation
- 🔌 WebSocket chat service details

**Updated Sections:**
- Phase 1: Prepare core infrastructure (3-4 weeks)
- Phase 2: Extract priority services (Auth, Activity, Chat)
- Phase 3: Keep remaining services in modular monolith
- Phase 4: WebSocket chat implementation

**Time to Read:** 40-50 minutes

---

### 3. [MICROSERVICES_ARCHITECTURE.md](./MICROSERVICES_ARCHITECTURE.md)

**Purpose:** Technical architecture for microservices

**Contents:**
- Service boundaries and descriptions
- Database-per-service strategy (MySQL focus)
- Inter-service communication patterns
- Data consistency strategies
- Security and monitoring setup

**Applicable Services:**
```
Priority Extract:
1. Auth Service       - JWT, OAuth (Google, Apple)
2. Activity Service   - Activities, types, locations
3. Chat Service       - WebSocket real-time messaging
4. User Service       - (Optional) User profiles, search

Keep in Monolith:
5. Social Service     - Friendships, requests, blocking
6. Notification Service - Push notifications
7. Media Service      - S3 uploads, profile pictures
8. Analytics Service  - Reports, feedback, share links
```

**Time to Read:** 45-60 minutes

---

### 4. [MICROSERVICES_IMPLEMENTATION_PLAN.md](./MICROSERVICES_IMPLEMENTATION_PLAN.md)

**Purpose:** Step-by-step migration plan tailored for selective approach on Railway

**Contents:**
- Phase 1: Preparation & Infrastructure (2-3 weeks)
- Phase 2: Extract Core Services
  - 2.1: Auth Service (4-6 weeks)
  - 2.2: Activity Service (4-6 weeks)
  - 2.3: Chat Service with WebSocket (3-4 weeks)
  - 2.4: User Service - Optional (3-4 weeks)
- Phase 3: API Gateway & Service Mesh (2-3 weeks)
- Phase 4: Optimization & Monitoring (ongoing)

**Total Timeline:** 3-5 months

**Railway-Specific:**
- MySQL provisioning per service
- Shared Redis with namespacing
- WebSocket deployment on Railway
- Cost optimization strategies

**Time to Read:** 60-90 minutes (reference document)

---

## 🎯 Quick Reference

| Topic | Information |
|-------|-------------|
| **Approach** | Selective microservices (3-4 services) |
| **Services to Extract** | Auth, Activity, Chat (WebSocket), optionally User |
| **Services in Monolith** | Social, Notification, Media, Analytics |
| **Database** | MySQL for all services (Cassandra optional for Chat) |
| **Cost** | $73-85/month (3.3-3.9x increase from $22/month) |
| **Timeline** | 3-5 months (10-15 hours/week part-time) |
| **Primary Goal** | Learning experience with microservices |
| **Secondary Goal** | Scale Auth and Activity services |

---

## 📊 Selective Microservices Architecture

```
┌──────────────────────────────────────────────────────────────┐
│                       API Gateway                             │
│               (Routing, Auth, Rate Limiting)                  │
└──────────────────────────────────────────────────────────────┘
                            │
        ┌───────────────────┼───────────────────┐
        │                   │                   │
        ▼                   ▼                   ▼
┌──────────────┐    ┌──────────────┐    ┌──────────────┐
│   Auth       │    │   Activity   │    │   Chat       │
│   Service    │    │   Service    │    │   Service    │
│              │    │              │    │  (WebSocket) │
│  (1GB RAM)   │    │  (1.5GB RAM) │    │  (512MB RAM) │
│   MySQL      │    │   MySQL      │    │   MySQL      │
└──────────────┘    └──────────────┘    └──────────────┘

        (Optional)
        ▼
┌──────────────┐
│   User       │
│   Service    │
│              │
│  (512MB RAM) │
│   MySQL      │
└──────────────┘

┌──────────────────────────────────────────────────────────────┐
│                   Modular Monolith                            │
│  (Social, Notification, Media, Analytics)                     │
│                                                               │
│  1GB RAM + MySQL                                              │
└──────────────────────────────────────────────────────────────┘

┌──────────────────────────────────────────────────────────────┐
│              Shared Infrastructure                            │
│  Redis (Caching + WebSocket Sessions)                        │
└──────────────────────────────────────────────────────────────┘
```

---

## 🚀 Implementation Roadmap

### Phase 1: Infrastructure (Weeks 1-3)
- [ ] Set up API Gateway (Spring Cloud Gateway)
- [ ] Configure shared Redis on Railway
- [ ] Create shared library modules (DTOs, utilities)
- [ ] Set up distributed tracing (Spring Cloud Sleuth)

### Phase 2a: Auth Service (Weeks 4-9)
- [ ] Extract Auth Service
- [ ] Migrate OAuth (Google, Apple)
- [ ] Implement JWT generation/validation
- [ ] Deploy to Railway with MySQL database
- [ ] Test end-to-end authentication flow

### Phase 2b: Activity Service (Weeks 10-15)
- [ ] Extract Activity Service
- [ ] Migrate activity CRUD operations
- [ ] Migrate activity types and templates
- [ ] Deploy to Railway with MySQL database
- [ ] Test with Auth Service integration

### Phase 2c: Chat Service (Weeks 16-19)
- [ ] Extract Chat Service
- [ ] **Implement WebSocket (STOMP) for real-time messaging**
- [ ] Implement REST API for message history
- [ ] Configure minimal resources (512MB)
- [ ] Deploy to Railway with MySQL database
- [ ] Test real-time messaging flow

### Phase 2d: User Service (Weeks 20-23, Optional)
- [ ] Evaluate need based on Auth/Activity/Chat experience
- [ ] Extract User Service if beneficial
- [ ] Migrate user profile and search functionality
- [ ] Deploy to Railway with MySQL database

### Phase 3: Monitoring & Optimization (Weeks 24+)
- [ ] Monitor service performance and costs
- [ ] Optimize resource allocation
- [ ] Document learnings
- [ ] Decide: expand, maintain, or consolidate

---

## 💰 Cost Breakdown

### Current State
| Component | Cost |
|-----------|------|
| Monolith (1GB) | $7 |
| MySQL (2GB) | $10 |
| Redis (512MB) | $5 |
| **Total** | **$22/month** |

### Selective Microservices (3 services)
| Component | Cost |
|-----------|------|
| Modular Monolith (1GB) | $7 |
| Auth Service (1GB) | $7 |
| Activity Service (1.5GB) | $10 |
| Chat Service (512MB) | $5 |
| API Gateway (512MB) | $5 |
| MySQL databases (4 × varies) | $34 |
| Redis (512MB shared) | $5 |
| **Total** | **$73/month** |

### With User Service (4 services)
| Additional | Cost |
|------------|------|
| User Service (512MB) | $5 |
| MySQL user_db (1GB) | $7 |
| **New Total** | **$85/month** |

**Investment:** $51-63/month extra ($612-756/year) for learning and experience

---

## 🎓 Learning Objectives

### Primary
1. **Microservices Architecture**
   - Service boundaries and domain-driven design
   - Inter-service communication (REST + events)
   - Database-per-service patterns

2. **WebSocket in Distributed Systems**
   - Real-time messaging implementation
   - Session management across instances
   - Spring WebSocket (STOMP) configuration

3. **Service Orchestration**
   - API Gateway routing and auth
   - Circuit breakers (Resilience4j)
   - Distributed tracing (Sleuth)

### Secondary
- MySQL per-service strategy
- Optional NoSQL (Cassandra) for chat
- Independent deployment workflows
- Railway platform expertise

---

## 📞 Key Decisions Summary

| Question | Answer |
|----------|--------|
| **Microservices or Monolith?** | Selective microservices (hybrid) |
| **Why?** | Learning experience + scale key services |
| **How many services?** | 3-4 core services |
| **Which services?** | Auth, Activity, Chat (WebSocket), optionally User |
| **Database?** | MySQL (all services) |
| **What about NoSQL?** | Optional Cassandra for Chat if volume grows |
| **What stays in monolith?** | Social, Notification, Media, Analytics |
| **Cost acceptable?** | Yes, 3.3-3.9x increase as learning investment |
| **Timeline?** | 3-5 months part-time |
| **Can we scale back?** | Yes, can consolidate if needed |

---

## 🔄 Flexibility Options

### Scale Up (if needed)
- Extract Social Service for independent scaling
- Extract Notification Service for higher throughput
- Extract Media Service for storage optimization
- Move toward full 8-service architecture

### Scale Down (if desired)
- Consolidate services back into modular monolith
- Keep architecture patterns and learnings
- Reduce costs while maintaining code quality

### Pivot Points
- After Auth Service: Evaluate complexity
- After Activity Service: Assess operational overhead
- After Chat Service: Decide on User Service extraction
- After 6 months: Comprehensive review

---

## 📚 Next Steps

1. **Review** [SELECTIVE_MICROSERVICES_DECISION.md](./SELECTIVE_MICROSERVICES_DECISION.md) (decision summary)
2. **Study** [MICROSERVICES_IMPLEMENTATION_PLAN.md](./MICROSERVICES_IMPLEMENTATION_PLAN.md) (detailed roadmap)
3. **Plan** Infrastructure setup (Week 0-3)
4. **Execute** Service extraction starting with Auth

---

**Document Status:** Updated for Selective Microservices Approach (November 9, 2025)  
**Version:** 2.0  
**Next Review:** June 9, 2026 (6 months post-implementation)

---

**Start Reading:** [SELECTIVE_MICROSERVICES_DECISION.md](./SELECTIVE_MICROSERVICES_DECISION.md) (15-20 min)

