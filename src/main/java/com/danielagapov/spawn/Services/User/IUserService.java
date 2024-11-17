package com.danielagapov.spawn.Services.User;

import com.danielagapov.spawn.Models.User;
import java.util.List;

public interface IUserService {
    public List<User> getAllUsers();
    public User getUserById(Long id);
    public List<User> getUsersByTagId(Long tagId);
    public User saveUser(User user);
}
