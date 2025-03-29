package com.danielagapov.spawn.DTOs;

import com.danielagapov.spawn.Enums.FeedbackType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

import java.io.Serializable;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateFeedbackSubmissionDTO implements Serializable {
    private FeedbackType type;
    private UUID fromUserId;
    private String fromUserEmail;
    private String message;
    private MultipartFile image;
} 