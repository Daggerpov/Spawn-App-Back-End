package com.danielagapov.spawn.user.api.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
public class BlockedUserCreationDTO {
    private UUID blockerId;
    private UUID blockedId;
    private String reason;

    public BlockedUserCreationDTO(UUID blockerId, UUID blockedId, String reason) {
        this.blockerId = blockerId;
        this.blockedId = blockedId;
        this.reason = reason;
    }
}
