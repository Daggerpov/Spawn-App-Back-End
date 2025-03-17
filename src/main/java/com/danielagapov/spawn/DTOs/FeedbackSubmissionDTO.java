package com.danielagapov.spawn.DTOs;

import com.danielagapov.spawn.Enums.FeedbackType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FeedbackSubmissionDTO {
    private FeedbackType type;
    private UUID fromUserId;
    private String fromUserEmail;
    private String message;
    private boolean isResolved;
    private String resolutionComment;
}
