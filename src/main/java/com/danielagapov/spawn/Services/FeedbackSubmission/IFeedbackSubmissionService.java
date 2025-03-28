package com.danielagapov.spawn.Services.FeedbackSubmission;

import com.danielagapov.spawn.DTOs.FeedbackSubmissionDTO;

import java.util.List;
import java.util.UUID;

public interface IFeedbackSubmissionService {
    FeedbackSubmissionDTO submitFeedback(FeedbackSubmissionDTO dto);
    List<FeedbackSubmissionDTO> getAllFeedbacks();

    FeedbackSubmissionDTO resolveFeedback(UUID id, String resolutionComment);

    void deleteFeedback(UUID id);
}
