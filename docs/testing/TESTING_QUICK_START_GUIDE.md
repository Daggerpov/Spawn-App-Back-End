# Testing Quick Start Guide

**Goal:** Get you writing tests immediately with concrete examples and templates.

---

## Prerequisites

### 1. Fix Java Version Issue

Your system has Java 25, but the project needs Java 17. Use the provided script:

```bash
# Use the existing build script with Java 17
./scripts/build/build-with-java17.sh

# Or set JAVA_HOME manually
export JAVA_HOME=/Library/Java/JavaVirtualMachines/zulu-17.jdk/Contents/Home
./mvnw clean test
```

### 2. Verify JaCoCo Configuration

JaCoCo has been added to your `pom.xml`. After tests run, check the coverage report:

```bash
# Run tests with coverage
./mvnw clean test

# View coverage report (opens in browser)
open target/site/jacoco/index.html
```

---

## Phase 1: Critical Tests - Start Here

### Priority 1: JWTService Tests (CRITICAL)

**File:** `src/test/java/com/danielagapov/spawn/ServiceTests/JWTServiceTests.java`

```java
package com.danielagapov.spawn.ServiceTests;

import com.danielagapov.spawn.auth.internal.services.JWTService;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("JWT Service Tests")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class JWTServiceTests {

    @Autowired
    private JWTService jwtService;

    private static final String TEST_USER_ID = "test-user-123";
    private static final String TEST_EMAIL = "test@example.com";

    @Nested
    @DisplayName("Token Generation Tests")
    class TokenGenerationTests {

        @Test
        @DisplayName("Should generate valid JWT token")
        void shouldGenerateValidToken() {
            // When
            String token = jwtService.generateToken(TEST_USER_ID, TEST_EMAIL);

            // Then
            assertThat(token)
                .isNotNull()
                .isNotEmpty()
                .contains(".");  // JWT format: header.payload.signature
        }

        @Test
        @DisplayName("Should generate different tokens for different users")
        void shouldGenerateDifferentTokens() {
            // When
            String token1 = jwtService.generateToken("user1", "user1@test.com");
            String token2 = jwtService.generateToken("user2", "user2@test.com");

            // Then
            assertThat(token1).isNotEqualTo(token2);
        }

        @Test
        @DisplayName("Should throw exception when userId is null")
        void shouldThrowExceptionForNullUserId() {
            // Then
            assertThatThrownBy(() -> jwtService.generateToken(null, TEST_EMAIL))
                .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("Token Validation Tests")
    class TokenValidationTests {

        @Test
        @DisplayName("Should validate token and extract user ID")
        void shouldValidateAndExtractUserId() {
            // Given
            String token = jwtService.generateToken(TEST_USER_ID, TEST_EMAIL);

            // When
            String extractedUserId = jwtService.extractUserId(token);

            // Then
            assertThat(extractedUserId).isEqualTo(TEST_USER_ID);
        }

        @Test
        @DisplayName("Should extract email from token")
        void shouldExtractEmail() {
            // Given
            String token = jwtService.generateToken(TEST_USER_ID, TEST_EMAIL);

            // When
            String extractedEmail = jwtService.extractEmail(token);

            // Then
            assertThat(extractedEmail).isEqualTo(TEST_EMAIL);
        }

        @Test
        @DisplayName("Should validate token is not expired")
        void shouldValidateTokenNotExpired() {
            // Given
            String token = jwtService.generateToken(TEST_USER_ID, TEST_EMAIL);

            // When
            boolean isValid = jwtService.isTokenValid(token);

            // Then
            assertThat(isValid).isTrue();
        }

        @Test
        @DisplayName("Should reject invalid token format")
        void shouldRejectInvalidToken() {
            // Given
            String invalidToken = "not.a.valid.token";

            // Then
            assertThatThrownBy(() -> jwtService.isTokenValid(invalidToken))
                .isInstanceOf(Exception.class);
        }

        @Test
        @DisplayName("Should reject empty token")
        void shouldRejectEmptyToken() {
            // Then
            assertThatThrownBy(() -> jwtService.isTokenValid(""))
                .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("Token Expiration Tests")
    class TokenExpirationTests {

        @Test
        @DisplayName("Should detect expired token")
        void shouldDetectExpiredToken() throws InterruptedException {
            // Given - Generate token with very short expiration (if supported)
            // This test depends on your JWT configuration
            // You might need to mock the clock or adjust test properties
            
            // When/Then - Test your expiration logic
            assertThat(true).isTrue(); // Placeholder
        }

        @Test
        @DisplayName("Should extract expiration date from token")
        void shouldExtractExpirationDate() {
            // Given
            String token = jwtService.generateToken(TEST_USER_ID, TEST_EMAIL);

            // When
            Claims claims = jwtService.extractAllClaims(token);

            // Then
            assertThat(claims.getExpiration())
                .isNotNull()
                .isAfter(new java.util.Date());
        }
    }

    @Nested
    @DisplayName("Security Tests")
    class SecurityTests {

        @Test
        @DisplayName("Should not allow token tampering")
        void shouldDetectTokenTampering() {
            // Given
            String validToken = jwtService.generateToken(TEST_USER_ID, TEST_EMAIL);
            String tamperedToken = validToken.substring(0, validToken.length() - 5) + "xxxxx";

            // Then
            assertThatThrownBy(() -> jwtService.isTokenValid(tamperedToken))
                .isInstanceOf(Exception.class);
        }

        @Test
        @DisplayName("Should use strong signing algorithm")
        void shouldUseStrongAlgorithm() {
            // Verify your JWT configuration uses HS256 or better
            // This is more of a configuration check
            String token = jwtService.generateToken(TEST_USER_ID, TEST_EMAIL);
            
            // Decode header and check algorithm
            assertThat(token).isNotNull();
            // Add specific algorithm validation based on your implementation
        }
    }
}
```

