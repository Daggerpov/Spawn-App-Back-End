# Code Coverage Analysis and Testing Plan

**Date:** December 23, 2025  
**Status:** Analysis Complete - Implementation Plan Ready

## Executive Summary

### Current Coverage Statistics
- **Total Source Files:** 268 Java files
- **Total Test Files:** 35 test files
- **Test-to-Source Ratio:** 13.1%
- **Estimated Line Coverage:** ~25-30% (based on test distribution)

### Coverage by Layer
| Layer | Total Files | Tested Files | Coverage % | Status |
|-------|-------------|--------------|------------|--------|
| Controllers | 18 | 9 | 50% | 🟡 Partial |
| Services | 51 | 19 | 37% | 🟡 Partial |
| Repositories | 20 | 3 | 15% | 🔴 Critical |
| Utilities | 38 | 0 | 0% | 🔴 Critical |
| Exceptions | 24 | 0 | 0% | 🟡 Low Priority |
| Domain Models | 25+ | 0 | 0% | 🟡 Low Priority |
| DTOs | 80+ | 0 | 0% | ⚪ Excluded |
| Config | 18 | 2 | 11% | 🟡 Partial |

---

## Detailed Coverage Analysis by Module

### 1. Activity Module

#### ✅ **Currently Tested**
**Controllers:**
- ✅ `ActivityController` - ActivityControllerTests.java
- ✅ `ActivityTypeController` - ActivityTypeControllerTests.java

**Services:**
- ✅ `ActivityService` - ActivityServiceTests.java
- ✅ `ActivityTypeService` - ActivityTypeServiceTests.java
- ✅ `ActivityExpirationService` - ActivityExpirationServiceTimezoneTests.java
- ✅ `LocationService` - LocationServiceTests.java

**Repositories:**
- ✅ `IActivityRepository` - ActivityRepositoryTests.java

**Integration:**
- ✅ ActivityTypeIntegrationTests.java
- ✅ ActivityTypePerformanceTests.java

#### 🔴 **Missing Tests**
**Services:**
- ❌ `CalendarService` - **HIGH PRIORITY**
- ❌ `ActivityCacheCleanupService` - **MEDIUM PRIORITY**

**Repositories:**
- ❌ `IActivityTypeRepository` - **HIGH PRIORITY**
- ❌ `IActivityUserRepository` - **HIGH PRIORITY**
- ❌ `ILocationRepository` - **MEDIUM PRIORITY**

**Utilities:**
- ❌ `ActivityMapper` - **HIGH PRIORITY**
- ❌ `ActivityTypeMapper` - **HIGH PRIORITY**
- ❌ `LocationMapper` - **MEDIUM PRIORITY**

**Domain:**
- ❌ `Activity` entity validation tests
- ❌ `ActivityType` entity validation tests
- ❌ `ActivityUser` composite key tests

---

### 2. Auth Module

#### ✅ **Currently Tested**
**Controllers:**
- ✅ `AuthController` - AuthControllerTests.java

**Services:**
- ✅ `AuthService` - AuthServiceTests.java
- ✅ `OAuthService` - OAuthServiceTests.java

#### 🔴 **Missing Tests**
**Services:**
- ❌ `JWTService` - **CRITICAL PRIORITY** (Security-critical)
- ❌ `EmailService` - **HIGH PRIORITY**

**Repositories:**
- ❌ `IEmailVerificationRepository` - **HIGH PRIORITY**
- ❌ `IUserIdExternalIdMapRepository` - **MEDIUM PRIORITY**

**Utilities:**
- ❌ `OAuthProvider` enum tests
- ❌ `VerificationCodeGenerator` - **HIGH PRIORITY** (Security)

**Domain:**
- ❌ `EmailVerification` entity validation
- ❌ `UserIdExternalIdMap` entity validation

---

### 3. User Module

#### ✅ **Currently Tested**
**Controllers:**
- ✅ `UserController` - UserControllerTests.java
- ✅ `BlockedUserController` - BlockedUserControllerTests.java
- ✅ `CalendarController` - CalendarControllerTests.java

**Services:**
- ✅ `UserService` - UserServiceTests.java
- ✅ `BlockedUserService` - BlockedUserServiceTests.java
- ✅ `UserSearchService` - UserSearchServiceTests.java
- ✅ `UserInterestService` - UserInterestServiceTest.java
- ✅ `FuzzySearchService` - FuzzySearchServiceTest.java

