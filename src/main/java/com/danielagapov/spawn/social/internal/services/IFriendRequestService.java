package com.danielagapov.spawn.social.internal.services;

import com.danielagapov.spawn.social.api.dto.CreateFriendRequestDTO;
import com.danielagapov.spawn.social.api.dto.FetchFriendRequestDTO;
import com.danielagapov.spawn.social.api.dto.FetchSentFriendRequestDTO;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Service interface for managing friend requests between users.
 * Handles creating, accepting, deleting friend requests and provides conversion utilities.
 */
public interface IFriendRequestService {
    
    /**
     * Saves a new friend request to the database. If a friend request already exists between the users,
     * returns the existing request. If a reverse friend request exists, auto-accepts it.
     * 
     * @param friendRequestDTO the friend request data to save
     * @return the saved CreateFriendRequestDTO
     * @throws com.danielagapov.spawn.Exceptions.Base.BaseSaveException if saving fails
     * @throws com.danielagapov.spawn.Exceptions.Base.BaseNotFoundException if sender or receiver user doesn't exist
     */
    CreateFriendRequestDTO saveFriendRequest(CreateFriendRequestDTO friendRequestDTO);

    /**
     * Retrieves all incoming friend requests for a user as FetchFriendRequestDTO objects.
     * Includes mutual friend counts for each request.
     * 
     * @param id the unique identifier of the user receiving friend requests
     * @return List of FetchFriendRequestDTO objects representing incoming friend requests
     * @throws com.danielagapov.spawn.Exceptions.Base.BaseNotFoundException if user doesn't exist
     */
    List<FetchFriendRequestDTO> getIncomingFetchFriendRequestsByUserId(UUID id);

    /**
     * Retrieves all incoming friend requests for a user as CreateFriendRequestDTO objects.
     * 
     * @param id the unique identifier of the user receiving friend requests
     * @return List of CreateFriendRequestDTO objects representing incoming friend requests
     * @throws com.danielagapov.spawn.Exceptions.Base.BaseNotFoundException if user doesn't exist
     */
    List<CreateFriendRequestDTO> getIncomingCreateFriendRequestsByUserId(UUID id);

    /**
     * Accepts a friend request by creating a bidirectional friendship between the users,
     * publishes notification events, and deletes the friend request.
     * 
     * @param id the unique identifier of the friend request to accept
     * @throws com.danielagapov.spawn.Exceptions.Base.BaseNotFoundException if friend request doesn't exist
     */
    void acceptFriendRequest(UUID id);

    /**
     * Deletes a friend request by its unique identifier.
     * 
     * @param id the unique identifier of the friend request to delete
     * @throws org.springframework.dao.DataAccessException if database deletion fails
     */
    void deleteFriendRequest(UUID id);

    /**
     * Converts a list of CreateFriendRequestDTO objects to FetchFriendRequestDTO objects
     * with additional user information and mutual friend counts.
     * 
     * @param friendRequests the list of CreateFriendRequestDTO objects to convert
     * @return List of FetchFriendRequestDTO objects with populated user details
     */
    List<FetchFriendRequestDTO> convertFriendRequestsToFetchFriendRequests(List<CreateFriendRequestDTO> friendRequests);

    /**
     * Retrieves all friend requests sent by a specific user as CreateFriendRequestDTO objects.
     * 
     * @param userId the unique identifier of the user who sent the friend requests
     * @return List of CreateFriendRequestDTO objects representing sent friend requests
     * @throws com.danielagapov.spawn.Exceptions.Base.BaseNotFoundException if user doesn't exist
     */
    List<CreateFriendRequestDTO> getSentFriendRequestsByUserId(UUID userId);
    
    /**
     * Retrieves all friend requests sent by a specific user as FetchSentFriendRequestDTO objects.
     * 
     * @param userId the unique identifier of the user who sent the friend requests
     * @return List of FetchSentFriendRequestDTO objects representing sent friend requests
     * @throws com.danielagapov.spawn.Exceptions.Base.BaseNotFoundException if user doesn't exist
     */
    List<FetchSentFriendRequestDTO> getSentFetchFriendRequestsByUserId(UUID userId);
    
    /**
     * Deletes any friend request that exists between two specific users.
     * 
     * @param senderId the unique identifier of the sender user
     * @param receiverId the unique identifier of the receiver user
     * @throws com.danielagapov.spawn.Exceptions.Base.BaseNotFoundException if either user doesn't exist
     */
    void deleteFriendRequestBetweenUsersIfExists(UUID senderId, UUID receiverId);
    
    /**
     * Gets the timestamp of the latest friend request involving the user
     * (either sent or received)
     * 
     * @param userId The user ID to get the latest friend request timestamp for
     * @return The timestamp of the latest friend request, or null if none found
     */
    Instant getLatestFriendRequestTimestamp(UUID userId);
}
