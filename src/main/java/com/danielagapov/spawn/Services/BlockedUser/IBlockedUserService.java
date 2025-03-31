package com.danielagapov.spawn.Services.BlockedUser;

import com.danielagapov.spawn.DTOs.BlockedUserDTO;

import java.util.List;
import java.util.UUID;

public interface IBlockedUserService {
    void blockUser(UUID blockerId, UUID blockedId, String reason);

    void unblockUser(UUID blockerId, UUID blockedId);

    boolean isBlocked(UUID blockerId, UUID blockedId);

    List<BlockedUserDTO> getBlockedUsers(UUID blockerId);

    //getblockedusers returns UUID
    List<UUID> getBlockedUserIds(UUID blockerId);
}
