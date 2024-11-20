package com.danielagapov.spawn.Controllers;

import com.danielagapov.spawn.DTOs.UserDTO;
import com.danielagapov.spawn.Services.User.IUserService;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

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
    public UserDTO getUser(@PathVariable UUID id) {
        return userService.getUserById(id);
    }

    // full path: /api/v1/users/mock-endpoint
    @GetMapping("mock-endpoint")
    public String getMockEndpoint() {
        return "This is the mock endpoint for users. Everything is working with it.";
    }

    // full path: /api/v1/users
    @PostMapping
    public UserDTO createUser(@RequestBody UserDTO newUser) {
        return userService.saveUser(newUser);
    }

    // full path: /api/v1/user/{id}
    @PutMapping("{id}")
    public UserDTO replaceUser(@RequestBody UserDTO newUser, @PathVariable UUID id) {
        return userService.replaceUser(newUser, id);
    }
}
