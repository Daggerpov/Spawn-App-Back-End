package com.danielagapov.spawn.Controllers;

import com.danielagapov.spawn.DTOs.FriendTagDTO;
import com.danielagapov.spawn.Exceptions.Base.*;
import com.danielagapov.spawn.Services.FriendTag.IFriendTagService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController()
@RequestMapping("api/v1/friendTags")
public class FriendTagController {
    private final IFriendTagService friendTagService;

    public FriendTagController(IFriendTagService friendTagService) {
        this.friendTagService = friendTagService;
    }

    // full path: /api/v1/friendTags
    @GetMapping
    public String getFriendTags() {
        return "These are the friendTags: " + friendTagService.getAllFriendTags();
    }

    // full path: /api/v1/friendTags/{id}
    @GetMapping("{id}")
    public FriendTagDTO getFriendTag(@PathVariable UUID id) {
        return friendTagService.getFriendTagById(id);
    }

    // full path: /api/v1/friendTags/mock-endpoint
    @GetMapping("mock-endpoint")
    public String getMockEndpoint() {
        return "This is the mock endpoint for friendTags. Everything is working with it.";
    }

    // full path: /api/v1/friendTags
    @PostMapping
    public FriendTagDTO createFriendTag(@RequestBody FriendTagDTO newFriendTag) {
        return friendTagService.saveFriendTag(newFriendTag);
    }

    // full path: /api/v1/friendTags/{id}
    @PutMapping("{id}")
    public FriendTagDTO replaceFriendTag(@RequestBody FriendTagDTO newFriendTag, @PathVariable UUID id) {
        return friendTagService.replaceFriendTag(newFriendTag, id);
    }

    // full path: /api/v1/friendTags/{id}
    @DeleteMapping("{id}")
    public ResponseEntity<Void> deleteFriendTag(@PathVariable UUID id) {
        try {
            boolean isDeleted = friendTagService.deleteFriendTagById(id);
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

    // full path: /api/v1/friendTags/{id}?userId=userId
    @PostMapping("{id}")
    public ResponseEntity<Void> addUserToFriendTag(@PathVariable UUID id, @RequestParam UUID userId) {
        try {
            friendTagService.saveUserToFriendTag(id, userId);
        } catch (BaseNotFoundException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND); // 404
        } catch (BaseSaveException e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR); // 500 TODO consider updating this
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR); // 500
        }
        return new ResponseEntity<>(HttpStatus.OK); //200
    }
}







