package com.danielagapov.spawn.Services.FriendTag;

import java.util.List;
import com.danielagapov.spawn.Models.FriendTag.FriendTag;

public interface IFriendTagService {
    public List<FriendTag> getAllFriendTags();
    public FriendTag getFriendTagById(Long id);
    public List<FriendTag> getFriendTagsByTagId(Long tagId);
    public FriendTag saveFriendTag(FriendTag event);
}