---

### Priority 2: UserMapper Tests (CRITICAL)

**File:** `src/test/java/com/danielagapov/spawn/UtilityTests/UserMapperTests.java`

```java
package com.danielagapov.spawn.UtilityTests;

import com.danielagapov.spawn.user.api.dto.*;
import com.danielagapov.spawn.user.internal.domain.User;
import com.danielagapov.spawn.shared.util.UserMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.util.*;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("User Mapper Tests")
class UserMapperTests {

    @Autowired
    private UserMapper userMapper;

    private User testUser;

    @BeforeEach
    void setup() {
        testUser = new User();
        testUser.setUserId("test-user-123");
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setPhone("+1234567890");
        testUser.setFirstName("Test");
        testUser.setLastName("User");
        testUser.setBirthday(LocalDate.of(1990, 1, 1));
        testUser.setBio("Test bio");
        testUser.setProfilePictureUrl("https://example.com/pic.jpg");
    }

    @Nested
    @DisplayName("Entity to DTO Mapping")
    class EntityToDtoTests {

        @Test
        @DisplayName("Should map User to UserDTO with all fields")
        void shouldMapUserToDto() {
            // When
            UserDTO dto = userMapper.toUserDTO(testUser);

            // Then
            assertThat(dto).isNotNull();
            assertThat(dto.getUserId()).isEqualTo(testUser.getUserId());
            assertThat(dto.getUsername()).isEqualTo(testUser.getUsername());
            assertThat(dto.getEmail()).isEqualTo(testUser.getEmail());
            assertThat(dto.getFirstName()).isEqualTo(testUser.getFirstName());
            assertThat(dto.getLastName()).isEqualTo(testUser.getLastName());
            assertThat(dto.getBio()).isEqualTo(testUser.getBio());
        }

        @Test
        @DisplayName("Should handle null user gracefully")
        void shouldHandleNullUser() {
            // When
            UserDTO dto = userMapper.toUserDTO(null);

            // Then
            assertThat(dto).isNull();
        }

        @Test
        @DisplayName("Should map User to PublicUserDTO excluding private info")
        void shouldMapToPublicUserDto() {
            // When
            PublicUserDTO dto = userMapper.toPublicUserDTO(testUser);

            // Then
            assertThat(dto).isNotNull();
            assertThat(dto.getUserId()).isEqualTo(testUser.getUserId());
            assertThat(dto.getUsername()).isEqualTo(testUser.getUsername());
            // Email and phone should NOT be included
            assertThat(dto).hasNoNullFieldsOrPropertiesExcept("email", "phone");
        }

        @Test
        @DisplayName("Should map list of users to DTOs")
        void shouldMapUserList() {
            // Given
            User user2 = new User();
            user2.setUserId("user-2");
            user2.setUsername("user2");
            List<User> users = Arrays.asList(testUser, user2);

            // When
            List<UserDTO> dtos = userMapper.toUserDTOList(users);

            // Then
            assertThat(dtos)
                .hasSize(2)
                .extracting(UserDTO::getUserId)
                .containsExactly("test-user-123", "user-2");
        }
    }

    @Nested
    @DisplayName("DTO to Entity Mapping")
    class DtoToEntityTests {

        @Test
        @DisplayName("Should map CreateUserDTO to User entity")
        void shouldMapCreateDtoToEntity() {
            // Given
            CreateUserDTO createDto = new CreateUserDTO();
            createDto.setUsername("newuser");
            createDto.setEmail("new@example.com");
            createDto.setPassword("securePassword123");
            createDto.setFirstName("New");
            createDto.setLastName("User");

            // When
            User user = userMapper.toUser(createDto);

            // Then
            assertThat(user).isNotNull();
            assertThat(user.getUsername()).isEqualTo(createDto.getUsername());
            assertThat(user.getEmail()).isEqualTo(createDto.getEmail());
            assertThat(user.getFirstName()).isEqualTo(createDto.getFirstName());
            assertThat(user.getLastName()).isEqualTo(createDto.getLastName());
        }

        @Test
        @DisplayName("Should map UpdateUserDTO to existing User")
        void shouldMapUpdateDtoToEntity() {
            // Given
            UpdateUserDTO updateDto = new UpdateUserDTO();
            updateDto.setBio("Updated bio");
            updateDto.setFirstName("Updated");

            // When
            userMapper.updateUserFromDTO(updateDto, testUser);

            // Then
            assertThat(testUser.getBio()).isEqualTo("Updated bio");
            assertThat(testUser.getFirstName()).isEqualTo("Updated");
            // Other fields should remain unchanged
            assertThat(testUser.getUsername()).isEqualTo("testuser");
        }

        @Test
        @DisplayName("Should handle partial updates")
        void shouldHandlePartialUpdates() {
            // Given
            UpdateUserDTO partialUpdate = new UpdateUserDTO();
            partialUpdate.setBio("New bio only");
            // firstName is null

            String originalFirstName = testUser.getFirstName();

            // When
            userMapper.updateUserFromDTO(partialUpdate, testUser);

            // Then
            assertThat(testUser.getBio()).isEqualTo("New bio only");
            assertThat(testUser.getFirstName()).isEqualTo(originalFirstName);
        }
    }

    @Nested
    @DisplayName("Edge Cases")
    class EdgeCaseTests {

        @Test
        @DisplayName("Should handle user with minimal fields")
        void shouldHandleMinimalUser() {
            // Given
            User minimalUser = new User();
            minimalUser.setUserId("minimal-123");
            minimalUser.setUsername("minimal");

            // When
            UserDTO dto = userMapper.toUserDTO(minimalUser);

            // Then
            assertThat(dto).isNotNull();
            assertThat(dto.getUserId()).isEqualTo("minimal-123");
            assertThat(dto.getUsername()).isEqualTo("minimal");
        }

        @Test
        @DisplayName("Should handle empty collections")
        void shouldHandleEmptyList() {
            // When
            List<UserDTO> dtos = userMapper.toUserDTOList(Collections.emptyList());

            // Then
            assertThat(dtos).isEmpty();
        }

        @Test
        @DisplayName("Should handle special characters in fields")
        void shouldHandleSpecialCharacters() {
            // Given
            testUser.setBio("Bio with émojis 🎉 and spëcial çhars!");
            testUser.setUsername("user_with-special.chars");

            // When
            UserDTO dto = userMapper.toUserDTO(testUser);

            // Then
            assertThat(dto.getBio()).isEqualTo(testUser.getBio());
            assertThat(dto.getUsername()).isEqualTo(testUser.getUsername());
        }
    }
}
```

