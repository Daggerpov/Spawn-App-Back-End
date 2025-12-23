package com.danielagapov.spawn.shared.events;

import java.util.UUID;

/**
 * Domain events for User module inter-module communication.
 * Used to break circular dependencies between Social and User modules.
 */
public final class UserEvents {
    
    private UserEvents() {
        // Utility class - prevent instantiation
    }
    
    // ========== User Entity Query Events ==========
    
    /**
     * Query event to request a user entity by ID.
     * Published by Social module, consumed by User module.
     */
    public record GetUserEntityQuery(
        UUID userId,
        UUID requestId  // Correlation ID for async response matching
    ) {}
    
    /**
     * Simplified user data for cross-module communication.
     * Avoids circular dependency by not using domain entities directly.
     */
    public record UserData(
        UUID id,
        String username,
        String profilePictureUrlString,
        String name,
        String bio,
        String email,
        String phoneNumber
    ) {}
    
    /**
     * Response event containing user data.
     * Published by User module in response to GetUserEntityQuery.
     */
    public record UserEntityResponse(
        UUID requestId,  // Correlation ID to match with query
        UserData userData,
        boolean found
    ) {}
    
    // ========== Friendship Check Events ==========
    
    /**
     * Query event to check if two users are friends.
     * Published by Social module, consumed by User module.
     */
    public record IsUserFriendQuery(
        UUID userAId,
        UUID userBId,
        UUID requestId  // Correlation ID for async response matching
    ) {}
    
    /**
     * Response event containing friendship status.
     * Published by User module in response to IsUserFriendQuery.
     */
    public record IsUserFriendResponse(
        UUID requestId,  // Correlation ID to match with query
        boolean areFriends
    ) {}
    
    // ========== Mutual Friend Count Events ==========
    
    /**
     * Query event to get mutual friend count between two users.
     * Published by Social module, consumed by User module.
     */
    public record GetMutualFriendCountQuery(
        UUID userAId,
        UUID userBId,
        UUID requestId  // Correlation ID for async response matching
    ) {}
    
    /**
     * Response event containing mutual friend count.
     * Published by User module in response to GetMutualFriendCountQuery.
     */
    public record MutualFriendCountResponse(
        UUID requestId,  // Correlation ID to match with query
        int count
    ) {}
    
    // ========== Save Friend Events ==========
    
    /**
     * Command event to create a friendship between two users.
     * Published by Social module, consumed by User module.
     */
    public record SaveFriendCommand(
        UUID userAId,
        UUID userBId,
        UUID requestId  // Correlation ID for async response matching
    ) {}
    
    /**
     * Response event confirming friendship was saved.
     * Published by User module in response to SaveFriendCommand.
     */
    public record SaveFriendResponse(
        UUID requestId,  // Correlation ID to match with query
        boolean success
    ) {}
}

