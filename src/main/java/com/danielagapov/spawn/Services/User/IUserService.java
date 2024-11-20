package com.danielagapov.spawn.Services.User;

import com.danielagapov.spawn.DTOs.UserDTO;

import java.util.List;

public interface IUserService {
    public List<UserDTO> getAllUsers();
    public UserDTO getUserById(Long id);
    public List<UserDTO> getUsersByTagId(Long tagId);
    public UserDTO saveUser(UserDTO user);
    public UserDTO replaceUser(UserDTO user, Long id);
}
