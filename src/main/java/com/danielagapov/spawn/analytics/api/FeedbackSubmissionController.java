package com.danielagapov.spawn.analytics.api;

import com.danielagapov.spawn.analytics.api.dto.CreateFeedbackSubmissionDTO;
import com.danielagapov.spawn.analytics.api.dto.FetchFeedbackSubmissionDTO;
import com.danielagapov.spawn.shared.util.FeedbackStatus;
import com.danielagapov.spawn.shared.exceptions.BaseNotFoundException;
import com.danielagapov.spawn.shared.exceptions.BaseSaveException;
import com.danielagapov.spawn.shared.exceptions.ILogger;
import com.danielagapov.spawn.analytics.internal.services.IFeedbackSubmissionService;
import com.danielagapov.spawn.shared.util.LoggingUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("api/v1/feedback")
public final class FeedbackSubmissionController {
    private final IFeedbackSubmissionService service;
    private final ILogger logger;

    public FeedbackSubmissionController(IFeedbackSubmissionService service, ILogger logger) {
        this.service = service;
        this.logger = logger;
    }

    // Full path: /api/v1/feedback
    /**
     * Endpoint for submitting user feedback (bug reports, feature requests, general feedback).
     * Image data is optional and can be included in the DTO.
     * @param dto The feedback details submitted by the user.
     * @return The saved feedback entity if successful, otherwise an error response.
     */
    @PostMapping
    public ResponseEntity<FetchFeedbackSubmissionDTO> submitFeedback(@RequestBody CreateFeedbackSubmissionDTO dto) {
        try {
            FetchFeedbackSubmissionDTO submittedFeedback = service.submitFeedback(dto);
            return new ResponseEntity<>(submittedFeedback, HttpStatus.CREATED);
        } catch (BaseSaveException e) {
            logger.error("Bad request for feedback submission: " + e.getMessage());
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        } catch (BaseNotFoundException e) {
            logger.error("User not found for feedback submission: " + LoggingUtils.formatUserIdInfo(dto.getFromUserId()) + ": " + e.getMessage());
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            logger.error("Error submitting feedback: " + e.getMessage());
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // Full path: /api/v1/feedback/resolve/{id}
    /**
     * Endpoint for resolving a feedback submission.
     * @param id The unique ID of the feedback submission to resolve.
     * @param resolutionComment An optional comment to add when resolving the feedback.
     * @return A success response if the feedback was resolved, otherwise an error response.
     */
    @PutMapping("/resolve/{id}")
    public ResponseEntity<FetchFeedbackSubmissionDTO> resolveFeedback(
            @PathVariable UUID id,
            @RequestBody(required = false) String resolutionComment
    ) {
        try {
            FetchFeedbackSubmissionDTO resolvedFeedback = service.resolveFeedback(id, resolutionComment);
            return new ResponseEntity<>(resolvedFeedback, HttpStatus.OK);
        } catch (BaseNotFoundException e) {
            logger.error("Feedback not found for resolution: " + id + ": " + e.getMessage());
            return new ResponseEntity<>(null, HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            logger.error("Error resolving feedback: " + id + ": " + e.getMessage());
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    
    // Full path: /api/v1/feedback/in-progress/{id}
    /**
     * Endpoint for marking a feedback submission as in progress.
     * @param id The unique ID of the feedback submission to mark as in progress.
     * @param comment An optional comment to add.
     * @return A success response if the feedback status was updated, otherwise an error response.
     */
    @PutMapping("/in-progress/{id}")
    public ResponseEntity<FetchFeedbackSubmissionDTO> markFeedbackInProgress(
            @PathVariable UUID id,
            @RequestBody(required = false) String comment
    ) {
        try {
            FetchFeedbackSubmissionDTO inProgressFeedback = service.markFeedbackInProgress(id, comment);
            return new ResponseEntity<>(inProgressFeedback, HttpStatus.OK);
        } catch (BaseNotFoundException e) {
            logger.error("Feedback not found for in-progress update: " + id + ": " + e.getMessage());
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            logger.error("Error marking feedback as in progress: " + id + ": " + e.getMessage());
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    
    // Full path: /api/v1/feedback/status/{id}
    /**
     * Endpoint for updating a feedback submission status.
     * @param id The unique ID of the feedback submission to update.
     * @param status The new status for the feedback.
     * @param comment An optional comment to add.
     * @return A success response if the feedback status was updated, otherwise an error response.
     */
    @PutMapping("/status/{id}")
    public ResponseEntity<FetchFeedbackSubmissionDTO> updateFeedbackStatus(
            @PathVariable UUID id,
            @RequestParam FeedbackStatus status,
            @RequestBody(required = false) String comment
    ) {
        try {
            FetchFeedbackSubmissionDTO updatedFeedback = service.updateFeedbackStatus(id, status, comment);
            return new ResponseEntity<>(updatedFeedback, HttpStatus.OK);
        } catch (BaseNotFoundException e) {
            logger.error("Feedback not found for status update: " + id + ": " + e.getMessage());
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            logger.error("Error updating feedback status for ID: " + id + ": " + e.getMessage());
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Endpoint for retrieving all feedback submissions.
     * This endpoint should be protected somehow since it's not meant to be publicly accessible.
     * @return A list of all feedback submissions if successful, otherwise an error response.
     */
    @GetMapping
    public ResponseEntity<List<FetchFeedbackSubmissionDTO>> getAllFeedbacks() {
        try {
            List<FetchFeedbackSubmissionDTO> feedbacks = service.getAllFeedbacks();
            return new ResponseEntity<>(feedbacks, HttpStatus.OK);
        } catch (BaseNotFoundException e) {
            logger.error("No feedback submissions found: " + e.getMessage());
            return new ResponseEntity<>(null, HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            logger.error("Error getting all feedback submissions: " + e.getMessage());
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Endpoint for deleting a feedback submission.
     * @param id The unique ID of the feedback submission to delete.
     * @return A no content response (204--standard for deletions)
     * if the feedback was deleted successfully, otherwise an error response.
     */
    @DeleteMapping("/delete/{id}")
    public ResponseEntity<FetchFeedbackSubmissionDTO> deleteFeedback(@PathVariable UUID id) {
        try {
            service.deleteFeedback(id);
            return new ResponseEntity<>(null, HttpStatus.NO_CONTENT);
        } catch (Exception e) {
            logger.error("Error deleting feedback: " + id + ": " + e.getMessage());
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
