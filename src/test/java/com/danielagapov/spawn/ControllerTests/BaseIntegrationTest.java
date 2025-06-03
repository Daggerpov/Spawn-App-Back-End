package com.danielagapov.spawn.ControllerTests;

import com.danielagapov.spawn.Config.TestS3Config;
import com.danielagapov.spawn.SpawnApplication;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * Base class for all controller integration tests.
 * Provides common configuration and utilities.
 */
@SpringBootTest(classes = {SpawnApplication.class, TestS3Config.class}, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@SpringJUnitConfig
@Transactional
public abstract class BaseIntegrationTest {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    protected static final String CONTENT_TYPE_JSON = "application/json";
    protected static final String AUTH_HEADER = "Authorization";
    protected static final String BEARER_PREFIX = "Bearer ";

    @BeforeEach
    void setUp() {
        // Common setup for all tests
        setupTestData();
    }

    protected abstract void setupTestData();

    /**
     * Helper method to create a mock JWT token for authentication
     */
    protected String createMockJwtToken(String username) {
        // In a real implementation, you would generate a proper test JWT
        // For now, return a mock token
        return "mock-jwt-token-for-" + username;
    }

    /**
     * Helper method to convert object to JSON string
     */
    protected String asJsonString(Object obj) throws Exception {
        return objectMapper.writeValueAsString(obj);
    }
} 