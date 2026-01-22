package com.danielagapov.spawn.user.internal.services;

import com.danielagapov.spawn.shared.events.UserEvents.*;
import com.danielagapov.spawn.shared.exceptions.Base.BaseNotFoundException;
import com.danielagapov.spawn.shared.exceptions.Logger.ILogger;
import com.danielagapov.spawn.user.internal.domain.User;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

/**
 * Event listener for User module.
 * Handles query events from other modules (primarily Social) and publishes responses.
 * This breaks the circular dependency between Social and User modules.
 */
@Service
public class UserEventListener {
    
    private final IUserService userService;
    private final ApplicationEventPublisher eventPublisher;
    private final ILogger logger;
    
    public UserEventListener(
            IUserService userService,
            ApplicationEventPublisher eventPublisher,
            ILogger logger) {
        this.userService = userService;
        this.eventPublisher = eventPublisher;
        this.logger = logger;
    }
    
    /**
     * Handle query for user entity by ID.
     * Responds to UserQueryService in the Social module.
     */
    @EventListener
    public void handleGetUserEntityQuery(GetUserEntityQuery query) {
        try {
            logger.info("Handling GetUserEntityQuery for user: " + query.userId());
            
            User user = userService.getUserEntityById(query.userId());
            UserData userData = new UserData(
                user.getId(),
                user.getUsername(),
                user.getProfilePictureUrlString(),
                user.getName(),
                user.getBio(),
                user.getEmail(),
                user.getPhoneNumber()
            );
            eventPublisher.publishEvent(new UserEntityResponse(query.requestId(), userData, true));
            
            logger.info("Published UserEntityResponse for user: " + query.userId());
        } catch (BaseNotFoundException e) {
            logger.warn("User not found for query: " + query.userId());
            eventPublisher.publishEvent(new UserEntityResponse(query.requestId(), null, false));
        } catch (Exception e) {
            logger.error("Error handling GetUserEntityQuery: " + e.getMessage());
            eventPublisher.publishEvent(new UserEntityResponse(query.requestId(), null, false));
        }
    }
    
    /**
     * Handle query to check if two users are friends.
     * Responds to UserQueryService in the Social module.
     */
    @EventListener
    public void handleIsUserFriendQuery(IsUserFriendQuery query) {
        try {
            logger.info("Handling IsUserFriendQuery for users: " + query.userAId() + " and " + query.userBId());
            
            boolean areFriends = userService.isUserFriendOfUser(query.userAId(), query.userBId());
            eventPublisher.publishEvent(new IsUserFriendResponse(query.requestId(), areFriends));
            
            logger.info("Published IsUserFriendResponse: " + areFriends);
        } catch (Exception e) {
            logger.error("Error handling IsUserFriendQuery: " + e.getMessage());
            eventPublisher.publishEvent(new IsUserFriendResponse(query.requestId(), false));
        }
    }
    
    /**
     * Handle query for mutual friend count.
     * Responds to UserQueryService in the Social module.
     */
    @EventListener
    public void handleGetMutualFriendCountQuery(GetMutualFriendCountQuery query) {
        try {
            logger.info("Handling GetMutualFriendCountQuery for users: " + query.userAId() + " and " + query.userBId());
            
            int count = userService.getMutualFriendCount(query.userAId(), query.userBId());
            eventPublisher.publishEvent(new MutualFriendCountResponse(query.requestId(), count));
            
            logger.info("Published MutualFriendCountResponse with count: " + count);
        } catch (Exception e) {
            logger.error("Error handling GetMutualFriendCountQuery: " + e.getMessage());
            eventPublisher.publishEvent(new MutualFriendCountResponse(query.requestId(), 0));
        }
    }
    
    /**
     * Handle command to save a friendship.
     * Responds to UserQueryService in the Social module.
     */
    @EventListener
    public void handleSaveFriendCommand(SaveFriendCommand command) {
        try {
            logger.info("Handling SaveFriendCommand for users: " + command.userAId() + " and " + command.userBId());
            
            userService.saveFriendToUser(command.userAId(), command.userBId());
            eventPublisher.publishEvent(new SaveFriendResponse(command.requestId(), true));
            
            logger.info("Published SaveFriendResponse: success");
        } catch (Exception e) {
            logger.error("Error saving friendship via event: " + e.getMessage());
            eventPublisher.publishEvent(new SaveFriendResponse(command.requestId(), false));
        }
    }
}

