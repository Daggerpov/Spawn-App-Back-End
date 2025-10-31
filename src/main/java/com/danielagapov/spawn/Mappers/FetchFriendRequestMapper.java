package com.danielagapov.spawn.Mappers;

import com.danielagapov.spawn.DTOs.FriendRequest.FetchFriendRequestDTO;
import com.danielagapov.spawn.DTOs.FriendRequest.FetchSentFriendRequestDTO;
import com.danielagapov.spawn.Models.FriendRequest;
import com.danielagapov.spawn.Models.User.User;

public final class FetchFriendRequestMapper {
    public static FetchFriendRequestDTO toDTO(FriendRequest friendRequest, int mutualFriendCount) {
        User sender = friendRequest.getSender();
        return new FetchFriendRequestDTO(friendRequest.getId(), UserMapper.toDTO(sender), mutualFriendCount);
    }
    
    public static FetchSentFriendRequestDTO toSentDTO(FriendRequest friendRequest) {
        User receiver = friendRequest.getReceiver();
        return new FetchSentFriendRequestDTO(friendRequest.getId(), UserMapper.toDTO(receiver));
    }
}
