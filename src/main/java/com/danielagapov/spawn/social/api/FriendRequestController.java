package com.danielagapov.spawn.social.api;

import com.danielagapov.spawn.social.api.dto.CreateFriendRequestDTO;
import com.danielagapov.spawn.social.api.dto.FetchFriendRequestDTO;
import com.danielagapov.spawn.social.api.dto.FetchSentFriendRequestDTO;
import com.danielagapov.spawn.shared.util.EntityType;
import com.danielagapov.spawn.shared.util.FriendRequestAction;
import com.danielagapov.spawn.shared.exceptions.BaseNotFoundException;
import com.danielagapov.spawn.shared.exceptions.BasesNotFoundException;
import com.danielagapov.spawn.shared.exceptions.ILogger;
import com.danielagapov.spawn.social.internal.services.IBlockedUserService;
import com.danielagapov.spawn.social.internal.services.IFriendRequestService;
import com.danielagapov.spawn.shared.util.LoggingUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("api/v1/friend-requests")
public final class FriendRequestController {

    private final IFriendRequestService friendRequestService;
    private final IBlockedUserService blockedUserService;
    private final ILogger logger;

    public FriendRequestController(IFriendRequestService friendRequestService, IBlockedUserService blockedUserService, ILogger logger) {
        this.friendRequestService = friendRequestService;
        this.blockedUserService = blockedUserService;
        this.logger = logger;
    }

    // returns ResponseEntity with list of FetchFriendRequestDTO (can be empty)
    //  or not found entity type (user)
    // full path: /api/v1/friend-requests/incoming/{userId}
    @GetMapping("incoming/{userId}")
    public ResponseEntity<?> getIncomingFriendRequestsByUserId(@PathVariable UUID userId) {
        if (userId == null) {
            logger.error("Invalid parameter: userId is null");
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
        try {
            List<FetchFriendRequestDTO> friendRequests = friendRequestService.getIncomingFetchFriendRequestsByUserId(userId);
            List<FetchFriendRequestDTO> filteredRequests = blockedUserService.filterOutBlockedUsers(friendRequests, userId);
            return new ResponseEntity<>(filteredRequests, HttpStatus.OK);
        } catch (BaseNotFoundException e) {
            logger.error("User not found for incoming friend requests: " + LoggingUtils.formatUserIdInfo(userId) + ": " + e.getMessage());
            return new ResponseEntity<>(e.entityType, HttpStatus.NOT_FOUND);
        } catch (BasesNotFoundException e) {
            if (e.entityType == EntityType.FriendRequest) {
                return new ResponseEntity<>(new ArrayList<>(), HttpStatus.OK);
            } else {
                logger.error("Bad request for incoming friend requests: " + e.getMessage());
                return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
            }
        } catch (Exception e) {
            logger.error("Error getting incoming friend requests for user: " + LoggingUtils.formatUserIdInfo(userId) + ": " + e.getMessage());
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // returns ResponseEntity with list of FetchFriendRequestDTO (can be empty)
    //  or not found entity type (user)
    // full path: /api/v1/friend-requests/sent/{userId}
    @GetMapping("sent/{userId}")
    public ResponseEntity<List<FetchSentFriendRequestDTO>> getSentFriendRequestsByUserId(@PathVariable UUID userId) {
        if (userId == null) {
            logger.error("Invalid parameter: userId is null");
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
        try {
            List<FetchSentFriendRequestDTO> friendRequests = friendRequestService.getSentFetchFriendRequestsByUserId(userId);
            List<FetchSentFriendRequestDTO> filteredRequests = blockedUserService.filterOutBlockedUsers(friendRequests, userId);
            return new ResponseEntity<>(filteredRequests, HttpStatus.OK);
        } catch (BaseNotFoundException e) {
            logger.error("User not found for sent friend requests: " + LoggingUtils.formatUserIdInfo(userId) + ": " + e.getMessage());
            return new ResponseEntity<>(new ArrayList<>(), HttpStatus.NOT_FOUND);
        } catch (BasesNotFoundException e) {
            if (e.entityType == EntityType.FriendRequest) {
                return new ResponseEntity<>(new ArrayList<>(), HttpStatus.OK);
            } else {
                logger.error("Bad request for sent friend requests: " + e.getMessage());
                return new ResponseEntity<>(new ArrayList<>(), HttpStatus.BAD_REQUEST);
            }
        } catch (Exception e) {
            logger.error("Error getting sent friend requests for user: " + LoggingUtils.formatUserIdInfo(userId) + ": " + e.getMessage());
            return new ResponseEntity<>(new ArrayList<>(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // full path: /api/v1/friend-requests
    @PostMapping
    public ResponseEntity<CreateFriendRequestDTO> createFriendRequest(@RequestBody CreateFriendRequestDTO friendRequest) {
        try {
            CreateFriendRequestDTO createdRequest = friendRequestService.saveFriendRequest(friendRequest);
            return new ResponseEntity<>(createdRequest, HttpStatus.CREATED);
        } catch (Exception e) {
            logger.error("Error creating friend request: " + e.getMessage());
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // returns ResponseEntity with void
    //  or not found entity type (friendRequestId)
    // full path: /api/v1/friend-requests/{friendRequestId}?friendRequestAction={accept/reject}
    @PutMapping("{friendRequestId}")
    public ResponseEntity<?> friendRequestAction(@PathVariable UUID friendRequestId, @RequestParam FriendRequestAction friendRequestAction) {
        if (friendRequestId == null) {
            logger.error("Invalid parameter: friendRequestId is null");
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
        
        logger.info("Processing friend request action: " + friendRequestAction + " for request ID: " + friendRequestId);
        
        try {
            if (friendRequestAction == FriendRequestAction.accept) {
                friendRequestService.acceptFriendRequest(friendRequestId);
                logger.info("Successfully accepted friend request: " + friendRequestId);
            } else if (friendRequestAction == FriendRequestAction.reject) {
                friendRequestService.deleteFriendRequest(friendRequestId);
                logger.info("Successfully rejected friend request: " + friendRequestId);
            } else {
                // deal with null/invalid argument for `friendRequestAction`
                logger.error("Invalid friend request action: " + friendRequestAction + " for request: " + friendRequestId);
                return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
            }

            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        } catch (BaseNotFoundException e) {
            logger.warn("Friend request not found (may have been already processed): " + friendRequestId + ": " + e.getMessage());
            // Return success instead of 404 to avoid client-side errors when friend request was already processed
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        } catch (Exception e) {
            logger.error("Error processing friend request action: " + friendRequestAction + " for request: " + friendRequestId + ": " + e.getMessage());
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // full path: /api/v1/friend-requests/{friendRequestId}
    // Allows clients to cancel/delete a friend request using HTTP DELETE semantics
    @DeleteMapping("{friendRequestId}")
    public ResponseEntity<?> deleteFriendRequest(@PathVariable UUID friendRequestId) {
        if (friendRequestId == null) {
            logger.error("Invalid parameter: friendRequestId is null");
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }

        try {
            friendRequestService.deleteFriendRequest(friendRequestId);
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        } catch (BaseNotFoundException e) {
            // If the request was already removed, treat as success to keep client idempotent
            logger.warn("Friend request not found (may have been already deleted): " + friendRequestId + ": " + e.getMessage());
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        } catch (Exception e) {
            logger.error("Error deleting friend request: " + friendRequestId + ": " + e.getMessage());
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
