package com.danielagapov.spawn.Mappers;

import com.danielagapov.spawn.DTOs.FriendRequestDTO;
import com.danielagapov.spawn.Models.FriendRequests;

import java.util.List;
import java.util.stream.Collectors;

public class FriendRequestMapper {

    public static FriendRequestDTO toDTO(FriendRequests friendRequest) {
        return new FriendRequestDTO(friendRequest.getId(), friendRequest.getSender(), friendRequest.getReceiver());
    }

    public static FriendRequests toEntity(FriendRequestDTO friendRequestDTO) {
        FriendRequests friendRequest = new FriendRequests();
        friendRequest.setId(friendRequestDTO.id());
        friendRequest.setSender(friendRequestDTO.sender());
        friendRequest.setReceiver(friendRequestDTO.receiver());
        return friendRequest;
    }

    public static List<FriendRequestDTO> toDTOList(List<FriendRequests> friendRequests) {
        return friendRequests.stream().map(FriendRequestMapper::toDTO).collect(Collectors.toList());
    }

    public static List<FriendRequests> toEntityList(List<FriendRequestDTO> friendRequestDTOList) {
        return friendRequestDTOList.stream().map(FriendRequestMapper::toEntity).collect(Collectors.toList());
    }
}
