package com.danielagapov.spawn.Mappers;

import com.danielagapov.spawn.DTOs.CreateFeedbackSubmissionDTO;
import com.danielagapov.spawn.DTOs.FetchFeedbackSubmissionDTO;
import com.danielagapov.spawn.Models.FeedbackSubmission;
import com.danielagapov.spawn.Models.User;

import java.util.List;
import java.util.stream.Collectors;

public class FeedbackSubmissionMapper {

    public static FetchFeedbackSubmissionDTO toDTO(FeedbackSubmission entity) {
        return new FetchFeedbackSubmissionDTO(
                entity.getId(),
                entity.getType(),
                entity.getFromUser() != null ? entity.getFromUser().getId() : null,
                entity.getFromUserEmail(),
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
        feedbackSubmission.setFromUserEmail(user != null ? user.getEmail() : null);
        feedbackSubmission.setMessage(dto.getMessage());
        feedbackSubmission.setImageUrl(dto.getImageUrl());
        return feedbackSubmission;
    }
    
    public static FeedbackSubmission toEntity(CreateFeedbackSubmissionDTO dto, User user) {
        FeedbackSubmission feedbackSubmission = new FeedbackSubmission();
        feedbackSubmission.setType(dto.getType());
        feedbackSubmission.setFromUser(user);
        feedbackSubmission.setFromUserEmail(user != null ? user.getEmail() : null);
        feedbackSubmission.setMessage(dto.getMessage());
        // No imageUrl field in CreateDTO, it will be set separately if needed
        return feedbackSubmission;
    }

    public static List<FetchFeedbackSubmissionDTO> toDTOList(List<FeedbackSubmission> entities) {
        return entities.stream()
                .map(FeedbackSubmissionMapper::toDTO)
                .collect(Collectors.toList());
    }

}