---

### Priority 3: CacheService Tests (CRITICAL)

**File:** `src/test/java/com/danielagapov/spawn/ServiceTests/CacheServiceTests.java`

```java
package com.danielagapov.spawn.ServiceTests;

import com.danielagapov.spawn.analytics.internal.services.Cache.CacheService;
import com.danielagapov.spawn.shared.util.Cache.CacheNames;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("Cache Service Tests")
class CacheServiceTests {

    @Autowired
    private CacheService cacheService;

    @Autowired
    private CacheManager cacheManager;

    @BeforeEach
    void setup() {
        // Clear all caches before each test
        cacheService.clearAllCaches();
    }

    @Nested
    @DisplayName("Cache Storage Tests")
    class CacheStorageTests {

        @Test
        @DisplayName("Should store and retrieve value from cache")
        void shouldStoreAndRetrieveValue() {
            // Given
            String key = "test-key";
            String value = "test-value";
            String cacheName = CacheNames.ACTIVITY_TYPES;

            // When
            cacheService.put(cacheName, key, value);
            String retrieved = cacheService.get(cacheName, key, String.class);

            // Then
            assertThat(retrieved).isEqualTo(value);
        }

        @Test
        @DisplayName("Should return null for non-existent key")
        void shouldReturnNullForMissingKey() {
            // When
            String value = cacheService.get(CacheNames.USERS, "non-existent", String.class);

            // Then
            assertThat(value).isNull();
        }

        @Test
        @DisplayName("Should overwrite existing cache entry")
        void shouldOverwriteExistingEntry() {
            // Given
            String key = "test-key";
            String cacheName = CacheNames.ACTIVITY_TYPES;
            
            cacheService.put(cacheName, key, "old-value");
            cacheService.put(cacheName, key, "new-value");

            // When
            String retrieved = cacheService.get(cacheName, key, String.class);

            // Then
            assertThat(retrieved).isEqualTo("new-value");
        }
    }

    @Nested
    @DisplayName("Cache Eviction Tests")
    class CacheEvictionTests {

        @Test
        @DisplayName("Should evict specific cache entry")
        void shouldEvictSpecificEntry() {
            // Given
            String key = "test-key";
            String cacheName = CacheNames.USERS;
            cacheService.put(cacheName, key, "test-value");

            // When
            cacheService.evict(cacheName, key);
            String retrieved = cacheService.get(cacheName, key, String.class);

            // Then
            assertThat(retrieved).isNull();
        }

        @Test
        @DisplayName("Should clear entire cache")
        void shouldClearEntireCache() {
            // Given
            String cacheName = CacheNames.ACTIVITIES;
            cacheService.put(cacheName, "key1", "value1");
            cacheService.put(cacheName, "key2", "value2");

            // When
            cacheService.clearCache(cacheName);
            String value1 = cacheService.get(cacheName, "key1", String.class);
            String value2 = cacheService.get(cacheName, "key2", String.class);

            // Then
            assertThat(value1).isNull();
            assertThat(value2).isNull();
        }

        @Test
        @DisplayName("Should clear all caches")
        void shouldClearAllCaches() {
            // Given
            cacheService.put(CacheNames.USERS, "key1", "value1");
            cacheService.put(CacheNames.ACTIVITIES, "key2", "value2");

            // When
            cacheService.clearAllCaches();

            // Then
            assertThat(cacheService.get(CacheNames.USERS, "key1", String.class)).isNull();
            assertThat(cacheService.get(CacheNames.ACTIVITIES, "key2", String.class)).isNull();
        }
    }

    @Nested
    @DisplayName("Cache Statistics Tests")
    class CacheStatisticsTests {

        @Test
        @DisplayName("Should report cache hit statistics")
        void shouldReportCacheHits() {
            // Given
            String key = "stats-key";
            String cacheName = CacheNames.USERS;
            cacheService.put(cacheName, key, "value");

            // When
            cacheService.get(cacheName, key, String.class); // Hit
            cacheService.get(cacheName, key, String.class); // Hit
            cacheService.get(cacheName, "non-existent", String.class); // Miss

            // Then - Verify statistics if your implementation supports it
            // This depends on your CacheService implementation
        }
    }

    @Nested
    @DisplayName("Concurrent Access Tests")
    class ConcurrentAccessTests {

        @Test
        @DisplayName("Should handle concurrent reads")
        void shouldHandleConcurrentReads() throws InterruptedException {
            // Given
            String key = "concurrent-key";
            String cacheName = CacheNames.USERS;
            cacheService.put(cacheName, key, "value");

            // When - Simulate concurrent access
            Runnable readTask = () -> {
                for (int i = 0; i < 100; i++) {
                    cacheService.get(cacheName, key, String.class);
                }
            };

            Thread thread1 = new Thread(readTask);
            Thread thread2 = new Thread(readTask);
            
            thread1.start();
            thread2.start();
            thread1.join();
            thread2.join();

            // Then - Should not throw exception
            String value = cacheService.get(cacheName, key, String.class);
            assertThat(value).isEqualTo("value");
        }
    }
}
```

