package com.danielagapov.spawn.Services.UserFriendTag;


import com.danielagapov.spawn.DTOs.User.UserDTO;

import java.util.List;
import java.util.UUID;

public interface IUserFriendTagService {

    List<UserDTO> getUsersByTagId(UUID tagId);

    List<UserDTO> getFriendsByFriendTagId(UUID friendTagId);

    List<UUID> getFriendUserIdsByFriendTagId(UUID friendTagId);
}
