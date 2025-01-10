package com.danielagapov.spawn.Services.FriendRequestService;

import com.danielagapov.spawn.DTOs.FriendRequestDTO;

import java.util.List;
import java.util.UUID;

public interface IFriendRequestService {
    public FriendRequestDTO saveFriendRequest(FriendRequestDTO friendRequestDTO);
    public List<FriendRequestDTO> getIncomingFriendRequestsByUserId(UUID id);
}
