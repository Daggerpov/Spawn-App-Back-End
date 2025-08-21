package com.danielagapov.spawn.DTOs.FriendRequest;

import com.danielagapov.spawn.DTOs.User.BaseUserDTO;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.UUID;

@Getter
@Setter
public class FetchSentFriendRequestDTO extends AbstractFriendRequestDTO implements Serializable {
    private BaseUserDTO receiverUser;

    public FetchSentFriendRequestDTO(UUID id, BaseUserDTO receiverUser) {
        super(id);
        this.receiverUser = receiverUser;
    }
}
