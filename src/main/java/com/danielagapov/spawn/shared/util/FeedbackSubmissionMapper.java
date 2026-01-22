package com.danielagapov.spawn.shared.util;

import com.danielagapov.spawn.analytics.api.dto.CreateFeedbackSubmissionDTO;
import com.danielagapov.spawn.analytics.api.dto.FetchFeedbackSubmissionDTO;
import com.danielagapov.spawn.shared.util.FeedbackStatus;
import com.danielagapov.spawn.analytics.internal.domain.FeedbackSubmission;
import com.danielagapov.spawn.user.internal.domain.User;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public final class FeedbackSubmissionMapper {

    public static FetchFeedbackSubmissionDTO toDTO(FeedbackSubmission entity) {
        User user = entity.getFromUser();
        return new FetchFeedbackSubmissionDTO(
                entity.getId(),
                entity.getType(),
                Optional.ofNullable(user).map(User::getId).orElse(null),
                entity.getFromUserEmail(),
                Optional.ofNullable(user).map(User::getName).orElse(null),
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
        
        // Set user information
        String name = Optional.ofNullable(user).map(User::getName).orElse(null);
        if (name != null) user.setName(name);

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
