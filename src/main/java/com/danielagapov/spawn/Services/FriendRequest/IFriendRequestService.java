package com.danielagapov.spawn.Services.FriendRequest;

import com.danielagapov.spawn.DTOs.FriendRequest.FetchFriendRequestDTO;
import com.danielagapov.spawn.DTOs.FriendRequest.FriendRequestDTO;

import java.util.List;
import java.util.UUID;

public interface IFriendRequestService {
    FriendRequestDTO saveFriendRequest(FriendRequestDTO friendRequestDTO);

    List<FetchFriendRequestDTO> getIncomingFetchFriendRequestsByUserId(UUID id);

    List<FriendRequestDTO> getIncomingFriendRequestsByUserId(UUID id);

    void acceptFriendRequest(UUID id);

    void deleteFriendRequest(UUID id);

    List<FetchFriendRequestDTO> convertFriendRequestsToFetchFriendRequests(List<FriendRequestDTO> friendRequests);

    List<FriendRequestDTO> getSentFriendRequestsByUserId(UUID userId);
}
