package com.danielagapov.spawn.Controllers;

import com.danielagapov.spawn.DTOs.FriendTagDTO;
import com.danielagapov.spawn.DTOs.FullFriendTagDTO;
import com.danielagapov.spawn.Exceptions.Base.BaseNotFoundException;
import com.danielagapov.spawn.Exceptions.Base.BaseSaveException;
import com.danielagapov.spawn.Services.FriendTag.IFriendTagService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("api/v1/friendTags")
public class FriendTagController {

    private final IFriendTagService friendTagService;

    public FriendTagController(IFriendTagService friendTagService) {
        this.friendTagService = friendTagService;
    }

    // full path: /api/v1/friendTags
    @GetMapping
    public ResponseEntity<List<FriendTagDTO>> getFriendTags() {
        try {
            return new ResponseEntity<>(friendTagService.getAllFriendTags(), HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // full path: /api/v1/friendTags/{id}
    @GetMapping("{id}")
    public ResponseEntity<FriendTagDTO> getFriendTag(@PathVariable UUID id) {
        try {
            return new ResponseEntity<>(friendTagService.getFriendTagById(id), HttpStatus.OK);
        } catch (BaseNotFoundException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // full path: /api/v1/friendTags/{id}?full=full
    @GetMapping("{id}")
    public ResponseEntity<FullFriendTagDTO> getFullFriendTag(@PathVariable UUID id, @RequestParam boolean full) {
        try {
            return new ResponseEntity<>(friendTagService.getFullFriendTagById(id), HttpStatus.OK);
        } catch (BaseNotFoundException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // full path: /api/v1/friendTags/mock-endpoint
    @GetMapping("mock-endpoint")
    public ResponseEntity<String> getMockEndpoint() {
        try {
            return new ResponseEntity<>("This is the mock endpoint for friendTags. Everything is working with it.", HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // full path: /api/v1/friendTags
    @PostMapping
    public ResponseEntity<FriendTagDTO> createFriendTag(@RequestBody FriendTagDTO newFriendTag) {
        try {
            return new ResponseEntity<>(friendTagService.saveFriendTag(newFriendTag), HttpStatus.CREATED);
        } catch (BaseSaveException e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // full path: /api/v1/friendTags/{id}
    @PutMapping("{id}")
    public ResponseEntity<FriendTagDTO> replaceFriendTag(@RequestBody FriendTagDTO newFriendTag, @PathVariable UUID id) {
        try {
            return new ResponseEntity<>(friendTagService.replaceFriendTag(newFriendTag, id), HttpStatus.OK);
        } catch (BaseNotFoundException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // full path: /api/v1/friendTags/{id}
    @DeleteMapping("{id}")
    public ResponseEntity<Void> deleteFriendTag(@PathVariable UUID id) {
        try {
            boolean isDeleted = friendTagService.deleteFriendTagById(id);
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

    // full path: /api/v1/friendTags/owner/{ownerId}
    @GetMapping("owner/{ownerId}")
    public ResponseEntity<List<FriendTagDTO>> getFriendTagsByOwnerId(@PathVariable UUID ownerId) {
        try {
            return new ResponseEntity<>(friendTagService.getFriendTagsByOwnerId(ownerId), HttpStatus.OK);
        } catch (BaseNotFoundException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // full path: /api/v1/friendTags/{id}?userId=userId
    @PostMapping("{id}")
    public ResponseEntity<Void> addUserToFriendTag(@PathVariable UUID id, @RequestParam UUID userId) {
        try {
            friendTagService.saveUserToFriendTag(id, userId);
            return new ResponseEntity<>(HttpStatus.OK);
        } catch (BaseNotFoundException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            // this also catches `BaseSaveException`, which we're treating the same way with a 500 error below
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