**Repositories:**
- ✅ `IUserRepository` - UserRepositoryTests.java

#### 🔴 **Missing Tests**
**Controllers:**
- ❌ `UserInterestController` - **HIGH PRIORITY**
- ❌ `UserSocialMediaController` - **HIGH PRIORITY**
- ❌ `UserStatsController` - **HIGH PRIORITY**
- ❌ `ReportController` - **HIGH PRIORITY**

**Services:**
- ❌ `UserInfoService` - **HIGH PRIORITY**
- ❌ `UserSocialMediaService` - **HIGH PRIORITY**
- ❌ `UserStatsService` - **HIGH PRIORITY**

**Repositories:**
- ❌ `IBlockedUserRepository` - **HIGH PRIORITY**
- ❌ `IUserSocialMediaRepository` - **MEDIUM PRIORITY**
- ❌ `UserInterestRepository` - **HIGH PRIORITY**

**Utilities:**
- ❌ `UserMapper` - **CRITICAL PRIORITY** (Core mapper)
- ❌ `PhoneNumberValidator` - **HIGH PRIORITY** (Security)
- ❌ `PhoneNumberMatchingUtil` - **HIGH PRIORITY**
- ❌ `FriendUserMapper` - **MEDIUM PRIORITY**

---

### 4. Social Module

#### ✅ **Currently Tested**
**Controllers:**
- ✅ `FriendRequestController` - FriendRequestControllerTests.java

**Services:**
- ✅ `FriendRequestService` - FriendRequestServiceTests.java

**Repositories:**
- ✅ `IFriendshipRepository` - FriendshipRepositoryTests.java

**Integration:**
- ✅ FriendshipIntegrationTests.java

#### 🔴 **Missing Tests**
**Repositories:**
- ❌ `IFriendRequestsRepository` - **HIGH PRIORITY**

**Utilities:**
- ❌ `FriendRequestMapper` - **MEDIUM PRIORITY**
- ❌ `FetchFriendRequestMapper` - **MEDIUM PRIORITY**
- ❌ `BlockedUserMapper` - **MEDIUM PRIORITY**

**Domain:**
- ❌ `Friendship` entity validation
- ❌ `FriendRequest` entity state transitions

---

### 5. Chat Module

#### ✅ **Currently Tested**
**Controllers:**
- ✅ `ChatMessageController` - ChatMessageControllerTests.java

**Services:**
- ✅ `ChatMessageService` - ChatMessageServiceTests.java

#### 🔴 **Missing Tests**
**Repositories:**
- ❌ `IChatMessageRepository` - **HIGH PRIORITY**
- ❌ `IChatMessageLikesRepository` - **MEDIUM PRIORITY**

**Utilities:**
- ❌ `ChatMessageMapper` - **HIGH PRIORITY**
- ❌ `ChatMessageLikesMapper` - **MEDIUM PRIORITY**

**Domain:**
- ❌ `ChatMessage` entity validation
- ❌ `ChatMessageLikes` relationship tests

---

### 6. Notification Module

#### ✅ **Currently Tested**
**Controllers:**
- ✅ `NotificationController` - NotificationControllerTests.java

**Services:**
- ✅ `NotificationService` - NotificationServiceTests.java

#### 🔴 **Missing Tests**
**Services:**
- ❌ `FCMService` - **CRITICAL PRIORITY** (External integration)

**Repositories:**
- ❌ `IDeviceTokenRepository` - **HIGH PRIORITY**
- ❌ `INotificationPreferencesRepository` - **HIGH PRIORITY**

**Domain:**
- ❌ `DeviceToken` entity validation
- ❌ `NotificationPreferences` entity validation

---

### 7. Analytics Module

#### ✅ **Currently Tested**
**Services:**
- ✅ `BetaAccessSignUpService` - BetaAccessSignUpServiceTests.java
- ✅ `FeedbackSubmissionService` - FeedbackSubmissionServiceTests.java

#### 🔴 **Missing Tests**
**Controllers:**
- ❌ `BetaAccessSignUpController` - **MEDIUM PRIORITY**
- ❌ `FeedbackSubmissionController` - **MEDIUM PRIORITY**
- ❌ `SearchAnalyticsController` - **HIGH PRIORITY**
- ❌ `ShareLinkController` - **HIGH PRIORITY**

