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

    public FetchFriendRequestDTO(UUID id, BaseUserDTO senderUser) {
        super(id);
        this.senderUser = senderUser;
    }


}