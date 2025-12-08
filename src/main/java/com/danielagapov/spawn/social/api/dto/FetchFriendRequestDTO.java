package com.danielagapov.spawn.social.api.dto;

import com.danielagapov.spawn.user.api.dto.BaseUserDTO;
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