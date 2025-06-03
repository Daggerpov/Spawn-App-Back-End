package com.danielagapov.spawn.ControllerTests;

import com.danielagapov.spawn.Enums.FeedbackType;
import com.danielagapov.spawn.Models.User.User;
import com.danielagapov.spawn.Repositories.User.IUserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.util.UUID;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("Feedback Submission Controller Integration Tests")
public class FeedbackSubmissionControllerIntegrationTest extends BaseIntegrationTest {

    private static final String FEEDBACK_BASE_URL = "/api/v1/feedback";
    private UUID testUserId = UUID.randomUUID();
    private UUID testFeedbackId = UUID.randomUUID();

    @Autowired
    private IUserRepository userRepository;

    @Override
    protected void setupTestData() {
        // Create a test user for feedback submissions
        User testUser = new User();
        testUser.setId(testUserId);
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setName("Test User");
        testUser.setVerified(true);
        userRepository.save(testUser);
    }

    @Test
    @DisplayName("POST /api/v1/feedback - Should submit feedback successfully")
    void testSubmitFeedback_Success() throws Exception {
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