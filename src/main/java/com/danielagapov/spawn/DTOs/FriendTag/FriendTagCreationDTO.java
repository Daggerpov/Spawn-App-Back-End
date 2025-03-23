package com.danielagapov.spawn.DTOs.FriendTag;

import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class FriendTagCreationDTO extends AbstractFriendTagDTO {
    private UUID ownerUserId;

    public FriendTagCreationDTO(UUID id, String displayName, String colorHexCode, UUID ownerUserId) {
        super(id, displayName, colorHexCode);
        this.ownerUserId = ownerUserId;
    }
}
