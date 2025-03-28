package com.danielagapov.spawn.Controllers;

import com.danielagapov.spawn.DTOs.User.AbstractUserDTO;
import com.danielagapov.spawn.DTOs.User.BaseUserDTO;
import com.danielagapov.spawn.DTOs.User.FriendUser.RecommendedFriendUserDTO;
import com.danielagapov.spawn.DTOs.User.UserDTO;
import com.danielagapov.spawn.DTOs.User.UserUpdateDTO;
import com.danielagapov.spawn.Exceptions.Base.BaseNotFoundException;
import com.danielagapov.spawn.Exceptions.Logger.ILogger;
import com.danielagapov.spawn.Services.FriendTag.IFriendTagService;
import com.danielagapov.spawn.Services.S3.IS3Service;
import com.danielagapov.spawn.Services.User.IUserService;
import com.danielagapov.spawn.Util.SearchedUserResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController()
@RequestMapping("api/v1/users")
public class UserController {
    private final IUserService userService;
    private final IS3Service s3Service;
    private final ILogger logger;
    private final IFriendTagService friendTagService;

    @Autowired
    public UserController(IUserService userService, IS3Service s3Service, ILogger logger, IFriendTagService friendTagService) {
        this.userService = userService;
        this.s3Service = s3Service;
        this.logger = logger;
        this.friendTagService = friendTagService;
    }

    // TL;DR: Don't remove this endpoint; it may become useful.
    @Deprecated(since = "Not being used on mobile currently.")
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

