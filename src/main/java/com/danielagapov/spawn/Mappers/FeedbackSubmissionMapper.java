package com.danielagapov.spawn.Mappers;

import com.danielagapov.spawn.DTOs.FeedbackSubmissionDTO;
import com.danielagapov.spawn.Models.FeedbackSubmission;
import com.danielagapov.spawn.Models.User;

import java.util.List;
import java.util.stream.Collectors;

public class FeedbackSubmissionMapper {

    public static FeedbackSubmissionDTO toDTO(FeedbackSubmission entity) {
        return new FeedbackSubmissionDTO(
                entity.getType(),
                entity.getFromUser() != null ? entity.getFromUser().getId() : null,
                entity.getFromUserEmail(),
                entity.getMessage(),
                entity.isResolved(),
                entity.getResolutionComment()
        );
    }

    public static FeedbackSubmission toEntity(FeedbackSubmissionDTO dto, User user) {
        FeedbackSubmission feedbackSubmission = new FeedbackSubmission();
        feedbackSubmission.setType(dto.getType());
        feedbackSubmission.setFromUser(user);
        feedbackSubmission.setFromUserEmail(dto.getFromUserEmail());
        feedbackSubmission.setMessage(dto.getMessage());
        return feedbackSubmission;
    }

    public static List<FeedbackSubmissionDTO> toDTOList(List<FeedbackSubmission> entities) {
        return entities.stream()
                .map(FeedbackSubmissionMapper::toDTO)
                .collect(Collectors.toList());
    }

}
