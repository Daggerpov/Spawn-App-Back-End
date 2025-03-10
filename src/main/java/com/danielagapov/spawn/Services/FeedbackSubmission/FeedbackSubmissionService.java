package com.danielagapov.spawn.Services.FeedbackSubmission;

import com.danielagapov.spawn.DTOs.FeedbackSubmissionDTO;
import com.danielagapov.spawn.Enums.EntityType;
import com.danielagapov.spawn.Exceptions.Base.BaseNotFoundException;
import com.danielagapov.spawn.Exceptions.Base.BaseSaveException;
import com.danielagapov.spawn.Exceptions.Logger.ILogger;
import com.danielagapov.spawn.Mappers.FeedbackSubmissionMapper;
import com.danielagapov.spawn.Models.FeedbackSubmission;
import com.danielagapov.spawn.Models.User;
import com.danielagapov.spawn.Repositories.IFeedbackSubmissionRepository;
import com.danielagapov.spawn.Repositories.IUserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class FeedbackSubmissionService implements IFeedbackSubmissionService {

    private final IFeedbackSubmissionRepository repository;
    private final ILogger logger;
    private final IUserRepository userRepository;

    @Autowired
    public FeedbackSubmissionService(IFeedbackSubmissionRepository repository,
                                     IUserRepository userRepository,
                                     ILogger logger) {
        this.repository = repository;
        this.logger = logger;
        this.userRepository = userRepository;
    }

    @Override
    public FeedbackSubmission submitFeedback(FeedbackSubmissionDTO dto) {
        try {
            UUID userId = dto.getFromUserId();

            if (userId == null && dto.getFromUserEmail() != null) {
                User user = userRepository.findByEmail(dto.getFromUserEmail());
                if (user == null) {
                    throw new BaseNotFoundException(EntityType.User, dto.getFromUserEmail());
                }
                userId = user.getId();
            }


            if (userId == null) {
                throw new BaseSaveException("Either userId or userEmail must be provided");
            }

            FeedbackSubmission feedback = FeedbackSubmissionMapper.toEntity(dto);
            feedback.setFromUserId(userId);

            if (feedback.getFromUserEmail() == null) {
                feedback.setFromUserEmail(dto.getFromUserEmail());
            }

            return repository.save(feedback);
        } catch (DataAccessException e) {
            logger.log(e.getMessage());
            throw new BaseSaveException("Failed to save feedback submission: " + e.getMessage());
        } catch (Exception e) {
            logger.log(e.getMessage());
            throw e;
        }
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
}
