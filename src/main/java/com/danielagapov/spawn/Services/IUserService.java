package com.danielagapov.spawn.Services;

import com.danielagapov.spawn.Models.User;

import java.util.List;
import java.util.Optional;

public interface IUserService {
    public List<User> getAllUsers();
    public Optional<User> getUserById(Long id);
    public User saveUser(User user);
}
