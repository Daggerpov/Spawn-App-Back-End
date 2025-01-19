package com.danielagapov.spawn.Mappers;

import com.danielagapov.spawn.DTOs.FriendRequestDTO;
import com.danielagapov.spawn.Models.FriendRequest;
import com.danielagapov.spawn.Models.User;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class FriendRequestMapper {

    public static FriendRequestDTO toDTO(FriendRequest friendRequest) {
        return new FriendRequestDTO(
                friendRequest.getId(),
                friendRequest.getSender().getId(),
                friendRequest.getReceiver().getId()
        );
    }

    public static FriendRequest toEntity(FriendRequestDTO friendRequestDTO, User sender, User receiver) {
        FriendRequest friendRequest = new FriendRequest();
        friendRequest.setId(friendRequestDTO.id());
        friendRequest.setSender(sender);
        friendRequest.setReceiver(receiver);
        return friendRequest;
    }

    public static List<FriendRequestDTO> toDTOList(List<FriendRequest> friendRequests) {
        return friendRequests.stream()
                .map(FriendRequestMapper::toDTO)
                .collect(Collectors.toList());
    }

    public static List<FriendRequest> toEntityList(List<FriendRequestDTO> friendRequestDTOList) {
        return friendRequestDTOList.stream()
                .map(FriendRequestMapper::toEntity)
                .collect(Collectors.toList());
    }
}
