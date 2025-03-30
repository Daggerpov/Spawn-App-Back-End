package com.danielagapov.spawn.ServiceTests;

import com.danielagapov.spawn.DTOs.CreateFeedbackSubmissionDTO;
import com.danielagapov.spawn.DTOs.FetchFeedbackSubmissionDTO;
import com.danielagapov.spawn.Enums.EntityType;
import com.danielagapov.spawn.Enums.FeedbackType;
import com.danielagapov.spawn.Exceptions.Base.BaseNotFoundException;
import com.danielagapov.spawn.Exceptions.Base.BaseSaveException;
import com.danielagapov.spawn.Exceptions.Logger.ILogger;
import com.danielagapov.spawn.Mappers.FeedbackSubmissionMapper;
import com.danielagapov.spawn.Models.FeedbackSubmission;
import com.danielagapov.spawn.Models.User;
import com.danielagapov.spawn.Repositories.IFeedbackSubmissionRepository;
import com.danielagapov.spawn.Repositories.IUserRepository;
import com.danielagapov.spawn.Services.FeedbackSubmission.FeedbackSubmissionService;
import com.danielagapov.spawn.Services.S3.IS3Service;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class FeedbackSubmissionServiceTests {

    @Mock
    private IFeedbackSubmissionRepository repository;

    @Mock
    private IUserRepository userRepository;

    @Mock
    private ILogger logger;
    
    @Mock
    private IS3Service s3Service;

    @InjectMocks
    private FeedbackSubmissionService service;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void submitFeedback_ShouldSaveFeedback_WhenValidInputProvided() throws IOException {
        // Arrange
        UUID userId = UUID.randomUUID();
        CreateFeedbackSubmissionDTO dto = new CreateFeedbackSubmissionDTO(
                FeedbackType.BUG,
                userId,
                "Test feedback message",
                null
        );

        User user = new User();
        user.setId(userId);
        user.setEmail("user@example.com");

        FeedbackSubmission feedbackSubmission = new FeedbackSubmission();
        feedbackSubmission.setId(UUID.randomUUID());
        feedbackSubmission.setType(FeedbackType.BUG);
        feedbackSubmission.setFromUser(user);
        feedbackSubmission.setFromUserEmail(user.getEmail());
        feedbackSubmission.setMessage("Test feedback message");
        
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(repository.save(any(FeedbackSubmission.class))).thenReturn(feedbackSubmission);

        // Act
        FetchFeedbackSubmissionDTO saved = service.submitFeedback(dto);

        // Assert
        assertNotNull(saved);
        assertEquals(dto.getType(), saved.getType());
        assertEquals(user.getEmail(), saved.getFromUserEmail());
        assertEquals(dto.getMessage(), saved.getMessage());
        verify(repository).save(any(FeedbackSubmission.class));
    }

    @Test
    public void submitFeedback_ShouldThrowException_WhenUserNotFound() {
        // Arrange
        UUID userId = UUID.randomUUID();
        CreateFeedbackSubmissionDTO dto = new CreateFeedbackSubmissionDTO(
                FeedbackType.BUG,
                userId,
                "Test feedback message",
                null
        );

        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(BaseNotFoundException.class, () -> service.submitFeedback(dto));
    }

    @Test
    public void submitFeedback_ShouldThrowException_WhenUserIdIsNull() {
        // Arrange
        CreateFeedbackSubmissionDTO dto = new CreateFeedbackSubmissionDTO(
                FeedbackType.BUG,
                null,
                "Test feedback message",
                null
        );

        // Act & Assert
        assertThrows(BaseNotFoundException.class, () -> service.submitFeedback(dto));
    }
    
    @Test
    public void submitFeedback_ShouldUploadImage_WhenImageProvided() throws IOException {
        // Arrange
        UUID userId = UUID.randomUUID();
        byte[] imageData = "test image data".getBytes();
        CreateFeedbackSubmissionDTO dto = new CreateFeedbackSubmissionDTO(
                FeedbackType.BUG,
                userId,
                "Test feedback message",
                imageData
        );

        User user = new User();
        user.setId(userId);
        user.setEmail("user@example.com");

        String imageUrl = "https://s3.example.com/images/feedback/123";
        
        FeedbackSubmission savedFeedback = new FeedbackSubmission();
        savedFeedback.setId(UUID.randomUUID());
        savedFeedback.setType(FeedbackType.BUG);
        savedFeedback.setFromUser(user);
        savedFeedback.setFromUserEmail("user@example.com");
        savedFeedback.setMessage("Test feedback message");
        savedFeedback.setImageUrl(imageUrl);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(s3Service.putObjectWithKey(eq(imageData), anyString())).thenReturn(imageUrl);
        when(repository.save(any(FeedbackSubmission.class))).thenReturn(savedFeedback);

        // Act
        FetchFeedbackSubmissionDTO result = service.submitFeedback(dto);

        // Assert
        assertNotNull(result);
        assertEquals(imageUrl, result.getImageUrl());
        verify(s3Service).putObjectWithKey(eq(imageData), anyString());
    }

    @Test
    public void resolveFeedback_ShouldSetResolvedFlagAndComment_WhenFeedbackExists() {
        // Arrange
        UUID feedbackId = UUID.randomUUID();
        FeedbackSubmission feedback = new FeedbackSubmission();
        feedback.setId(feedbackId);
        feedback.setType(FeedbackType.BUG);
        feedback.setMessage("Test message");
        feedback.setFromUserEmail("test@example.com");
        
        FeedbackSubmission updatedFeedback = new FeedbackSubmission();
        updatedFeedback.setId(feedbackId);
        updatedFeedback.setType(FeedbackType.BUG);
        updatedFeedback.setMessage("Test message");
        updatedFeedback.setFromUserEmail("test@example.com");
        updatedFeedback.setResolved(true);
        updatedFeedback.setResolutionComment("Resolved reason");

        when(repository.findById(feedbackId)).thenReturn(Optional.of(feedback));
        when(repository.save(any(FeedbackSubmission.class))).thenReturn(updatedFeedback);

        // Act
        FetchFeedbackSubmissionDTO result = service.resolveFeedback(feedbackId, "Resolved reason");

        // Assert
        assertTrue(result.isResolved());
        assertEquals("Resolved reason", result.getResolutionComment());
        verify(repository).save(feedback);
    }

    @Test
    public void resolveFeedback_ShouldThrowException_WhenFeedbackNotFound() {
        // Arrange
        UUID feedbackId = UUID.randomUUID();
        when(repository.findById(feedbackId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(BaseNotFoundException.class, () -> service.resolveFeedback(feedbackId, "Reason"));
    }

    @Test
    public void getAllFeedbacks_ShouldReturnListOfDTOs_WhenFeedbacksExist() {
        // Arrange
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail("user@example.com");
        
        FeedbackSubmission feedback = new FeedbackSubmission();
        feedback.setId(UUID.randomUUID());
        feedback.setType(FeedbackType.BUG);
        feedback.setFromUser(user);
        feedback.setMessage("Feedback message");
        feedback.setFromUserEmail("user@example.com");
        feedback.setResolved(false);
        feedback.setResolutionComment(null);

        when(repository.findAll()).thenReturn(List.of(feedback));

        // Act
        List<FetchFeedbackSubmissionDTO> dtos = service.getAllFeedbacks();

        // Assert
        assertEquals(1, dtos.size());
        assertEquals("Feedback message", dtos.get(0).getMessage());
        assertEquals("user@example.com", dtos.get(0).getFromUserEmail());
    }

    @Test
    public void deleteFeedback_ShouldDeleteFeedback_WhenFeedbackExists() {
        // Arrange
        UUID feedbackId = UUID.randomUUID();
        doNothing().when(repository).deleteById(feedbackId);

        // Act & Assert
        assertDoesNotThrow(() -> service.deleteFeedback(feedbackId));
        verify(repository, times(1)).deleteById(feedbackId);
    }

    @Test
    public void deleteFeedback_ShouldLogError_WhenDeletionFails() {
        // Arrange
        UUID feedbackId = UUID.randomUUID();
        RuntimeException exception = new RuntimeException("Deletion failed");
        doThrow(exception).when(repository).deleteById(feedbackId);

        // Act & Assert
        RuntimeException thrown = assertThrows(RuntimeException.class, () -> service.deleteFeedback(feedbackId));
        assertEquals(exception, thrown);
        verify(logger, times(1)).error(eq("Deletion failed"));
        verify(repository, times(1)).deleteById(feedbackId);
    }
}