**Services:**
- ❌ `SearchAnalyticsService` - **HIGH PRIORITY**
- ❌ `ShareLinkService` - **HIGH PRIORITY**
- ❌ `ShareLinkCleanupService` - **MEDIUM PRIORITY**
- ❌ `ReportContentService` - **HIGH PRIORITY**
- ❌ `CacheService` - **CRITICAL PRIORITY** (Performance-critical)

**Repositories:**
- ❌ `IBetaAccessSignUpRepository` - **LOW PRIORITY**
- ❌ `IFeedbackSubmissionRepository` - **LOW PRIORITY**
- ❌ `IReportedContentRepository` - **HIGH PRIORITY**
- ❌ `ShareLinkRepository` - **HIGH PRIORITY**

**Utilities:**
- ❌ `ShareCodeGenerator` - **HIGH PRIORITY** (Security)
- ❌ `BetaAccessSignUpMapper` - **LOW PRIORITY**
- ❌ `FeedbackSubmissionMapper` - **LOW PRIORITY**

---

### 8. Media Module

#### ✅ **Currently Tested**
**Services:**
- ✅ `S3Service` - S3ServiceTests.java

#### 🔴 **Missing Tests**
- All tests exist (1/1 service tested)

---

### 9. Shared/Utilities Module

#### ✅ **Currently Tested**
- ⚠️ **NONE** - This is a major gap

#### 🔴 **Missing Tests - CRITICAL**
**Cache Utilities:**
- ❌ `CacheEvictionHelper` - **CRITICAL PRIORITY**
- ❌ `CacheNames` validation

**Core Utilities:**
- ❌ `RetryHelper` - **CRITICAL PRIORITY** (Resilience)
- ❌ `LoggingUtils` - **MEDIUM PRIORITY**
- ❌ `Triple` data structure
- ❌ `ErrorResponse` serialization

**Validators:**
- ❌ `PhoneNumberValidator` - **HIGH PRIORITY**
- ❌ `PhoneNumberMatchingUtil` - **HIGH PRIORITY**

**Mappers:** (All HIGH priority)
- ❌ `UserMapper`
- ❌ `ActivityMapper`
- ❌ `ActivityTypeMapper`
- ❌ `ChatMessageMapper`
- ❌ `FriendRequestMapper`
- ❌ And 5 more mappers...

---

### 10. Exception Handling

#### 🔴 **Missing Tests**
**Base Exception Handlers:**
- ❌ `BaseExceptionHandler` - **HIGH PRIORITY**
- ❌ Custom exception responses

**Custom Exceptions:** (MEDIUM Priority)
- ❌ `AccountAlreadyExistsException`
- ❌ `ActivityFullException`
- ❌ `TokenExpiredException`
- ❌ And 20+ more custom exceptions

---

## Priority-Based Implementation Plan

### 🔴 **Phase 1: Critical Security & Core Functionality (Week 1-2)**
Priority: **CRITICAL** - These gaps pose security risks or affect core functionality

1. **Security-Critical Services**
   - `JWTService` - Token generation/validation
   - `VerificationCodeGenerator` - Code generation security
   - `PhoneNumberValidator` - Input validation
   - `ShareCodeGenerator` - Share code security

2. **Core Mappers**
   - `UserMapper` - Most frequently used
   - `ActivityMapper` - Core functionality
   - `ChatMessageMapper` - Message handling

3. **Cache Management**
   - `CacheService` - Performance critical
   - `CacheEvictionHelper` - Memory management
   - `RetryHelper` - Resilience

**Estimated Test Files:** 10-12 new test classes  
**Estimated Coverage Increase:** +15-20%

---

### 🟡 **Phase 2: High-Traffic Features (Week 3-4)**
Priority: **HIGH** - Features with heavy usage

4. **User Module Completion**
   - `UserInterestController` + tests
   - `UserSocialMediaController` + tests
   - `UserStatsController` + tests
   - `UserInfoService` + tests
   - Repository tests for user-related data

5. **Analytics & Reporting**
   - `SearchAnalyticsController` + tests
   - `ShareLinkController` + tests
   - `ReportContentService` + tests
   - `SearchAnalyticsService` + tests

6. **Repository Layer**
   - `IActivityTypeRepository` tests
   - `IActivityUserRepository` tests
   - `IChatMessageRepository` tests
   - `IBlockedUserRepository` tests
   - `IDeviceTokenRepository` tests

