package com.danielagapov.spawn.ServiceTests;

import com.danielagapov.spawn.DTOs.FeedbackSubmissionDTO;
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

        FeedbackSubmission saved = service.submitFeedback(dto);

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
    public void deleteFeedback_ShouldDeleteAndReturnDTO_WhenFeedbackExists() {
        UUID feedbackId = UUID.randomUUID();

        FeedbackSubmission feedback = new FeedbackSubmission();
        feedback.setId(feedbackId);
        feedback.setMessage("Delete me!");
        feedback.setFromUserEmail("delete@example.com");
        feedback.setResolved(false);
        feedback.setResolutionComment(null);

        when(repository.findById(feedbackId)).thenReturn(Optional.of(feedback));

        FeedbackSubmissionDTO deletedDTO = service.deleteFeedback(feedbackId);

        assertNotNull(deletedDTO);
        assertEquals("Delete me!", deletedDTO.getMessage());
        assertEquals("delete@example.com", deletedDTO.getFromUserEmail());
        verify(repository, times(1)).delete(feedback);
    }

    @Test
    public void deleteFeedback_ShouldThrowException_WhenFeedbackNotFound() {
        UUID feedbackId = UUID.randomUUID();

        when(repository.findById(feedbackId)).thenReturn(Optional.empty());

        assertThrows(BaseNotFoundException.class, () -> service.deleteFeedback(feedbackId));
        verify(repository, never()).delete(any());
    }

}
