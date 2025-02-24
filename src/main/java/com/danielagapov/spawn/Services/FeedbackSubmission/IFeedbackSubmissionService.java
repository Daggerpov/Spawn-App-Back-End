package com.danielagapov.spawn.Services.FeedbackSubmission;

import com.danielagapov.spawn.DTOs.FeedbackSubmissionDTO;
import com.danielagapov.spawn.Models.FeedbackSubmission;

import java.util.List;

public interface IFeedbackSubmissionService {
    FeedbackSubmission submitFeedback(FeedbackSubmissionDTO dto);
    List<FeedbackSubmissionDTO> getAllFeedbacks();
}
