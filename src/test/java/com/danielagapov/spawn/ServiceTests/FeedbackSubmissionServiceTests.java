package com.danielagapov.spawn.ServiceTests;

import com.danielagapov.spawn.DTOs.CreateFeedbackSubmissionDTO;
import com.danielagapov.spawn.DTOs.FetchFeedbackSubmissionDTO;
import com.danielagapov.spawn.Enums.FeedbackType;
import com.danielagapov.spawn.Exceptions.Base.BaseNotFoundException;
import com.danielagapov.spawn.Exceptions.Base.BaseSaveException;
import com.danielagapov.spawn.Exceptions.Logger.ILogger;
import com.danielagapov.spawn.Models.FeedbackSubmission;
import com.danielagapov.spawn.Models.User;
import com.danielagapov.spawn.Repositories.IFeedbackSubmissionRepository;
import com.danielagapov.spawn.Repositories.IUserRepository;
import com.danielagapov.spawn.Services.FeedbackSubmission.FeedbackSubmissionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

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

    @InjectMocks
    private FeedbackSubmissionService service;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void submitFeedback_ShouldSaveFeedback_WhenValidInputProvided() {
        UUID userId = UUID.randomUUID();
        FeedbackSubmissionDTO dto = new FeedbackSubmissionDTO(
                UUID.randomUUID(),
                FeedbackType.BUG,
                userId,
                "user@example.com",
                "Test feedback message",
                false,
                null
        );

        User user = new User();
        user.setId(userId);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(repository.save(any(FeedbackSubmission.class))).thenAnswer(invocation -> invocation.getArgument(0));

        FeedbackSubmissionDTO saved = service.submitFeedback(dto);

        assertNotNull(saved);
        assertEquals(dto.getType(), saved.getType());
        assertEquals(dto.getFromUserEmail(), saved.getFromUserEmail());
        assertEquals(dto.getMessage(), saved.getMessage());
    }

    @Test
    public void submitFeedback_ShouldThrowException_WhenUserNotFound() {
        UUID userId = UUID.randomUUID();
        FeedbackSubmissionDTO dto = new FeedbackSubmissionDTO(
                UUID.randomUUID(),
                FeedbackType.BUG,
                userId,
                "user@example.com",
                "Test feedback message",
                false,
                null
        );

        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThrows(BaseNotFoundException.class, () -> service.submitFeedback(dto));
    }

    @Test
    public void submitFeedback_ShouldThrowException_WhenUserIdIsNull() {
        FeedbackSubmissionDTO dto = new FeedbackSubmissionDTO(
                UUID.randomUUID(),
                FeedbackType.BUG,
                null,
                "user@example.com",
                "Test feedback message",
                false,
                null
        );

        assertThrows(BaseSaveException.class, () -> service.submitFeedback(dto));
    }

    @Test
    public void resolveFeedback_ShouldSetResolvedFlagAndComment_WhenFeedbackExists() {
        UUID feedbackId = UUID.randomUUID();
        FeedbackSubmission feedback = new FeedbackSubmission();
        feedback.setId(feedbackId);
        feedback.setType(FeedbackType.BUG); // <-- FIXED LINE
        feedback.setMessage("Test message");
        feedback.setFromUserEmail("test@example.com");

        when(repository.findById(feedbackId)).thenReturn(Optional.of(feedback));
        when(repository.save(any(FeedbackSubmission.class))).thenAnswer(invocation -> invocation.getArgument(0));

        FeedbackSubmissionDTO result = service.resolveFeedback(feedbackId, "Resolved reason");

        assertTrue(result.isResolved());
        assertEquals("Resolved reason", result.getResolutionComment());
        verify(repository).save(feedback);
    }


    @Test
    public void resolveFeedback_ShouldThrowException_WhenFeedbackNotFound() {
        UUID feedbackId = UUID.randomUUID();

        when(repository.findById(feedbackId)).thenReturn(Optional.empty());

        assertThrows(BaseNotFoundException.class, () -> service.resolveFeedback(feedbackId, "Reason"));
    }

    @Test
    public void getAllFeedbacks_ShouldReturnListOfDTOs_WhenFeedbacksExist() {
        FeedbackSubmission feedback = new FeedbackSubmission();
        feedback.setMessage("Feedback message");
        feedback.setFromUserEmail("user@example.com");
        feedback.setResolved(false);
        feedback.setResolutionComment(null);

        when(repository.findAll()).thenReturn(List.of(feedback));

        List<FeedbackSubmissionDTO> dtos = service.getAllFeedbacks();

        assertEquals(1, dtos.size());
        assertEquals("Feedback message", dtos.get(0).getMessage());
    }

    @Test
    public void deleteFeedback_ShouldDeleteFeedback_WhenFeedbackExists() {
        UUID feedbackId = UUID.randomUUID();

        // Since the method now returns void, we'll just verify the deleteById method is called
        doNothing().when(repository).deleteById(feedbackId);

        // This should not throw any exception
        assertDoesNotThrow(() -> service.deleteFeedback(feedbackId));

        // Verify that deleteById was called with the correct ID
        verify(repository, times(1)).deleteById(feedbackId);
    }

    @Test
    public void deleteFeedback_ShouldLogError_WhenDeletionFails() {
        UUID feedbackId = UUID.randomUUID();

        // Simulate an exception during deletion
        doThrow(new RuntimeException("Deletion failed")).when(repository).deleteById(feedbackId);

        // Verify that the exception is rethrown and logged
        RuntimeException exception = assertThrows(RuntimeException.class, () -> service.deleteFeedback(feedbackId));

        verify(logger, times(1)).error(eq("Deletion failed"));
        verify(repository, times(1)).deleteById(feedbackId);
    }
}
