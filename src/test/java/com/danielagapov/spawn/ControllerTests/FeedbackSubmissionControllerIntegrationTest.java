package com.danielagapov.spawn.ControllerTests;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.util.UUID;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("Feedback Submission Controller Integration Tests")
public class FeedbackSubmissionControllerIntegrationTest extends BaseIntegrationTest {

    private static final String FEEDBACK_BASE_URL = "/api/v1/feedback";
    private UUID testUserId = UUID.randomUUID();
    private UUID testFeedbackId = UUID.randomUUID();

    @Override
    protected void setupTestData() {
        // Setup test feedback submissions and users for testing
    }

    @Test
    @DisplayName("POST /api/v1/feedback - Should submit feedback successfully")
    void testSubmitFeedback_Success() throws Exception {
        String feedbackJson = "{"
                + "\"title\":\"Test Feedback\","
                + "\"description\":\"This is a test feedback submission\","
                + "\"feedbackType\":\"BUG_REPORT\","
                + "\"fromUserId\":\"" + testUserId + "\""
                + "}";

        mockMvc.perform(MockMvcRequestBuilders.post(FEEDBACK_BASE_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(feedbackJson))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("PUT /api/v1/feedback/resolve/{id} - Should resolve feedback")
    void testResolveFeedback() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.put(FEEDBACK_BASE_URL + "/resolve/" + testFeedbackId)
                .param("resolutionComment", "Feedback has been resolved"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("PUT /api/v1/feedback/in-progress/{id} - Should mark feedback as in progress")
    void testMarkFeedbackInProgress() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.put(FEEDBACK_BASE_URL + "/in-progress/" + testFeedbackId))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("PUT /api/v1/feedback/status/{id} - Should update feedback status")
    void testUpdateFeedbackStatus() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.put(FEEDBACK_BASE_URL + "/status/" + testFeedbackId)
                .param("status", "RESOLVED"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /api/v1/feedback - Should get all feedback submissions")
    void testGetAllFeedback() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get(FEEDBACK_BASE_URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @DisplayName("DELETE /api/v1/feedback/delete/{id} - Should delete feedback submission")
    void testDeleteFeedback() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.delete(FEEDBACK_BASE_URL + "/delete/" + testFeedbackId))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("POST /api/v1/feedback - Should return bad request for invalid feedback data")
    void testSubmitFeedback_InvalidData() throws Exception {
        String invalidJson = "{}";

        mockMvc.perform(MockMvcRequestBuilders.post(FEEDBACK_BASE_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("PUT /api/v1/feedback/resolve/{id} - Should return not found for non-existent feedback")
    void testResolveFeedback_NotFound() throws Exception {
        UUID nonExistentId = UUID.randomUUID();
        mockMvc.perform(MockMvcRequestBuilders.put(FEEDBACK_BASE_URL + "/resolve/" + nonExistentId)
                .param("resolutionComment", "Test comment"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("DELETE /api/v1/feedback/delete/{id} - Should return not found for non-existent feedback")
    void testDeleteFeedback_NotFound() throws Exception {
        UUID nonExistentId = UUID.randomUUID();
        mockMvc.perform(MockMvcRequestBuilders.delete(FEEDBACK_BASE_URL + "/delete/" + nonExistentId))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("PUT /api/v1/feedback/status/{id} - Should handle invalid status")
    void testUpdateFeedbackStatus_InvalidStatus() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.put(FEEDBACK_BASE_URL + "/status/" + testFeedbackId)
                .param("status", "INVALID_STATUS"))
                .andExpect(status().isBadRequest());
    }
} 