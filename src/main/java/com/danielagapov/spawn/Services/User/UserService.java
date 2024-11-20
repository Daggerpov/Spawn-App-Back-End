package com.danielagapov.spawn.Services.User;

import com.danielagapov.spawn.Exceptions.Base.BaseNotFoundException;
import com.danielagapov.spawn.Exceptions.Base.BaseSaveException;
import com.danielagapov.spawn.Exceptions.Base.BasesNotFoundException;
import com.danielagapov.spawn.Models.User.User;
import com.danielagapov.spawn.Repositories.IUserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserService implements IUserService {
    private final IUserRepository repository;

    public UserService(IUserRepository repository) {
        this.repository = repository;
    }

    public List<User> getAllUsers() {
        try {
            return repository.findAll();
        } catch (DataAccessException e) {
            throw new BasesNotFoundException();
        }
    }

    public User getUserById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new BaseNotFoundException(id));
    }

    public List<User> getUsersByTagId(Long tagId) {
        // TODO: change this logic later, once tags are setup.
        try {
            return repository.findAll();
        } catch (DataAccessException e) {
            throw new RuntimeException("Error retrieving users", e);
        }
    }

    public User saveUser(User user) {
        try {
            return repository.save(user);
        } catch (DataAccessException e) {
            throw new BaseSaveException("Failed to save user: " + e.getMessage());
        }
    }

    public User replaceUser(User newUser, Long id) {
        return repository.findById(id).map(user -> {
            user.setBio(newUser.getBio());
            user.setFirstName(newUser.getFirstName());
            user.setLastName(newUser.getLastName());
            user.setUsername(newUser.getUsername());
            return repository.save(user);
        }).orElseGet(() -> {
            return repository.save(newUser);
        });
    }
}