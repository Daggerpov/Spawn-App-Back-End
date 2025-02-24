package com.danielagapov.spawn.Mappers;

import com.danielagapov.spawn.DTOs.FeedbackSubmissionDTO;
import com.danielagapov.spawn.Models.FeedbackSubmission;

import java.util.List;
import java.util.stream.Collectors;

public class FeedbackSubmissionMapper {

    public static FeedbackSubmissionDTO toDTO(FeedbackSubmission entity) {
        return new FeedbackSubmissionDTO(
                entity.getType(),
                entity.getFromUserId(),
                null,
                entity.getMessage()
        );
    }

    public static FeedbackSubmission toEntity(FeedbackSubmissionDTO dto) {
        return new FeedbackSubmission(
                null,
                dto.getType(),
                dto.getFromUserId(),
                null,
                dto.getMessage()
        );
    }

    public static List<FeedbackSubmissionDTO> toDTOList(List<FeedbackSubmission> entities) {
        return entities.stream()
                .map(FeedbackSubmissionMapper::toDTO)
                .collect(Collectors.toList());
    }

}
