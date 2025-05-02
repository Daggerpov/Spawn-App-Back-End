package com.danielagapov.spawn.Mappers;

import com.danielagapov.spawn.DTOs.CreateFeedbackSubmissionDTO;
import com.danielagapov.spawn.DTOs.FetchFeedbackSubmissionDTO;
import com.danielagapov.spawn.Enums.FeedbackStatus;
import com.danielagapov.spawn.Models.FeedbackSubmission;
import com.danielagapov.spawn.Models.User.User;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class FeedbackSubmissionMapper {

    public static FetchFeedbackSubmissionDTO toDTO(FeedbackSubmission entity) {
        User user = entity.getFromUser();
        return new FetchFeedbackSubmissionDTO(
                entity.getId(),
                entity.getType(),
                Optional.ofNullable(user).map(User::getId).orElse(null),
                entity.getFromUserEmail(),
                Optional.ofNullable(user).map(User::getFirstName).orElse(null),
                Optional.ofNullable(user).map(User::getLastName).orElse(null),
                entity.getMessage(),
                entity.getStatus(),
                entity.getResolutionComment(),
                entity.getImageUrl(),
                entity.getSubmittedAt()
        );
    }

    public static FeedbackSubmission toEntity(FetchFeedbackSubmissionDTO dto, User user) {
        FeedbackSubmission feedbackSubmission = new FeedbackSubmission();
        feedbackSubmission.setType(dto.getType());
        feedbackSubmission.setFromUser(user);
        feedbackSubmission.setFromUserEmail(
                Optional.ofNullable(user)
                        .map(User::getEmail)
                        .orElse(null)
        );
        feedbackSubmission.setMessage(dto.getMessage());
        feedbackSubmission.setStatus(dto.getStatus());
        feedbackSubmission.setImageUrl(dto.getImageUrl());
        feedbackSubmission.setSubmittedAt(dto.getSubmittedAt());
        return feedbackSubmission;
    }
    
    public static FeedbackSubmission toEntity(CreateFeedbackSubmissionDTO dto, User user) {
        FeedbackSubmission feedbackSubmission = new FeedbackSubmission();
        feedbackSubmission.setType(dto.getType());
        feedbackSubmission.setFromUser(user);
        
        // Set user information - first try from user object, then from DTO if available
        String firstName = Optional.ofNullable(user).map(User::getFirstName).orElse(null);
        String lastName = Optional.ofNullable(user).map(User::getLastName).orElse(null);

        if (firstName != null) user.setFirstName(firstName);
        if (lastName != null) user.setLastName(lastName);

        feedbackSubmission.setFromUserEmail(
                Optional.ofNullable(user)
                        .map(User::getEmail)
                        .orElse(null)
        );
        feedbackSubmission.setMessage(dto.getMessage());
        feedbackSubmission.setStatus(FeedbackStatus.PENDING);
        return feedbackSubmission;
    }

    public static List<FetchFeedbackSubmissionDTO> toDTOList(List<FeedbackSubmission> entities) {
        return entities.stream()
                .map(FeedbackSubmissionMapper::toDTO)
                .collect(Collectors.toList());
    }
}
