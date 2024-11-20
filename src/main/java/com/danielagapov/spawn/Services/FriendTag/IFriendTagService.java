package com.danielagapov.spawn.Services.FriendTag;

import java.util.List;

import com.danielagapov.spawn.DTOs.FriendTagDTO;

public interface IFriendTagService {
    public List<FriendTagDTO> getAllFriendTags();
    public FriendTagDTO getFriendTagById(Long id);
    public List<FriendTagDTO> getFriendTagsByTagId(Long tagId);
    public FriendTagDTO saveFriendTag(FriendTagDTO friendTag);
    public FriendTagDTO replaceFriendTag(FriendTagDTO friendTag, Long tagId);
}