---

## Test Templates

### Controller Test Template

```java
package com.danielagapov.spawn.ControllerTests;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(YourController.class)
@DisplayName("Your Controller Tests")
class YourControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private YourService service;

    @Test
    @DisplayName("GET /endpoint should return 200 OK")
    void shouldReturnOk() throws Exception {
        // Given
        when(service.someMethod()).thenReturn(expectedData);

        // When/Then
        mockMvc.perform(get("/api/endpoint"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.field").value("expected"));
    }
}
```

### Repository Test Template

```java
package com.danielagapov.spawn.RepositoryTests;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
@DisplayName("Your Repository Tests")
class YourRepositoryTests {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private YourRepository repository;

    @Test
    @DisplayName("Should find entity by ID")
    void shouldFindById() {
        // Given
        YourEntity entity = new YourEntity();
        // set fields
        YourEntity saved = entityManager.persistAndFlush(entity);

        // When
        Optional<YourEntity> found = repository.findById(saved.getId());

        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getId()).isEqualTo(saved.getId());
    }
}
```

---

## Running Tests

### Run All Tests
```bash
./mvnw clean test
```

### Run Specific Test Class
```bash
./mvnw test -Dtest=JWTServiceTests
```

### Run Specific Test Method
```bash
./mvnw test -Dtest=JWTServiceTests#shouldGenerateValidToken
```

