package com.danielagapov.spawn.shared.events;

import com.danielagapov.spawn.user.api.dto.BaseUserDTO;
import com.danielagapov.spawn.user.api.dto.RecommendedFriendUserDTO;
import com.danielagapov.spawn.shared.util.SearchedUserResult;

import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Domain events for User Search module inter-module communication.
 * Used to break circular dependencies and allow other modules to query user search functionality.
 */
public final class UserSearchEvents {
    
    private UserSearchEvents() {
        // Utility class - prevent instantiation
    }
    
    // ========== Recommended Friends Query Events ==========
    
    /**
     * Query event to request limited recommended friends for a user.
     * Published by other modules, consumed by User module.
     */
    public record GetLimitedRecommendedFriendsQuery(
        UUID userId,
        UUID requestId  // Correlation ID for async response matching
    ) {}
    
    /**
     * Response event containing recommended friends list.
     * Published by User module in response to GetLimitedRecommendedFriendsQuery.
     */
    public record RecommendedFriendsResponse(
        UUID requestId,  // Correlation ID to match with query
        List<RecommendedFriendUserDTO> recommendedFriends
    ) {}
    
    // ========== Search Friends Query Events ==========
    
    /**
     * Query event to search for recommended friends by search query.
     * Published by other modules, consumed by User module.
     */
    public record GetRecommendedFriendsBySearchQuery(
        UUID requestingUserId,
        String searchQuery,
        UUID requestId  // Correlation ID for async response matching
    ) {}
    
    /**
     * Response event containing search results.
     * Published by User module in response to GetRecommendedFriendsBySearchQuery.
     */
    public record SearchedUserResultResponse(
        UUID requestId,  // Correlation ID to match with query
        SearchedUserResult searchedUserResult
    ) {}
    
    // ========== User Search Query Events ==========
    
    /**
     * Query event to search for users by query string.
     * Published by other modules, consumed by User module.
     */
    public record SearchByQueryEvent(
        String searchQuery,
        UUID requestingUserId,  // Can be null
        UUID requestId  // Correlation ID for async response matching
    ) {}
    
    /**
     * Response event containing search results.
     * Published by User module in response to SearchByQueryEvent.
     */
    public record SearchByQueryResponse(
        UUID requestId,  // Correlation ID to match with query
        List<BaseUserDTO> users
    ) {}
    
    // ========== Excluded User IDs Query Events ==========
    
    /**
     * Query event to get excluded user IDs for a user.
     * Published by other modules, consumed by User module.
     */
    public record GetExcludedUserIdsQuery(
        UUID userId,
        UUID requestId  // Correlation ID for async response matching
    ) {}
    
    /**
     * Response event containing excluded user IDs.
     * Published by User module in response to GetExcludedUserIdsQuery.
     */
    public record ExcludedUserIdsResponse(
        UUID requestId,  // Correlation ID to match with query
        Set<UUID> excludedUserIds
    ) {}
}

