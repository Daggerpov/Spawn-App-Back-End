package com.danielagapov.spawn.Services.User;

import com.danielagapov.spawn.DTOs.UserDTO;
import com.danielagapov.spawn.Models.FriendRequests;

import java.util.List;
import java.util.UUID;

public interface IUserService {
    public List<UserDTO> getAllUsers();
    public UserDTO getUserById(UUID id);
    public List<UserDTO> getUsersByTagId(UUID tagId);
    public UserDTO saveUser(UserDTO user);
    public UserDTO replaceUser(UserDTO user, UUID id);
    public FriendRequests saveFriendRequest(FriendRequests friendRequest);
}
