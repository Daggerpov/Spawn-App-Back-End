package com.danielagapov.spawn.Controllers;

import com.danielagapov.spawn.DTOs.User.AbstractUserDTO;
import com.danielagapov.spawn.DTOs.User.FriendUser.RecommendedFriendUserDTO;
import com.danielagapov.spawn.DTOs.User.UserDTO;
import com.danielagapov.spawn.Exceptions.Base.BaseNotFoundException;
import com.danielagapov.spawn.Services.S3.IS3Service;
import com.danielagapov.spawn.Services.User.IUserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashSet;
import java.util.List;
import java.util.UUID;

@RestController()
@RequestMapping("api/v1/users")
public class UserController {
    private final IUserService userService;
    private final IS3Service s3Service;

    public UserController(IUserService userService, IS3Service s3Service) {
        this.userService = userService;
        this.s3Service = s3Service;
    }

    // full path: /api/v1/users?full=full
    @GetMapping
    public ResponseEntity<List<? extends AbstractUserDTO>> getUsers(@RequestParam(value = "full", required = false) boolean full) {
        try {
            if (full) {
                return new ResponseEntity<>(userService.convertUsersToFullUsers(userService.getAllUsers(), new HashSet<>()), HttpStatus.OK);
            } else {
                return new ResponseEntity<>(userService.getAllUsers(), HttpStatus.OK);
            }
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // full path: /api/v1/users/{id}?full=full
    @GetMapping("{id}")
    public ResponseEntity<AbstractUserDTO> getUser(@PathVariable UUID id, @RequestParam(value = "full", required = false) boolean full) {
        try {
            if (full) {
                return new ResponseEntity<>(userService.getFullUserById(id), HttpStatus.OK);
            } else {
                return new ResponseEntity<>(userService.getUserById(id), HttpStatus.OK);
            }
        } catch (BaseNotFoundException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // full path: /api/v1/users/{id}/friends
    @GetMapping("{id}/friends")
    public ResponseEntity<List<? extends AbstractUserDTO>> getUserFriends(@PathVariable UUID id) {
        try {
            return new ResponseEntity<>(userService.getFullFriendUsersByUserId(id), HttpStatus.OK);
        } catch (BaseNotFoundException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // full path: /api/v1/users/mock-endpoint
    @GetMapping("mock-endpoint")
    public ResponseEntity<String> getMockEndpoint() {
        try {
            return new ResponseEntity<>("This is the mock endpoint for users. Everything is working with it.", HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // full path: /api/v1/users/friendTag/{tagId}?full=full
    @GetMapping("friendTag/{tagId}")
    public ResponseEntity<List<? extends AbstractUserDTO>> getUsersByFriendTag(@PathVariable UUID tagId, @RequestParam(value = "full", required = false) boolean full) {
        try {
            if (full) {
                return new ResponseEntity<>(userService.convertUsersToFullUsers(userService.getUsersByTagId(tagId), new HashSet<>()), HttpStatus.OK);
            } else {
                return new ResponseEntity<>(userService.getUsersByTagId(tagId), HttpStatus.OK);
            }
        } catch (BaseNotFoundException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // full path: /api/v1/users
    @PostMapping
    public ResponseEntity<UserDTO> createUser(@RequestParam("user") UserDTO newUser, @RequestParam("pfp") byte[] file) {
        try {
            return new ResponseEntity<>(userService.saveUserWithProfilePicture(newUser, file), HttpStatus.CREATED);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // full path: /api/v1/users/{id}
    @PutMapping("{id}")
    public ResponseEntity<UserDTO> replaceUser(@RequestBody UserDTO newUser, @PathVariable UUID id) {
        try {
            return new ResponseEntity<>(userService.replaceUser(newUser, id), HttpStatus.OK);
        } catch (BaseNotFoundException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // full path: /api/v1/users/{id}
    @DeleteMapping("{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable UUID id) {
        try {
            boolean isDeleted = userService.deleteUserById(id);
            if (isDeleted) {
                return new ResponseEntity<>(HttpStatus.NO_CONTENT);
            } else {
                return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
            }
        } catch (BaseNotFoundException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // full path: /api/v1/users/{id}/removeFriend?friendId=friendId
    @DeleteMapping("{id}/removeFriend")
    public ResponseEntity<Void> deleteFriendFromUser(@PathVariable UUID id, @RequestParam UUID friendId) {
        try {
            userService.removeFriend(id, friendId);
            return new ResponseEntity<>(HttpStatus.OK);
        } catch (BaseNotFoundException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // full path: /api/v1/users/{id}/recommended-friends
    @GetMapping("{id}/recommended-friends")
    public ResponseEntity<List<RecommendedFriendUserDTO>> getRecommendedFriends(@PathVariable UUID id) {
        try {
            return new ResponseEntity<>(userService.getRecommendedFriendsForUserId(id), HttpStatus.OK);
        } catch (BaseNotFoundException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // full path: /api/v1/users/{id}/update-pfp
    @PatchMapping("{id}/update-pfp")
    public ResponseEntity<UserDTO> updatePfp(@PathVariable UUID id, @RequestBody byte[] file) {
        try {
            return new ResponseEntity<>(s3Service.updateProfilePicture(file, id), HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // full path: /api/v1/users/default-pfp
    @GetMapping("default-pfp")
    public ResponseEntity<String> getDefaultProfilePicture() {
        try {
            return new ResponseEntity<>(s3Service.getDefaultProfilePicture(), HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Deprecated(since = "For testing purposes")
    @PostMapping("s3/test-s3")
    public String testPostS3(@RequestBody byte[] file) {
        try {
            return s3Service.putObject(file);
        } catch (Exception e) {
            return e.getMessage();
        }
    }
}
