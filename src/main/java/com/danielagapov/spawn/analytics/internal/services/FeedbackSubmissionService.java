package com.danielagapov.spawn.analytics.internal.services;

import com.danielagapov.spawn.analytics.api.dto.CreateFeedbackSubmissionDTO;
import com.danielagapov.spawn.analytics.api.dto.FetchFeedbackSubmissionDTO;
import com.danielagapov.spawn.shared.util.EntityType;
import com.danielagapov.spawn.shared.util.FeedbackStatus;
import com.danielagapov.spawn.shared.exceptions.BaseNotFoundException;
import com.danielagapov.spawn.shared.exceptions.BaseSaveException;
import com.danielagapov.spawn.shared.exceptions.ILogger;
import com.danielagapov.spawn.shared.util.FeedbackSubmissionMapper;
import com.danielagapov.spawn.analytics.internal.domain.FeedbackSubmission;
import com.danielagapov.spawn.user.internal.domain.User;
import com.danielagapov.spawn.analytics.internal.repositories.IFeedbackSubmissionRepository;
import com.danielagapov.spawn.user.internal.repositories.IUserRepository;
import com.danielagapov.spawn.media.internal.services.IS3Service;
import com.danielagapov.spawn.shared.util.LoggingUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class FeedbackSubmissionService implements IFeedbackSubmissionService {

    private final IFeedbackSubmissionRepository repository;
    private final ILogger logger;
    private final IUserRepository userRepository;
    private final IS3Service s3Service;

    @Autowired
    public FeedbackSubmissionService(IFeedbackSubmissionRepository repository,
                                     IUserRepository userRepository,
                                     ILogger logger,
                                     IS3Service s3Service) {
        this.repository = repository;
        this.logger = logger;
        this.userRepository = userRepository;
        this.s3Service = s3Service;
    }

    @Override
    @Transactional
    public FetchFeedbackSubmissionDTO submitFeedback(CreateFeedbackSubmissionDTO dto) {
        try {
            // Find user
            UUID userId = dto.getFromUserId();
            User user = Optional.ofNullable(userId)
                    .flatMap(userRepository::findById)
                    .orElseThrow(() -> new BaseNotFoundException(EntityType.User, userId));

            logger.info("Submitting feedback from user: " + LoggingUtils.formatUserInfo(user));

            // Upload image to S3 if present
            String imageUrl = null;
            byte[] imageData = dto.getImageData();
            if (imageData != null && imageData.length > 0) {
                logger.info("Uploading feedback image for user: " + LoggingUtils.formatUserInfo(user));
                imageUrl = s3Service.putObjectWithKey(imageData, "feedback/" + UUID.randomUUID());
            }

            // Create entity from DTO
            FeedbackSubmission feedbackSubmission = FeedbackSubmissionMapper.toEntity(dto, user);

            // Set image URL if we uploaded an image
            if (imageUrl != null) {
                feedbackSubmission.setImageUrl(imageUrl);
            }

            // Save and return the entity
            FeedbackSubmission saved = repository.save(feedbackSubmission);
            logger.info("Feedback saved successfully from user: " + LoggingUtils.formatUserInfo(user) +
                    " with ID: " + saved.getId());
            return FeedbackSubmissionMapper.toDTO(saved);
        } catch (DataAccessException e) {
            logger.error("Failed to save feedback submission from user " +
                    LoggingUtils.formatUserIdInfo(dto.getFromUserId()) + ": " + e.getMessage());
            throw new BaseSaveException("Failed to save feedback submission: " + e.getMessage());
        } catch (Exception e) {
            logger.error("Error submitting feedback from user " +
                    LoggingUtils.formatUserIdInfo(dto.getFromUserId()) + ": " + e.getMessage());
            throw e;
        }
    }

    @Override
    @Transactional
    public FetchFeedbackSubmissionDTO resolveFeedback(UUID id, String resolutionComment) {
        try {
            FeedbackSubmission feedback = repository.findById(id)
                    .orElseThrow(() -> new BaseNotFoundException(EntityType.FeedbackSubmission, id));

            User submitter = feedback.getFromUser();
            logger.info("Resolving feedback with ID: " + id + " from user: " +
                    LoggingUtils.formatUserInfo(submitter));

            feedback.setStatus(FeedbackStatus.RESOLVED);

            // Always set the resolution comment, even if it's empty
            // This ensures frontend knows a comment was provided but might be empty
            feedback.setResolutionComment(resolutionComment != null ? resolutionComment.trim() : "");

            FeedbackSubmission updated = repository.save(feedback);
            logger.info("Feedback with ID: " + id + " marked as resolved successfully");
            return FeedbackSubmissionMapper.toDTO(updated);
        } catch (Exception e) {
            logger.error("Error resolving feedback with ID: " + id + ": " + e.getMessage());
            throw e;
        }
    }

    @Override
    @Transactional
    public FetchFeedbackSubmissionDTO markFeedbackInProgress(UUID id, String comment) {
        try {
            FeedbackSubmission feedback = repository.findById(id)
                    .orElseThrow(() -> new BaseNotFoundException(EntityType.FeedbackSubmission, id));

            User submitter = feedback.getFromUser();
            logger.info("Marking feedback with ID: " + id + " from user: " +
                    LoggingUtils.formatUserInfo(submitter) + " as in progress");

            feedback.setStatus(FeedbackStatus.IN_PROGRESS);

            // Set comment if provided
            if (comment != null && !comment.trim().isEmpty()) {
                feedback.setResolutionComment(comment.trim());
            }

            FeedbackSubmission updated = repository.save(feedback);
            logger.info("Feedback with ID: " + id + " marked as in progress successfully");
            return FeedbackSubmissionMapper.toDTO(updated);
        } catch (Exception e) {
            logger.error("Error marking feedback with ID: " + id + " as in progress: " + e.getMessage());
            throw e;
        }
    }

    @Override
    @Transactional
    public FetchFeedbackSubmissionDTO updateFeedbackStatus(UUID id, FeedbackStatus status, String comment) {
        try {
            FeedbackSubmission feedback = repository.findById(id)
                    .orElseThrow(() -> new BaseNotFoundException(EntityType.FeedbackSubmission, id));

            User submitter = feedback.getFromUser();
            logger.info("Updating feedback with ID: " + id + " from user: " +
                    LoggingUtils.formatUserInfo(submitter) + " to status: " + status);

            feedback.setStatus(status);

            // Set comment if provided
            if (comment != null && !comment.trim().isEmpty()) {
                feedback.setResolutionComment(comment.trim());
            }

            FeedbackSubmission updated = repository.save(feedback);
            logger.info("Feedback with ID: " + id + " status updated successfully to: " + status);
            return FeedbackSubmissionMapper.toDTO(updated);
        } catch (Exception e) {
            logger.error("Error updating feedback status with ID: " + id + ": " + e.getMessage());
            throw e;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<FetchFeedbackSubmissionDTO> getAllFeedbacks() {
        try {
            logger.info("Retrieving all feedback submissions");
            List<FeedbackSubmission> feedbacks = repository.findAll();
            List<FetchFeedbackSubmissionDTO> feedbackDTOs = feedbacks.stream()
                    .map(FeedbackSubmissionMapper::toDTO)
                    .collect(Collectors.toList());
            logger.info("Retrieved " + feedbackDTOs.size() + " feedback submissions");
            return feedbackDTOs;
        } catch (DataAccessException e) {
            logger.error("Error fetching feedbacks from database: " + e.getMessage());
            throw new RuntimeException("Error fetching feedbacks from database", e);
        }
    }

    @Override
    @Transactional
    public void deleteFeedback(UUID id) {
        try {
            FeedbackSubmission feedback = repository.findById(id).orElse(null);
            if (feedback != null) {
                User submitter = feedback.getFromUser();
                logger.info("Deleting feedback with ID: " + id + " from user: " +
                        LoggingUtils.formatUserInfo(submitter));
            } else {
                logger.info("Deleting feedback with ID: " + id + " (feedback details not available)");
            }

            repository.deleteById(id);
            logger.info("Feedback with ID: " + id + " deleted successfully");
        } catch (Exception e) {
            logger.error("Error deleting feedback with ID: " + id + ": " + e.getMessage());
            throw e;
        }
    }
}
