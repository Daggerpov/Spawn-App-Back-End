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
    private String blockedName;
    private String blockedProfilePicture;

    @JsonProperty("reason")
    private String reason;

    public BlockedUserDTO(UUID id, UUID blockerId, UUID blockedId, String blockerUsername, String blockedUsername, 
                          String blockedName, String blockedProfilePicture, String reason) {
        this.id = id;
        this.blockerId = blockerId;
        this.blockedId = blockedId;
        this.blockerUsername = blockerUsername;
        this.blockedUsername = blockedUsername;
        this.blockedName = blockedName;
        this.blockedProfilePicture = blockedProfilePicture;
        this.reason = reason;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        BlockedUserDTO that = (BlockedUserDTO) obj;
        return blockerId != null ? blockerId.equals(that.blockerId) : that.blockerId == null &&
               blockedId != null ? blockedId.equals(that.blockedId) : that.blockedId == null;
    }

    @Override
    public int hashCode() {
        int result = blockerId != null ? blockerId.hashCode() : 0;
        result = 31 * result + (blockedId != null ? blockedId.hashCode() : 0);
        return result;
    }
}
