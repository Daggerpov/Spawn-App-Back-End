package com.danielagapov.spawn.user.internal.services;

import com.danielagapov.spawn.user.api.dto.FriendUser.FullFriendUserDTO;
import com.danielagapov.spawn.user.api.dto.FriendUser.MinimalFriendDTO;
import com.danielagapov.spawn.user.internal.domain.User;

import java.util.List;
import java.util.UUID;

/**
 * Service interface for querying friendship-related data.
 * This service breaks the circular dependency between UserService and UserSearchService
 * by extracting common friendship query operations.
 * 
 * Following CQRS principles, this service handles only READ operations for friendships.
 */
public interface IUserFriendshipQueryService {
    
    /**
     * Retrieves the user IDs of all friends of a specific user.
     *
     * @param userId the unique identifier of the user
     * @return List of UUID objects representing friend user IDs
     * @throws com.danielagapov.spawn.shared.exceptions.Base.BaseNotFoundException if user doesn't exist
     */
    List<UUID> getFriendUserIdsByUserId(UUID userId);
    
    /**
     * Retrieves all friends of a user as User entities.
     *
     * @param userId the unique identifier of the user
     * @return List of User entities representing the user's friends
     * @throws com.danielagapov.spawn.shared.exceptions.Base.BaseNotFoundException if user doesn't exist
     */
    List<User> getFriendUsersByUserId(UUID userId);
    
    /**
     * Retrieves all friends of a user as FullFriendUserDTO objects with complete information.
     *
     * @param requestingUserId the unique identifier of the user requesting their friends
     * @return List of FullFriendUserDTO objects representing the user's friends
     * @throws com.danielagapov.spawn.shared.exceptions.Base.BaseNotFoundException if user doesn't exist
     */
    List<FullFriendUserDTO> getFullFriendUsersByUserId(UUID requestingUserId);
    
    /**
     * Retrieves all friends of a user as MinimalFriendDTO objects with only essential fields.
     * This is optimized for friend selection lists (activity creation, activity types) to reduce memory usage.
     * 
     * Fields included: id, username, name, profilePicture
     * Fields excluded: bio, email, hasCompletedOnboarding, provider
     *
     * @param requestingUserId the unique identifier of the user requesting their friends
     * @return List of MinimalFriendDTO objects representing the user's friends
     * @throws com.danielagapov.spawn.shared.exceptions.Base.BaseNotFoundException if user doesn't exist
     */
    List<MinimalFriendDTO> getMinimalFriendUsersByUserId(UUID requestingUserId);
    
    /**
     * Checks if a user is a friend of another user.
     *
     * @param userId The ID of the user to check
     * @param potentialFriendId The ID of the potential friend
     * @return True if the users are friends, false otherwise
     */
    boolean isUserFriendOfUser(UUID userId, UUID potentialFriendId);
    
    /**
     * Calculates the number of mutual friends between two users.
     *
     * @param userId1 the unique identifier of the first user
     * @param userId2 the unique identifier of the second user
     * @return the number of mutual friends between the users
     */
    int getMutualFriendCount(UUID userId1, UUID userId2);
    
    /**
     * Retrieves a user entity by their unique identifier.
     *
     * @param userId the unique identifier of the user
     * @return User entity object
     * @throws com.danielagapov.spawn.shared.exceptions.Base.BaseNotFoundException if user with given ID is not found
     */
    User getUserEntityById(UUID userId);
    
    /**
     * Retrieves all active users from the database.
     *
     * @return List of User entities representing all active users
     * @throws com.danielagapov.spawn.shared.exceptions.Base.BasesNotFoundException if database access fails
     */
    List<User> getAllActiveUsers();
}

