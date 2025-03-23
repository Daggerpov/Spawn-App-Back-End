package com.danielagapov.spawn.Services.FriendRequest;

import com.danielagapov.spawn.DTOs.FriendRequest.CreateFriendRequestDTO;
import com.danielagapov.spawn.DTOs.FriendRequest.FetchFriendRequestDTO;

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
}
