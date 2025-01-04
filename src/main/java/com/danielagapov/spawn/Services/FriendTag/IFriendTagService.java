package com.danielagapov.spawn.Services.FriendTag;

import com.danielagapov.spawn.DTOs.FriendTagDTO;

import java.util.List;
import java.util.UUID;

public interface IFriendTagService {
    public List<FriendTagDTO> getAllFriendTags();
    public FriendTagDTO getFriendTagById(UUID id);
    public List<FriendTagDTO> getFriendTagsByOwnerId(UUID ownerId);
    public FriendTagDTO saveFriendTag(FriendTagDTO friendTag);
    public FriendTagDTO replaceFriendTag(FriendTagDTO friendTag, UUID tagId);
    public boolean deleteFriendTagById(UUID id);
    public void saveUserToFriendTag(UUID id, UUID userId);
}
