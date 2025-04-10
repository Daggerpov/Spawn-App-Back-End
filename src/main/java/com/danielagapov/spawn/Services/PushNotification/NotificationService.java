package com.danielagapov.spawn.Services.PushNotification;

import com.danielagapov.spawn.DTOs.DeviceTokenDTO;
import com.danielagapov.spawn.DTOs.Notification.NotificationPreferencesDTO;
import com.danielagapov.spawn.Enums.DeviceType;
import com.danielagapov.spawn.Events.NotificationEvent;
import com.danielagapov.spawn.Events.PushRegistrationNotificationEvent;
import com.danielagapov.spawn.Exceptions.Logger.ILogger;
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
import org.springframework.transaction.annotation.Transactional;

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
    public void registerDeviceToken(DeviceTokenDTO deviceTokenDTO) {
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
    public void unregisterDeviceToken(String token) {
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
            logger.info("Getting notification preferences for user: " + userId + " with names: " + user.getFirstName()
                    + " " + user.getLastName() + " and username: " + user.getUsername());

            NotificationPreferences preferences = preferencesRepository.findByUser(user).orElse(null);

            // throw e if no preferences exist
            if (preferences == null) {
                logger.info("No notification preferences found for user: " + userId + " with names: "
                        + user.getFirstName() + " " + user.getLastName() + " and username: " + user.getUsername());
                throw new Exception("No notification preferences found for user: " + userId + " with names: "
                        + user.getFirstName() + " " + user.getLastName() + " and username: " + user.getUsername());
            }

            // Map entity to DTO
            NotificationPreferencesDTO preferencesDTO = new NotificationPreferencesDTO(
                    preferences.isFriendRequestsEnabled(),
                    preferences.isEventInvitesEnabled(),
                    preferences.isEventUpdatesEnabled(),
                    preferences.isChatMessagesEnabled(),
                    userId);

            logger.info("Retrieved notification preferences for user: " + userId + " with names: " + user.getFirstName()
                    + " " + user.getLastName() + " and username: " + user.getUsername());
            return preferencesDTO;
        } catch (Exception e) {
            // Return default preferences if not found
            NotificationPreferencesDTO preferences = new NotificationPreferencesDTO(true, true, true, true, userId);
            saveNotificationPreferences(preferences);
            return preferences;
        }
    }

    /**
     * Save notification preferences for a user
     */
    @Transactional
    public NotificationPreferencesDTO saveNotificationPreferences(NotificationPreferencesDTO preferencesDTO) {
        try {
            User user = userService.getUserEntityById(preferencesDTO.getUserId());
            logger.info("Saving notification preferences for user: " + preferencesDTO.getUserId() + " with names: "
                    + user.getFirstName() + " " + user.getLastName() + " and username: " + user.getUsername());

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
            logger.info("Notification preferences saved successfully for user: " + preferencesDTO.getUserId()
                    + " with names: " + user.getFirstName() + " " + user.getLastName() + " and username: "
                    + user.getUsername());

            // Return updated DTO
            return new NotificationPreferencesDTO(
                    preferences.isFriendRequestsEnabled(),
                    preferences.isEventInvitesEnabled(),
                    preferences.isEventUpdatesEnabled(),
                    preferences.isChatMessagesEnabled(),
                    preferencesDTO.getUserId());
        } catch (Exception e) {
            logger.error("Error saving notification preferences for user " + preferencesDTO.getUserId() + ": "
                    + e.getMessage());
            throw e;
        }
    }

    /**
     * Send a notification to a user
     */
    public void sendNotificationToUser(UUID userId, String title, String message, Map<String, String> data) {
        try {
            User user = userService.getUserEntityById(userId);
            logger.info(String.format(
                    "Sending notification to user %s with names: %s %s and username: %s: Title: '%s', Message: '%s'",
                    userId, user.getFirstName(), user.getLastName(), user.getUsername(), title, message));

            List<DeviceToken> deviceTokens = deviceTokenRepository.findByUserId(userId);

            if (deviceTokens.isEmpty()) {
                logger.warn("No device tokens found for user: " + userId + " with names: " + user.getFirstName() + " "
                        + user.getLastName() + " and username: " + user.getUsername());
                return;
            }

            logger.info("Found " + deviceTokens.size() + " device(s) for user: " + userId + " with names: "
                    + user.getFirstName() + " " + user.getLastName() + " and username: " + user.getUsername());

            for (DeviceToken deviceToken : deviceTokens) {
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
                    // Continue trying other devices
                    continue;
                }
            }
        } catch (Exception e) {
            logger.error("Error sending notification to user " + userId + ": " + e.getMessage());
            throw e;
        }
    }

    /**
     * Process notification events
     */
    @EventListener
    public void handleNotificationEvent(NotificationEvent event) {
        try {
            // Get the list of target users from the event
            List<UUID> targetUserIds = event.getTargetUserIds();

            logger.info("Handling notification event: " + event.getClass().getSimpleName() +
                    " for " + targetUserIds.size() + " users");

            // Skip if there are no target users
            if (targetUserIds.isEmpty()) {
                logger.warn("Event has no target users, skipping");
                return;
            }

            // Send the notification to all target users
            for (UUID userId : targetUserIds) {
                try {
                    // Check user's notification preferences
                    User user = userService.getUserEntityById(userId);
                    NotificationPreferences preferences = preferencesRepository.findByUser(user).orElse(null);

                    // If no preferences exist, use default settings (send notification)
                    boolean shouldSendNotification = true;

                    // Apply notification preferences if they exist
                    if (preferences != null) {
                        switch (event.getType()) {
                            case FRIEND_REQUEST:
                            case FRIEND_REQUEST_ACCEPTED:
                                shouldSendNotification = preferences.isFriendRequestsEnabled();
                                break;
                            case EVENT_INVITE:
                                shouldSendNotification = preferences.isEventInvitesEnabled();
                                break;
                            case EVENT_UPDATE:
                            case EVENT_PARTICIPATION:
                            case EVENT_PARTICIPATION_REVOKED:
                                shouldSendNotification = preferences.isEventUpdatesEnabled();
                                break;
                            case NEW_COMMENT:
                                shouldSendNotification = preferences.isChatMessagesEnabled();
                                break;
                            default:
                                // For other notification types, default to sending
                                break;
                        }
                    }

                    logger.info("Sending notification to user " + userId + " with names: " + user.getFirstName() + " " + user.getLastName() + " and username: ");

                    // Only send notification if preferences allow it
                    if (shouldSendNotification) {
                        sendNotificationToUser(
                                userId,
                                event.getTitle(),
                                event.getMessage(),
                                event.getData());
                    } else {
                        logger.info("Notification skipped for user " + userId + " with names: " + user.getFirstName()
                                + " " + user.getLastName() + " and username: " + user.getUsername() +
                                " due to preferences setting for type " + event.getType());
                    }
                } catch (Exception e) {
                    logger.error("Error processing notification for user " + userId + ": " + e.getMessage());
                    throw e;
                }
            }

            logger.info("Notification event processing completed");
        } catch (Exception e) {
            logger.error("Error handling notification event: " + e.getMessage());
            throw e;
        }
    }
}