package com.danielagapov.spawn.social.api.dto;

import com.danielagapov.spawn.user.api.dto.BaseUserDTO;
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
