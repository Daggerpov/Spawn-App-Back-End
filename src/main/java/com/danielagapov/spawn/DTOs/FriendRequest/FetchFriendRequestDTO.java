package com.danielagapov.spawn.DTOs.FriendRequest;

import com.danielagapov.spawn.DTOs.User.FriendUser.PotentialFriendUserDTO;
import com.danielagapov.spawn.Models.FriendRequest;
import com.danielagapov.spawn.Models.User;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.List;
import java.util.UUID;


@Getter
@Setter
public class FetchFriendRequestDTO extends AbstractFriendRequestDTO implements Serializable {
    private PotentialFriendUserDTO senderUser;

    public FetchFriendRequestDTO(UUID id, PotentialFriendUserDTO senderUser) {
        super(id);
        this.senderUser = senderUser;
    }

    public static FetchFriendRequestDTO fromEntity(FriendRequest friendRequest) {
        User sender = friendRequest.getSender();
        return new FetchFriendRequestDTO(friendRequest.getId(), PotentialFriendUserDTO.fromUserEntity(sender));
    }

    public static List<FetchFriendRequestDTO> fromEntityList(List<FriendRequest> friendRequests) {
        return friendRequests.stream().map(FetchFriendRequestDTO::fromEntity).toList();
    }
}