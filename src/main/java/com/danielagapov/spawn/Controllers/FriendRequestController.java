package com.danielagapov.spawn.Controllers;

import com.danielagapov.spawn.DTOs.FriendRequest.CreateFriendRequestDTO;
import com.danielagapov.spawn.DTOs.FriendRequest.FetchFriendRequestDTO;
import com.danielagapov.spawn.Enums.EntityType;
import com.danielagapov.spawn.Enums.FriendRequestAction;
import com.danielagapov.spawn.Exceptions.Base.BaseNotFoundException;
import com.danielagapov.spawn.Exceptions.Base.BasesNotFoundException;
import com.danielagapov.spawn.Exceptions.Logger.ILogger;
import com.danielagapov.spawn.Services.FriendRequest.IFriendRequestService;
import com.danielagapov.spawn.Util.LoggingUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("api/v1/friend-requests")
public class FriendRequestController {

    private final IFriendRequestService friendRequestService;
    private final ILogger logger;

    public FriendRequestController(IFriendRequestService friendRequestService, ILogger logger) {
        this.friendRequestService = friendRequestService;
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
            return new ResponseEntity<>(friendRequestService.getIncomingFetchFriendRequestsByUserId(userId), HttpStatus.OK);
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

            return new ResponseEntity<>(HttpStatus.OK);
        } catch (BaseNotFoundException e) {
            logger.warn("Friend request not found (may have been already processed): " + friendRequestId + ": " + e.getMessage());
            // Return success instead of 404 to avoid client-side errors when friend request was already processed
            return new ResponseEntity<>(HttpStatus.OK);
        } catch (Exception e) {
            logger.error("Error processing friend request action: " + friendRequestAction + " for request: " + friendRequestId + ": " + e.getMessage());
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
