package com.danielagapov.spawn.Services.FriendTag;

import com.danielagapov.spawn.DTOs.FriendTag.AbstractFriendTagDTO;
import com.danielagapov.spawn.DTOs.FriendTag.FriendTagDTO;
import com.danielagapov.spawn.DTOs.FriendTag.FullFriendTagDTO;
import com.danielagapov.spawn.DTOs.User.FullUserDTO;

import java.util.List;
import java.util.UUID;

public interface IFriendTagService {
    List<FriendTagDTO> getAllFriendTags();

    // CRUD operations:
    FriendTagDTO getFriendTagById(UUID id);

    FriendTagDTO saveFriendTag(AbstractFriendTagDTO friendTag);

    FriendTagDTO replaceFriendTag(FriendTagDTO friendTag, UUID tagId);

    boolean deleteFriendTagById(UUID id);

    // owner-related:
    List<FriendTagDTO> getFriendTagsByOwnerId(UUID ownerId);

    List<UUID> getFriendTagIdsByOwnerUserId(UUID id);

    // friend-related:
    FriendTagDTO getPertainingFriendTagByUserIds(UUID ownerUserId, UUID friendUserId);

    void saveUserToFriendTag(UUID id, UUID userId);

    void removeUserFromFriendTag(UUID id, UUID userId);

    List<FullFriendTagDTO> getPertainingFullFriendTagsForFriend(UUID ownerUserId, UUID friendUserId);

    List<FriendTagDTO> getPertainingFriendTagsForFriend(UUID ownerUserId, UUID friendUserId);

    List<FullUserDTO> getFriendsNotAddedToTag(UUID friendTagId);

    List<FullFriendTagDTO> getTagsNotAddedToFriend(UUID ownerUserId, UUID friendUserId);

    void saveUsersToFriendTag(UUID friendTagId, List<FullUserDTO> friends);

    void addFriendToFriendTags(List<UUID> friendTagIds, UUID friendUserId);

    // full helpers:
    List<FullFriendTagDTO> convertFriendTagsToFullFriendTags(List<FriendTagDTO> friendTags);

    List<FullFriendTagDTO> getFullFriendTagsByOwnerId(UUID ownerId);

    FullFriendTagDTO getFullFriendTagById(UUID id);

    FullFriendTagDTO getFullFriendTagByFriendTag(FriendTagDTO friendTag);

    List<FullFriendTagDTO> getAllFullFriendTags();
}
