package com.danielagapov.spawn.Services.FriendRequest;

import com.danielagapov.spawn.DTOs.FriendRequest.CreateFriendRequestDTO;
import com.danielagapov.spawn.DTOs.FriendRequest.FetchFriendRequestDTO;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface IFriendRequestService {
    CreateFriendRequestDTO saveFriendRequest(CreateFriendRequestDTO friendRequestDTO);

    List<FetchFriendRequestDTO> getIncomingFetchFriendRequestsByUserId(UUID id);

    List<CreateFriendRequestDTO> getIncomingCreateFriendRequestsByUserId(UUID id);

    void acceptFriendRequest(UUID id);

    void deleteFriendRequest(UUID id);

    List<FetchFriendRequestDTO> convertFriendRequestsToFetchFriendRequests(List<CreateFriendRequestDTO> friendRequests);

    List<CreateFriendRequestDTO> getSentFriendRequestsByUserId(UUID userId);
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
