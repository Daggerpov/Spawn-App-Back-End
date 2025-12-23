package com.danielagapov.spawn.user.internal.services;

import com.danielagapov.spawn.user.api.dto.BaseUserDTO;
import com.danielagapov.spawn.user.api.dto.RecommendedFriendUserDTO;
import com.danielagapov.spawn.shared.util.SearchedUserResult;

import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Service interface for querying user search-related data.
 * This service breaks the circular dependency between UserService and UserSearchService
 * by extracting common search query operations.
 * 
 * Following CQRS principles, this service handles only READ operations for user search.
 */
public interface IUserSearchQueryService {
    
    /**
     * Returns the top recommended friends for a user with the most mutual friends.
     * This method is cached at the UserService level.
     *
     * @param userId the unique identifier of the user requesting recommendations
     * @return List of RecommendedFriendUserDTO objects with mutual friend counts
     */
    List<RecommendedFriendUserDTO> getLimitedRecommendedFriendsForUserId(UUID userId);
    
    /**
     * Searches for recommended friends and existing friends based on a search query.
     *
     * @param requestingUserId the unique identifier of the user performing the search
     * @param searchQuery the search string to filter users by name or username
     * @return SearchedUserResult containing all matching users with their relationship types
     */
    SearchedUserResult getRecommendedFriendsBySearch(UUID requestingUserId, String searchQuery);
    
    /**
     * Searches for users by a query string (name or username).
     *
     * @param searchQuery the search string to filter users by name or username
     * @param requestingUserId the unique identifier of the user performing the search (can be null)
     * @return List of BaseUserDTO objects matching the search query
     */
    List<BaseUserDTO> searchByQuery(String searchQuery, UUID requestingUserId);
    
    /**
     * Gets the set of user IDs that should be excluded from recommendations for a user.
     * This includes existing friends, pending friend requests, blocked users, and the user themselves.
     *
     * @param userId the unique identifier of the user
     * @return Set of UUID objects representing users to exclude from recommendations
     */
    Set<UUID> getExcludedUserIds(UUID userId);
}

