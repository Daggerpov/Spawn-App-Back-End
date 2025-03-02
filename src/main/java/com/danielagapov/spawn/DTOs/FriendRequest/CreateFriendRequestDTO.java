package com.danielagapov.spawn.DTOs.FriendRequest;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.UUID;

@Getter
@Setter
public class CreateFriendRequestDTO extends AbstractFriendRequestDTO implements Serializable {
    private UUID senderUserId;
    private UUID receiverUserId;

    public CreateFriendRequestDTO(UUID id, UUID senderUserId, UUID receiverUserId) {
        super(id);
        this.senderUserId = senderUserId;
        this.receiverUserId = receiverUserId;
    }
}
