package com.danielagapov.spawn.Mappers;

import com.danielagapov.spawn.DTOs.FriendRequest.FetchFriendRequestDTO;
import com.danielagapov.spawn.Models.FriendRequest;
import com.danielagapov.spawn.Models.User;

import java.util.List;

public class FetchFriendRequestMapper {
    public static FetchFriendRequestDTO toDTO(FriendRequest friendRequest) {
        User sender = friendRequest.getSender();
        return new FetchFriendRequestDTO(friendRequest.getId(), PotentialFriendUserMapper.toDTO(sender));
    }

    public static List<FetchFriendRequestDTO> toDTOList(List<FriendRequest> friendRequests) {
        return friendRequests.stream().map(FetchFriendRequestMapper::toDTO).toList();
    }
}
