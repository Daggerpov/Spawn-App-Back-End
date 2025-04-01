package com.danielagapov.spawn.DTOs.BlockedUser;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
public class BlockedUserDTO {
    private UUID id;
    private UUID blockerId;
    private UUID blockedId;
    private String blockerUsername;
    private String blockedUsername;

    @JsonProperty("reason")
    private String reason;

    public BlockedUserDTO(UUID id, UUID blockerId, UUID blockedId, String blockerUsername, String blockedUsername, String reason) {
        this.id = id;
        this.blockerId = blockerId;
        this.blockedId = blockedId;
        this.blockerUsername = blockerUsername;
        this.blockedUsername = blockedUsername;
        this.reason = reason;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        BlockedUserDTO that = (BlockedUserDTO) obj;
        return blockerId.equals(that.blockerId) && blockedId.equals(that.blockedId);
    }
}
