package com.danielagapov.spawn.DTOs;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
public class FriendTagDTO extends AbstractFriendTagDTO implements Serializable {
    UUID ownerUserId;
    List<UUID> friendUserIds;
    public FriendTagDTO(UUID id, String displayName, String colorHexCode,
                            UUID ownerUserId,
                            List<UUID> friendUserIds,
                            boolean isEveryone) {
        super(id, displayName, colorHexCode, isEveryone);
        this.ownerUserId = ownerUserId;
        this.friendUserIds = friendUserIds;
    }
}