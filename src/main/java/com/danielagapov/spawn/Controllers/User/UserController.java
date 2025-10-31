package com.danielagapov.spawn.Controllers.User;

import com.danielagapov.spawn.DTOs.User.*;
import com.danielagapov.spawn.DTOs.User.FriendUser.RecommendedFriendUserDTO;
import com.danielagapov.spawn.DTOs.User.Profile.UserProfileInfoDTO;
import com.danielagapov.spawn.DTOs.ContactCrossReferenceRequestDTO;
import com.danielagapov.spawn.DTOs.ContactCrossReferenceResponseDTO;
import com.danielagapov.spawn.Exceptions.Base.BaseNotFoundException;
import com.danielagapov.spawn.Exceptions.Logger.ILogger;
import com.danielagapov.spawn.Services.Auth.IAuthService;
import com.danielagapov.spawn.Services.BlockedUser.IBlockedUserService;
import com.danielagapov.spawn.Services.S3.IS3Service;
import com.danielagapov.spawn.Services.User.IUserService;
import com.danielagapov.spawn.Util.LoggingUtils;
import com.danielagapov.spawn.Util.SearchedUserResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController()
@RequestMapping("api/v1/users")
public final class UserController {
    private final IUserService userService;
    private final IS3Service s3Service;
    private final IBlockedUserService blockedUserService;
    private final ILogger logger;
    private final IAuthService authService;

    @Autowired
    public UserController(IUserService userService, IS3Service s3Service, ILogger logger, IAuthService authService, IBlockedUserService blockedUserService) {
        this.userService = userService;
        this.s3Service = s3Service;
        this.blockedUserService = blockedUserService;
        this.logger = logger;
        this.authService = authService;
    }

