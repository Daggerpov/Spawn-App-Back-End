package com.danielagapov.spawn.ServiceTests;

import com.danielagapov.spawn.shared.exceptions.Logger.ILogger;
import com.danielagapov.spawn.user.internal.domain.UserInterest;
import com.danielagapov.spawn.user.internal.domain.User;
import com.danielagapov.spawn.user.internal.repositories.IUserRepository;
import com.danielagapov.spawn.user.internal.repositories.UserInterestRepository;
import com.danielagapov.spawn.user.internal.services.UserInterestService;
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
        UserInterest interest1 = new UserInterest(testUser, "hiking");
        UserInterest interest2 = new UserInterest(testUser, "cooking");
        List<UserInterest> userInterests = Arrays.asList(interest1, interest2);

        when(userInterestRepository.findByUserId(testUserId)).thenReturn(userInterests);

        List<String> result = userInterestService.getUserInterests(testUserId);

        assertEquals(2, result.size());
        assertTrue(result.contains("hiking"));
        assertTrue(result.contains("cooking"));
        verify(userInterestRepository).findByUserId(testUserId);
    }

    @Test
    void getUserInterests_ShouldReturnEmptyList_WhenUserHasNoInterests() {
        when(userInterestRepository.findByUserId(testUserId)).thenReturn(Arrays.asList());

        List<String> result = userInterestService.getUserInterests(testUserId);

        assertTrue(result.isEmpty());
        verify(userInterestRepository).findByUserId(testUserId);
    }

    @Test
    void addUserInterest_ShouldReturnInterestName_WhenSuccessful() {
        String interestName = "photography";
        when(userInterestRepository.findByUserIdAndInterestIgnoreCase(testUserId, interestName))
            .thenReturn(Optional.empty());
        when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));
        when(userInterestRepository.save(any(UserInterest.class))).thenReturn(testUserInterest);

        String result = userInterestService.addUserInterest(testUserId, interestName);

        assertEquals("hiking", result);
        verify(userRepository).findById(testUserId);
        verify(userInterestRepository).save(any(UserInterest.class));
    }

    @Test
    void addUserInterest_ShouldReturnExisting_WhenDuplicate() {
        String interestName = "hiking";
        when(userInterestRepository.findByUserIdAndInterestIgnoreCase(testUserId, interestName))
            .thenReturn(Optional.of(testUserInterest));

        String result = userInterestService.addUserInterest(testUserId, interestName);

        assertEquals("hiking", result);
        verify(userRepository, never()).findById(any());
        verify(userInterestRepository, never()).save(any(UserInterest.class));
    }

    @Test
    void addUserInterest_ShouldThrowException_WhenUserNotFound() {
        String interestName = "photography";
        when(userInterestRepository.findByUserIdAndInterestIgnoreCase(testUserId, interestName))
            .thenReturn(Optional.empty());
        when(userRepository.findById(testUserId)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class,
            () -> userInterestService.addUserInterest(testUserId, interestName));

        assertTrue(exception.getMessage().contains("User not found"));
        verify(userRepository).findById(testUserId);
        verify(userInterestRepository, never()).save(any(UserInterest.class));
    }

    @Test
    void removeUserInterest_ShouldReturnTrue_WhenInterestExists() {
        when(userInterestRepository.findByUserIdAndInterestIgnoreCase(testUserId, "hiking"))
            .thenReturn(Optional.of(testUserInterest));

        boolean result = userInterestService.removeUserInterest(testUserId, "hiking");

        assertTrue(result);
        verify(userInterestRepository).findByUserIdAndInterestIgnoreCase(testUserId, "hiking");
        verify(userInterestRepository).delete(testUserInterest);
    }

    @Test
    void removeUserInterest_ShouldReturnTrue_WhenCaseDiffers() {
        when(userInterestRepository.findByUserIdAndInterestIgnoreCase(testUserId, "Hiking"))
            .thenReturn(Optional.of(testUserInterest));

        boolean result = userInterestService.removeUserInterest(testUserId, "Hiking");

        assertTrue(result);
        verify(userInterestRepository).delete(testUserInterest);
    }

    @Test
    void removeUserInterest_ShouldReturnFalse_WhenInterestNotFound() {
        when(userInterestRepository.findByUserIdAndInterestIgnoreCase(testUserId, "nonexistent"))
            .thenReturn(Optional.empty());

        boolean result = userInterestService.removeUserInterest(testUserId, "nonexistent");

        assertFalse(result);
        verify(userInterestRepository, never()).delete(any(UserInterest.class));
    }

    @Test
    void replaceUserInterests_ShouldReplaceAll() {
        List<String> newInterests = Arrays.asList("cooking", "hiking", "photography");
        when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));
        when(userInterestRepository.save(any(UserInterest.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        List<String> result = userInterestService.replaceUserInterests(testUserId, newInterests);

        assertEquals(3, result.size());
        assertTrue(result.contains("cooking"));
        assertTrue(result.contains("hiking"));
        assertTrue(result.contains("photography"));
        verify(userInterestRepository).deleteAllByUserId(testUserId);
        verify(userInterestRepository, times(3)).save(any(UserInterest.class));
    }

    @Test
    void replaceUserInterests_ShouldDeduplicateCaseInsensitive() {
        List<String> newInterests = Arrays.asList("Hiking", "hiking", "HIKING");
        when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));
        when(userInterestRepository.save(any(UserInterest.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        List<String> result = userInterestService.replaceUserInterests(testUserId, newInterests);

        assertEquals(1, result.size());
        assertEquals("Hiking", result.get(0));
        verify(userInterestRepository, times(1)).save(any(UserInterest.class));
    }

    @Test
    void replaceUserInterests_ShouldSkipEmptyAndWhitespace() {
        List<String> newInterests = Arrays.asList("hiking", "", "  ", "cooking");
        when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));
        when(userInterestRepository.save(any(UserInterest.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        List<String> result = userInterestService.replaceUserInterests(testUserId, newInterests);

        assertEquals(2, result.size());
        assertTrue(result.contains("hiking"));
        assertTrue(result.contains("cooking"));
        verify(userInterestRepository, times(2)).save(any(UserInterest.class));
    }

    @Test
    void replaceUserInterests_ShouldThrowException_WhenUserNotFound() {
        when(userRepository.findById(testUserId)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class,
            () -> userInterestService.replaceUserInterests(testUserId, Arrays.asList("hiking")));

        assertTrue(exception.getMessage().contains("User not found"));
    }
}
