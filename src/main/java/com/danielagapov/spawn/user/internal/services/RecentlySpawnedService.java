package com.danielagapov.spawn.user.internal.services;

import com.danielagapov.spawn.activity.api.IActivityService;
import com.danielagapov.spawn.activity.api.dto.UserIdActivityTimeDTO;
import com.danielagapov.spawn.shared.exceptions.Logger.ILogger;
import com.danielagapov.spawn.shared.util.LoggingUtils;
import com.danielagapov.spawn.shared.util.ParticipationStatus;
import com.danielagapov.spawn.user.api.dto.RecentlySpawnedUserDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service implementation for retrieving users that a user has recently done activities with.
 * 
 * This service breaks the circular dependency between UserService and ActivityService by:
 * - Depending on IActivityService (for activity queries)
 * - Depending on IUserSearchQueryService (for user queries, not IUserService)
 * - Not being depended upon by either UserService or ActivityService
 */
@Service
public class RecentlySpawnedService implements IRecentlySpawnedService {
    
    private final IActivityService activityService;
    private final IUserSearchQueryService userSearchQueryService;
    private final IUserService userService;
    private final ILogger logger;
    
    private static final int ACTIVITY_LIMIT = 10;
    private static final int USER_LIMIT = 40;
    
    @Autowired
    public RecentlySpawnedService(
            IActivityService activityService,
            IUserSearchQueryService userSearchQueryService,
            IUserService userService,
            ILogger logger) {
        this.activityService = activityService;
        this.userSearchQueryService = userSearchQueryService;
        this.userService = userService;
        this.logger = logger;
    }
    
    @Override
    public List<RecentlySpawnedUserDTO> getRecentlySpawnedWithUsers(UUID requestingUserId) {
        try {
            // Use UTC for consistent timezone comparison across server and client timezones
            OffsetDateTime now = OffsetDateTime.now(java.time.ZoneOffset.UTC);
            
            // Get past activities the user participated in
            List<UUID> pastActivityIds = activityService.getPastActivityIdsForUser(
                    requestingUserId, 
                    ParticipationStatus.participating, 
                    now, 
                    Limit.of(ACTIVITY_LIMIT)
            );
            
            // Get other users from those activities
            List<UserIdActivityTimeDTO> pastActivityParticipantIds = activityService.getOtherUserIdsByActivityIds(
                    pastActivityIds, 
                    requestingUserId, 
                    ParticipationStatus.participating
            );
            
            // Get users to exclude (e.g., already friends, blocked)
            Set<UUID> excludedIds = userSearchQueryService.getExcludedUserIds(requestingUserId);
            
            // Convert to DTOs, filtering excluded users
            return pastActivityParticipantIds.stream()
                    .filter(e -> !excludedIds.contains(e.getUserId()))
                    .map(e -> new RecentlySpawnedUserDTO(
                            userService.getBaseUserById(e.getUserId()), 
                            e.getStartTime()
                    ))
                    .limit(USER_LIMIT)
                    .collect(Collectors.toList());
                    
        } catch (Exception e) {
            logger.error("Error fetching recently spawned-with users for user: " 
                    + LoggingUtils.formatUserIdInfo(requestingUserId) + ". " + e.getMessage());
            throw e;
        }
    }
}
