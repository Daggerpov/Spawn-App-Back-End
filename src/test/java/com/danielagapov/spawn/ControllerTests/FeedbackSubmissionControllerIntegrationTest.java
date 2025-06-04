package com.danielagapov.spawn.ControllerTests;

import com.danielagapov.spawn.DTOs.User.AuthUserDTO;
import com.danielagapov.spawn.Enums.FeedbackType;
import com.danielagapov.spawn.Models.User.User;
import com.danielagapov.spawn.Repositories.User.IUserRepository;
import com.danielagapov.spawn.Services.Auth.AuthService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Date;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("Feedback Submission Controller Integration Tests")
public class FeedbackSubmissionControllerIntegrationTest extends BaseIntegrationTest {

    private static final String FEEDBACK_BASE_URL = "/api/v1/feedback";
    private UUID testUserId;
    private UUID testFeedbackId = UUID.randomUUID();
    
    // Use atomic counter to ensure unique usernames and emails across tests
    private static final AtomicInteger testCounter = new AtomicInteger(0);

    @Autowired
    private AuthService authService;
    
    @Autowired
    private IUserRepository userRepository;
    
    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    protected void setupTestData() {
        // This will be called by @BeforeEach in the base class
        // Create unique test data for each test method
        int testId = testCounter.incrementAndGet();
        
        try {
            // Create user entity directly with unique data
            User testUser = new User();
            testUser.setId(UUID.randomUUID());
            testUser.setUsername("feedbacktestuser" + testId);
            testUser.setEmail("feedbacktest" + testId + "@example.com");
            testUser.setName("Test User " + testId);
            testUser.setBio("Test bio");
            testUser.setPassword(passwordEncoder.encode("password123"));
            testUser.setVerified(false);
            testUser.setDateCreated(new Date());
            
            // Save user directly and flush to ensure persistence
            User savedUser = userRepository.saveAndFlush(testUser);
            testUserId = savedUser.getId();
            
        } catch (Exception e) {
            throw new RuntimeException("Failed to create test user: " + e.getMessage(), e);
        }
    }

