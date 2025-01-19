package com.danielagapov.spawn.Mappers;

import com.danielagapov.spawn.DTOs.FriendRequestDTO;
import com.danielagapov.spawn.DTOs.FriendTagDTO;
import com.danielagapov.spawn.DTOs.UserDTO;
import com.danielagapov.spawn.Models.FriendRequest;

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

    public static FriendRequest toEntity(FriendRequestDTO friendRequestDTO) {
        FriendRequest friendRequest = new FriendRequest();
        friendRequest.setId(friendRequestDTO.id());
        friendRequest.setSender(UserMapper.toEntity(friendRequestDTO.sender()));
        friendRequest.setReceiver(UserMapper.toEntity(friendRequestDTO.receiver()));
        return friendRequest;
    }

    public static List<FriendRequestDTO> toDTOList(List<FriendRequest> friendRequests,
                                                   List<List<UserDTO>> allSenderFriends,
                                                   List<List<FriendTagDTO>> allSenderFriendTags,
                                                   List<List<UserDTO>> allReceiverFriends,
                                                   List<List<FriendTagDTO>> allReceiverFriendTags) {
        return friendRequests.stream()
                .map(friendRequest -> {
                    int index = friendRequests.indexOf(friendRequest); // Match data by index
                    return toDTO(
                            friendRequest,
                            allSenderFriends.get(index),
                            allSenderFriendTags.get(index),
                            allReceiverFriends.get(index),
                            allReceiverFriendTags.get(index)
                    );
                })
                .collect(Collectors.toList());
    }

    public static List<FriendRequest> toEntityList(List<FriendRequestDTO> friendRequestDTOList) {
        return friendRequestDTOList.stream()
                .map(FriendRequestMapper::toEntity)
                .collect(Collectors.toList());
    }
}
