package com.danielagapov.spawn.DTOs.FriendRequest;

import com.danielagapov.spawn.DTOs.User.BaseUserDTO;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.UUID;


@Getter
@Setter
public class FetchFriendRequestDTO extends AbstractFriendRequestDTO implements Serializable {
    private BaseUserDTO senderUser;
    private int mutualFriendCount;

    public FetchFriendRequestDTO(UUID id, BaseUserDTO senderUser, int mutualFriendCount) {
        super(id);
        this.senderUser = senderUser;
        this.mutualFriendCount = mutualFriendCount;
    }


}