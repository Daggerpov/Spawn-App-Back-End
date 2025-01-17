package com.danielagapov.spawn.Controllers;

import com.danielagapov.spawn.DTOs.FriendRequestDTO;
import com.danielagapov.spawn.Exceptions.Base.BaseNotFoundException;
import com.danielagapov.spawn.Services.FriendRequestService.IFriendRequestService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("api/v1/users")
public class FriendRequestController {

    private final IFriendRequestService friendRequestService;

    public FriendRequestController(IFriendRequestService friendRequestService) {
        this.friendRequestService = friendRequestService;
    }

    // full path: /api/v1/users/{id}/friend-requests
    @GetMapping("{id}/friend-requests")
    public ResponseEntity<List<FriendRequestDTO>> getIncomingFriendRequests(@PathVariable UUID id) {
        try {
            return new ResponseEntity<>(friendRequestService.getIncomingFriendRequestsByUserId(id), HttpStatus.OK);
        } catch (BaseNotFoundException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // full path: /api/v1/users/{userId}/friend-requests/{friendRequestId}/accept
    @PutMapping("{userId}/friend-requests/{friendRequestId}/accept")
    public ResponseEntity<Void> acceptFriendRequest(@PathVariable UUID userId, @PathVariable UUID friendRequestId) {
        try {
            friendRequestService.acceptFriendRequest(friendRequestId);
            return new ResponseEntity<>(HttpStatus.OK);
        } catch (BaseNotFoundException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // full path: /api/v1/users/{userId}/friend-requests/{friendRequestId}/reject
    @PutMapping("{userId}/friend-requests/{friendRequestId}/reject")
    public ResponseEntity<Void> rejectFriendRequest(@PathVariable UUID userId, @PathVariable UUID friendRequestId) {
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