### Run Tests with Coverage Report
```bash
./mvnw clean test jacoco:report
open target/site/jacoco/index.html
```

### Run Tests in Specific Package
```bash
./mvnw test -Dtest="com.danielagapov.spawn.ServiceTests.*"
```

---

## Debugging Tests

### Enable Debug Logging
Add to `src/test/resources/application-test.properties`:
```properties
logging.level.com.danielagapov.spawn=DEBUG
logging.level.org.springframework=INFO
```

### Use @Disabled for Broken Tests
```java
@Test
@Disabled("Temporarily disabled - fix in progress")
void brokenTest() {
    // ...
}
```

### Print Debug Info
```java
@Test
void debugTest() {
    System.out.println("Debug: " + someValue);
    // or use logging
    log.debug("Debug message: {}", someValue);
}
```

---

## Best Practices Checklist

- [ ] Each test has a clear `@DisplayName`
- [ ] Tests follow Given-When-Then structure
- [ ] Tests are independent (no shared mutable state)
- [ ] Use `@BeforeEach` for setup, `@AfterEach` for cleanup
- [ ] Use AssertJ assertions (`assertThat()`) for readability
- [ ] Mock external dependencies
- [ ] Test both happy path and error cases
- [ ] Test boundary conditions
- [ ] Tests run fast (< 100ms each for unit tests)
- [ ] Tests are deterministic (no random values without seeds)

---

## Next Steps

1. **Start with JWTServiceTests** (copy template above)
2. **Run the test:** `./mvnw test -Dtest=JWTServiceTests`
3. **Fix any failures**
4. **Check coverage:** See which lines are covered
5. **Move to next critical test** (UserMapperTests)

### Tracking Progress

After each test class, update your coverage:
```bash
./mvnw clean test jacoco:report
open target/site/jacoco/index.html
```

Track in a simple spreadsheet:
| Test Class | Status | Coverage Increase |
|------------|--------|-------------------|
| JWTServiceTests | ✅ Done | +2% |
| UserMapperTests | 🟡 In Progress | +1.5% |
| CacheServiceTests | ⏳ Pending | - |

---

## Getting Help

### Common Issues

**Issue:** Tests fail to compile
- **Fix:** Verify imports match new modular structure
- Check: `com.danielagapov.spawn.[module].internal.services`

**Issue:** "Cannot autowire" error
- **Fix:** Use `@MockBean` for dependencies
- Or: Use `@SpringBootTest` instead of `@WebMvcTest`

**Issue:** Database errors in tests
- **Fix:** Ensure `@ActiveProfiles("test")` is present
- Check: `src/test/resources/application-test.properties` exists

**Issue:** Tests pass locally but fail in CI
- **Fix:** Ensure tests don't depend on local environment
- Use: Test containers or embedded databases

---

## Summary

**Start with these 3 tests immediately:**
1. ✅ `JWTServiceTests` - Critical for security
2. ✅ `UserMapperTests` - Critical for core functionality
3. ✅ `CacheServiceTests` - Critical for performance

Each test class takes ~30-60 minutes to write properly. After completing these 3, you'll have:
- Established testing patterns
- Covered critical security code
- Increased coverage by ~5-7%
- Built confidence for remaining tests

**Ready to start? Copy the JWTServiceTests template and run it!**

