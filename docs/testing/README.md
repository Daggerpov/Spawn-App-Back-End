# Testing Initiative - Comprehensive Coverage Plan

**Project:** Spawn App Back-End  
**Initiative Start:** December 23, 2025  
**Current Coverage:** ~30%  
**Target Coverage:** 95%+  
**Timeline:** 10 weeks (Phases 1-6)

---

## 📊 Quick Status

| Metric | Current | Target | Progress |
|--------|---------|--------|----------|
| **Line Coverage** | 30% | 95% | ▱▱▱▱▱▱▱▱▱▱ 0% |
| **Test Files** | 35 | 100+ | ▰▰▰▱▱▱▱▱▱▱ 35% |
| **Critical Tests** | 0 | 12 | ▱▱▱▱▱▱▱▱▱▱ 0% |
| **Phase** | Not Started | Phase 1 | 🔴 |

---

## 📚 Documentation Index

### 1. **[CODE_COVERAGE_ANALYSIS.md](./CODE_COVERAGE_ANALYSIS.md)** ⭐ Start Here
   - **What:** Comprehensive analysis of current coverage and gaps
   - **Includes:**
     - Detailed breakdown by module (Activity, Auth, User, etc.)
     - 89 specific components needing tests
     - Priority-based implementation plan (Phases 1-6)
     - Coverage targets by layer
   - **Use When:** Understanding overall strategy

### 2. **[TESTING_QUICK_START_GUIDE.md](./TESTING_QUICK_START_GUIDE.md)** 🚀 Start Coding
   - **What:** Practical guide to writing tests immediately
   - **Includes:**
     - 3 complete test examples (JWT, UserMapper, Cache)
     - Test templates for Controllers, Services, Repositories
     - Running and debugging tests
     - Best practices checklist
   - **Use When:** Ready to write tests NOW

### 3. **[TEST_COVERAGE_TRACKER.md](./TEST_COVERAGE_TRACKER.md)** 📈 Track Progress
   - **What:** Detailed tracking spreadsheet for all 100+ tests
   - **Includes:**
     - Phase-by-phase checklist
     - Daily/weekly goals
     - Coverage metrics
     - Blocker tracking
   - **Use When:** Tracking progress, planning sprints

---

## 🎯 Critical Gaps Identified

### 🔴 **Security-Critical (Immediate Priority)**
- `JWTService` - Token generation/validation (0% coverage)
- `VerificationCodeGenerator` - Security codes (0% coverage)
- `PhoneNumberValidator` - Input validation (0% coverage)
- `ShareCodeGenerator` - Share link security (0% coverage)

### 🔴 **Core Functionality (High Priority)**
- `UserMapper` - Most frequently used mapper (0% coverage)
- `ActivityMapper` - Core business logic (0% coverage)
- `CacheService` - Performance-critical (0% coverage)
- `FCMService` - Push notifications (0% coverage)

### 🟡 **Coverage Gaps by Layer**
- **Repositories:** Only 15% covered (3 of 20 tested)
- **Utilities:** 0% covered (0 of 38 tested) ⚠️
- **Controllers:** 50% covered (9 of 18 tested)
- **Services:** 37% covered (19 of 51 tested)

---

## 🗓️ 10-Week Implementation Plan

### **Phase 1: Critical Security & Core** (Week 1-2)
- **Goal:** Test all security-critical components
- **Deliverables:** 10-12 test files
- **Coverage:** 30% → 45%
- **Tests:**
  - JWTService, validators, core mappers
  - Cache management (CacheService, RetryHelper)
  
**Quick Start:**
```bash
# Start with these 3 tests
1. JWTServiceTests (copy from Quick Start Guide)
2. UserMapperTests (copy from Quick Start Guide)
3. CacheServiceTests (copy from Quick Start Guide)
```

### **Phase 2: High-Traffic Features** (Week 3-4)
- **Goal:** Test heavily-used user and analytics features
- **Deliverables:** 15-18 test files
- **Coverage:** 45% → 70%
- **Tests:**
  - 4 User controllers + services
  - Analytics controllers
  - Repository layer (6 repositories)

### **Phase 3: External Services** (Week 5-6)
- **Goal:** Test integrations and background services
- **Deliverables:** 12-15 test files
- **Coverage:** 70% → 85%
- **Tests:**
  - FCMService, EmailService
  - Background cleanup services
  - Remaining repositories

### **Phase 4: Domain Validation** (Week 7)
- **Goal:** Entity integrity and relationships
- **Deliverables:** 8-10 test files
- **Coverage:** 85% → 90%

