# Testing Quick Reference Card

**Keep This Handy!** 📌

---

## 🚀 Quick Start (Copy-Paste Ready)

### Run Tests
```bash
# All tests with coverage
./mvnw clean test jacoco:report && open target/site/jacoco/index.html

# Specific test
./mvnw test -Dtest=JWTServiceTests

# Specific method
./mvnw test -Dtest=JWTServiceTests#shouldGenerateValidToken

# Package
./mvnw test -Dtest="com.danielagapov.spawn.ServiceTests.*"
```

### Fix Java Version
```bash
./scripts/build/build-with-java17.sh
# or
export JAVA_HOME=/Library/Java/JavaVirtualMachines/zulu-17.jdk/Contents/Home
```

---

## 📝 Test Template (Copy-Paste)

```java
package com.danielagapov.spawn.ServiceTests;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("Your Service Tests")
class YourServiceTests {

    @Autowired
    private YourService service;

    @MockBean
    private DependencyService dependency;

    @Nested
    @DisplayName("Feature Tests")
    class FeatureTests {

        @Test
        @DisplayName("Should succeed when valid input")
        void shouldSucceed() {
            // Given
            when(dependency.someMethod()).thenReturn("value");
            
            // When
            String result = service.doSomething("input");
            
            // Then
            assertThat(result).isEqualTo("expected");
            verify(dependency).someMethod();
        }

        @Test
        @DisplayName("Should throw exception when invalid input")
        void shouldThrowException() {
            // Then
            assertThatThrownBy(() -> service.doSomething(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cannot be null");
        }
    }
}
```

---

## 🎯 Priority Tests (Do First)

### Week 1
1. **JWTServiceTests** - Security critical
2. **VerificationCodeGeneratorTests** - Security
3. **PhoneNumberValidatorTests** - Validation
4. **UserMapperTests** - Core mapper
5. **ActivityMapperTests** - Core mapper

**Target:** 30% → 38% coverage

### Week 2
6. **ActivityTypeMapperTests** - Core mapper
7. **ChatMessageMapperTests** - Messaging
8. **CacheServiceTests** - Performance
9. **CacheEvictionHelperTests** - Memory
10. **RetryHelperTests** - Resilience

**Target:** 38% → 45% coverage

---

## 📚 Documentation Map

| Need | Open This |
|------|-----------|
| **Overview & Strategy** | `docs/CODE_COVERAGE_ANALYSIS.md` |
| **Write Tests Now** | `docs/TESTING_QUICK_START_GUIDE.md` |
| **Track Progress** | `docs/TEST_COVERAGE_TRACKER.md` |
| **Navigate All** | `./README.md` |
| **Summary** | `docs/TESTING_INITIATIVE_SUMMARY.md` |
| **This Card** | `docs/TESTING_QUICK_REFERENCE.md` |

---

## ✅ Test Checklist (Per Test)

Before committing:
- [ ] Uses `@DisplayName` everywhere
- [ ] Follows Given-When-Then
- [ ] Tests success case
- [ ] Tests failure cases
- [ ] Tests boundaries
- [ ] Uses `@Nested` classes
- [ ] Achieves >= 80% coverage
- [ ] All tests pass locally
- [ ] Updated tracker

---

## 🔧 Common Issues

| Problem | Solution |
|---------|----------|
| **Java version error** | `./scripts/build/build-with-java17.sh` |
| **Cannot autowire** | Add `@MockBean` for dependencies |
| **Database errors** | Add `@ActiveProfiles("test")` |
| **Tests won't compile** | Fix imports: `com.danielagapov.spawn.[module].internal.*` |
| **Coverage not showing** | Check pom.xml has JaCoCo plugin |

---

## 📊 Current Status (Update Weekly)

```
Coverage:    [30]% → Target: 95%
Test Files:  [35] → Target: 100+
Phase:       [Not Started] → Current Phase
This Week:   Test #[__] of 10
```

---

## 🎓 AssertJ Cheat Sheet