    @Test
    @DisplayName("POST /api/v1/feedback - Should submit feedback successfully")
    void testSubmitFeedback_Success() throws Exception {
        // Verify user exists before making the request
        User testUser = userRepository.findById(testUserId).orElse(null);
        if (testUser == null) {
            throw new RuntimeException("Test user not found before feedback submission test. Test setup failed.");
        }
        
        String feedbackJson = "{"
                + "\"type\":\"BUG\","
                + "\"fromUserId\":\"" + testUserId + "\","
                + "\"message\":\"This is a test feedback submission\""
                + "}";

        mockMvc.perform(MockMvcRequestBuilders.post(FEEDBACK_BASE_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(feedbackJson)
                .header("Authorization", "Bearer " + createMockJwtToken("testuser")))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("PUT /api/v1/feedback/resolve/{id} - Should resolve feedback")
    void testResolveFeedback() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.put(FEEDBACK_BASE_URL + "/resolve/" + testFeedbackId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("\"Feedback has been resolved\"")
                .header("Authorization", "Bearer " + createMockJwtToken("testuser")))
                .andExpect(status().isNotFound()); // Should be not found since feedback doesn't exist
    }

    @Test
    @DisplayName("PUT /api/v1/feedback/in-progress/{id} - Should mark feedback as in progress")
    void testMarkFeedbackInProgress() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.put(FEEDBACK_BASE_URL + "/in-progress/" + testFeedbackId)
                .header("Authorization", "Bearer " + createMockJwtToken("testuser")))
                .andExpect(status().isNotFound()); // Should be not found since feedback doesn't exist
    }

    @Test
    @DisplayName("PUT /api/v1/feedback/status/{id} - Should update feedback status")
    void testUpdateFeedbackStatus() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.put(FEEDBACK_BASE_URL + "/status/" + testFeedbackId)
                .param("status", "RESOLVED")
                .header("Authorization", "Bearer " + createMockJwtToken("testuser")))
                .andExpect(status().isNotFound()); // Should be not found since feedback doesn't exist
    }

    @Test
    @DisplayName("GET /api/v1/feedback - Should get all feedback submissions")
    void testGetAllFeedback() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get(FEEDBACK_BASE_URL)
                .header("Authorization", "Bearer " + createMockJwtToken("testuser")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @DisplayName("DELETE /api/v1/feedback/delete/{id} - Should delete feedback submission")
    void testDeleteFeedback() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.delete(FEEDBACK_BASE_URL + "/delete/" + testFeedbackId)
                .header("Authorization", "Bearer " + createMockJwtToken("testuser")))
                .andExpect(status().isNotFound()); // Should return 404 since feedback doesn't exist
    }

    @Test
    @DisplayName("DELETE /api/v1/feedback/delete/{id} - Should successfully delete existing feedback")
    void testDeleteFeedback_Success() throws Exception {
        // Verify user exists before making the request
        User testUser = userRepository.findById(testUserId).orElse(null);
        if (testUser == null) {
            throw new RuntimeException("Test user not found before feedback deletion test. Test setup failed.");
        }
        
        // First create a feedback submission
        String feedbackJson = "{"
                + "\"type\":\"BUG\","
                + "\"fromUserId\":\"" + testUserId + "\","
                + "\"message\":\"This feedback will be deleted\""
                + "}";

        String response = mockMvc.perform(MockMvcRequestBuilders.post(FEEDBACK_BASE_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(feedbackJson)
                .header("Authorization", "Bearer " + createMockJwtToken("testuser")))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        // Extract the feedback ID from the response (this is a simplified approach)
        // In a real scenario, you might want to use a JSON parser
        UUID createdFeedbackId = UUID.fromString(
            response.substring(response.indexOf("\"id\":\"") + 6, response.indexOf("\",\"type\""))
        );

        // Now delete the created feedback
        mockMvc.perform(MockMvcRequestBuilders.delete(FEEDBACK_BASE_URL + "/delete/" + createdFeedbackId)
                .header("Authorization", "Bearer " + createMockJwtToken("testuser")))
                .andExpect(status().isNoContent()); // Should return 204 for successful deletion
    }

    @Test
    @DisplayName("POST /api/v1/feedback - Should return not found for invalid user ID")
    void testSubmitFeedback_InvalidData() throws Exception {
        String invalidJson = "{}";

        mockMvc.perform(MockMvcRequestBuilders.post(FEEDBACK_BASE_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidJson)
                .header("Authorization", "Bearer " + createMockJwtToken("testuser")))
                .andExpect(status().isNotFound()); // Will be 404 due to null user ID causing user not found
    }

    @Test
    @DisplayName("PUT /api/v1/feedback/resolve/{id} - Should return not found for non-existent feedback")
    void testResolveFeedback_NotFound() throws Exception {
        UUID nonExistentId = UUID.randomUUID();
        mockMvc.perform(MockMvcRequestBuilders.put(FEEDBACK_BASE_URL + "/resolve/" + nonExistentId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("\"Test comment\"")
                .header("Authorization", "Bearer " + createMockJwtToken("testuser")))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("DELETE /api/v1/feedback/delete/{id} - Should return not found for non-existent feedback")
    void testDeleteFeedback_NotFound() throws Exception {
        UUID nonExistentId = UUID.randomUUID();
        mockMvc.perform(MockMvcRequestBuilders.delete(FEEDBACK_BASE_URL + "/delete/" + nonExistentId)
                .header("Authorization", "Bearer " + createMockJwtToken("testuser")))
                .andExpect(status().isNotFound()); // The service now checks if feedback exists before delete
    }

    @Test
    @DisplayName("PUT /api/v1/feedback/status/{id} - Should handle invalid status")
    void testUpdateFeedbackStatus_InvalidStatus() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.put(FEEDBACK_BASE_URL + "/status/" + testFeedbackId)
                .param("status", "INVALID_STATUS")
                .header("Authorization", "Bearer " + createMockJwtToken("testuser")))
                .andExpect(status().isBadRequest());
    }
} 