    // TL;DR: Don't remove this endpoint; it may become useful.
    @Deprecated(since = "Not being used on mobile currently.")
    // full path: /api/v1/users/{id}?full=full
    @GetMapping("{id}")
    public ResponseEntity<Object> getUser(@PathVariable UUID id, @RequestParam(value = "full", required = false) boolean full) {
        if (id == null) return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        try {
            if (full) {
                return new ResponseEntity<>(userService.getUserWithFriendsAndTags(id), HttpStatus.OK);
            } else {
                return new ResponseEntity<>(userService.getUserById(id), HttpStatus.OK);
            }
        } catch (BaseNotFoundException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // full path: /api/v1/users/friends/{id}
    @GetMapping("friends/{id}")
    public ResponseEntity<List<? extends AbstractUserDTO>> getUserFriends(@PathVariable UUID id) {
        if (id == null) return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        try {
            return new ResponseEntity<>(userService.getFullFriendUsersByUserId(id), HttpStatus.OK);
        } catch (BaseNotFoundException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // TL;DR: Don't remove this endpoint; it may become useful.
    @Deprecated(since = "Not being used on mobile currently.")
    // full path: /api/v1/users/friendTag/{tagId}?full=full
    @GetMapping("friendTag/{tagId}")
    public ResponseEntity<Object> getUsersByFriendTag(@PathVariable UUID tagId, @RequestParam(value = "full", required = false) boolean full) {
        if (tagId == null) return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        try {
            if (full) {
                List<UserDTO> users = userService.getUsersByTagId(tagId);
                List<Map<String, Object>> enrichedUsers = new ArrayList<>();
                
                for (UserDTO user : users) {
                    Map<String, Object> enrichedUser = new HashMap<>();
                    enrichedUser.put("user", user);
                    enrichedUser.put("friends", userService.getFriendsByUserId(user.getId()));
                    enrichedUser.put("friendTags", friendTagService.getFriendTagsByOwnerId(user.getId()));
                    enrichedUsers.add(enrichedUser);
                }
                
                return new ResponseEntity<>(enrichedUsers, HttpStatus.OK);
            } else {
                return new ResponseEntity<>(userService.getUsersByTagId(tagId), HttpStatus.OK);
            }
        } catch (BaseNotFoundException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // TL;DR: Don't remove this endpoint; it may become useful.
    @Deprecated(since = "Not being used on mobile currently.")
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
    @DeleteMapping("{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable UUID id) {
        if (id == null) return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
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

    // full path: /api/v1/users/recommended-friends/{id}
    @GetMapping("recommended-friends/{id}")
    public ResponseEntity<List<RecommendedFriendUserDTO>> getRecommendedFriends(@PathVariable UUID id) {
        if (id == null) return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        try {
            return new ResponseEntity<>(userService.getLimitedRecommendedFriendsForUserId(id), HttpStatus.OK);
        } catch (BaseNotFoundException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // full path: /api/v1/users/update-pfp/{id}
    @PatchMapping("update-pfp/{id}")
    public ResponseEntity<UserDTO> updatePfp(@PathVariable UUID id, @RequestBody byte[] file) {
        if (id == null) return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        try {
            logger.info("Received request to update profile picture for user " + id + " (file size: " + file.length + " bytes)");
            UserDTO updatedUser = s3Service.updateProfilePicture(file, id);
            logger.info("Successfully updated profile picture for user " + id + ": " + updatedUser.getProfilePicture());
            return new ResponseEntity<>(updatedUser, HttpStatus.OK);
        } catch (Exception e) {
            logger.error("Error updating profile picture for user " + id + ": " + e.getMessage());
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // full path: /api/v1/users/default-pfp
    @GetMapping("default-pfp")
    public ResponseEntity<String> getDefaultProfilePicture() {
        try {
            logger.info("Received request for default profile picture");
            String defaultPfp = s3Service.getDefaultProfilePicture();
            logger.info("Returning default profile picture: " + defaultPfp);
            return new ResponseEntity<>(defaultPfp, HttpStatus.OK);
        } catch (Exception e) {
            logger.error("Error retrieving default profile picture: " + e.getMessage());
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Deprecated(since = "For testing purposes")
    @PostMapping("s3/test-s3")
    public String testPostS3(@RequestBody byte[] file) {
        try {
            logger.info("Received test S3 upload request (file size: " + file.length + " bytes)");
            String url = s3Service.putObject(file);
            logger.info("Successfully uploaded test file to S3: " + url);
            return url;
        } catch (Exception e) {
            logger.error("Error in test S3 upload: " + e.getMessage());
            return e.getMessage();
        }
    }

    // full path: /api/v1/users/update/{id}
    @PatchMapping("update/{id}")
    public ResponseEntity<BaseUserDTO> updateUser(@PathVariable UUID id, @RequestBody UserUpdateDTO updateDTO) {
        if (id == null) return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        try {
            logger.info("Received request to update user " + id + ": " + 
                        "username=" + updateDTO.getUsername() + ", " +
                        "firstName=" + updateDTO.getFirstName() + ", " +
                        "lastName=" + updateDTO.getLastName() + ", " +
                        "bio=" + updateDTO.getBio());
            
            BaseUserDTO updatedUser = userService.updateUser(
                id, 
                updateDTO.getBio(), 
                updateDTO.getUsername(), 
                updateDTO.getFirstName(), 
                updateDTO.getLastName()
            );
            
            logger.info("Successfully updated user " + id + ": " +
                        "username=" + updatedUser.getUsername() + ", " +
                        "firstName=" + updatedUser.getFirstName() + ", " +
                        "lastName=" + updatedUser.getLastName() + ", " +
                        "bio=" + updatedUser.getBio());
            
            return new ResponseEntity<>(updatedUser, HttpStatus.OK);
        } catch (BaseNotFoundException e) {
            logger.error("User not found for update: " + id);
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            logger.error("Error updating user " + id + ": " + e.getMessage());
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // full path: /api/v1/users/filtered/{requestingUserId}?query=searchQuery
    @GetMapping("filtered/{requestingUserId}")
    public ResponseEntity<SearchedUserResult> getRecommendedFriendsBySearch(@PathVariable UUID requestingUserId, @RequestParam String searchQuery) {
        try {
            return new ResponseEntity<>(userService.getRecommendedFriendsBySearch(requestingUserId, searchQuery), HttpStatus.OK);
        } catch (BaseNotFoundException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
