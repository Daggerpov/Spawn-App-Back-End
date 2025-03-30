package com.danielagapov.spawn.Services.FeedbackSubmission;

import com.danielagapov.spawn.DTOs.CreateFeedbackSubmissionDTO;
import com.danielagapov.spawn.DTOs.FetchFeedbackSubmissionDTO;
import com.danielagapov.spawn.Enums.EntityType;
import com.danielagapov.spawn.Exceptions.Base.BaseNotFoundException;
import com.danielagapov.spawn.Exceptions.Base.BaseSaveException;
import com.danielagapov.spawn.Exceptions.Logger.ILogger;
import com.danielagapov.spawn.Mappers.FeedbackSubmissionMapper;
import com.danielagapov.spawn.Models.FeedbackSubmission;
import com.danielagapov.spawn.Models.User;
import com.danielagapov.spawn.Repositories.IFeedbackSubmissionRepository;
import com.danielagapov.spawn.Repositories.IUserRepository;
import com.danielagapov.spawn.Services.S3.IS3Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

import java.io.IOException;
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
    public FetchFeedbackSubmissionDTO submitFeedback(CreateFeedbackSubmissionDTO dto) {
        try {
            UUID userId = dto.getFromUserId();
            User user = Optional.ofNullable(userId)
                    .flatMap(userRepository::findById)
                    .orElseThrow(() -> new BaseNotFoundException(EntityType.User, userId));

            FeedbackSubmission saved = repository.save(FeedbackSubmissionMapper.toEntity(dto, user));
            return FeedbackSubmissionMapper.toDTO(saved, user);
        } catch (DataAccessException e) {
            logger.error(e.getMessage());
            throw new BaseSaveException("Failed to save feedback submission: " + e.getMessage());
        } catch (BaseNotFoundException e) {
            logger.error(e.getMessage());
            throw e;
        } catch (Exception e) {
            logger.error(e.getMessage());
            throw e;
        }
    }

    @Override
    public FeedbackSubmission submitFeedbackWithImage(CreateFeedbackSubmissionDTO dto) throws IOException {
        try {
            // Upload image to S3 if present
            String imageUrl = null;
            byte[] imageData = dto.getImageData();
            if (imageData != null && imageData.length > 0) {
                imageUrl = s3Service.putObjectWithKey(imageData, "feedback/" + UUID.randomUUID());
            }
            
            // Find user if ID is provided
            UUID fromUserId = dto.getFromUserId();
            User user = Optional.ofNullable(fromUserId)
                    .flatMap(userRepository::findById)
                    .orElseThrow(() -> new BaseNotFoundException(EntityType.User, fromUserId));
            
            // Create entity from DTO
            FeedbackSubmission feedbackSubmission = FeedbackSubmissionMapper.toEntity(dto, user);
            feedbackSubmission.setImageUrl(imageUrl);
            
            // Save and return the entity
            return repository.save(feedbackSubmission);
        } catch (DataAccessException e) {
            logger.error(e.getMessage());
            throw new BaseSaveException("Failed to save feedback submission with image: " + e.getMessage());
        } catch (BaseNotFoundException e) {
            logger.error(e.getMessage());
            throw e;
        } catch (IOException e) {
            logger.error("Failed to process image: " + e.getMessage());
            throw e;
        } catch (Exception e) {
            logger.error(e.getMessage());
            throw e;
        }
    }

    @Override
    public FetchFeedbackSubmissionDTO resolveFeedback(UUID id, String resolutionComment) {
        FeedbackSubmission feedback = repository.findById(id)
                .orElseThrow(() -> new BaseNotFoundException(EntityType.FeedbackSubmission, id));

        feedback.setResolved(true);

        if (Optional.ofNullable(resolutionComment).filter(s -> !s.isBlank()).isPresent()) {
            feedback.setResolutionComment(resolutionComment);
        }

        FeedbackSubmission updated = repository.save(feedback);
        return FeedbackSubmissionMapper.toDTO(updated, updated.getFromUser());
    }


    @Override
    public List<FetchFeedbackSubmissionDTO> getAllFeedbacks() {
        try {
            List<FeedbackSubmission> feedbacks = repository.findAll();
            return feedbacks.stream()
                    .map(feedback -> FeedbackSubmissionMapper.toDTO(feedback, feedback.getFromUser()))
                    .collect(Collectors.toList());
        } catch (DataAccessException e) {
            throw new RuntimeException("Error fetching feedbacks from database", e);
        }
    }

    @Override
    public void deleteFeedback(UUID id) {
        try {
            repository.deleteById(id);
        } catch (Exception e) {
            logger.error(e.getMessage());
            throw e;
        }
    }
}