    // full path: /api/v1/users/friends/{id}
    @GetMapping("friends/{id}")
    public ResponseEntity<List<? extends AbstractUserDTO>> getUserFriends(@PathVariable UUID id) {
        if (id == null) {
            logger.error("Invalid parameter: user ID is null");
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
        try {
            List<? extends AbstractUserDTO> friends = userService.getFullFriendUsersByUserId(id);
            List<? extends AbstractUserDTO> filteredFriends = blockedUserService.filterOutBlockedUsers(friends, id);
            return new ResponseEntity<>(filteredFriends, HttpStatus.OK);
        } catch (BaseNotFoundException e) {
            logger.error("User not found for friends retrieval: " + LoggingUtils.formatUserIdInfo(id) + ": " + e.getMessage());
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            logger.error("Error getting friends for user: " + LoggingUtils.formatUserIdInfo(id) + ": " + e.getMessage());
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // full path: /api/v1/users/{id}
    @GetMapping("{id}")
    public ResponseEntity<BaseUserDTO> getUser(@PathVariable UUID id) {
        if (id == null) {
            logger.error("Invalid parameter: user ID is null");
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
        try {
            return new ResponseEntity<>(userService.getBaseUserById(id), HttpStatus.OK);
        } catch (BaseNotFoundException e) {
            logger.error("User not found: " + LoggingUtils.formatUserIdInfo(id) + ": " + e.getMessage());
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            logger.error("Error getting user: " + LoggingUtils.formatUserIdInfo(id) + ": " + e.getMessage());
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // full path: /api/v1/users/{id}
    @DeleteMapping("{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable UUID id) {
        if (id == null) {
            logger.error("Invalid parameter: user ID is null");
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
        try {
            userService.deleteUserById(id);
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        } catch (BaseNotFoundException e) {
            logger.error("User not found for deletion: " + LoggingUtils.formatUserIdInfo(id) + ": " + e.getMessage());
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            logger.error("Error deleting user: " + LoggingUtils.formatUserIdInfo(id) + ": " + e.getMessage());
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // full path: /api/v1/users/recommended-friends/{id}
    @GetMapping("recommended-friends/{id}")
    public ResponseEntity<List<RecommendedFriendUserDTO>> getRecommendedFriends(@PathVariable UUID id) {
        if (id == null) {
            logger.error("Invalid parameter: user ID is null");
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
        try {
            List<RecommendedFriendUserDTO> recommendations = userService.getLimitedRecommendedFriendsForUserId(id);
            List<RecommendedFriendUserDTO> filteredRecommendations = blockedUserService.filterOutBlockedUsers(recommendations, id);
            return new ResponseEntity<>(filteredRecommendations, HttpStatus.OK);
        } catch (BaseNotFoundException e) {
            logger.error("User not found for recommended friends: " + LoggingUtils.formatUserIdInfo(id) + ": " + e.getMessage());
            return new ResponseEntity<List<RecommendedFriendUserDTO>>(HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            logger.error("Error getting recommended friends for user: " + LoggingUtils.formatUserIdInfo(id) + ": " + e.getMessage());
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // full path: /api/v1/users/update-pfp/{id}
    @PatchMapping("update-pfp/{id}")
    public ResponseEntity<UserDTO> updatePfp(@PathVariable UUID id, @RequestBody byte[] file) {
        if (id == null) {
            logger.error("Invalid parameter: user ID is null");
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
        try {
            UserDTO updatedUser = s3Service.updateProfilePicture(file, id);
            return new ResponseEntity<>(updatedUser, HttpStatus.OK);
        } catch (Exception e) {
            logger.error("Error updating profile picture for user " + LoggingUtils.formatUserIdInfo(id) + ": " + e.getMessage());
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // full path: /api/v1/users/{id}/optional-details
    @PostMapping("{id}/optional-details")
    public ResponseEntity<?> setOptionalDetails(@PathVariable UUID id, @RequestBody OptionalDetailsDTO optionalDetailsDTO) {
        if (id == null) {
            logger.error("Invalid parameter: user ID is null");
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
        try {
            BaseUserDTO baseUserDTO = userService.setOptionalDetails(id, optionalDetailsDTO);
            return new ResponseEntity<>(baseUserDTO, HttpStatus.OK);
        } catch (Exception e) {
            logger.error("Error retrieving default profile picture: " + e.getMessage());
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // full path: /api/v1/users/default-pfp
    @GetMapping("default-pfp")
    public ResponseEntity<String> getDefaultProfilePicture() {
        try {
            String defaultPfp = s3Service.getDefaultProfilePicture();
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
            String url = s3Service.putObject(file);
            return url;
        } catch (Exception e) {
            logger.error("Error in test S3 upload: " + e.getMessage());
            return e.getMessage();
        }
    }

    // full path: /api/v1/users/update/{id}
    @PatchMapping("update/{id}")
    public ResponseEntity<BaseUserDTO> updateUser(@PathVariable UUID id, @RequestBody UserUpdateDTO updateDTO) {
        if (id == null) {
            logger.error("Invalid parameter: user ID is null");
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
        try {
            BaseUserDTO updatedUser = userService.updateUser(
                    id,
                    updateDTO
            );
            if (updateDTO.getUsername() != null) {
                HttpHeaders headers = authService.makeHeadersForTokens(updatedUser.getUsername());
                return ResponseEntity.ok().headers(headers).body(updatedUser);
            }
            return new ResponseEntity<>(updatedUser, HttpStatus.OK);
        } catch (BaseNotFoundException e) {
            logger.error("User not found for update: " + LoggingUtils.formatUserIdInfo(id) + ", entity type: " + e.entityType);
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            logger.error("Error updating user " + LoggingUtils.formatUserIdInfo(id) + ": " + e.getMessage());
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // full path: /api/v1/users/filtered/{requestingUserId}?query=searchQuery
    @GetMapping("filtered/{requestingUserId}")
    public ResponseEntity<SearchedUserResult> getRecommendedFriendsBySearch(
            @PathVariable UUID requestingUserId, 
            @RequestParam(required = false, defaultValue = "") String searchQuery) {
        try {
            SearchedUserResult result = userService.getRecommendedFriendsBySearch(requestingUserId, searchQuery);
            // Apply blocked user filtering to the users in the result
            List<SearchResultUserDTO> filteredUsers = blockedUserService.filterOutBlockedUsers(result.getUsers(), requestingUserId);
            SearchedUserResult filteredResult = new SearchedUserResult(filteredUsers);
            return new ResponseEntity<>(filteredResult, HttpStatus.OK);
        } catch (BaseNotFoundException e) {
            logger.error("User not found for recommended friends search: " + LoggingUtils.formatUserIdInfo(requestingUserId) + ": " + e.getMessage());
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            logger.error("Error getting recommended friends by search for user: " + LoggingUtils.formatUserIdInfo(requestingUserId) + ": " + e.getMessage());
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // full path: /api/v1/users/search
    @GetMapping("search")
    public ResponseEntity<List<BaseUserDTO>> searchForUsers(
            @RequestParam(required = false, defaultValue = "") String searchQuery,
            @RequestParam(required = false) UUID requestingUserId) {
        try {
            List<BaseUserDTO> users = userService.searchByQuery(searchQuery, requestingUserId);
            // Apply blocked user filtering if requestingUserId is provided
            if (requestingUserId != null) {
                users = blockedUserService.filterOutBlockedUsers(users, requestingUserId);
            }
            return new ResponseEntity<>(users, HttpStatus.OK);
        } catch (Exception e) {
            logger.error("Error searching for users with query: " + searchQuery + ": " + e.getMessage());
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // full path: /api/v1/users/{userId}/recent-users
    @GetMapping("{userId}/recent-users")
    public ResponseEntity<List<RecentlySpawnedUserDTO>> getRecentlySpawnedWithUsers(@PathVariable UUID userId) {
        try {
            List<RecentlySpawnedUserDTO> recentUsers = userService.getRecentlySpawnedWithUsers(userId);
            List<RecentlySpawnedUserDTO> filteredRecentUsers = blockedUserService.filterOutBlockedUsers(recentUsers, userId);
            return new ResponseEntity<>(filteredRecentUsers, HttpStatus.OK);
        } catch (Exception e) {
            logger.error("Error getting recently spawned with users for user: " + LoggingUtils.formatUserIdInfo(userId) + ": " + e.getMessage());
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // full path: /api/v1/users/{userId}/is-friend/{potentialFriendId}
    @GetMapping("{userId}/is-friend/{potentialFriendId}")
    public ResponseEntity<Boolean> isUserFriendOfUser(
            @PathVariable UUID userId,
            @PathVariable UUID potentialFriendId) {
        if (userId == null || potentialFriendId == null) {
            logger.error("Invalid parameters: userId or potentialFriendId is null");
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
        
        try {
            boolean isFriend = userService.isUserFriendOfUser(userId, potentialFriendId);
            return new ResponseEntity<>(isFriend, HttpStatus.OK);
        } catch (BaseNotFoundException e) {
            logger.error("User not found for friend check: " + e.getMessage());
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            logger.error("Error checking if user " + LoggingUtils.formatUserIdInfo(userId) + " is friend of user " + LoggingUtils.formatUserIdInfo(potentialFriendId) + ": " + e.getMessage());
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // full path: /api/v1/users/{userId}/profile-info
    @GetMapping("{userId}/profile-info")
    public ResponseEntity<UserProfileInfoDTO> getUserProfileInfo(@PathVariable UUID userId) {
        try {
            UserProfileInfoDTO profileInfo = userService.getUserProfileInfo(userId);
            return ResponseEntity.ok(profileInfo);
        } catch (Exception e) {
            logger.error("Error getting user profile info for user: " + LoggingUtils.formatUserIdInfo(userId) + ": " + e.getMessage());
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // full path: /api/v1/users/contacts/cross-reference
    @PostMapping("contacts/cross-reference")
    public ResponseEntity<ContactCrossReferenceResponseDTO> crossReferenceContacts(
            @RequestBody ContactCrossReferenceRequestDTO request) {
        if (request.getPhoneNumbers() == null || request.getPhoneNumbers().isEmpty()) {
            logger.error("Invalid request: phone numbers list is empty or null");
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
        if (request.getRequestingUserId() == null) {
            logger.error("Invalid request: requesting user ID is null");
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
        
        try {
            List<BaseUserDTO> matchingUsers = userService.findUsersByPhoneNumbers(
                request.getPhoneNumbers(), 
                request.getRequestingUserId()
            );
            
            // Filter out blocked users
            List<BaseUserDTO> filteredUsers = blockedUserService.filterOutBlockedUsers(
                matchingUsers, 
                request.getRequestingUserId()
            );
            
            ContactCrossReferenceResponseDTO response = new ContactCrossReferenceResponseDTO(filteredUsers);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (BaseNotFoundException e) {
            logger.error("User not found for contact cross-reference: " + LoggingUtils.formatUserIdInfo(request.getRequestingUserId()) + ": " + e.getMessage());
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            logger.error("Error cross-referencing contacts for user: " + LoggingUtils.formatUserIdInfo(request.getRequestingUserId()) + ": " + e.getMessage());
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
