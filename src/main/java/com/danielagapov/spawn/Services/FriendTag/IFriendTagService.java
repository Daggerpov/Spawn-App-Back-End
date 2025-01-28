package com.danielagapov.spawn.Services.FriendTag;

import com.danielagapov.spawn.DTOs.FriendTagDTO;
import com.danielagapov.spawn.DTOs.FullFriendTagDTO;

import java.util.List;
import java.util.UUID;

public interface IFriendTagService {
    List<FriendTagDTO> getAllFriendTags();
    FriendTagDTO getFriendTagById(UUID id);
    FullFriendTagDTO getFullFriendTagById(UUID id);
    List<FriendTagDTO> getFriendTagsByOwnerId(UUID ownerId);
    List<FullFriendTagDTO> getFullFriendTagsByOwnerId(UUID ownerId);
    FriendTagDTO saveFriendTag(FriendTagDTO friendTag);
    FriendTagDTO replaceFriendTag(FriendTagDTO friendTag, UUID tagId);
    boolean deleteFriendTagById(UUID id);
    void saveUserToFriendTag(UUID id, UUID userId);
    List<UUID> getFriendTagIdsByOwnerUserId(UUID id);
    FullFriendTagDTO getFullFriendTagByFriendTag(FriendTagDTO friendTag);
    FriendTagDTO getPertainingFriendTagByUserIds(UUID ownerUserId, UUID friendUserId);
}
