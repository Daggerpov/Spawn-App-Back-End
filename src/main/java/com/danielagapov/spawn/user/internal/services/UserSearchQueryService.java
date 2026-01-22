package com.danielagapov.spawn.user.internal.services;

import com.danielagapov.spawn.shared.events.UserSearchEvents.*;
import com.danielagapov.spawn.shared.exceptions.Logger.ILogger;
import com.danielagapov.spawn.user.api.dto.BaseUserDTO;
import com.danielagapov.spawn.user.api.dto.FriendUser.RecommendedFriendUserDTO;
import com.danielagapov.spawn.shared.util.SearchedUserResult;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Service for querying user search data from the User module via events.
 * This breaks circular dependencies by allowing other modules to query user search functionality
 * without direct dependencies.
 * 
 * Following CQRS principles, this service handles only READ operations for user search.
 */
@Service
public class UserSearchQueryService implements IUserSearchQueryService {
    
    private static final long QUERY_TIMEOUT_MS = 5000; // 5 second timeout
    
    private final ApplicationEventPublisher eventPublisher;
    private final ILogger logger;
    
    // Pending query futures for async response matching
    private final ConcurrentHashMap<UUID, CompletableFuture<List<RecommendedFriendUserDTO>>> pendingRecommendedFriendsQueries = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, CompletableFuture<SearchedUserResult>> pendingSearchedUserResultQueries = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, CompletableFuture<List<BaseUserDTO>>> pendingSearchByQueryQueries = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, CompletableFuture<Set<UUID>>> pendingExcludedUserIdsQueries = new ConcurrentHashMap<>();
    
    public UserSearchQueryService(
            ApplicationEventPublisher eventPublisher,
            ILogger logger) {
        this.eventPublisher = eventPublisher;
        this.logger = logger;
    }
    
    /**
     * Get limited recommended friends for a user via event query.
     */
    @Override
    public List<RecommendedFriendUserDTO> getLimitedRecommendedFriendsForUserId(UUID userId) {
        UUID requestId = UUID.randomUUID();
        CompletableFuture<List<RecommendedFriendUserDTO>> future = new CompletableFuture<>();
        
        pendingRecommendedFriendsQueries.put(requestId, future);
        
        try {
            // Publish query event
            eventPublisher.publishEvent(new GetLimitedRecommendedFriendsQuery(userId, requestId));
            
            // Wait for response with timeout
            return future.get(QUERY_TIMEOUT_MS, TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            logger.error("Timeout waiting for recommended friends for userId " + userId);
            return List.of();
        } catch (Exception e) {
            logger.error("Error getting recommended friends for userId " + userId + ": " + e.getMessage());
            return List.of();
        } finally {
            pendingRecommendedFriendsQueries.remove(requestId);
        }
    }
    
    /**
     * Get recommended friends by search query via event query.
     */
    @Override
    public SearchedUserResult getRecommendedFriendsBySearch(UUID requestingUserId, String searchQuery) {
        UUID requestId = UUID.randomUUID();
        CompletableFuture<SearchedUserResult> future = new CompletableFuture<>();
        
        pendingSearchedUserResultQueries.put(requestId, future);
        
        try {
            // Publish query event
            eventPublisher.publishEvent(new GetRecommendedFriendsBySearchQuery(requestingUserId, searchQuery, requestId));
            
            // Wait for response with timeout
            return future.get(QUERY_TIMEOUT_MS, TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            logger.error("Timeout waiting for recommended friends by search for userId " + requestingUserId);
            return new SearchedUserResult(List.of());
        } catch (Exception e) {
            logger.error("Error getting recommended friends by search for userId " + requestingUserId + ": " + e.getMessage());
            return new SearchedUserResult(List.of());
        } finally {
            pendingSearchedUserResultQueries.remove(requestId);
        }
    }
    
    /**
     * Search users by query string via event query.
     */
    @Override
    public List<BaseUserDTO> searchByQuery(String searchQuery, UUID requestingUserId) {
        UUID requestId = UUID.randomUUID();
        CompletableFuture<List<BaseUserDTO>> future = new CompletableFuture<>();
        
        pendingSearchByQueryQueries.put(requestId, future);
        
        try {
            // Publish query event
            eventPublisher.publishEvent(new SearchByQueryEvent(searchQuery, requestingUserId, requestId));
            
            // Wait for response with timeout
            return future.get(QUERY_TIMEOUT_MS, TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            logger.error("Timeout waiting for search by query: " + searchQuery);
            return List.of();
        } catch (Exception e) {
            logger.error("Error searching by query: " + searchQuery + ": " + e.getMessage());
            return List.of();
        } finally {
            pendingSearchByQueryQueries.remove(requestId);
        }
    }
    
    /**
     * Get excluded user IDs for a user via event query.
     */
    @Override
    public Set<UUID> getExcludedUserIds(UUID userId) {
        UUID requestId = UUID.randomUUID();
        CompletableFuture<Set<UUID>> future = new CompletableFuture<>();
        
        pendingExcludedUserIdsQueries.put(requestId, future);
        
        try {
            // Publish query event
            eventPublisher.publishEvent(new GetExcludedUserIdsQuery(userId, requestId));
            
            // Wait for response with timeout
            return future.get(QUERY_TIMEOUT_MS, TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            logger.warn("Timeout getting excluded user IDs for userId " + userId);
            return Set.of();
        } catch (Exception e) {
            logger.error("Error getting excluded user IDs for userId " + userId + ": " + e.getMessage());
            return Set.of();
        } finally {
            pendingExcludedUserIdsQueries.remove(requestId);
        }
    }
    
    // ========== Event Listeners for Response Handling ==========
    
    /**
     * Handle response for recommended friends query.
     */
    @EventListener
    public void handleRecommendedFriendsResponse(RecommendedFriendsResponse response) {
        CompletableFuture<List<RecommendedFriendUserDTO>> future = pendingRecommendedFriendsQueries.get(response.requestId());
        if (future != null) {
            future.complete(response.recommendedFriends());
        }
    }
    
    /**
     * Handle response for recommended friends by search query.
     */
    @EventListener
    public void handleSearchedUserResultResponse(SearchedUserResultResponse response) {
        CompletableFuture<SearchedUserResult> future = pendingSearchedUserResultQueries.get(response.requestId());
        if (future != null) {
            future.complete(response.searchedUserResult());
        }
    }
    
    /**
     * Handle response for search by query.
     */
    @EventListener
    public void handleSearchByQueryResponse(SearchByQueryResponse response) {
        CompletableFuture<List<BaseUserDTO>> future = pendingSearchByQueryQueries.get(response.requestId());
        if (future != null) {
            future.complete(response.users());
        }
    }
    
    /**
     * Handle response for excluded user IDs query.
     */
    @EventListener
    public void handleExcludedUserIdsResponse(ExcludedUserIdsResponse response) {
        CompletableFuture<Set<UUID>> future = pendingExcludedUserIdsQueries.get(response.requestId());
        if (future != null) {
            future.complete(response.excludedUserIds());
        }
    }
}
