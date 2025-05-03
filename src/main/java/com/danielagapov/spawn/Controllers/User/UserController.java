package com.danielagapov.spawn.Controllers.User;

import com.danielagapov.spawn.DTOs.User.AbstractUserDTO;
import com.danielagapov.spawn.DTOs.User.BaseUserDTO;
import com.danielagapov.spawn.DTOs.User.FriendUser.RecommendedFriendUserDTO;
import com.danielagapov.spawn.DTOs.User.UserDTO;
import com.danielagapov.spawn.DTOs.User.UserUpdateDTO;
import com.danielagapov.spawn.Exceptions.Base.BaseNotFoundException;
import com.danielagapov.spawn.Exceptions.Logger.ILogger;
import com.danielagapov.spawn.Services.S3.IS3Service;
import com.danielagapov.spawn.Services.User.IUserService;
import com.danielagapov.spawn.Util.SearchedUserResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController()
@RequestMapping("api/v1/users")
public class UserController {
    private final IUserService userService;
    private final IS3Service s3Service;
    private final ILogger logger;

    @Autowired
    public UserController(IUserService userService, IS3Service s3Service, ILogger logger) {
        this.userService = userService;
        this.s3Service = s3Service;
        this.logger = logger;
    }

    // full path: /api/v1/users/friends/{id}
    @GetMapping("friends/{id}")
    public ResponseEntity<List<? extends AbstractUserDTO>> getUserFriends(@PathVariable UUID id) {
        if (id == null) return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        try {
            return new ResponseEntity<>(userService.getFullFriendUsersByUserId(id), HttpStatus.OK);
        } catch (BaseNotFoundException e) {
            return new ResponseEntity<List<? extends AbstractUserDTO>>(HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // full path: /api/v1/users/{id}
    @DeleteMapping("{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable UUID id) {
        if (id == null) return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        try {
            userService.deleteUserById(id);
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
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
            return new ResponseEntity<List<RecommendedFriendUserDTO>>(HttpStatus.NOT_FOUND);
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
            logger.error("User not found for update: " + id + ", entity type: " + e.entityType);
            return new ResponseEntity<BaseUserDTO>(HttpStatus.NOT_FOUND);
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
            return new ResponseEntity<SearchedUserResult>(HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // full path: /api/v1/users/search
    @GetMapping("search")
    public ResponseEntity<List<BaseUserDTO>> searchForUsers(@RequestBody String searchQuery) {
        try {
            return new ResponseEntity<>(userService.searchByQuery(searchQuery), HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // full path: /api/v1/users/{userId}/recent-users
    @GetMapping("{userId}/recent-users")
    public ResponseEntity<List<BaseUserDTO>> getRecentlySpawnedWithUsers(@PathVariable UUID userId) {
        try {
            return new ResponseEntity<>(userService.getRecentlySpawnedWithUsers(userId), HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
