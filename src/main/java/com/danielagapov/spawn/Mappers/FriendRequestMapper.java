package com.danielagapov.spawn.Mappers;

import com.danielagapov.spawn.DTOs.FriendRequest.CreateFriendRequestDTO;
import com.danielagapov.spawn.Models.FriendRequest;
import com.danielagapov.spawn.Models.User;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

public class FriendRequestMapper {

    public static CreateFriendRequestDTO toDTO(FriendRequest friendRequest) {
        return new CreateFriendRequestDTO(
                friendRequest.getId(),
                friendRequest.getSender().getId(),
                friendRequest.getReceiver().getId()
        );
    }

    public static FriendRequest toEntity(CreateFriendRequestDTO friendRequestDTO, User sender, User receiver) {
        FriendRequest friendRequest = new FriendRequest();
        friendRequest.setId(friendRequestDTO.getId());
        friendRequest.setSender(sender);
        friendRequest.setReceiver(receiver);
        friendRequest.setCreatedAt(Instant.now());
        return friendRequest;
    }

    public static List<CreateFriendRequestDTO> toDTOList(List<FriendRequest> friendRequests) {
        return friendRequests.stream()
                .map(FriendRequestMapper::toDTO)
                .collect(Collectors.toList());
    }

    public static List<FriendRequest> toEntityList(List<CreateFriendRequestDTO> friendRequestDTOList, List<User> userSenders, List<User> userReceivers) {
        return friendRequestDTOList.stream()
                .map(dto -> {
                    // Find sender user by matching senderId from userSenders list
                    User sender = userSenders.stream()
                            .filter(user -> user.getId().equals(dto.getSenderUserId()))
                            .findFirst()
                            .orElse(null);

                    // Find receiver user by matching receiverId from userReceivers list
                    User receiver = userReceivers.stream()
                            .filter(user -> user.getId().equals(dto.getReceiverUserId()))
                            .findFirst()
                            .orElse(null);

                    return toEntity(dto, sender, receiver); // Convert DTO to entity
                })
                .collect(Collectors.toList());
    }


}
