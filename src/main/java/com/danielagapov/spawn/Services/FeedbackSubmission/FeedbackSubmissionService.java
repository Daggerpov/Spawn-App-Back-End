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
    public FetchFeedbackSubmissionDTO submitFeedback(CreateFeedbackSubmissionDTO dto) throws IOException {
        try {
            // Find user
            UUID userId = dto.getFromUserId();
            User user = Optional.ofNullable(userId)
                    .flatMap(userRepository::findById)
                    .orElseThrow(() -> new BaseNotFoundException(EntityType.User, userId));
            
            // Update first/last name if provided in the DTO
            boolean userUpdated = false;
            if (dto.getFirstName() != null && !dto.getFirstName().isEmpty() && 
                (user.getFirstName() == null || !user.getFirstName().equals(dto.getFirstName()))) {
                user.setFirstName(dto.getFirstName());
                userUpdated = true;
            }
            
            if (dto.getLastName() != null && !dto.getLastName().isEmpty() && 
                (user.getLastName() == null || !user.getLastName().equals(dto.getLastName()))) {
                user.setLastName(dto.getLastName());
                userUpdated = true;
            }
            
            // Save user if updated
            if (userUpdated) {
                userRepository.save(user);
            }

            // Upload image to S3 if present
            String imageUrl = null;
            byte[] imageData = dto.getImageData();
            if (imageData != null && imageData.length > 0) {
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
            return FeedbackSubmissionMapper.toDTO(saved);
        } catch (DataAccessException e) {
            logger.error(e.getMessage());
            throw new BaseSaveException("Failed to save feedback submission: " + e.getMessage());
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

        // Always set the resolution comment, even if it's empty
        // This ensures frontend knows a comment was provided but might be empty
        feedback.setResolutionComment(resolutionComment != null ? resolutionComment.trim() : "");

        FeedbackSubmission updated = repository.save(feedback);
        return FeedbackSubmissionMapper.toDTO(updated);
    }


    @Override
    public List<FetchFeedbackSubmissionDTO> getAllFeedbacks() {
        try {
            List<FeedbackSubmission> feedbacks = repository.findAll();
            return feedbacks.stream()
                    .map(FeedbackSubmissionMapper::toDTO)
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
