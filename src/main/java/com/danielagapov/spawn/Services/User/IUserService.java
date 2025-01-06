package com.danielagapov.spawn.Services.User;

import com.danielagapov.spawn.DTOs.UserDTO;
import com.danielagapov.spawn.Models.FriendTag;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface IUserService {
    public List<UserDTO> getAllUsers();

    // CRUD operations:
    public UserDTO getUserById(UUID id);
    public UserDTO saveUser(UserDTO user);
    public UserDTO replaceUser(UserDTO user, UUID id);
    public boolean deleteUserById(UUID id);


    // For Friend Tags:

    public List<UserDTO> getUsersByTagId(UUID tagId);
    public Map<FriendTag, UserDTO> getOwnerMap();
    public Map<FriendTag, List<UserDTO>> getFriendsMap();
    public List<UserDTO> getFriendsByFriendTagId(UUID friendTagId);
    public List<UserDTO> getFriendsByUserId(UUID userId);

    public void removeFriend(UUID userId, UUID friendId);

    public List<UserDTO> getRecommendedFriends(UUID id);
}