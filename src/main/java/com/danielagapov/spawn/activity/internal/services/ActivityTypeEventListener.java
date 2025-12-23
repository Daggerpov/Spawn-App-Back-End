package com.danielagapov.spawn.activity.internal.services;

import com.danielagapov.spawn.shared.events.UserActivityTypeEvents.*;
import com.danielagapov.spawn.shared.exceptions.Logger.ILogger;
import com.danielagapov.spawn.user.internal.domain.User;
import com.danielagapov.spawn.user.internal.repositories.IUserRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Event listener for Activity module to handle user-related events.
 * Specifically handles initializing default activity types for new users.
 * This breaks the circular dependency between User and Activity modules.
 */
@Service
public class ActivityTypeEventListener {
    
    private final IActivityTypeService activityTypeService;
    private final IUserRepository userRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final ILogger logger;
    
    public ActivityTypeEventListener(
            IActivityTypeService activityTypeService,
            IUserRepository userRepository,
            ApplicationEventPublisher eventPublisher,
            ILogger logger) {
        this.activityTypeService = activityTypeService;
        this.userRepository = userRepository;
        this.eventPublisher = eventPublisher;
        this.logger = logger;
    }
    
    /**
     * Handles the UserCreatedEvent to initialize default activity types for a new user.
     * This replaces the direct call from UserService to ActivityTypeService.
     */
    @EventListener
    @Transactional
    public void handleUserCreatedEvent(UserCreatedEvent event) {
        try {
            logger.info("Handling UserCreatedEvent for user: " + event.username() + " (ID: " + event.userId() + ")");
            
            // Fetch the user entity
            User user = userRepository.findById(event.userId())
                .orElseThrow(() -> new IllegalStateException("User not found: " + event.userId()));
            
            // Initialize default activity types
            activityTypeService.initializeDefaultActivityTypesForUser(user);
            
            // Publish success event
            eventPublisher.publishEvent(new DefaultActivityTypesInitializedEvent(event.userId(), 4));
            
            logger.info("Successfully initialized default activity types for user: " + event.username());
        } catch (Exception e) {
            logger.error("Failed to initialize default activity types for user " + event.userId() + ": " + e.getMessage());
            // Don't re-throw - we don't want to fail user creation if activity types fail to initialize
        }
    }
}

