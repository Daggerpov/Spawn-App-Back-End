package com.danielagapov.spawn.Controllers;

import com.danielagapov.spawn.Models.User.User;
import com.danielagapov.spawn.Services.User.IUserService;
import org.springframework.web.bind.annotation.*;

@RestController()
@RequestMapping("api/v1/users")
public class UserController {
    private final IUserService userService;

    public UserController(IUserService userService) {
        this.userService = userService;
    }

    // full path: /api/v1/users
    @GetMapping
    public String getUsers() {
        return "These are the users: " + userService.getAllUsers();
    }

    // full path: /api/v1/users/{id}
    @GetMapping("{id}")
    public User getUser(@PathVariable Long id) {
        return userService.getUserById(id);
    }

    // full path: /api/v1/users/mock-endpoint
    @GetMapping("mock-endpoint")
    public String getMockEndpoint() {
        return "This is the mock endpoint for users. Everything is working with it.";
    }

    // full path: /api/v1/users
    @PostMapping
    public User createUser(@RequestBody User newUser) {
        return userService.saveUser(newUser);
    }
}
