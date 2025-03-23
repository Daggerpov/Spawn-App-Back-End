package com.danielagapov.spawn.Services.PushNotification;

import com.danielagapov.spawn.DTOs.DeviceTokenDTO;
import com.danielagapov.spawn.DTOs.Notification.NotificationPreferencesDTO;
import com.danielagapov.spawn.Enums.DeviceType;
import com.danielagapov.spawn.Events.NotificationEvent;
import com.danielagapov.spawn.Events.PushRegistrationNotificationEvent;
import com.danielagapov.spawn.Models.DeviceToken;
import com.danielagapov.spawn.Models.NotificationPreferences;
import com.danielagapov.spawn.Models.User;
import com.danielagapov.spawn.Repositories.IDeviceTokenRepository;
import com.danielagapov.spawn.Repositories.INotificationPreferencesRepository;
import com.danielagapov.spawn.Services.User.IUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for handling notifications to users across different device types
 */
@Service
public class NotificationService {

    private final IDeviceTokenRepository deviceTokenRepository;
    private final INotificationPreferencesRepository preferencesRepository;
    private final IUserService userService;
    private final Map<DeviceType, NotificationStrategy> strategies;
    private final ApplicationEventPublisher eventPublisher;

    @Autowired
    public NotificationService(
            IDeviceTokenRepository deviceTokenRepository,
            INotificationPreferencesRepository preferencesRepository,
            IUserService userService,
            List<NotificationStrategy> strategyList,
            ApplicationEventPublisher eventPublisher) {
        this.deviceTokenRepository = deviceTokenRepository;
        this.preferencesRepository = preferencesRepository;
        this.userService = userService;
        this.eventPublisher = eventPublisher;
        
        // Create a map of device types to strategies
        this.strategies = strategyList.stream()
                .collect(Collectors.toMap(NotificationStrategy::getDeviceType, strategy -> strategy));
    }

    /**
     * Register a device token for a user
     */
    public void registerDeviceToken(DeviceTokenDTO deviceTokenDTO) {
        String token = deviceTokenDTO.getToken();
        User user = userService.getUserEntityById(deviceTokenDTO.getUserId());
        
        // Check if token already exists
        if (deviceTokenRepository.existsByToken(token)) {
            deviceTokenRepository.deleteByToken(token);
        }

        DeviceToken deviceToken = new DeviceToken();
        deviceToken.setUser(user);
        deviceToken.setToken(token);
        deviceToken.setDeviceType(deviceTokenDTO.getDeviceType());
        
        deviceTokenRepository.save(deviceToken);
        
        // Send a test notification to confirm registration
        eventPublisher.publishEvent(new PushRegistrationNotificationEvent(user));
    }

    /**
     * Unregister a device token
     */
    public void unregisterDeviceToken(String token) {
        if (deviceTokenRepository.existsByToken(token)) {
            deviceTokenRepository.deleteByToken(token);
        }
    }

    /**
     * Get notification preferences for a user
     */
    public NotificationPreferencesDTO getNotificationPreferences(UUID userId) {
        User user = userService.getUserEntityById(userId);
        NotificationPreferences preferences = preferencesRepository.findByUser(user).orElse(null);
        
        // Return null if no preferences exist
        if (preferences == null) {
            return null;
        }
        
        // Map entity to DTO
        return new NotificationPreferencesDTO(
            preferences.isFriendRequestsEnabled(),
            preferences.isEventInvitesEnabled(),
            preferences.isEventUpdatesEnabled(),
            preferences.isChatMessagesEnabled(),
            userId
        );
    }
    
    /**
     * Save notification preferences for a user
     */
    public NotificationPreferencesDTO saveNotificationPreferences(NotificationPreferencesDTO preferencesDTO) {
        User user = userService.getUserEntityById(preferencesDTO.getUserId());
        
        // Find existing preferences or create new
        NotificationPreferences preferences = preferencesRepository.findByUser(user)
            .orElse(new NotificationPreferences());
        
        // Update entity
        preferences.setUser(user);
        preferences.setFriendRequestsEnabled(preferencesDTO.isFriendRequestsEnabled());
        preferences.setEventInvitesEnabled(preferencesDTO.isEventInvitesEnabled());
        preferences.setEventUpdatesEnabled(preferencesDTO.isEventUpdatesEnabled());
        preferences.setChatMessagesEnabled(preferencesDTO.isChatMessagesEnabled());
        
        // Save
        preferences = preferencesRepository.save(preferences);
        
        // Return updated DTO
        return new NotificationPreferencesDTO(
            preferences.isFriendRequestsEnabled(),
            preferences.isEventInvitesEnabled(),
            preferences.isEventUpdatesEnabled(),
            preferences.isChatMessagesEnabled(),
            preferencesDTO.getUserId()
        );
    }

    /**
     * Send a notification to a user
     */
    public void sendNotificationToUser(UUID userId, String title, String message, Map<String, String> data) {
        List<DeviceToken> deviceTokens = deviceTokenRepository.findByUserId(userId);

        for (DeviceToken deviceToken : deviceTokens) {
            DeviceType deviceType = deviceToken.getDeviceType();
            NotificationStrategy strategy = strategies.get(deviceType);
            
            if (strategy != null) {
                strategy.sendNotificationToDevice(deviceToken.getToken(), title, message, data);
            }
        }
    }

    /**
     * Process notification events
     */
    @EventListener
    public void handleNotificationEvent(NotificationEvent event) {
        // Get the list of target users from the event
        List<UUID> targetUserIds = event.getTargetUserIds();
        
        // Skip if there are no target users
        if (targetUserIds.isEmpty()) {
            return;
        }
        
        // Send the notification to all target users
        for (UUID userId : targetUserIds) {
            sendNotificationToUser(
                    userId,
                    event.getTitle(),
                    event.getMessage(),
                    event.getData()
            );
        }
    }
} 