package com.danielagapov.spawn.DTOs;

import com.danielagapov.spawn.Enums.FeedbackStatus;
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
    private FeedbackStatus status;
    private String resolutionComment;
    private String imageUrl;
    private OffsetDateTime submittedAt;
    
    /**
     * Legacy support method to maintain compatibility with frontend code
     * @return true if status is RESOLVED, false otherwise
     */
    public boolean isResolved() {
        return this.status == FeedbackStatus.RESOLVED;
    }
    
    /**
     * Legacy support method to set isResolved based on status
     */
    public void setIsResolved(boolean resolved) {
        this.status = resolved ? FeedbackStatus.RESOLVED : FeedbackStatus.PENDING;
    }
}
