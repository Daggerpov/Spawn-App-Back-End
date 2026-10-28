package com.danielagapov.spawn.user.internal.services;

import com.danielagapov.spawn.activity.api.dto.UserIdActivityTimeDTO;
import com.danielagapov.spawn.shared.exceptions.Logger.ILogger;
import com.danielagapov.spawn.shared.feign.ActivityServiceClient;
import com.danielagapov.spawn.shared.util.LoggingUtils;
import com.danielagapov.spawn.shared.util.ParticipationStatus;
import com.danielagapov.spawn.user.api.dto.RecentlySpawnedUserDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class RecentlySpawnedService implements IRecentlySpawnedService {
    
    private final ActivityServiceClient activityServiceClient;
    private final IUserSearchQueryService userSearchQueryService;
    private final IUserService userService;
    private final ILogger logger;
    
    private static final int ACTIVITY_LIMIT = 10;
    private static final int USER_LIMIT = 40;
    
    @Autowired
    public RecentlySpawnedService(
            ActivityServiceClient activityServiceClient,
            IUserSearchQueryService userSearchQueryService,
            IUserService userService,
            ILogger logger) {
        this.activityServiceClient = activityServiceClient;
        this.userSearchQueryService = userSearchQueryService;
        this.userService = userService;
        this.logger = logger;
    }
    
    @Override
    public List<RecentlySpawnedUserDTO> getRecentlySpawnedWithUsers(UUID requestingUserId) {
        try {
            List<UUID> pastActivityIds = activityServiceClient.getPastActivityIdsForUser(
                    requestingUserId, 
                    ParticipationStatus.participating, 
                    ACTIVITY_LIMIT
            );
            
            List<UserIdActivityTimeDTO> pastActivityParticipantIds = activityServiceClient.getOtherUsersByActivities(
                    pastActivityIds, 
                    requestingUserId, 
                    ParticipationStatus.participating
            );
            
            Set<UUID> excludedIds = userSearchQueryService.getExcludedUserIds(requestingUserId);
            
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
