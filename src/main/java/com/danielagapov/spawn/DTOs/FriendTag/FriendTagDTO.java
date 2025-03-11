package com.danielagapov.spawn.DTOs.FriendTag;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
public class FriendTagDTO extends AbstractFriendTagDTO implements Serializable {
    private UUID ownerUserId;
    private List<UUID> friendUserIds;
    @JsonProperty("isEveryone") // Explicitly define JSON property name
    private boolean isEveryone;

    public FriendTagDTO(UUID id, String displayName, String colorHexCode,
                        UUID ownerUserId,
                        List<UUID> friendUserIds,
                        boolean isEveryone) {
        super(id, displayName, colorHexCode);
        this.ownerUserId = ownerUserId;
        this.friendUserIds = friendUserIds;
        this.isEveryone = isEveryone;
    }
}