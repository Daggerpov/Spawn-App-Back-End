package com.danielagapov.spawn.Services.FriendTag;

import com.danielagapov.spawn.DTOs.FriendTagDTO;
import com.danielagapov.spawn.DTOs.FullFriendTagDTO;

import java.util.List;
import java.util.UUID;

public interface IFriendTagService {
    List<FriendTagDTO> getAllFriendTags();

    // CRUD operations:
    FriendTagDTO getFriendTagById(UUID id);
    FriendTagDTO saveFriendTag(FriendTagDTO friendTag);
    FriendTagDTO replaceFriendTag(FriendTagDTO friendTag, UUID tagId);
    boolean deleteFriendTagById(UUID id);

    // owner-related:
    List<FriendTagDTO> getFriendTagsByOwnerId(UUID ownerId);
    List<UUID> getFriendTagIdsByOwnerUserId(UUID id);

    // friend-related:
    FriendTagDTO getPertainingFriendTagByUserIds(UUID ownerUserId, UUID friendUserId);
    void saveUserToFriendTag(UUID id, UUID userId);
    List<FullFriendTagDTO> getPertainingFriendTagsForFriend(UUID ownerUserId, UUID friendUserId);

    // full helpers:
    List<FullFriendTagDTO> convertFriendTagsToFullFriendTags(List<FriendTagDTO> friendTags);
    List<FullFriendTagDTO> getFullFriendTagsByOwnerId(UUID ownerId);
    FullFriendTagDTO getFullFriendTagById(UUID id);
    FullFriendTagDTO getFullFriendTagByFriendTag(FriendTagDTO friendTag);
    List<FullFriendTagDTO> getAllFullFriendTags();
}
