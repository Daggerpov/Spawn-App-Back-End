package com.danielagapov.spawn.DTOs.FriendRequest;

import com.danielagapov.spawn.DTOs.User.UserDTO;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.UUID;


@Getter
@Setter
public class FetchFriendRequestDTO extends AbstractFriendRequestDTO implements Serializable {
    private UserDTO senderUser;

    public FetchFriendRequestDTO(UUID id, UserDTO senderUser) {
        super(id);
        this.senderUser = senderUser;
    }
}
