package com.danielagapov.spawn.Services.BlockedUser;

import com.danielagapov.spawn.DTOs.BlockedUser.BlockedUserDTO;

import java.util.List;
import java.util.UUID;

public interface IBlockedUserService {
    void blockUser(UUID blockerId, UUID blockedId, String reason);

    void unblockUser(UUID blockerId, UUID blockedId);

    boolean isBlocked(UUID blockerId, UUID blockedId);

    List<BlockedUserDTO> getBlockedUsers(UUID blockerId);

    //getblockedusers returns UUID
    List<UUID> getBlockedUserIds(UUID blockerId);

    void removeFriendshipBetweenUsers(UUID userAId, UUID userBId);
    
    /**
     * Filter out blocked users from a list of user objects.
     * This method filters out users that are blocked by the requesting user,
     * as well as users who have blocked the requesting user.
     * 
     * @param users The list of user objects to filter (must have getId() method)
     * @param requestingUserId The ID of the user making the request
     * @param <T> The type of user object (must have getId() method)
     * @return A filtered list with blocked users removed
     */
    <T> List<T> filterOutBlockedUsers(List<T> users, UUID requestingUserId);
}
