package com.danielagapov.spawn.Controllers;


import com.danielagapov.spawn.DTOs.FeedbackSubmissionDTO;
import com.danielagapov.spawn.Exceptions.Base.BaseNotFoundException;
import com.danielagapov.spawn.Exceptions.Base.BaseSaveException;
import com.danielagapov.spawn.Models.FeedbackSubmission;
import com.danielagapov.spawn.Services.FeedbackSubmission.IFeedbackSubmissionService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
     * @param dto The feedback details submitted by the user.
     * @return The saved feedback entity if successful, otherwise an error response.
     */
    @PostMapping
    public ResponseEntity<FeedbackSubmission> submitFeedback(@RequestBody FeedbackSubmissionDTO dto) {
        try {
            return new ResponseEntity<>(service.submitFeedback(dto), HttpStatus.CREATED);
        } catch (BaseSaveException e) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
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
    public ResponseEntity<Void> resolveFeedback(
            @PathVariable UUID id,
            @RequestBody(required = false) String resolutionComment
    ) {
        try {
            service.resolveFeedback(id, resolutionComment);
            return ResponseEntity.ok().build();
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
    public ResponseEntity<List<FeedbackSubmissionDTO>> getAllFeedbacks() {
        try {
            return new ResponseEntity<>(service.getAllFeedbacks(), HttpStatus.OK);
        } catch (BaseNotFoundException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
