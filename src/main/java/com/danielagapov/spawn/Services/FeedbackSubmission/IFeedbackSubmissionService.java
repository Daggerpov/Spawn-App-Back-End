package com.danielagapov.spawn.Services.FeedbackSubmission;

import com.danielagapov.spawn.DTOs.FeedbackSubmissionDTO;
import com.danielagapov.spawn.Enums.FeedbackType;
import com.danielagapov.spawn.Models.FeedbackSubmission;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
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
     * Submits user feedback with an image attachment
     * 
     * @param type The feedback type
     * @param fromUserId The user ID of the submitter
     * @param fromUserEmail The email of the submitter
     * @param message The feedback message
     * @param image Optional image attachment
     * @return The created feedback submission entity
     * @throws IOException If there is an error processing the image
     */
    FeedbackSubmission submitFeedbackWithImage(FeedbackType type, UUID fromUserId, String fromUserEmail, 
                                             String message, MultipartFile image) throws IOException;
    
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
