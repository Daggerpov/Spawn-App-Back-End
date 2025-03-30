package com.danielagapov.spawn.Mappers;

import com.danielagapov.spawn.DTOs.CreateFeedbackSubmissionDTO;
import com.danielagapov.spawn.DTOs.FetchFeedbackSubmissionDTO;
import com.danielagapov.spawn.Models.FeedbackSubmission;
import com.danielagapov.spawn.Models.User;

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
                entity.isResolved(),
                entity.getResolutionComment(),
                entity.getImageUrl()
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
        feedbackSubmission.setImageUrl(dto.getImageUrl());
        return feedbackSubmission;
    }
    
    public static FeedbackSubmission toEntity(CreateFeedbackSubmissionDTO dto, User user) {
        FeedbackSubmission feedbackSubmission = new FeedbackSubmission();
        feedbackSubmission.setType(dto.getType());
        feedbackSubmission.setFromUser(user);
        feedbackSubmission.setFromUserEmail(
                Optional.ofNullable(user)
                        .map(User::getEmail)
                        .orElse(null)
        );
        feedbackSubmission.setMessage(dto.getMessage());
        return feedbackSubmission;
    }

    public static List<FetchFeedbackSubmissionDTO> toDTOList(List<FeedbackSubmission> entities) {
        return entities.stream()
                .map(FeedbackSubmissionMapper::toDTO)
                .collect(Collectors.toList());
    }
}