### **Phase 5: Exception Handling** (Week 8)
- **Goal:** Error handling and edge cases
- **Deliverables:** 10-12 test files
- **Coverage:** 90% → 93%

### **Phase 6: Performance & Integration** (Week 9-10)
- **Goal:** E2E testing and performance validation
- **Deliverables:** 8-10 test files
- **Coverage:** 93% → **95%+**

---

## 🚀 Getting Started (First 30 Minutes)

### Step 1: Fix Java Version (5 min)
```bash
# Your system has Java 25, project needs Java 17
./scripts/build/build-with-java17.sh
```

### Step 2: Verify JaCoCo Setup (2 min)
JaCoCo has been added to `pom.xml`. Verify:
```bash
./mvnw clean test
open target/site/jacoco/index.html
```

### Step 3: Copy First Test Template (10 min)
Open `do./TESTING_QUICK_START_GUIDE.md` and copy `JWTServiceTests` template.

Create file:
```bash
touch src/test/java/com/danielagapov/spawn/ServiceTests/JWTServiceTests.java
```

Paste the template, adjust for your `JWTService` implementation.

### Step 4: Run Your First Test (5 min)
```bash
./mvnw test -Dtest=JWTServiceTests
```

### Step 5: Check Coverage (5 min)
```bash
./mvnw clean test jacoco:report
open target/site/jacoco/index.html
```

Look for the `JWTService` class and see green coverage!

### Step 6: Update Tracker (3 min)
In `TEST_COVERAGE_TRACKER.md`, mark JWTServiceTests as ✅ Done.

---

## 📋 Test Writing Checklist

For each new test file:
- [ ] Copy appropriate template from Quick Start Guide
- [ ] Use `@DisplayName` for clarity
- [ ] Follow Given-When-Then structure
- [ ] Test happy path + error cases
- [ ] Test boundary conditions
- [ ] Use `@Nested` classes for organization
- [ ] Achieve >= 80% coverage for target class
- [ ] All tests pass: `./mvnw test -Dtest=YourTest`
- [ ] Update TEST_COVERAGE_TRACKER.md
- [ ] Commit with message: "test: add YourService tests"

---

## 📊 Current Test Inventory (35 files)

### ✅ **What's Already Tested**

**Controllers (9):**
- ActivityController, ActivityTypeController
- AuthController
- ChatMessageController
- BlockedUserController, CalendarController
- FriendRequestController
- NotificationController
- UserController

**Services (19):**
- ActivityService, ActivityTypeService, ActivityExpirationService
- AuthService, OAuthService
- BetaAccessSignUpService, FeedbackSubmissionService
- BlockedUserService, ChatMessageService
- FriendRequestService
- FuzzySearchService, UserSearchService
- LocationService
- NotificationService
- S3Service
- UserService, UserInterestService

**Repositories (3):**
- IActivityRepository
- IFriendshipRepository
- IUserRepository

**Integration (2):**
- ActivityTypeIntegrationTests
- FriendshipIntegrationTests

---

## ❌ **What's Missing (65+ files needed)**

### Critical (12 files)
- JWTService, EmailService, FCMService
- Core mappers (User, Activity, ActivityType)
- Validators (Phone, Email)
- Cache utilities
- Security generators

### High Priority (30+ files)
- 4 User controllers + services
- 4 Analytics controllers + services
- 17 Repositories
- Additional mappers

### Medium Priority (23+ files)
- Background services
- Entity validation tests
- Exception handlers
- Relationship tests

---

## 🎯 Success Metrics

### Quantitative
- ✅ **95%+ line coverage** (from 30%)
- ✅ **85%+ branch coverage** (from ~20%)
- ✅ **100+ test files** (from 35)
- ✅ **< 5 min full test suite** execution time

### Qualitative
- ✅ Team confidence in making changes
- ✅ Tests catch regressions early
- ✅ Tests serve as documentation
- ✅ Easy to maintain and update

---

## 🛠️ Tools & Commands

### Running Tests
```bash
# All tests
./mvnw clean test

# Specific test class
./mvnw test -Dtest=JWTServiceTests

# Specific test method
./mvnw test -Dtest=JWTServiceTests#shouldGenerateValidToken

# All tests in package
./mvnw test -Dtest="com.danielagapov.spawn.ServiceTests.*"

# With coverage report
./mvnw clean test jacoco:report
```

