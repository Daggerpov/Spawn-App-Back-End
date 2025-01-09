package com.danielagapov.spawn.Services.User;

import com.danielagapov.spawn.DTOs.FriendRequestDTO;
import com.danielagapov.spawn.DTOs.UserDTO;

import java.util.List;
import java.util.UUID;

public interface IUserService {
    List<UserDTO> getAllUsers();
    UserDTO getUserById(UUID id);
    List<UserDTO> getUsersByTagId(UUID tagId);
    UserDTO saveUser(UserDTO user);
    UserDTO replaceUser(UserDTO user, UUID id);
    boolean deleteUserById(UUID id);
    FriendRequestDTO saveFriendRequest(FriendRequestDTO friendRequestDTO);
}
