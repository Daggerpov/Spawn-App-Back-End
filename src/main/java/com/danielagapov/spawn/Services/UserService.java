package com.danielagapov.spawn.Services;

import com.danielagapov.spawn.Exceptions.User.UserNotFoundException;
import com.danielagapov.spawn.Exceptions.User.UserSaveException;
import com.danielagapov.spawn.Models.User;
import com.danielagapov.spawn.Repositories.IUserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class UserService implements IUserService{
    private final IUserRepository repository;

    @Autowired
    public UserService(IUserRepository repository) {
        this.repository = repository;
    }

    public List<User> getAllUsers() {
        try {
            return repository.findAll();
        } catch (DataAccessException e) {
            throw new RuntimeException("Error retrieving users", e);
        }
    }

    public User getUserById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new UserNotFoundException(id));
    }

    public User saveUser(User user) {
        try {
            return repository.save(user);
        } catch (DataAccessException e) {
            throw new UserSaveException("Failed to save user: " + e.getMessage());
        }
    }
}