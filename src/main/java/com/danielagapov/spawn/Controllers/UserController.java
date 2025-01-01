package com.danielagapov.spawn.Controllers;

import com.danielagapov.spawn.DTOs.FriendRequestDTO;
import com.danielagapov.spawn.DTOs.UserDTO;
import com.danielagapov.spawn.Exceptions.Base.BaseNotFoundException;
import com.danielagapov.spawn.Services.FriendRequestService.IFriendRequestService;
import com.danielagapov.spawn.Services.User.IUserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController()
@RequestMapping("api/v1/users")
public class UserController {
    private final IUserService userService;
    private final IFriendRequestService friendRequestService;

    public UserController(IUserService userService, IFriendRequestService friendRequestService) {
        this.userService = userService;
        this.friendRequestService = friendRequestService;
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

    // full path: /api/v1/users/friendtag/{tagId}
    @GetMapping("/friendTag/{tagId}")
    public List<UserDTO> getUsersByFriendTag(@PathVariable UUID tagId) {
        return userService.getUsersByTagId(tagId);
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

    // full path: /api/v1/user/{id}
    @DeleteMapping("{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable UUID id) {
        try {
            boolean isDeleted = userService.deleteUserById(id);
            if (isDeleted) {
                return new ResponseEntity<>(HttpStatus.NO_CONTENT); // Success
            } else {
                return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR); // Deletion failed
            }
        } catch (BaseNotFoundException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND); // Resource not found
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR); // Unexpected error
        }
    }

    // full path: /api/v1/user/friend-request
    @PostMapping("friend-request")
    public FriendRequestDTO createFriendRequest(@RequestBody FriendRequestDTO friendReq) {
        return friendRequestService.saveFriendRequest(friendReq);
    }
}