```java
// Basic assertions
assertThat(value).isEqualTo(expected);
assertThat(value).isNotNull();
assertThat(value).isInstanceOf(String.class);

// String assertions
assertThat(string).startsWith("prefix");
assertThat(string).contains("text");
assertThat(string).matches("regex");

// Collection assertions
assertThat(list).hasSize(3);
assertThat(list).contains(item);
assertThat(list).containsExactly(item1, item2);
assertThat(list).isEmpty();

// Exception assertions
assertThatThrownBy(() -> service.method())
    .isInstanceOf(Exception.class)
    .hasMessageContaining("error");

// Object assertions
assertThat(obj)
    .hasFieldOrPropertyWithValue("field", value)
    .extracting("field")
    .isEqualTo(value);
```

---

## 🏃 Daily Workflow

### Morning (15 min)
1. Check TEST_COVERAGE_TRACKER.md
2. Identify today's test target
3. Open TESTING_QUICK_START_GUIDE.md

### During Development (1-2 hours)
1. Copy test template
2. Write tests (Given-When-Then)
3. Run: `./mvnw test -Dtest=YourTest`
4. Fix failures
5. Check coverage: `./mvnw jacoco:report`

### End of Day (10 min)
1. Commit: `git commit -m "test: add YourService tests"`
2. Update tracker: Mark test as ✅ Done
3. Note any blockers

### Friday Review (30 min)
1. Run full suite: `./mvnw clean test`
2. Generate report: `open target/site/jacoco/index.html`
3. Update weekly section in tracker
4. Plan next week

---

## 📈 Coverage Targets

| Week | Target | Tests | Focus |
|------|--------|-------|-------|
| 1-2  | 45%    | +10   | Security & Core |
| 3-4  | 70%    | +18   | Controllers & Services |
| 5-6  | 85%    | +15   | External Services |
| 7    | 90%    | +10   | Domain Validation |
| 8    | 93%    | +12   | Exception Handling |
| 9-10 | 95%    | +10   | Performance & E2E |

---

## 🎯 Success Metrics

### Quality Gates
- Line Coverage: >= 95%
- Branch Coverage: >= 85%
- Test Suite Speed: < 5 minutes
- Test Reliability: 100% pass rate

### Per Test File
- Line Coverage: >= 80%
- Branch Coverage: >= 75%
- Test Speed: < 100ms (unit tests)
- Assertions: Meaningful and specific

---

## 🆘 Help Commands

```bash
# Get test help
./mvnw test --help

# Debug test
./mvnw test -Dtest=YourTest -X

# Skip tests
./mvnw install -DskipTests

# Run with profile
./mvnw test -Dspring.profiles.active=test

# Parallel execution
./mvnw test -T 4

# Continuous testing (with entr)
find src/test -name "*.java" | entr ./mvnw test
```

---

## 💡 Pro Tips

1. **Write tests first** - TDD when adding new features
2. **Keep tests fast** - Mock external dependencies
3. **One assertion concept per test** - Easier to debug
4. **Use descriptive names** - Test name = documentation
5. **Test behaviors, not implementation** - More maintainable
6. **DRY in setup, not in tests** - Tests should be explicit
7. **Use @Nested** - Organize related tests
8. **Commit often** - One test class per commit

---

## 🔗 Key Files

### Configuration
- `pom.xml` - JaCoCo configuration
- `src/test/resources/application-test.properties` - Test config

### Reports
- `target/site/jacoco/index.html` - Coverage report
- `target/surefire-reports/` - Test results

### Documentation
- `docs/CODE_COVERAGE_ANALYSIS.md` - Gap analysis
- `docs/TESTING_QUICK_START_GUIDE.md` - Examples
- `docs/TEST_COVERAGE_TRACKER.md` - Progress

---

## 🎉 Celebration Milestones

- [ ] First test written ✨
- [ ] 10 tests completed 🎯
- [ ] 45% coverage reached 🚀
- [ ] 70% coverage reached 💪
- [ ] 85% coverage reached 🔥
- [ ] 95% coverage reached 🏆

---

## 📞 Quick Links

- **JaCoCo Docs:** https://www.jacoco.org/jacoco/trunk/doc/
- **JUnit 5:** https://junit.org/junit5/docs/current/user-guide/
- **AssertJ:** https://assertj.github.io/doc/
- **Spring Testing:** https://spring.io/guides/gs/testing-web/
- **Mockito:** https://javadoc.io/doc/org.mockito/mockito-core/latest/

---

**Print this card or keep it open while writing tests!**

**Last Updated:** December 23, 2025  
**Version:** 1.0

