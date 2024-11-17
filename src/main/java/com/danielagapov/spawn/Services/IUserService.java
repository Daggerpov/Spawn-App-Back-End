package com.danielagapov.spawn.Services;

import com.danielagapov.spawn.Models.User;

import java.util.List;

public interface IUserService {
    public List<User> getAllUsers();
    public User getUserById(Long id);
    public User saveUser(User user);
}
