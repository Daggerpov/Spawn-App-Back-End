package com.danielagapov.spawn.Controllers;


import com.danielagapov.spawn.DTOs.FeedbackSubmissionDTO;
import com.danielagapov.spawn.Enums.FeedbackType;
import com.danielagapov.spawn.Exceptions.Base.BaseNotFoundException;
import com.danielagapov.spawn.Exceptions.Base.BaseSaveException;
import com.danielagapov.spawn.Models.FeedbackSubmission;
import com.danielagapov.spawn.Services.FeedbackSubmission.IFeedbackSubmissionService;
import com.danielagapov.spawn.Services.S3.IS3Service;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("api/v1/feedback")
public class FeedbackSubmissionController {
    private final IFeedbackSubmissionService service;
    private final IS3Service s3Service;

    public FeedbackSubmissionController(IFeedbackSubmissionService service, IS3Service s3Service) {
        this.service = service;
        this.s3Service = s3Service;
    }

    // Full path: /api/v1/feedback
    /**
     * Endpoint for submitting user feedback (bug reports, feature requests, general feedback).
     * @param dto The feedback details submitted by the user.
     * @return The saved feedback entity if successful, otherwise an error response.
     */
    @PostMapping
    public ResponseEntity<FeedbackSubmissionDTO> submitFeedback(@RequestBody FeedbackSubmissionDTO dto) {
        try {
            return new ResponseEntity<>(service.submitFeedback(dto), HttpStatus.CREATED);
        } catch (BaseSaveException e) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Endpoint for submitting user feedback with an image attachment.
     * @param type The feedback type.
     * @param fromUserId The user ID of the submitter.
     * @param fromUserEmail The email of the submitter.
     * @param message The feedback message.
     * @param image Optional image attachment.
     * @return The saved feedback entity if successful, otherwise an error response.
     */
    @PostMapping(value = "/with-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<FeedbackSubmission> submitFeedbackWithImage(
            @RequestParam("type") FeedbackType type,
            @RequestParam(value = "fromUserId", required = false) UUID fromUserId,
            @RequestParam(value = "fromUserEmail", required = false) String fromUserEmail,
            @RequestParam("message") String message,
            @RequestParam(value = "image", required = false) MultipartFile image
    ) {
        try {
            String imageUrl = null;
            
            // Upload image to S3 if present
            if (image != null && !image.isEmpty()) {
                imageUrl = s3Service.putObjectWithKey(image.getBytes(), "feedback/" + UUID.randomUUID());
            }
            
            // Create DTO with image URL
            FeedbackSubmissionDTO dto = new FeedbackSubmissionDTO();
            dto.setType(type);
            dto.setFromUserId(fromUserId);
            dto.setFromUserEmail(fromUserEmail);
            dto.setMessage(message);
            dto.setImageUrl(imageUrl);
            
            return new ResponseEntity<>(service.submitFeedback(dto), HttpStatus.CREATED);
        } catch (BaseSaveException e) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        } catch (IOException e) {
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
    public ResponseEntity<FeedbackSubmissionDTO> resolveFeedback(
            @PathVariable UUID id,
            @RequestBody(required = false) String resolutionComment
    ) {
        try {
            FeedbackSubmissionDTO resolvedFeedback = service.resolveFeedback(id, resolutionComment);
            return new ResponseEntity<>(resolvedFeedback, HttpStatus.OK);
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

    /**
     * Endpoint for deleting a feedback submission.
     * @param id The unique ID of the feedback submission to delete.
     * @return A no content response (204--standard for deletions)
     * if the feedback was deleted successfully, otherwise an error response.
     */
    @DeleteMapping("/delete/{id}")
    public ResponseEntity<FeedbackSubmissionDTO> deleteFeedback(@PathVariable UUID id) {
        try {
            service.deleteFeedback(id);
            return new ResponseEntity<>(null, HttpStatus.NO_CONTENT);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
