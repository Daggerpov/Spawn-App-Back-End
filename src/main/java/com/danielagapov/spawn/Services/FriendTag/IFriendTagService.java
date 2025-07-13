package com.danielagapov.spawn.Services.FriendTag;

import com.danielagapov.spawn.DTOs.FriendTag.AbstractFriendTagDTO;
import com.danielagapov.spawn.DTOs.FriendTag.FriendTagDTO;
import com.danielagapov.spawn.DTOs.User.BaseUserDTO;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface IFriendTagService {
    // CRUD operations:
    FriendTagDTO getFriendTagById(UUID id);

    FriendTagDTO saveFriendTag(AbstractFriendTagDTO friendTag);

    boolean deleteFriendTagById(UUID id);

    // owner-related:
    List<FriendTagDTO> getFriendTagsByOwnerId(UUID ownerId);

    List<UUID> getFriendTagIdsByOwnerUserId(UUID id);

    // friend-related:
    Optional<FriendTagDTO> getPertainingFriendTagBetweenUsers(UUID ownerUserId, UUID friendUserId);

    void saveUserToFriendTag(UUID id, UUID userId);

    void removeUserFromFriendTag(UUID id, UUID userId);

    // Get all friend IDs associated with a specific tag
    List<UUID> getFriendIdsByTagId(UUID tagId);
}
