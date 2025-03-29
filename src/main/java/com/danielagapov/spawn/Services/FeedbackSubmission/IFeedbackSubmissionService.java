package com.danielagapov.spawn.Services.FeedbackSubmission;

import com.danielagapov.spawn.DTOs.FeedbackSubmissionDTO;

import java.util.List;
import java.util.UUID;

public interface IFeedbackSubmissionService {
    /**
     * Submits user feedback
     * 
     * @param dto The feedback data transfer object
     * @return The created feedback submission entity
     */
    FeedbackSubmissionDTO submitFeedback(FeedbackSubmissionDTO dto);
    
    /**
     * Marks a feedback submission as resolved with an optional resolution comment
     * 
     * @param id The ID of the feedback submission to resolve
     * @param resolutionComment An optional comment explaining the resolution
     * @return The updated feedback submission DTO
     */
    FeedbackSubmissionDTO resolveFeedback(UUID id, String resolutionComment);
    
    /**
     * Retrieves all feedback submissions
     * 
     * @return A list of all feedback submission DTOs
     */
    List<FeedbackSubmissionDTO> getAllFeedbacks();
    
    /**
     * Deletes a feedback submission
     * 
     * @param id The ID of the feedback submission to delete
     */
    void deleteFeedback(UUID id);
}
