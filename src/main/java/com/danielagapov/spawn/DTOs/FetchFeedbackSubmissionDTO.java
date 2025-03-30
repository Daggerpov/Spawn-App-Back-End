package com.danielagapov.spawn.DTOs;

import com.danielagapov.spawn.Enums.FeedbackType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FetchFeedbackSubmissionDTO implements Serializable {
    private UUID id;
    private FeedbackType type;
    private UUID fromUserId;
    private String fromUserEmail;
    private String firstName;
    private String lastName;
    private String message;
    private boolean isResolved;
    private String resolutionComment;
    private String imageUrl;
    private OffsetDateTime submittedAt;
}
