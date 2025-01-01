package com.danielagapov.spawn.Mappers;

import com.danielagapov.spawn.DTOs.FriendRequestDTO;
import com.danielagapov.spawn.Models.FriendRequests;
import com.danielagapov.spawn.Repositories.IUserFriendTagRepository;
import com.danielagapov.spawn.Repositories.IUserRepository;
import com.danielagapov.spawn.Services.FriendTag.FriendTagService;
import com.danielagapov.spawn.Services.User.UserService;

import java.util.List;
import java.util.stream.Collectors;

public class FriendRequestMapper {

    public static FriendRequestDTO toDTO(FriendRequests friendRequest, UserService userService, FriendTagService ftService) {
        return new FriendRequestDTO(
                friendRequest.getId(),
                UserMapper.toDTO(friendRequest.getSender(), userService, ftService),
                UserMapper.toDTO(friendRequest.getReceiver(), userService, ftService)
        );
    }

    public static FriendRequests toEntity(FriendRequestDTO friendRequestDTO) {
        FriendRequests friendRequest = new FriendRequests();
        friendRequest.setId(friendRequestDTO.id());
        friendRequest.setSender(UserMapper.toEntity(friendRequestDTO.sender()));
        friendRequest.setReceiver(UserMapper.toEntity(friendRequestDTO.receiver()));
        return friendRequest;
    }

    public static List<FriendRequestDTO> toDTOList(List<FriendRequests> friendRequests,
                                                   FriendTagService ftService,
                                                   UserService userService) {
        return friendRequests.stream().map(friendTag -> toDTO(friendTag, userService, ftService)).collect(Collectors.toList());
    }

    public static List<FriendRequests> toEntityList(List<FriendRequestDTO> friendRequestDTOList) {
        return friendRequestDTOList.stream()
                .map(FriendRequestMapper::toEntity)
                .collect(Collectors.toList());
    }
}
