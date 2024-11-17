package com.danielagapov.spawn.Controllers;

import com.danielagapov.spawn.Models.User;
import com.danielagapov.spawn.Services.IUserService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController()
@RequestMapping("/api/v1/users")
public class UserController {
    private final IUserService userService;

    public UserController(IUserService userService) {
        this.userService = userService;
    }

    @GetMapping("/")
    public String getUsers() {
        return "These are the users: " + userService.getAllUsers();
    }

    @GetMapping("/mock-endpoint")
    public String getMockEndpoint() {
        return "This is the mock endpoint. Everything is working with it.";
    }

    @GetMapping("/{id}")
    public User getUser(@PathVariable Long id) {
        return userService.getUserById(id);
    }

    // get user by tag
    @GetMapping("/tag/{id}")
    public List<User> getUsersByTagId(@PathVariable Long id) {
        return userService.getUsersByTagId(id);
    }

    @PostMapping("/")
    public User createUser(@RequestBody User newUser) {
        return userService.saveUser(newUser);
    }
}
