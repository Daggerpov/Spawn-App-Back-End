package com.danielagapov.spawn.Controllers;

import com.danielagapov.spawn.DTOs.FriendRequest.CreateFriendRequestDTO;
import com.danielagapov.spawn.DTOs.FriendRequest.FetchFriendRequestDTO;
import com.danielagapov.spawn.Enums.EntityType;
import com.danielagapov.spawn.Enums.FriendRequestAction;
import com.danielagapov.spawn.Exceptions.Base.BaseNotFoundException;
import com.danielagapov.spawn.Exceptions.Base.BasesNotFoundException;
import com.danielagapov.spawn.Services.FriendRequest.IFriendRequestService;
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

    public FriendRequestController(IFriendRequestService friendRequestService) {
        this.friendRequestService = friendRequestService;
    }

    // returns ResponseEntity with list of FetchFriendRequestDTO (can be empty)
    //  or not found entity type (user)
    // full path: /api/v1/friend-requests/incoming/{userId}
    @GetMapping("incoming/{userId}")
    public ResponseEntity<?> getIncomingFriendRequestsByUserId(@PathVariable UUID userId) {
        if (userId == null) return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        try {
            return new ResponseEntity<>(friendRequestService.getIncomingFetchFriendRequestsByUserId(userId), HttpStatus.OK);
        } catch (BaseNotFoundException e) {
            return new ResponseEntity<>(e.entityType, HttpStatus.NOT_FOUND);
        } catch (BasesNotFoundException e) {
            if (e.entityType == EntityType.FriendRequest) {
                return new ResponseEntity<>(new ArrayList<>(), HttpStatus.OK);
            } else {
                return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
            }
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // full path: /api/v1/friend-requests
    @PostMapping
    public ResponseEntity<CreateFriendRequestDTO> createFriendRequest(@RequestBody CreateFriendRequestDTO friendRequest) {
        try {
            return new ResponseEntity<>(friendRequestService.saveFriendRequest(friendRequest), HttpStatus.CREATED);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // returns ResponseEntity with void
    //  or not found entity type (friendRequestId)
    // full path: /api/v1/friend-requests/{friendRequestId}?friendRequestAction={accept/reject}
    @PutMapping("{friendRequestId}")
    public ResponseEntity<?> friendRequestAction(@PathVariable UUID friendRequestId, @RequestParam FriendRequestAction friendRequestAction) {
        if (friendRequestId == null) return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        try {
            if (friendRequestAction == FriendRequestAction.accept) {
                friendRequestService.acceptFriendRequest(friendRequestId);
            } else if (friendRequestAction == FriendRequestAction.reject) {
                friendRequestService.deleteFriendRequest(friendRequestId);
            } else {
                // deal with null/invalid argument for `friendRequestAction`
                return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
            }

            return new ResponseEntity<>(HttpStatus.OK);
        } catch (BaseNotFoundException e) {
            return new ResponseEntity<>(e.entityType, HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
