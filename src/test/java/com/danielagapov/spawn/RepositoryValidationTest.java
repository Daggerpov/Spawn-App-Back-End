package com.danielagapov.spawn;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class RepositoryValidationTest {

    @Test
    void contextLoads() {
        // This test verifies that the Spring application context loads successfully
        // with the test profile configuration
    }
} 