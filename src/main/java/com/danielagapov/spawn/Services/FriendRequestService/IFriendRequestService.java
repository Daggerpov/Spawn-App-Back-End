package com.danielagapov.spawn.Services.FriendRequestService;

import com.danielagapov.spawn.DTOs.FriendRequestDTO;
import com.danielagapov.spawn.DTOs.FriendTagDTO;
import com.danielagapov.spawn.DTOs.UserDTO;

import java.util.List;

public interface IFriendRequestService {
    public FriendRequestDTO saveFriendRequest(FriendRequestDTO friendRequestDTO,
                                              List<UserDTO> senderFriends,
                                              List<FriendTagDTO> senderFriendTags,
                                              List<UserDTO> receiverFriends,
                                              List<FriendTagDTO> receiverFriendTags);
}
