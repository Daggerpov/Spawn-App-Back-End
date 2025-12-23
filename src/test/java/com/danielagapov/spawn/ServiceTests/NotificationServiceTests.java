package com.danielagapov.spawn.ServiceTests;

import com.danielagapov.spawn.notification.api.dto.DeviceTokenDTO;
import com.danielagapov.spawn.notification.api.dto.NotificationPreferencesDTO;
import com.danielagapov.spawn.notification.internal.domain.DeviceToken;
import com.danielagapov.spawn.notification.internal.domain.NotificationPreferences;
import com.danielagapov.spawn.notification.internal.repositories.IDeviceTokenRepository;
import com.danielagapov.spawn.notification.internal.repositories.INotificationPreferencesRepository;
import com.danielagapov.spawn.notification.internal.services.NotificationService;
import com.danielagapov.spawn.shared.exceptions.Logger.ILogger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessException;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for NotificationService
 * Tests notification preferences and device token management with edge cases
 */
@ExtendWith(MockitoExtension.class)
class NotificationServiceTests {

    @Mock
    private IDeviceTokenRepository deviceTokenRepository;

    @Mock
    private INotificationPreferencesRepository notificationPreferencesRepository;

    @Mock
    private ILogger logger;

    @InjectMocks
    private NotificationService notificationService;

    private UUID userId;
    private String deviceToken;
    private DeviceTokenDTO deviceTokenDTO;
    private DeviceToken deviceTokenEntity;
    private NotificationPreferencesDTO preferencesDTO;
    private NotificationPreferences preferencesEntity;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        deviceToken = "test-device-token-12345";
        
        deviceTokenDTO = new DeviceTokenDTO(userId, deviceToken);
        
        deviceTokenEntity = new DeviceToken();
        deviceTokenEntity.setUserId(userId);
        deviceTokenEntity.setToken(deviceToken);
        
        preferencesDTO = new NotificationPreferencesDTO(
            userId, true, true, true, true, false
        );
        
