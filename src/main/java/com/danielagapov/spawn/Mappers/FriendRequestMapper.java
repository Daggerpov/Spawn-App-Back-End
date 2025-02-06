package com.danielagapov.spawn.Mappers;

import com.danielagapov.spawn.DTOs.FriendRequestDTO;
import com.danielagapov.spawn.Exceptions.Models.FriendRequest;
import com.danielagapov.spawn.Exceptions.Models.User;

import java.util.List;
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

    public static List<FriendRequest> toEntityList(List<FriendRequestDTO> friendRequestDTOList, List<User> userSenders, List<User> userReceivers) {
        return friendRequestDTOList.stream()
                .map(dto -> {
                    // Find sender user by matching senderId from userSenders list
                    User sender = userSenders.stream()
                            .filter(user -> user.getId().equals(dto.senderUserId()))
                            .findFirst()
                            .orElse(null);

                    // Find receiver user by matching receiverId from userReceivers list
                    User receiver = userReceivers.stream()
                            .filter(user -> user.getId().equals(dto.receiverUserId()))
                            .findFirst()
                            .orElse(null);

                    return toEntity(dto, sender, receiver); // Convert DTO to entity
                })
                .collect(Collectors.toList());
    }


}
