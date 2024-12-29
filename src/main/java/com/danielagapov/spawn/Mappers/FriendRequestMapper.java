package com.danielagapov.spawn.Mappers;

import com.danielagapov.spawn.DTOs.FriendRequestDTO;
import com.danielagapov.spawn.Models.FriendRequests;
import com.danielagapov.spawn.Repositories.IUserFriendTagRepository;
import com.danielagapov.spawn.Repositories.IUserRepository;

import java.util.List;
import java.util.stream.Collectors;

public class FriendRequestMapper {

    public static FriendRequestDTO toDTO(FriendRequests friendRequest, IUserFriendTagRepository uftRepository,
                                         IUserRepository userRepository) {
        return new FriendRequestDTO(
                friendRequest.getId(),
                UserMapper.toDTO(friendRequest.getSender(), uftRepository, userRepository),
                UserMapper.toDTO(friendRequest.getReceiver(), uftRepository, userRepository)
        );
    }

    public static FriendRequests toEntity(FriendRequestDTO friendRequestDTO, IUserRepository userRepository) {
        FriendRequests friendRequest = new FriendRequests();
        friendRequest.setId(friendRequestDTO.id());
        friendRequest.setSender(UserMapper.toEntity(friendRequestDTO.sender(), userRepository));
        friendRequest.setReceiver(UserMapper.toEntity(friendRequestDTO.receiver(), userRepository));
        return friendRequest;
    }

    public static List<FriendRequestDTO> toDTOList(List<FriendRequests> friendRequests,
                                                   IUserFriendTagRepository uftRepository,
                                                   IUserRepository userRepository) {
        return friendRequests.stream().map(friendTag -> toDTO(friendTag, uftRepository, userRepository)).collect(Collectors.toList());
    }

    public static List<FriendRequests> toEntityList(List<FriendRequestDTO> friendRequestDTOList,
                                                    IUserRepository userRepository) {
        return friendRequestDTOList.stream()
                .map(friendRequest -> toEntity(friendRequest, userRepository))
                .collect(Collectors.toList());
    }
}
