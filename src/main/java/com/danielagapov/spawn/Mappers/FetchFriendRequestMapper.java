package com.danielagapov.spawn.Mappers;

import com.danielagapov.spawn.DTOs.FriendRequest.FetchFriendRequestDTO;
import com.danielagapov.spawn.Models.FriendRequest;
import com.danielagapov.spawn.Models.User;
import com.danielagapov.spawn.Services.User.IUserService;

import java.util.List;
import java.util.UUID;

public class FetchFriendRequestMapper {

    private static IUserService userService;

    public static void setUserService(IUserService service) {
        userService = service;
    }
    public static FetchFriendRequestDTO toDTO(FriendRequest friendRequest, UUID receiverId) {
        User sender = friendRequest.getSender();
        int mutualFriendCount = userService.getMutualFriendCount(receiverId, sender.getId());
        return new FetchFriendRequestDTO(friendRequest.getId(), UserMapper.toDTO(sender), mutualFriendCount);
    }

    public static List<FetchFriendRequestDTO> toDTOList(List<FriendRequest> friendRequests, UUID receiverId) {
        return friendRequests.stream()
                .map(friendRequest -> toDTO(friendRequest, receiverId))
                .toList();
    }
}