        preferencesEntity = new NotificationPreferences();
        preferencesEntity.setUserId(userId);
        preferencesEntity.setFriendRequestsEnabled(true);
        preferencesEntity.setActivityInvitesEnabled(true);
        preferencesEntity.setActivityUpdatesEnabled(true);
        preferencesEntity.setNewMessagesEnabled(true);
        preferencesEntity.setSoundEnabled(false);
    }

    // MARK: - Register Device Token Tests

    @Test
    void registerDeviceToken_ShouldSaveToken_WhenValidToken() {
        when(deviceTokenRepository.save(any(DeviceToken.class))).thenReturn(deviceTokenEntity);

        assertDoesNotThrow(() -> notificationService.registerDeviceToken(deviceTokenDTO));

        verify(deviceTokenRepository, times(1)).save(any(DeviceToken.class));
    }

    @Test
    void registerDeviceToken_ShouldThrowException_WhenDatabaseError() {
        when(deviceTokenRepository.save(any(DeviceToken.class)))
                .thenThrow(new DataAccessException("Database error") {});

        assertThrows(DataAccessException.class, 
            () -> notificationService.registerDeviceToken(deviceTokenDTO));

        verify(deviceTokenRepository, times(1)).save(any(DeviceToken.class));
    }

    @Test
    void registerDeviceToken_ShouldHandleLongToken_WhenTokenIsVeryLong() {
        String longToken = "a".repeat(500);
        DeviceTokenDTO longTokenDTO = new DeviceTokenDTO(userId, longToken);
        DeviceToken longTokenEntity = new DeviceToken();
        longTokenEntity.setUserId(userId);
        longTokenEntity.setToken(longToken);
        
        when(deviceTokenRepository.save(any(DeviceToken.class))).thenReturn(longTokenEntity);

        assertDoesNotThrow(() -> notificationService.registerDeviceToken(longTokenDTO));

        verify(deviceTokenRepository, times(1)).save(any(DeviceToken.class));
    }

    @Test
    void registerDeviceToken_ShouldHandleSpecialCharacters_WhenTokenHasSpecialChars() {
        String specialToken = "token:with/special+chars=123";
        DeviceTokenDTO specialTokenDTO = new DeviceTokenDTO(userId, specialToken);
        DeviceToken specialTokenEntity = new DeviceToken();
        specialTokenEntity.setUserId(userId);
        specialTokenEntity.setToken(specialToken);
        
        when(deviceTokenRepository.save(any(DeviceToken.class))).thenReturn(specialTokenEntity);

        assertDoesNotThrow(() -> notificationService.registerDeviceToken(specialTokenDTO));

        verify(deviceTokenRepository, times(1)).save(any(DeviceToken.class));
    }

    // MARK: - Unregister Device Token Tests

    @Test
    void unregisterDeviceToken_ShouldDeleteToken_WhenTokenExists() {
        when(deviceTokenRepository.findByToken(deviceToken)).thenReturn(Optional.of(deviceTokenEntity));
        doNothing().when(deviceTokenRepository).delete(deviceTokenEntity);

        assertDoesNotThrow(() -> notificationService.unregisterDeviceToken(deviceToken));

        verify(deviceTokenRepository, times(1)).findByToken(deviceToken);
        verify(deviceTokenRepository, times(1)).delete(deviceTokenEntity);
    }

    @Test
    void unregisterDeviceToken_ShouldDoNothing_WhenTokenNotFound() {
        when(deviceTokenRepository.findByToken(deviceToken)).thenReturn(Optional.empty());

        assertDoesNotThrow(() -> notificationService.unregisterDeviceToken(deviceToken));

        verify(deviceTokenRepository, times(1)).findByToken(deviceToken);
        verify(deviceTokenRepository, never()).delete(any());
    }

    @Test
    void unregisterDeviceToken_ShouldThrowException_WhenDatabaseError() {
        when(deviceTokenRepository.findByToken(deviceToken)).thenReturn(Optional.of(deviceTokenEntity));
        doThrow(new DataAccessException("Database error") {})
                .when(deviceTokenRepository).delete(deviceTokenEntity);

        assertThrows(DataAccessException.class, 
            () -> notificationService.unregisterDeviceToken(deviceToken));

        verify(deviceTokenRepository, times(1)).delete(deviceTokenEntity);
    }

    // MARK: - Get Notification Preferences Tests

    @Test
    void getNotificationPreferences_ShouldReturnPreferences_WhenPreferencesExist() {
        when(notificationPreferencesRepository.findByUserId(userId))
                .thenReturn(Optional.of(preferencesEntity));

        NotificationPreferencesDTO result = notificationService.getNotificationPreferences(userId);

        assertNotNull(result);
        assertEquals(userId, result.getUserId());
        assertTrue(result.isFriendRequestsEnabled());
        assertTrue(result.isActivityInvitesEnabled());
        verify(notificationPreferencesRepository, times(1)).findByUserId(userId);
    }

    @Test
    void getNotificationPreferences_ShouldReturnDefaults_WhenPreferencesNotFound() {
        when(notificationPreferencesRepository.findByUserId(userId))
                .thenReturn(Optional.empty());

        NotificationPreferencesDTO result = notificationService.getNotificationPreferences(userId);

        assertNotNull(result);
        assertEquals(userId, result.getUserId());
        // Verify default values (all enabled)
        assertTrue(result.isFriendRequestsEnabled());
        assertTrue(result.isActivityInvitesEnabled());
        assertTrue(result.isActivityUpdatesEnabled());
        assertTrue(result.isNewMessagesEnabled());
        assertTrue(result.isSoundEnabled());
        verify(notificationPreferencesRepository, times(1)).findByUserId(userId);
    }

    @Test
    void getNotificationPreferences_ShouldThrowException_WhenDatabaseError() {
        when(notificationPreferencesRepository.findByUserId(userId))
                .thenThrow(new DataAccessException("Database error") {});

        assertThrows(DataAccessException.class, 
            () -> notificationService.getNotificationPreferences(userId));

        verify(notificationPreferencesRepository, times(1)).findByUserId(userId);
    }

    // MARK: - Save Notification Preferences Tests

    @Test
    void saveNotificationPreferences_ShouldSavePreferences_WhenValidPreferences() {
        when(notificationPreferencesRepository.save(any(NotificationPreferences.class)))
                .thenReturn(preferencesEntity);

        NotificationPreferencesDTO result = notificationService.saveNotificationPreferences(preferencesDTO);

        assertNotNull(result);
        assertEquals(userId, result.getUserId());
        assertTrue(result.isFriendRequestsEnabled());
        verify(notificationPreferencesRepository, times(1)).save(any(NotificationPreferences.class));
    }

    @Test
    void saveNotificationPreferences_ShouldHandleAllDisabled_WhenAllPreferencesDisabled() {
        NotificationPreferencesDTO allDisabledDTO = new NotificationPreferencesDTO(
            userId, false, false, false, false, false
        );
        NotificationPreferences allDisabledEntity = new NotificationPreferences();
        allDisabledEntity.setUserId(userId);
        allDisabledEntity.setFriendRequestsEnabled(false);
        allDisabledEntity.setActivityInvitesEnabled(false);
        allDisabledEntity.setActivityUpdatesEnabled(false);
        allDisabledEntity.setNewMessagesEnabled(false);
        allDisabledEntity.setSoundEnabled(false);
        
        when(notificationPreferencesRepository.save(any(NotificationPreferences.class)))
                .thenReturn(allDisabledEntity);

        NotificationPreferencesDTO result = notificationService.saveNotificationPreferences(allDisabledDTO);

        assertNotNull(result);
        assertFalse(result.isFriendRequestsEnabled());
        assertFalse(result.isActivityInvitesEnabled());
        assertFalse(result.isActivityUpdatesEnabled());
        assertFalse(result.isNewMessagesEnabled());
        assertFalse(result.isSoundEnabled());
        verify(notificationPreferencesRepository, times(1)).save(any(NotificationPreferences.class));
    }

    @Test
    void saveNotificationPreferences_ShouldHandlePartialPreferences_WhenSomeDisabled() {
        NotificationPreferencesDTO partialDTO = new NotificationPreferencesDTO(
            userId, false, false, true, true, false
        );
        NotificationPreferences partialEntity = new NotificationPreferences();
        partialEntity.setUserId(userId);
        partialEntity.setFriendRequestsEnabled(false);
        partialEntity.setActivityInvitesEnabled(false);
        partialEntity.setActivityUpdatesEnabled(true);
        partialEntity.setNewMessagesEnabled(true);
        partialEntity.setSoundEnabled(false);
        
        when(notificationPreferencesRepository.save(any(NotificationPreferences.class)))
                .thenReturn(partialEntity);

        NotificationPreferencesDTO result = notificationService.saveNotificationPreferences(partialDTO);

        assertNotNull(result);
        assertFalse(result.isFriendRequestsEnabled());
        assertFalse(result.isActivityInvitesEnabled());
        assertTrue(result.isActivityUpdatesEnabled());
        assertTrue(result.isNewMessagesEnabled());
        verify(notificationPreferencesRepository, times(1)).save(any(NotificationPreferences.class));
    }

    @Test
    void saveNotificationPreferences_ShouldThrowException_WhenDatabaseError() {
        when(notificationPreferencesRepository.save(any(NotificationPreferences.class)))
                .thenThrow(new DataAccessException("Database error") {});

        assertThrows(DataAccessException.class, 
            () -> notificationService.saveNotificationPreferences(preferencesDTO));

        verify(notificationPreferencesRepository, times(1)).save(any(NotificationPreferences.class));
    }

    // MARK: - Edge Case Tests

    @Test
    void registerDeviceToken_ShouldHandleMultipleTokensSameUser_WhenUserHasMultipleDevices() {
        String token1 = "token-1";
        String token2 = "token-2";
        DeviceTokenDTO dto1 = new DeviceTokenDTO(userId, token1);
        DeviceTokenDTO dto2 = new DeviceTokenDTO(userId, token2);
        
        DeviceToken entity1 = new DeviceToken();
        entity1.setUserId(userId);
        entity1.setToken(token1);
        
        DeviceToken entity2 = new DeviceToken();
        entity2.setUserId(userId);
        entity2.setToken(token2);
        
        when(deviceTokenRepository.save(any(DeviceToken.class)))
                .thenReturn(entity1)
                .thenReturn(entity2);

        assertDoesNotThrow(() -> notificationService.registerDeviceToken(dto1));
        assertDoesNotThrow(() -> notificationService.registerDeviceToken(dto2));

        verify(deviceTokenRepository, times(2)).save(any(DeviceToken.class));
    }

    @Test
    void unregisterDeviceToken_ShouldHandleNullToken_WhenTokenIsNull() {
        when(deviceTokenRepository.findByToken(null)).thenReturn(Optional.empty());

        assertDoesNotThrow(() -> notificationService.unregisterDeviceToken(null));

        verify(deviceTokenRepository, times(1)).findByToken(null);
        verify(deviceTokenRepository, never()).delete(any());
    }

    @Test
    void unregisterDeviceToken_ShouldHandleEmptyToken_WhenTokenIsEmpty() {
        when(deviceTokenRepository.findByToken("")).thenReturn(Optional.empty());

        assertDoesNotThrow(() -> notificationService.unregisterDeviceToken(""));

        verify(deviceTokenRepository, times(1)).findByToken("");
        verify(deviceTokenRepository, never()).delete(any());
    }

    @Test
    void getNotificationPreferences_ShouldHandleNullUserId_WhenUserIdIsNull() {
        when(notificationPreferencesRepository.findByUserId(null))
                .thenReturn(Optional.empty());

        NotificationPreferencesDTO result = notificationService.getNotificationPreferences(null);

        assertNotNull(result);
        assertNull(result.getUserId());
        verify(notificationPreferencesRepository, times(1)).findByUserId(null);
    }

    @Test
    void saveNotificationPreferences_ShouldHandleConcurrentUpdates_WhenMultipleUpdates() {
        when(notificationPreferencesRepository.save(any(NotificationPreferences.class)))
                .thenReturn(preferencesEntity);

        NotificationPreferencesDTO result1 = notificationService.saveNotificationPreferences(preferencesDTO);
        NotificationPreferencesDTO result2 = notificationService.saveNotificationPreferences(preferencesDTO);

        assertNotNull(result1);
        assertNotNull(result2);
        verify(notificationPreferencesRepository, times(2)).save(any(NotificationPreferences.class));
    }

    @Test
    void registerDeviceToken_ShouldHandleEmptyToken_WhenTokenIsEmpty() {
        DeviceTokenDTO emptyTokenDTO = new DeviceTokenDTO(userId, "");
        DeviceToken emptyTokenEntity = new DeviceToken();
        emptyTokenEntity.setUserId(userId);
        emptyTokenEntity.setToken("");
        
        when(deviceTokenRepository.save(any(DeviceToken.class))).thenReturn(emptyTokenEntity);

        assertDoesNotThrow(() -> notificationService.registerDeviceToken(emptyTokenDTO));

        verify(deviceTokenRepository, times(1)).save(any(DeviceToken.class));
    }

    @Test
    void saveNotificationPreferences_ShouldUpdateExisting_WhenPreferencesAlreadyExist() {
        NotificationPreferences existingPreferences = new NotificationPreferences();
        existingPreferences.setUserId(userId);
        existingPreferences.setFriendRequestsEnabled(false);
        
        when(notificationPreferencesRepository.findByUserId(userId))
                .thenReturn(Optional.of(existingPreferences));
        when(notificationPreferencesRepository.save(any(NotificationPreferences.class)))
                .thenReturn(preferencesEntity);

        NotificationPreferencesDTO result = notificationService.saveNotificationPreferences(preferencesDTO);

        assertNotNull(result);
        verify(notificationPreferencesRepository, times(1)).save(any(NotificationPreferences.class));
    }

    @Test
    void registerDeviceToken_ShouldOverwriteExisting_WhenSameTokenRegisteredAgain() {
        when(deviceTokenRepository.save(any(DeviceToken.class))).thenReturn(deviceTokenEntity);

        assertDoesNotThrow(() -> notificationService.registerDeviceToken(deviceTokenDTO));
        assertDoesNotThrow(() -> notificationService.registerDeviceToken(deviceTokenDTO));

        verify(deviceTokenRepository, times(2)).save(any(DeviceToken.class));
    }

    @Test
    void getNotificationPreferences_ShouldReturnConsistentDefaults_WhenCalledMultipleTimes() {
        when(notificationPreferencesRepository.findByUserId(userId))
                .thenReturn(Optional.empty());

        NotificationPreferencesDTO result1 = notificationService.getNotificationPreferences(userId);
        NotificationPreferencesDTO result2 = notificationService.getNotificationPreferences(userId);

        assertEquals(result1.isFriendRequestsEnabled(), result2.isFriendRequestsEnabled());
        assertEquals(result1.isActivityInvitesEnabled(), result2.isActivityInvitesEnabled());
        assertEquals(result1.isActivityUpdatesEnabled(), result2.isActivityUpdatesEnabled());
        verify(notificationPreferencesRepository, times(2)).findByUserId(userId);
    }
}

