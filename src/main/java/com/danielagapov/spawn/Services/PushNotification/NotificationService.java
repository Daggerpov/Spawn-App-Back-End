package com.danielagapov.spawn.Services.PushNotification;

import com.danielagapov.spawn.DTOs.DeviceTokenDTO;
import com.danielagapov.spawn.Enums.DeviceType;
import com.danielagapov.spawn.Events.NotificationEvent;
import com.danielagapov.spawn.Models.DeviceToken;
import com.danielagapov.spawn.Models.User;
import com.danielagapov.spawn.Repositories.IDeviceTokenRepository;
import com.danielagapov.spawn.Services.User.IUserService;
import org.springframework.beans.factory.annotation.Autowired;
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
    private final IUserService userService;
    private final Map<DeviceType, NotificationStrategy> strategies;

    @Autowired
    public NotificationService(
            IDeviceTokenRepository deviceTokenRepository,
            IUserService userService,
            List<NotificationStrategy> strategyList) {
        this.deviceTokenRepository = deviceTokenRepository;
        this.userService = userService;
        
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
        Map<String, String> data = new HashMap<>();
        data.put("type", "registration");
        sendNotificationToUser(
                user.getId(),
                "Push Notifications Enabled",
                "You will now receive notifications from Spawn App",
                data
        );
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
     * Send notifications to multiple users
     */
    public void sendNotificationToUsers(List<UUID> userIds, String title, String message, Map<String, String> data) {
        for (UUID userId : userIds) {
            sendNotificationToUser(userId, title, message, data);
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