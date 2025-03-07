package com.danielagapov.spawn.DTOs.FriendRequest;

import com.danielagapov.spawn.DTOs.User.FriendUser.PotentialFriendUserDTO;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.UUID;


@Getter
@Setter
public class FetchFriendRequestDTO extends AbstractFriendRequestDTO implements Serializable {
    private PotentialFriendUserDTO senderUser;

    public FetchFriendRequestDTO(UUID id, PotentialFriendUserDTO senderUser) {
        super(id);
        this.senderUser = senderUser;
    }


}