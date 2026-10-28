package com.danielagapov.spawn.shared.config;

import com.danielagapov.spawn.shared.events.UserActivityTypeEvents.*;
import com.danielagapov.spawn.shared.exceptions.Logger.ILogger;
import com.danielagapov.spawn.shared.feign.ActivityServiceClient;
import com.danielagapov.spawn.user.internal.domain.User;
import com.danielagapov.spawn.user.internal.repositories.IUserRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Event listener to handle user-related events for activity type initialization.
 * When a user is created, initializes default activity types via activity-service.
 */
@Service
public class ActivityTypeEventListener {

    private final ActivityServiceClient activityServiceClient;
    private final IUserRepository userRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final ILogger logger;

    public ActivityTypeEventListener(
            ActivityServiceClient activityServiceClient,
            IUserRepository userRepository,
            ApplicationEventPublisher eventPublisher,
            ILogger logger) {
        this.activityServiceClient = activityServiceClient;
        this.userRepository = userRepository;
        this.eventPublisher = eventPublisher;
        this.logger = logger;
    }

    @EventListener
    @Transactional
    public void handleUserCreatedEvent(UserCreatedEvent event) {
        try {
            logger.info("Handling UserCreatedEvent for user: " + event.username() + " (ID: " + event.userId() + ")");

            User user = userRepository.findById(event.userId())
                .orElseThrow(() -> new IllegalStateException("User not found: " + event.userId()));

            activityServiceClient.initializeActivityTypesForUser(event.userId());

            eventPublisher.publishEvent(new DefaultActivityTypesInitializedEvent(event.userId(), 4));

            logger.info("Successfully initialized default activity types for user: " + event.username());
        } catch (Exception e) {
            logger.error("Failed to initialize default activity types for user " + event.userId() + ": " + e.getMessage());
        }
    }
}
