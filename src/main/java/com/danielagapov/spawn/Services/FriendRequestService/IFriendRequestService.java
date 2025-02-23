package com.danielagapov.spawn.Services.FriendRequestService;

import com.danielagapov.spawn.DTOs.FriendRequest.FriendRequestDTO;
import com.danielagapov.spawn.DTOs.FriendRequest.FullFriendRequestDTO;

import java.util.List;
import java.util.UUID;

public interface IFriendRequestService {
    FriendRequestDTO saveFriendRequest(FriendRequestDTO friendRequestDTO);
    List<FullFriendRequestDTO> getIncomingFriendRequestsByUserId(UUID id);

    void acceptFriendRequest(UUID id);
    void deleteFriendRequest(UUID id);

    List<FullFriendRequestDTO> convertFriendRequestsToFullFriendRequests (List<FriendRequestDTO> friendRequests);

    List<FriendRequestDTO> getSentFriendRequestsByUserId(UUID userId);
}
