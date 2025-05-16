package com.danielagapov.spawn.Controllers;

import com.danielagapov.spawn.DTOs.CreateFeedbackSubmissionDTO;
import com.danielagapov.spawn.DTOs.FetchFeedbackSubmissionDTO;
import com.danielagapov.spawn.Enums.FeedbackStatus;
import com.danielagapov.spawn.Exceptions.Base.BaseNotFoundException;
import com.danielagapov.spawn.Exceptions.Base.BaseSaveException;
import com.danielagapov.spawn.Services.FeedbackSubmission.IFeedbackSubmissionService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("api/v1/feedback")
public class FeedbackSubmissionController {
    private final IFeedbackSubmissionService service;

    public FeedbackSubmissionController(IFeedbackSubmissionService service) {
        this.service = service;
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
            return new ResponseEntity<>(service.submitFeedback(dto), HttpStatus.CREATED);
        } catch (BaseSaveException e) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        } catch (BaseNotFoundException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        } catch (Exception e) {
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
            return new ResponseEntity<>(null, HttpStatus.NOT_FOUND);
        } catch (Exception e) {
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
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        } catch (Exception e) {
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
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        } catch (Exception e) {
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
            return new ResponseEntity<>(service.getAllFeedbacks(), HttpStatus.OK);
        } catch (BaseNotFoundException e) {
            return new ResponseEntity<>(null, HttpStatus.NOT_FOUND);
        } catch (Exception e) {
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
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
