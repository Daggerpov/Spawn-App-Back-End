package com.danielagapov.spawn.Services.FeedbackSubmission;

import com.danielagapov.spawn.DTOs.CreateFeedbackSubmissionDTO;
import com.danielagapov.spawn.DTOs.FetchFeedbackSubmissionDTO;
import com.danielagapov.spawn.Enums.FeedbackStatus;
import com.danielagapov.spawn.Models.FeedbackSubmission;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

public interface IFeedbackSubmissionService {
    /**
     * Submits user feedback with or without an image attachment
     * 
     * @param dto The feedback data transfer object
     * @return The created feedback submission entity
     * @throws IOException If there is an error processing the image
     */
    FetchFeedbackSubmissionDTO submitFeedback(CreateFeedbackSubmissionDTO dto) throws IOException;
    
    /**
     * Marks a feedback submission as resolved with an optional resolution comment
     * 
     * @param id The ID of the feedback submission to resolve
     * @param resolutionComment An optional comment to add when resolving the feedback
     * @return The updated feedback submission DTO
     */
    FetchFeedbackSubmissionDTO resolveFeedback(UUID id, String resolutionComment);
    
    /**
     * Marks a feedback submission as in progress with an optional comment
     * 
     * @param id The ID of the feedback submission to mark as in progress
     * @param comment An optional comment to add
     * @return The updated feedback submission DTO
     */
    FetchFeedbackSubmissionDTO markFeedbackInProgress(UUID id, String comment);
    
    /**
     * Updates a feedback submission status with an optional comment
     * 
     * @param id The ID of the feedback submission to update
     * @param status The new status for the feedback
     * @param comment An optional comment to add
     * @return The updated feedback submission DTO
     */
    FetchFeedbackSubmissionDTO updateFeedbackStatus(UUID id, FeedbackStatus status, String comment);
    
    /**
     * Retrieves all feedback submissions
     * 
     * @return A list of all feedback submission DTOs
     */
    List<FetchFeedbackSubmissionDTO> getAllFeedbacks();
    
    /**
     * Deletes a feedback submission
     * 
     * @param id The ID of the feedback submission to delete
     */
    void deleteFeedback(UUID id);
}
