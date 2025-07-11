package com.danielagapov.spawn.ServiceTests;

import com.danielagapov.spawn.Exceptions.Logger.ILogger;
import com.danielagapov.spawn.Models.User.Profile.UserInterest;
import com.danielagapov.spawn.Models.User.User;
import com.danielagapov.spawn.Repositories.User.IUserRepository;
import com.danielagapov.spawn.Repositories.User.Profile.UserInterestRepository;
import com.danielagapov.spawn.Services.UserInterest.UserInterestService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class UserInterestServiceTest {

    @Mock
    private UserInterestRepository userInterestRepository;

    @Mock
    private IUserRepository userRepository;

    @Mock
    private ILogger logger;

    @InjectMocks
    private UserInterestService userInterestService;

    private UUID testUserId;
    private User testUser;
    private UserInterest testUserInterest;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        
        testUserId = UUID.randomUUID();
        testUser = new User();
        testUser.setId(testUserId);
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        
        testUserInterest = new UserInterest();
        testUserInterest.setId(UUID.randomUUID());
        testUserInterest.setUser(testUser);
        testUserInterest.setInterest("hiking");
        testUserInterest.setCreatedAt(Instant.now());
    }

    @Test
    void getUserInterests_ShouldReturnListOfInterests_WhenUserHasInterests() {
        // Arrange
        UserInterest interest1 = new UserInterest(testUser, "hiking");
        UserInterest interest2 = new UserInterest(testUser, "cooking");
        List<UserInterest> userInterests = Arrays.asList(interest1, interest2);
        
        when(userInterestRepository.findByUserId(testUserId)).thenReturn(userInterests);

        // Act
        List<String> result = userInterestService.getUserInterests(testUserId);

        // Assert
        assertEquals(2, result.size());
        assertTrue(result.contains("hiking"));
        assertTrue(result.contains("cooking"));
        verify(userInterestRepository).findByUserId(testUserId);
    }

    @Test
    void getUserInterests_ShouldReturnEmptyList_WhenUserHasNoInterests() {
        // Arrange
        when(userInterestRepository.findByUserId(testUserId)).thenReturn(Arrays.asList());

        // Act
        List<String> result = userInterestService.getUserInterests(testUserId);

        // Assert
        assertTrue(result.isEmpty());
        verify(userInterestRepository).findByUserId(testUserId);
    }

    @Test
    void addUserInterest_ShouldReturnInterestName_WhenSuccessful() {
        // Arrange
        String interestName = "photography";
        when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));
        when(userInterestRepository.save(any(UserInterest.class))).thenReturn(testUserInterest);

        // Act
        String result = userInterestService.addUserInterest(testUserId, interestName);

        // Assert
        assertEquals("hiking", result); // testUserInterest has "hiking" as interest
        verify(userRepository).findById(testUserId);
        verify(userInterestRepository).save(any(UserInterest.class));
    }

    @Test
    void addUserInterest_ShouldThrowException_WhenUserNotFound() {
        // Arrange
        String interestName = "photography";
        when(userRepository.findById(testUserId)).thenReturn(Optional.empty());

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, 
            () -> userInterestService.addUserInterest(testUserId, interestName));
        
        assertTrue(exception.getMessage().contains("User not found"));
        verify(userRepository).findById(testUserId);
        verify(userInterestRepository, never()).save(any(UserInterest.class));
    }

    @Test
    void removeUserInterest_ShouldReturnTrue_WhenInterestExists() {
        // Arrange
        String encodedInterest = "hiking";
        when(userInterestRepository.findByUserId(testUserId))
            .thenReturn(Arrays.asList(testUserInterest));
        when(userInterestRepository.findByUserIdAndInterest(testUserId, "hiking"))
            .thenReturn(Optional.of(testUserInterest));

        // Act
        boolean result = userInterestService.removeUserInterest(testUserId, encodedInterest);

        // Assert
        assertTrue(result);
        verify(userInterestRepository).findByUserId(testUserId);
        verify(userInterestRepository).findByUserIdAndInterest(testUserId, "hiking");
        verify(userInterestRepository).delete(testUserInterest);
        verify(logger).info(contains("Attempting to remove interest"));
        verify(logger).info(contains("Successfully removed interest"));
    }

    @Test
    void removeUserInterest_ShouldReturnFalse_WhenInterestNotFound() {
        // Arrange
        String encodedInterest = "nonexistent";
        when(userInterestRepository.findByUserId(testUserId))
            .thenReturn(Arrays.asList(testUserInterest));
        when(userInterestRepository.findByUserIdAndInterest(testUserId, "nonexistent"))
            .thenReturn(Optional.empty());

        // Act
        boolean result = userInterestService.removeUserInterest(testUserId, encodedInterest);

        // Assert
        assertFalse(result);
        verify(userInterestRepository).findByUserId(testUserId);
        verify(userInterestRepository).findByUserIdAndInterest(testUserId, "nonexistent");
        verify(userInterestRepository, never()).delete(any(UserInterest.class));
        verify(logger).info(contains("Attempting to remove interest"));
        verify(logger).warn(contains("Interest 'nonexistent' not found"));
    }

    @Test
    void removeUserInterest_ShouldHandleUrlEncodedInterest_WhenInterestHasSpaces() {
        // Arrange
        String encodedInterest = "rock%20climbing";
        String decodedInterest = "rock climbing";
        
        UserInterest spaceInterest = new UserInterest(testUser, decodedInterest);
        when(userInterestRepository.findByUserId(testUserId))
            .thenReturn(Arrays.asList(spaceInterest));
        when(userInterestRepository.findByUserIdAndInterest(testUserId, decodedInterest))
            .thenReturn(Optional.of(spaceInterest));

        // Act
        boolean result = userInterestService.removeUserInterest(testUserId, encodedInterest);

        // Assert
        assertTrue(result);
        verify(userInterestRepository).findByUserId(testUserId);
        verify(userInterestRepository).findByUserIdAndInterest(testUserId, decodedInterest);
        verify(userInterestRepository).delete(spaceInterest);
        verify(logger, times(3)).info(contains("rock climbing")); // Called three times: attempting + debug listing + successfully
    }

    @Test
    void removeUserInterest_ShouldHandleUrlEncodedInterest_WhenInterestHasSpecialCharacters() {
        // Arrange
        String encodedInterest = "caf%C3%A9%20culture";
        String decodedInterest = "café culture";
        
        UserInterest specialInterest = new UserInterest(testUser, decodedInterest);
        when(userInterestRepository.findByUserId(testUserId))
            .thenReturn(Arrays.asList(specialInterest));
        when(userInterestRepository.findByUserIdAndInterest(testUserId, decodedInterest))
            .thenReturn(Optional.of(specialInterest));

        // Act
        boolean result = userInterestService.removeUserInterest(testUserId, encodedInterest);

        // Assert
        assertTrue(result);
        verify(userInterestRepository).findByUserId(testUserId);
        verify(userInterestRepository).findByUserIdAndInterest(testUserId, decodedInterest);
        verify(userInterestRepository).delete(specialInterest);
        verify(logger, times(3)).info(contains("café culture")); // Called three times: attempting + debug listing + successfully
    }

    @Test
    void removeUserInterest_ShouldThrowException_WhenRepositoryThrowsException() {
        // Arrange
        String encodedInterest = "hiking";
        when(userInterestRepository.findByUserIdAndInterest(testUserId, "hiking"))
            .thenThrow(new RuntimeException("Database connection failed"));

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class,
            () -> userInterestService.removeUserInterest(testUserId, encodedInterest));
        
        assertTrue(exception.getMessage().contains("Failed to remove user interest"));
        verify(logger).error(contains("Error removing interest"));
    }

    @Test
    void removeUserInterest_ShouldLogError_WhenUnexpectedExceptionOccurs() {
        // Arrange
        String encodedInterest = "hiking";
        when(userInterestRepository.findByUserIdAndInterest(testUserId, "hiking"))
            .thenReturn(Optional.of(testUserInterest));
        doThrow(new RuntimeException("Delete failed"))
            .when(userInterestRepository).delete(testUserInterest);

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class,
            () -> userInterestService.removeUserInterest(testUserId, encodedInterest));
        
        assertTrue(exception.getMessage().contains("Failed to remove user interest"));
        verify(logger).error(contains("Error removing interest"));
    }
} 