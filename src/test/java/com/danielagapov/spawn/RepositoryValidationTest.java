package com.danielagapov.spawn;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.jpa.show-sql=true",
    "spring.jpa.properties.hibernate.format_sql=true",
    "spring.profiles.active=test"
})
public class RepositoryValidationTest {
    
    @Test
    public void contextLoads() {
        // This test will fail if there are any JPQL syntax errors
        // Spring will validate all @Query annotations during context initialization
        // Success means all repository queries are syntactically correct
    }
} 