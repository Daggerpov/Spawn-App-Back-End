package com.danielagapov.spawn.DTOs;

import com.danielagapov.spawn.Enums.FeedbackType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.UUID;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class CreateFeedbackSubmissionDTO implements Serializable {
    private FeedbackType type;
    private UUID fromUserId;
    private String firstName;
    private String lastName;
    private String message;
    private byte[] imageData;
    
    // Constructor without firstName and lastName for backward compatibility
    public CreateFeedbackSubmissionDTO(FeedbackType type, UUID fromUserId, String message, byte[] imageData) {
        this.type = type;
        this.fromUserId = fromUserId;
        this.message = message;
        this.imageData = imageData;
    }
} 