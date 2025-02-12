package com.danielagapov.spawn.Controllers;

import com.danielagapov.spawn.DTOs.FriendRequestDTO;
import com.danielagapov.spawn.DTOs.FullFriendRequestDTO;
import com.danielagapov.spawn.Exceptions.Base.BaseNotFoundException;
import com.danielagapov.spawn.Services.FriendRequestService.IFriendRequestService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("api/v1/friend-requests")
public class FriendRequestController {

    private final IFriendRequestService friendRequestService;

    public FriendRequestController(IFriendRequestService friendRequestService) {
        this.friendRequestService = friendRequestService;
    }

    // full path: /api/v1/friend-requests/incoming/{userId}
    @GetMapping("incoming/{userId}")
    public ResponseEntity<List<FullFriendRequestDTO>> getIncomingFriendRequestsByUserId(@PathVariable UUID userId) {
        try {
            return new ResponseEntity<>(friendRequestService.getIncomingFriendRequestsByUserId(userId), HttpStatus.OK);
        } catch (BaseNotFoundException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // full path: /api/v1/friend-requests
    @PostMapping
    public ResponseEntity<FriendRequestDTO> createFriendRequest(@RequestBody FriendRequestDTO friendRequest) {
        try {
            return new ResponseEntity<>(friendRequestService.saveFriendRequest(friendRequest), HttpStatus.CREATED);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // full path: /api/v1/friend-requests/{friendRequestId}/accept
    @PutMapping("{friendRequestId}/accept")
    public ResponseEntity<Void> acceptFriendRequest(@PathVariable UUID friendRequestId) {
        try {
            friendRequestService.acceptFriendRequest(friendRequestId);
            return new ResponseEntity<>(HttpStatus.OK);
        } catch (BaseNotFoundException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // full path: /api/v1/friend-requests/{friendRequestId}/reject
    @PutMapping("{friendRequestId}/reject")
    public ResponseEntity<Void> rejectFriendRequest(@PathVariable UUID friendRequestId) {
        try {
            friendRequestService.deleteFriendRequest(friendRequestId);
            return new ResponseEntity<>(HttpStatus.OK);
        } catch (BaseNotFoundException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
