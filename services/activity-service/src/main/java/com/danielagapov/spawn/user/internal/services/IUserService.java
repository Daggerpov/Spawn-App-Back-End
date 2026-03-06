package com.danielagapov.spawn.user.internal.services;

import com.danielagapov.spawn.user.api.dto.BaseUserDTO;
import com.danielagapov.spawn.user.api.dto.UserDTO;
import com.danielagapov.spawn.user.api.dto.FriendUser.FullFriendUserDTO;
import com.danielagapov.spawn.user.internal.domain.User;

import java.util.List;
import java.util.UUID;

public interface IUserService {
    UserDTO getUserById(UUID id);
    User getUserEntityById(UUID id);
    BaseUserDTO getBaseUserById(UUID id);
    List<FullFriendUserDTO> getFullFriendUsersByUserId(UUID requestingUserId);
}