### Viewing Coverage
```bash
# HTML report
open target/site/jacoco/index.html

# CSV report
cat target/site/jacoco/jacoco.csv

# XML report (for CI/CD)
cat target/site/jacoco/jacoco.xml
```

### Test Development
```bash
# Run tests in watch mode (with entr)
find src/test -name "*.java" | entr ./mvnw test -Dtest=JWTServiceTests

# Run with debug logging
./mvnw test -Dtest=JWTServiceTests -Dlogging.level.com.danielagapov.spawn=DEBUG
```

---

## 📖 Testing Standards

### Test Structure
```java
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("Service Name Tests")
class ServiceNameTests {
    
    @Nested
    @DisplayName("Feature Name Tests")
    class FeatureTests {
        
        @Test
        @DisplayName("Should succeed when valid input")
        void testHappyPath() {
            // Given - Setup test data
            // When - Execute the code under test
            // Then - Verify results with assertions
        }
    }
}
```

### Assertion Style
Use AssertJ for fluent, readable assertions:
```java
assertThat(result)
    .isNotNull()
    .hasFieldOrPropertyWithValue("status", "success")
    .extracting("data")
    .isNotEmpty();
```

### Mocking Strategy
```java
// Mock external dependencies
@MockBean
private ExternalService externalService;

// Use real beans for simple components
@Autowired
private MapperClass mapper;

// Use test containers for databases (if needed)
@Container
static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15");
```

---

## 📞 Support & Resources

### Documentation
- **Analysis:** `CODE_COVERAGE_ANALYSIS.md` - Strategy and planning
- **Quick Start:** `TESTING_QUICK_START_GUIDE.md` - Code examples
- **Tracker:** `TEST_COVERAGE_TRACKER.md` - Progress tracking

### External Resources
- [JUnit 5 User Guide](https://junit.org/junit5/docs/current/user-guide/)
- [AssertJ Documentation](https://assertj.github.io/doc/)
- [Spring Boot Testing](https://spring.io/guides/gs/testing-web/)
- [JaCoCo Documentation](https://www.jacoco.org/jacoco/trunk/doc/)

### Common Issues
| Problem | Solution | Reference |
|---------|----------|-----------|
| Java version mismatch | Use `build-with-java17.sh` | Quick Start p.1 |
| Tests won't compile | Check imports match modular structure | TESTS_STATUS.md |
| Database errors | Verify `@ActiveProfiles("test")` | Quick Start p.8 |
| Coverage not showing | Check JaCoCo in pom.xml | This file |

---

## 🎉 Milestones

- [ ] **Milestone 1:** Phase 1 complete - 45% coverage (Week 2)
- [ ] **Milestone 2:** Phase 2 complete - 70% coverage (Week 4)
- [ ] **Milestone 3:** Phase 3 complete - 85% coverage (Week 6)
- [ ] **Milestone 4:** Phase 4 complete - 90% coverage (Week 7)
- [ ] **Milestone 5:** Phase 5 complete - 93% coverage (Week 8)
- [ ] **Milestone 6:** Phase 6 complete - **95%+ coverage** (Week 10) 🏆

---

## 🔄 Weekly Review Process

Every Friday:
1. Run full test suite with coverage
2. Update TEST_COVERAGE_TRACKER.md
3. Review and address any blockers
4. Plan next week's tests
5. Celebrate wins! 🎉

---

## ✨ Quick Reference

| Need | Document | Section |
|------|----------|---------|
| Strategy overview | CODE_COVERAGE_ANALYSIS.md | Executive Summary |
| Specific gaps | CODE_COVERAGE_ANALYSIS.md | Detailed Coverage Analysis |
| Write first test | TESTING_QUICK_START_GUIDE.md | Priority 1-3 |
| Test templates | TESTING_QUICK_START_GUIDE.md | Test Templates |
| Track progress | TEST_COVERAGE_TRACKER.md | Phase checklists |
| Daily goals | TEST_COVERAGE_TRACKER.md | Weekly Progress Log |

---

## 🚦 Getting Started Today

**You have everything you need to start:**

1. ✅ JaCoCo configured in pom.xml
2. ✅ Comprehensive analysis complete (89 components identified)
3. ✅ 3 priority test templates ready to use
4. ✅ Tracking system in place
5. ✅ 10-week plan with clear milestones

**Next Action:**
```bash
# Open the Quick Start Guide
open do./TESTING_QUICK_START_GUIDE.md

# Copy JWTServiceTests template
# Start writing tests!
```

---

**Questions?** Review the documentation or check the Common Issues section above.

**Let's achieve 95% coverage! 🎯**