**Estimated Test Files:** 15-18 new test classes  
**Estimated Coverage Increase:** +20-25%

---

### 🟢 **Phase 3: Integration & External Services (Week 5-6)**
Priority: **MEDIUM** - External integrations and cleanup services

7. **External Service Integration**
   - `FCMService` - Firebase Cloud Messaging
   - `EmailService` - Email delivery

8. **Background Services**
   - `ActivityCacheCleanupService`
   - `ShareLinkCleanupService`

9. **Calendar & Location**
   - `CalendarService` complete tests
   - `ILocationRepository` tests
   - `LocationMapper` tests

10. **Additional Repositories**
    - `IEmailVerificationRepository`
    - `IFriendRequestsRepository`
    - `INotificationPreferencesRepository`
    - `ShareLinkRepository`

**Estimated Test Files:** 12-15 new test classes  
**Estimated Coverage Increase:** +15-18%

---

### 🔵 **Phase 4: Domain Models & Entity Validation (Week 7)**
Priority: **MEDIUM** - Ensure entity integrity

11. **Entity Validation Tests**
    - `User` entity constraints
    - `Activity` entity validation
    - `FriendRequest` state transitions
    - Composite key validations

12. **Relationship Tests**
    - User-Activity relationships
    - Friendship bidirectionality
    - Chat message associations

**Estimated Test Files:** 8-10 new test classes  
**Estimated Coverage Increase:** +8-10%

---

### ⚪ **Phase 5: Exception Handling & Edge Cases (Week 8)**
Priority: **LOW-MEDIUM** - Completeness

13. **Exception Handler Tests**
    - `BaseExceptionHandler` behavior
    - Custom exception responses
    - Error response formatting

14. **Edge Case Coverage**
    - Boundary value testing
    - Concurrent access scenarios
    - Race condition tests

**Estimated Test Files:** 10-12 new test classes  
**Estimated Coverage Increase:** +5-8%

---

### 🎯 **Phase 6: Performance & Integration (Week 9-10)**
Priority: **OPTIMIZATION**

15. **Performance Test Suite**
    - Load testing for high-traffic endpoints
    - Cache performance validation
    - Database query optimization tests

16. **End-to-End Integration Tests**
    - Complete user journey tests
    - Activity lifecycle tests
    - Friend request workflows
    - Chat message flows

17. **Contract Testing**
    - API contract validation
    - DTO serialization tests
    - External API mocking

**Estimated Test Files:** 8-10 new test classes  
**Estimated Coverage Increase:** +5-7%

---

## Target Coverage Goals

### By Phase Completion
| Phase | Phase Coverage Target | Cumulative Coverage | Status |
|-------|----------------------|---------------------|--------|
| Current | ~25-30% | 30% | ✅ Baseline |
| Phase 1 | +15-20% | 45-50% | 🎯 Security |
| Phase 2 | +20-25% | 65-75% | 🎯 Functionality |
| Phase 3 | +15-18% | 80-85% | 🎯 Integration |
| Phase 4 | +8-10% | 88-90% | 🎯 Validation |
| Phase 5 | +5-8% | 93-95% | 🎯 Robustness |
| Phase 6 | +5-7% | **95-98%** | 🏆 Excellence |

### Coverage Targets by Layer
| Layer | Current | Phase 3 Target | Final Target |
|-------|---------|----------------|--------------|
| Controllers | 50% | 85% | 95% |
| Services | 37% | 80% | 95% |
| Repositories | 15% | 90% | 98% |
| Utilities | 0% | 85% | 95% |
| Domain | 0% | 70% | 85% |
| Integration | Good | Excellent | Comprehensive |

---

## Recommended Testing Standards

### 1. Unit Test Requirements
- **Minimum Coverage:** 80% line coverage per class
- **Branch Coverage:** 75% minimum
- **Test Types:**
  - Happy path scenarios
  - Error handling
  - Boundary conditions
  - Null/empty input validation

### 2. Integration Test Requirements
- **Coverage:** All critical user flows
- **Focus Areas:**
  - Multi-service interactions
  - Database transactions
  - Cache synchronization
  - External API calls

