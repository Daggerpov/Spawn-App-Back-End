package com.danielagapov.spawn.user.internal.services;

import com.danielagapov.spawn.user.api.dto.RecentlySpawnedUserDTO;

import java.util.List;
import java.util.UUID;

/**
 * Service interface for retrieving users that a user has recently done activities with.
 * 
 * This service exists to break the circular dependency between UserService and ActivityService.
 * It handles the cross-module query that needs both Activity and User data.
 */
public interface IRecentlySpawnedService {
    
    /**
     * Retrieves users who have recently done activities (spawned) that the requesting user participated in.
     *
     * @param requestingUserId the unique identifier of the user making the request
     * @return List of RecentlySpawnedUserDTO objects representing recently active users
     */
    List<RecentlySpawnedUserDTO> getRecentlySpawnedWithUsers(UUID requestingUserId);
}
