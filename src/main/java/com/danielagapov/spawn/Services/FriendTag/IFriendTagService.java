package com.danielagapov.spawn.Services.FriendTag;

import com.danielagapov.spawn.DTOs.FriendTag.AbstractFriendTagDTO;
import com.danielagapov.spawn.DTOs.FriendTag.FriendTagDTO;
import com.danielagapov.spawn.DTOs.FriendTag.FullFriendTagDTO;
import com.danielagapov.spawn.DTOs.User.BaseUserDTO;

import java.util.List;
import java.util.Optional;
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

    /**
     * Optimized method to get all friend tags with their friends for a user in a single database query
     *
     * @param ownerId The ID of the user who owns the friend tags
     * @return List of FullFriendTagDTO objects with all friend data included
     */
    List<FullFriendTagDTO> getFullFriendTagsWithFriendsByOwnerId(UUID ownerId);

    // friend-related:
    Optional<FriendTagDTO> getPertainingFriendTagBetweenUsers(UUID ownerUserId, UUID friendUserId);

    void saveUserToFriendTag(UUID id, UUID userId);

    void removeUserFromFriendTag(UUID id, UUID userId);

    List<FullFriendTagDTO> getPertainingFullFriendTagsForFriend(UUID ownerUserId, UUID friendUserId);

    List<FriendTagDTO> getPertainingFriendTagsForFriend(UUID ownerUserId, UUID friendUserId);

    List<BaseUserDTO> getFriendsNotAddedToTag(UUID friendTagId);

    List<FullFriendTagDTO> getTagsNotAddedToFriend(UUID ownerUserId, UUID friendUserId);

    void saveUsersToFriendTag(UUID friendTagId, List<BaseUserDTO> friends);

    void bulkAddUsersToFriendTag(UUID friendTagId, List<BaseUserDTO> friends);

    void addFriendToFriendTags(List<UUID> friendTagIds, UUID friendUserId);

    // Get all friend IDs associated with a specific tag
    List<UUID> getFriendIdsByTagId(UUID tagId);

    // full helpers:
    List<FullFriendTagDTO> convertFriendTagsToFullFriendTags(List<FriendTagDTO> friendTags);

    FullFriendTagDTO getFullFriendTagById(UUID id);

    FullFriendTagDTO getFullFriendTagByFriendTag(FriendTagDTO friendTag);
}
