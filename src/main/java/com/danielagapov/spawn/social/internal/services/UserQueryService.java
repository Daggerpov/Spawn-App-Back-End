package com.danielagapov.spawn.social.internal.services;

import com.danielagapov.spawn.shared.events.UserEvents.*;
import com.danielagapov.spawn.shared.exceptions.Base.BaseNotFoundException;
import com.danielagapov.spawn.shared.exceptions.Logger.ILogger;
import com.danielagapov.spawn.shared.util.EntityType;
import com.danielagapov.spawn.user.internal.domain.User;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Service for querying user data from the User module via events.
 * This replaces the direct dependency on IUserService in Social module services,
 * breaking the circular dependency between Social and User modules.
 */
@Service
public class UserQueryService implements IUserQueryService {
    
    private static final long QUERY_TIMEOUT_MS = 5000; // 5 second timeout
    
    private final ApplicationEventPublisher eventPublisher;
    private final ILogger logger;
    
    // Pending query futures for async response matching
    private final ConcurrentHashMap<UUID, CompletableFuture<UserData>> pendingUserQueries = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, CompletableFuture<Boolean>> pendingFriendCheckQueries = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, CompletableFuture<Integer>> pendingMutualCountQueries = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, CompletableFuture<Boolean>> pendingSaveFriendQueries = new ConcurrentHashMap<>();
    
    public UserQueryService(
            ApplicationEventPublisher eventPublisher,
            ILogger logger) {
        this.eventPublisher = eventPublisher;
        this.logger = logger;
    }
    
    /**
     * Get a user entity by ID via event query.
     * Returns a User domain object reconstructed from UserData.
     */
    public User getUserEntityById(UUID userId) {
        UUID requestId = UUID.randomUUID();
        CompletableFuture<UserData> future = new CompletableFuture<>();
        
        pendingUserQueries.put(requestId, future);
        
        try {
            // Publish query event
            eventPublisher.publishEvent(new GetUserEntityQuery(userId, requestId));
            
            // Wait for response with timeout
            UserData userData = future.get(QUERY_TIMEOUT_MS, TimeUnit.MILLISECONDS);
            
            if (userData == null) {
                throw new BaseNotFoundException(EntityType.User, userId);
            }
            
            // Convert UserData back to User entity
            return convertToUser(userData);
        } catch (TimeoutException e) {
            logger.error("Timeout waiting for user entity for userId " + userId);
            throw new BaseNotFoundException(EntityType.User, userId);
        } catch (BaseNotFoundException e) {
            throw e;
        } catch (Exception e) {
            logger.error("Error getting user entity for userId " + userId + ": " + e.getMessage());
            throw new BaseNotFoundException(EntityType.User, userId);
        } finally {
            pendingUserQueries.remove(requestId);
        }
    }
    
    /**
     * Check if two users are friends via event query.
     */
    public boolean isUserFriendOfUser(UUID userAId, UUID userBId) {
        UUID requestId = UUID.randomUUID();
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        
        pendingFriendCheckQueries.put(requestId, future);
        
        try {
            // Publish query event
            eventPublisher.publishEvent(new IsUserFriendQuery(userAId, userBId, requestId));
            
            // Wait for response with timeout
            return future.get(QUERY_TIMEOUT_MS, TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            logger.warn("Timeout checking friendship status between " + userAId + " and " + userBId);
            return false;
        } catch (Exception e) {
            logger.error("Error checking friendship status: " + e.getMessage());
            return false;
        } finally {
            pendingFriendCheckQueries.remove(requestId);
        }
    }
    
    /**
     * Get mutual friend count between two users via event query.
     */
    public int getMutualFriendCount(UUID userAId, UUID userBId) {
        UUID requestId = UUID.randomUUID();
        CompletableFuture<Integer> future = new CompletableFuture<>();
        
        pendingMutualCountQueries.put(requestId, future);
        
        try {
            // Publish query event
            eventPublisher.publishEvent(new GetMutualFriendCountQuery(userAId, userBId, requestId));
            
            // Wait for response with timeout
            return future.get(QUERY_TIMEOUT_MS, TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            logger.warn("Timeout getting mutual friend count between " + userAId + " and " + userBId);
            return 0;
        } catch (Exception e) {
            logger.error("Error getting mutual friend count: " + e.getMessage());
            return 0;
        } finally {
            pendingMutualCountQueries.remove(requestId);
        }
    }
    
    /**
     * Save a friendship between two users via event command.
     */
    public boolean saveFriendToUser(UUID userAId, UUID userBId) {
        UUID requestId = UUID.randomUUID();
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        
        pendingSaveFriendQueries.put(requestId, future);
        
        try {
            // Publish command event
            eventPublisher.publishEvent(new SaveFriendCommand(userAId, userBId, requestId));
            
            // Wait for response with timeout
            return future.get(QUERY_TIMEOUT_MS, TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            logger.error("Timeout saving friendship between " + userAId + " and " + userBId);
            return false;
        } catch (Exception e) {
            logger.error("Error saving friendship: " + e.getMessage());
            return false;
        } finally {
            pendingSaveFriendQueries.remove(requestId);
        }
    }
    
    /**
     * Converts UserData to User entity.
     */
    private User convertToUser(UserData data) {
        User user = new User();
        user.setId(data.id());
        user.setUsername(data.username());
        user.setProfilePictureUrlString(data.profilePictureUrlString());
        user.setName(data.name());
        user.setBio(data.bio());
        user.setEmail(data.email());
        user.setPhoneNumber(data.phoneNumber());
        return user;
    }
    
    // ========== Event Listeners for Response Handling ==========
    
    /**
     * Handle response for user entity query.
     */
    @EventListener
    public void handleUserEntityResponse(UserEntityResponse response) {
        CompletableFuture<UserData> future = pendingUserQueries.get(response.requestId());
        if (future != null) {
            if (response.found()) {
                future.complete(response.userData());
            } else {
                future.complete(null);
            }
        }
    }
    
    /**
     * Handle response for friendship check query.
     */
    @EventListener
    public void handleIsUserFriendResponse(IsUserFriendResponse response) {
        CompletableFuture<Boolean> future = pendingFriendCheckQueries.get(response.requestId());
        if (future != null) {
            future.complete(response.areFriends());
        }
    }
    
    /**
     * Handle response for mutual friend count query.
     */
    @EventListener
    public void handleMutualFriendCountResponse(MutualFriendCountResponse response) {
        CompletableFuture<Integer> future = pendingMutualCountQueries.get(response.requestId());
        if (future != null) {
            future.complete(response.count());
        }
    }
    
    /**
     * Handle response for save friend command.
     */
    @EventListener
    public void handleSaveFriendResponse(SaveFriendResponse response) {
        CompletableFuture<Boolean> future = pendingSaveFriendQueries.get(response.requestId());
        if (future != null) {
            future.complete(response.success());
        }
    }
}

