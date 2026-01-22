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

/**
 * Event listener for User Search module.
 * Handles query events from other modules and publishes responses.
 * This breaks circular dependencies by using events for cross-module communication.
 */
@Service
public class UserSearchEventListener {
    
    private final IUserSearchService userSearchService;
    private final ApplicationEventPublisher eventPublisher;
    private final ILogger logger;
    
    public UserSearchEventListener(
            IUserSearchService userSearchService,
            ApplicationEventPublisher eventPublisher,
            ILogger logger) {
        this.userSearchService = userSearchService;
        this.eventPublisher = eventPublisher;
        this.logger = logger;
    }
    
    /**
     * Handle query for limited recommended friends.
     * Responds to UserSearchQueryService in other modules.
     */
    @EventListener
    public void handleGetLimitedRecommendedFriendsQuery(GetLimitedRecommendedFriendsQuery query) {
        try {
            logger.info("Handling GetLimitedRecommendedFriendsQuery for user: " + query.userId());
            
            List<RecommendedFriendUserDTO> recommendedFriends = userSearchService.getLimitedRecommendedFriendsForUserId(query.userId());
            eventPublisher.publishEvent(new RecommendedFriendsResponse(query.requestId(), recommendedFriends));
            
            logger.info("Published RecommendedFriendsResponse for user: " + query.userId());
        } catch (Exception e) {
            logger.error("Error handling GetLimitedRecommendedFriendsQuery: " + e.getMessage());
            eventPublisher.publishEvent(new RecommendedFriendsResponse(query.requestId(), List.of()));
        }
    }
    
    /**
     * Handle query for recommended friends by search.
     * Responds to UserSearchQueryService in other modules.
     */
    @EventListener
    public void handleGetRecommendedFriendsBySearchQuery(GetRecommendedFriendsBySearchQuery query) {
        try {
            logger.info("Handling GetRecommendedFriendsBySearchQuery for user: " + query.requestingUserId() + " with search: " + query.searchQuery());
            
            SearchedUserResult searchedUserResult = userSearchService.getRecommendedFriendsBySearch(query.requestingUserId(), query.searchQuery());
            eventPublisher.publishEvent(new SearchedUserResultResponse(query.requestId(), searchedUserResult));
            
            logger.info("Published SearchedUserResultResponse for user: " + query.requestingUserId());
        } catch (Exception e) {
            logger.error("Error handling GetRecommendedFriendsBySearchQuery: " + e.getMessage());
            eventPublisher.publishEvent(new SearchedUserResultResponse(query.requestId(), new SearchedUserResult(List.of())));
        }
    }
    
    /**
     * Handle query for search by query string.
     * Responds to UserSearchQueryService in other modules.
     */
    @EventListener
    public void handleSearchByQueryEvent(SearchByQueryEvent query) {
        try {
            logger.info("Handling SearchByQueryEvent with search: " + query.searchQuery());
            
            List<BaseUserDTO> users = userSearchService.searchByQuery(query.searchQuery(), query.requestingUserId());
            eventPublisher.publishEvent(new SearchByQueryResponse(query.requestId(), users));
            
            logger.info("Published SearchByQueryResponse for search: " + query.searchQuery());
        } catch (Exception e) {
            logger.error("Error handling SearchByQueryEvent: " + e.getMessage());
            eventPublisher.publishEvent(new SearchByQueryResponse(query.requestId(), List.of()));
        }
    }
    
    /**
     * Handle query for excluded user IDs.
     * Responds to UserSearchQueryService in other modules.
     */
    @EventListener
    public void handleGetExcludedUserIdsQuery(GetExcludedUserIdsQuery query) {
        try {
            logger.info("Handling GetExcludedUserIdsQuery for user: " + query.userId());
            
            Set<UUID> excludedUserIds = userSearchService.getExcludedUserIds(query.userId());
            eventPublisher.publishEvent(new ExcludedUserIdsResponse(query.requestId(), excludedUserIds));
            
            logger.info("Published ExcludedUserIdsResponse for user: " + query.userId());
        } catch (Exception e) {
            logger.error("Error handling GetExcludedUserIdsQuery: " + e.getMessage());
            eventPublisher.publishEvent(new ExcludedUserIdsResponse(query.requestId(), Set.of()));
        }
    }
}

