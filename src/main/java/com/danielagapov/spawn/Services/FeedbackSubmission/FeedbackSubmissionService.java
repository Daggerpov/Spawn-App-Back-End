package com.danielagapov.spawn.Services.FeedbackSubmission;

import com.danielagapov.spawn.DTOs.FeedbackSubmissionDTO;
import com.danielagapov.spawn.Enums.EntityType;
import com.danielagapov.spawn.Enums.FeedbackType;
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
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
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
    public FeedbackSubmissionDTO submitFeedback(FeedbackSubmissionDTO dto) {
        try {
            UUID userId = dto.getFromUserId();
            User user = null;

            // If we have a user ID, try to find the user
            if (userId != null) {
                user = userRepository.findById(userId)
                        .orElse(null);
            }

            FeedbackSubmission saved = repository.save(FeedbackSubmissionMapper.toEntity(dto, user));
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
    public FeedbackSubmission submitFeedbackWithImage(FeedbackType type, UUID fromUserId, String fromUserEmail, 
                                                    String message, MultipartFile image) throws IOException {
        try {
            // Upload image to S3 if present
            String imageUrl = null;
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
            
            // Find user if ID is provided
            User user = null;
            if (fromUserId != null) {
                user = userRepository.findById(fromUserId)
                        .orElse(null);
            }
            
            // Save and return the entity
            return repository.save(FeedbackSubmissionMapper.toEntity(dto, user));
        } catch (DataAccessException e) {
            logger.error(e.getMessage());
            throw new BaseSaveException("Failed to save feedback submission with image: " + e.getMessage());
        } catch (IOException e) {
            logger.error(e.getMessage());
            throw e;
        } catch (Exception e) {
            logger.error(e.getMessage());
            throw e;
        }
    }

    @Override
    public FeedbackSubmissionDTO resolveFeedback(UUID id, String resolutionComment) {
        FeedbackSubmission feedback = repository.findById(id)
                .orElseThrow(() -> new BaseNotFoundException(EntityType.FeedbackSubmission, id));

        feedback.setResolved(true);

        if (resolutionComment != null && !resolutionComment.isBlank()) {
            feedback.setResolutionComment(resolutionComment);
        }

        FeedbackSubmission updated = repository.save(feedback);
        return FeedbackSubmissionMapper.toDTO(updated);
    }


    @Override
    public List<FeedbackSubmissionDTO> getAllFeedbacks() {
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
