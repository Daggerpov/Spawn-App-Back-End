package com.danielagapov.spawn.Services.FriendTag;

import java.util.List;
import java.util.UUID;

import com.danielagapov.spawn.DTOs.FriendTagDTO;

public interface IFriendTagService {
    public List<FriendTagDTO> getAllFriendTags();
    public FriendTagDTO getFriendTagById(UUID id);
    public List<FriendTagDTO> getFriendTagsByTagId(UUID tagId);
    public FriendTagDTO saveFriendTag(FriendTagDTO friendTag);
    public FriendTagDTO replaceFriendTag(FriendTagDTO friendTag, UUID tagId);
}
