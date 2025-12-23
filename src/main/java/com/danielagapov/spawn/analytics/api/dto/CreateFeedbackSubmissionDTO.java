package com.danielagapov.spawn.analytics.api.dto;

import com.danielagapov.spawn.shared.util.FeedbackType;
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
    private String message;
    private byte[] imageData;
} 