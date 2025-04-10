package com.danielagapov.spawn.Services.PushNotification;

import com.danielagapov.spawn.DTOs.DeviceTokenDTO;
import com.danielagapov.spawn.DTOs.Notification.NotificationPreferencesDTO;
import com.danielagapov.spawn.Enums.DeviceType;
import com.danielagapov.spawn.Enums.NotificationType;
import com.danielagapov.spawn.Events.NotificationEvent;
import com.danielagapov.spawn.Events.PushRegistrationNotificationEvent;
import com.danielagapov.spawn.Exceptions.Logger.ILogger;
import com.danielagapov.spawn.Models.DeviceToken;
import com.danielagapov.spawn.Models.NotificationPreferences;
import com.danielagapov.spawn.Models.User;
import com.danielagapov.spawn.Repositories.IDeviceTokenRepository;
import com.danielagapov.spawn.Repositories.INotificationPreferencesRepository;
import com.danielagapov.spawn.Services.User.IUserService;
import com.danielagapov.spawn.Utils.LoggingUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    private final ILogger logger;

    @Autowired
    public NotificationService(
            IDeviceTokenRepository deviceTokenRepository,
            INotificationPreferencesRepository preferencesRepository,
            IUserService userService,
            List<NotificationStrategy> strategyList,
            ApplicationEventPublisher eventPublisher,
            ILogger logger) {
        this.deviceTokenRepository = deviceTokenRepository;
        this.preferencesRepository = preferencesRepository;
        this.userService = userService;
        this.eventPublisher = eventPublisher;
        this.logger = logger;
        
        // Create a map of device types to strategies
        this.strategies = strategyList.stream()
                .collect(Collectors.toMap(NotificationStrategy::getDeviceType, strategy -> strategy));
        
        logger.info("NotificationService initialized with " + strategies.size() + " strategies");
    }

    /**
     * Register a device token for a user
     */
    @Transactional
    public void registerDeviceToken(DeviceTokenDTO deviceTokenDTO) throws Exception {
        try {
            String token = deviceTokenDTO.getToken();
            User user = userService.getUserEntityById(deviceTokenDTO.getUserId());

            logger.info(String.format(
                    "Registering device token for user: %s with names: %s %s and username: %s, device type: %s",
                    user.getId(), user.getFirstName(), user.getLastName(), user.getUsername(),
                    deviceTokenDTO.getDeviceType()));

            // Use a more reliable approach to handle existing tokens
            List<DeviceToken> existingTokens = deviceTokenRepository.findByToken(token);
            if (!existingTokens.isEmpty()) {
                logger.info("Token already exists, updating existing record instead of creating new");
                DeviceToken existingToken = existingTokens.get(0);
                existingToken.setUser(user);
                existingToken.setDeviceType(deviceTokenDTO.getDeviceType());
                deviceTokenRepository.save(existingToken);
            } else {
                // Create new token if it doesn't exist
                DeviceToken deviceToken = new DeviceToken();
                deviceToken.setUser(user);
                deviceToken.setToken(token);
                deviceToken.setDeviceType(deviceTokenDTO.getDeviceType());
                deviceTokenRepository.save(deviceToken);
            }
            
            logger.info("Device token saved successfully for user: " + user.getId() + " with names: "
                    + user.getFirstName() + " " + user.getLastName() + " and username: " + user.getUsername());

            // Send a test notification to confirm registration
            eventPublisher.publishEvent(new PushRegistrationNotificationEvent(user));
            logger.info("Sent test notification for token registration confirmation");
        } catch (Exception e) {
            logger.error("Error registering device token: " + e.getMessage());
            throw e;
        }
    }
    
    /**
     * Unregister a device token
     */
    @Transactional
    public void unregisterDeviceToken(String token) throws Exception {
        try {
            logger.info("Unregistering device token: " + token);
            if (deviceTokenRepository.existsByToken(token)) {
                deviceTokenRepository.deleteByToken(token);
                logger.info("Device token unregistered successfully");
            } else {
                logger.warn("Attempted to unregister non-existent token: " + token);
            }
        } catch (Exception e) {
            logger.error("Error unregistering device token: " + e.getMessage());
            throw e;
        }
    }

    /**
     * Get notification preferences for a user
     * 
     * @throws Exception
     */
    @Transactional(readOnly = true)
    public NotificationPreferencesDTO getNotificationPreferences(UUID userId) throws Exception {
        try {
            User user = userService.getUserEntityById(userId);
            logger.info("Getting notification preferences for user: " + LoggingUtils.formatUserInfo(user));
            
            NotificationPreferences preferences = preferencesRepository.findByUser(user).orElse(null);
            
            // throw e if no preferences exist
            if (preferences == null) {
                logger.info("No notification preferences found for user: " + LoggingUtils.formatUserInfo(user));
                throw new Exception("No notification preferences found for user: " + LoggingUtils.formatUserInfo(user));
            }
            
            // Map entity to DTO
            NotificationPreferencesDTO preferencesDTO = mapPreferencesToDTO(preferences, userId);
            
            logger.info("Retrieved notification preferences for user: " + LoggingUtils.formatUserInfo(user));
            return preferencesDTO;
        } catch (Exception e) {
            logger.error("Error getting notification preferences for user " + LoggingUtils.formatUserIdInfo(userId) + ": " + e.getMessage());
            throw e;
        }
    }
    
    /**
     * Map notification preferences entity to DTO
     */
    private NotificationPreferencesDTO mapPreferencesToDTO(NotificationPreferences preferences, UUID userId) {
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
    @Transactional
    public NotificationPreferencesDTO saveNotificationPreferences(NotificationPreferencesDTO preferencesDTO) throws Exception {
        try {
            User user = userService.getUserEntityById(preferencesDTO.getUserId());
            logger.info("Saving notification preferences for user: " + LoggingUtils.formatUserInfo(user));
            
            NotificationPreferences savedPreferences = savePreferencesInternal(user, preferencesDTO);
            
            logger.info("Notification preferences saved successfully for user: " + LoggingUtils.formatUserInfo(user));
            
            // Return updated DTO
            return mapPreferencesToDTO(savedPreferences, preferencesDTO.getUserId());
        } catch (Exception e) {
            logger.error("Error saving notification preferences for user " + LoggingUtils.formatUserIdInfo(preferencesDTO.getUserId()) + ": "
                    + e.getMessage());
            throw e;
        }
    }
    
    /**
     * Internal method to save notification preferences
     */
    private NotificationPreferences savePreferencesInternal(User user, NotificationPreferencesDTO preferencesDTO) {
        // Find existing preferences or create new
        NotificationPreferences preferences = preferencesRepository.findByUser(user)
            .orElse(new NotificationPreferences());
        
        // Update entity
        preferences.setUser(user);
        preferences.setFriendRequestsEnabled(preferencesDTO.isFriendRequestsEnabled());
        preferences.setEventInvitesEnabled(preferencesDTO.isEventInvitesEnabled());
        preferences.setEventUpdatesEnabled(preferencesDTO.isEventUpdatesEnabled());
        preferences.setChatMessagesEnabled(preferencesDTO.isChatMessagesEnabled());
        
        // Save and return
        return preferencesRepository.save(preferences);
    }

    /**
     * Send a notification to a user
     */
    public void sendNotificationToUser(UUID userId, String title, String message, Map<String, String> data) throws Exception {
        try {
            User user = userService.getUserEntityById(userId);
            logger.info(String.format(
                    "Sending notification to user %s: Title: '%s', Message: '%s'",
                    LoggingUtils.formatUserInfo(user), title, message));
            
            List<DeviceToken> deviceTokens = deviceTokenRepository.findByUserId(userId);

            if (deviceTokens.isEmpty()) {
                logger.warn("No device tokens found for user: " + LoggingUtils.formatUserInfo(user));
                return;
            }
            
            logger.info("Found " + deviceTokens.size() + " device(s) for user: " + LoggingUtils.formatUserInfo(user));
            
            sendNotificationsToDevices(deviceTokens, title, message, data);
        } catch (Exception e) {
            logger.error("Error sending notification to user " + LoggingUtils.formatUserIdInfo(userId) + ": " + e.getMessage());
            throw e;
        }
    }
    
    /**
     * Send notifications to all devices
     */
    private void sendNotificationsToDevices(List<DeviceToken> deviceTokens, String title, String message, Map<String, String> data) {
        for (DeviceToken deviceToken : deviceTokens) {
            sendNotificationToDevice(deviceToken, title, message, data);
        }
    }
    
    /**
     * Send notification to a single device
     */
    private void sendNotificationToDevice(DeviceToken deviceToken, String title, String message, Map<String, String> data) {
        try {
            DeviceType deviceType = deviceToken.getDeviceType();
            NotificationStrategy strategy = strategies.get(deviceType);
            
            if (strategy != null) {
                logger.info("Using " + deviceType + " strategy to send notification");
                strategy.sendNotificationToDevice(deviceToken.getToken(), title, message, data);
            } else {
                logger.warn("No strategy found for device type: " + deviceType);
            }
        } catch (Exception e) {
            logger.error("Error sending notification to device: " + e.getMessage());
            // Continue with other devices
        }
    }

    /**
     * Process notification events
     */
    @EventListener
    public void handleNotificationEvent(NotificationEvent event) throws Exception {
        try {
            List<UUID> targetUserIds = event.getTargetUserIds();
            
            logger.info("Handling notification event: " + event.getClass().getSimpleName() + 
                    " for " + targetUserIds.size() + " users");
            
            if (targetUserIds.isEmpty()) {
                logger.warn("Event has no target users, skipping");
                return;
            }
            
            processNotificationsForUsers(targetUserIds, event);
            
            logger.info("Notification event processing completed");
        } catch (Exception e) {
            logger.error("Error handling notification event: " + e.getMessage());
            throw e;
        }
    }
    
    /**
     * Process notifications for multiple users
     */
    private void processNotificationsForUsers(List<UUID> userIds, NotificationEvent event) throws Exception {
        for (UUID userId : userIds) {
            processNotificationForUser(userId, event);
        }
    }
    
    /**
     * Process notification for a single user
     */
    private void processNotificationForUser(UUID userId, NotificationEvent event) throws Exception {
        try {
            User user = userService.getUserEntityById(userId);
            NotificationPreferences preferences = preferencesRepository.findByUser(user).orElse(null);
            
            boolean shouldSendNotification = shouldSendNotificationBasedOnPreferences(preferences, event.getType());
            
            if (shouldSendNotification) {
                sendNotificationToUser(
                        userId,
                        event.getTitle(),
                        event.getMessage(),
                        event.getData());
            } else {
                logger.info("Notification skipped for user " + LoggingUtils.formatUserInfo(user) +
                        " due to preferences setting for type " + event.getType());
            }
        } catch (Exception e) {
            logger.error("Error processing notification for user " + LoggingUtils.formatUserIdInfo(userId) + ": " + e.getMessage());
            throw e;
        }
    }
    
    /**
     * Determine if notification should be sent based on user preferences
     */
    private boolean shouldSendNotificationBasedOnPreferences(NotificationPreferences preferences, 
                                                           NotificationType type) {
        // If no preferences exist, use default settings (send notification)
        if (preferences == null) {
            return true;
        }
        
        // Apply notification preferences
        switch (type) {
            case FRIEND_REQUEST:
            case FRIEND_REQUEST_ACCEPTED:
                return preferences.isFriendRequestsEnabled();
            case EVENT_INVITE:
                return preferences.isEventInvitesEnabled();
            case EVENT_UPDATE:
            case EVENT_PARTICIPATION:
            case EVENT_PARTICIPATION_REVOKED:
                return preferences.isEventUpdatesEnabled();
            case NEW_COMMENT:
                return preferences.isChatMessagesEnabled();
            default:
                // For other notification types, default to sending
                return true;
        }
    }
}