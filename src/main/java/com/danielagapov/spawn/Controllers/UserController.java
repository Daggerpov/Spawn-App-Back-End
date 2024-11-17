package com.danielagapov.spawn.Controllers;

import com.danielagapov.spawn.Models.User;
import com.danielagapov.spawn.Repositories.IUserRepository;
import org.springframework.web.bind.annotation.*;

@RestController()
@RequestMapping("/api/v1/users")
public class UserController {
    private final IUserRepository repository;

    UserController (IUserRepository repository) {
        this.repository = repository;
    }

    @GetMapping("/")
    public String getUsers() {
        // TODO: fill in some mock users
        return "These are the users: ";
    }

    @GetMapping("/{id}")
    public User getUser() {
        User u = new User();
        u.setFirstName("Example First Name");
        return u;
    }

    @PostMapping("/")
    public User createUser(@RequestBody User newUser) {
        return repository.save(newUser);
    }
}
