package com.danielagapov.spawn.social.internal.services;

import com.danielagapov.spawn.user.internal.domain.User;

import java.util.UUID;

/**
 * Interface for querying user data from the User module via events.
 * This breaks the circular dependency between Social and User modules.
 */
public interface IUserQueryService {
    
    /**
     * Get a user entity by ID via event query.
     * Returns a User domain object reconstructed from UserData.
     * 
     * @param userId The ID of the user to retrieve
     * @return User entity
     * @throws com.danielagapov.spawn.shared.exceptions.Base.BaseNotFoundException if user not found
     */
    User getUserEntityById(UUID userId);
    
    /**
     * Check if two users are friends via event query.
     * 
     * @param userAId First user ID
     * @param userBId Second user ID
     * @return true if users are friends, false otherwise
     */
    boolean isUserFriendOfUser(UUID userAId, UUID userBId);
    
    /**
     * Get mutual friend count between two users via event query.
     * 
     * @param userAId First user ID
     * @param userBId Second user ID
     * @return Count of mutual friends
     */
    int getMutualFriendCount(UUID userAId, UUID userBId);
    
    /**
     * Save a friendship between two users via event command.
     * 
     * @param userAId First user ID
     * @param userBId Second user ID
     * @return true if friendship saved successfully, false otherwise
     */
    boolean saveFriendToUser(UUID userAId, UUID userBId);
}