### 3. Test Structure Standards
```java
// Recommended test class structure
@SpringBootTest
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ServiceNameTests {
    
    @Autowired
    private ServiceToTest service;
    
    @MockBean
    private DependencyToMock dependency;
    
    @BeforeAll
    void setupClass() { }
    
    @BeforeEach
    void setup() { }
    
    @Nested
    @DisplayName("Feature Name Tests")
    class FeatureTests {
        
        @Test
        @DisplayName("Should succeed when valid input")
        void testHappyPath() { }
        
        @Test
        @DisplayName("Should throw exception when invalid input")
        void testErrorCase() { }
    }
}
```

### 4. Mocking Strategy
- **Use @MockBean for:**
  - External services (S3, FCM, Email)
  - Repositories (when testing services)
  - Time-dependent components
  
- **Use Real Beans for:**
  - Mappers and utilities
  - Simple validators
  - In-memory repositories (H2 for tests)

### 5. Test Data Management
- **Use @Sql scripts** for complex data setup
- **Use Test Builders** for entity creation
- **Use Test Fixtures** for common test data
- **Implement @DataJpaTest** for repository tests

---

## Implementation Approach

### Week-by-Week Breakdown

#### **Weeks 1-2: Phase 1 (Critical)**
- **Day 1-2:** JWT & Authentication tests
- **Day 3-4:** Core mapper tests (User, Activity)
- **Day 5-7:** Cache & utility tests
- **Day 8-10:** Validation & security tests

#### **Weeks 3-4: Phase 2 (High Priority)**
- **Day 1-3:** User module controller tests
- **Day 4-6:** Analytics controller tests
- **Day 7-10:** Repository layer tests

#### **Weeks 5-6: Phase 3 (External Services)**
- **Day 1-3:** FCM & Email service tests
- **Day 4-6:** Background service tests
- **Day 7-10:** Calendar & location tests

#### **Weeks 7-8: Phase 4-5 (Validation & Exceptions)**
- **Week 7:** Entity validation tests
- **Week 8:** Exception handling tests

#### **Weeks 9-10: Phase 6 (Performance & Integration)**
- **Week 9:** Performance test suite
- **Week 10:** End-to-end integration tests

---

## Tools & Configuration

### 1. Add JaCoCo to pom.xml
Already configured with:
- Line coverage reporting
- Exclusions for DTOs and configs
- Report generation on test execution

### 2. CI/CD Integration
**Recommended:**
```yaml
# Add to GitHub Actions/CI pipeline
- name: Run Tests with Coverage
  run: ./mvnw clean test
  
- name: Generate Coverage Report
  run: ./mvnw jacoco:report
  
- name: Check Coverage Thresholds
  run: ./mvnw jacoco:check
  
- name: Upload Coverage to Codecov
  uses: codecov/codecov-action@v3
```

### 3. Coverage Quality Gates
**Recommended Thresholds:**
```xml
<!-- Add to pom.xml jacoco configuration -->
<limit>
    <counter>LINE</counter>
    <value>COVEREDRATIO</value>
    <minimum>0.80</minimum>
</limit>
<limit>
    <counter>BRANCH</counter>
    <value>COVEREDRATIO</value>
    <minimum>0.75</minimum>
</limit>
```

### 4. Test Categorization
Use JUnit 5 tags for test organization:
```java
@Tag("unit")
@Tag("integration")
@Tag("performance")
@Tag("security")
```

---

## Success Metrics

### Quantitative Metrics
- **Line Coverage:** 95%+ target
- **Branch Coverage:** 85%+ target
- **Mutation Test Score:** 80%+ (future enhancement)
- **Test Execution Time:** < 5 minutes for full suite

### Qualitative Metrics
- **Code Confidence:** Team feels safe making changes
- **Bug Detection:** Tests catch regressions before production
- **Documentation:** Tests serve as usage examples
- **Maintainability:** Tests are easy to update

---

## Next Steps

1. **Review & Approve Plan** - Stakeholder sign-off
2. **Set Up Infrastructure** - JaCoCo, CI/CD integration
3. **Create Test Templates** - Standardized test structure
4. **Start Phase 1** - Critical security tests
5. **Weekly Progress Reviews** - Track coverage improvements

---

## Conclusion

The current test coverage of ~30% leaves significant gaps, particularly in:
- **Security-critical components** (JWT, validation)
- **Repository layer** (only 15% covered)
- **Utility classes** (0% coverage)

Following this 10-week plan will increase coverage from **30% to 95%+**, significantly improving code quality, reducing bugs, and increasing development velocity through better regression detection.

**Recommended Action:** Start with Phase 1 immediately to address critical security gaps, then proceed systematically through subsequent phases.

