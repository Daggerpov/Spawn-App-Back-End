package com.danielagapov.spawn.Controllers;

import com.danielagapov.spawn.DTOs.FriendTag.FriendTagCreationDTO;
import com.danielagapov.spawn.DTOs.FriendTag.FriendTagDTO;
import com.danielagapov.spawn.DTOs.User.BaseUserDTO;
import com.danielagapov.spawn.Enums.EntityType;
import com.danielagapov.spawn.Enums.FriendTagAction;
import com.danielagapov.spawn.Exceptions.Base.BaseNotFoundException;
import com.danielagapov.spawn.Exceptions.Base.BasesNotFoundException;
import com.danielagapov.spawn.Services.FriendTag.IFriendTagService;
import com.danielagapov.spawn.Services.User.IUserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("api/v1/friendTags")
public class FriendTagController {

    private final IFriendTagService friendTagService;
    private final IUserService userService;

    public FriendTagController(IFriendTagService friendTagService, IUserService userService) {
        this.friendTagService = friendTagService;
        this.userService = userService;
    }

    // full path: /api/v1/friendTags
    @PostMapping
    public ResponseEntity<FriendTagDTO> createFriendTag(@RequestBody FriendTagCreationDTO newFriendTag) {
        try {
            return new ResponseEntity<>(friendTagService.saveFriendTag(newFriendTag), HttpStatus.CREATED);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // returns ResponseEntity with FriendTagDTO or friend tag entity type (BaseNotFound)
    // full path: /api/v1/friendTags/{id}
    @PutMapping("{id}")
    public ResponseEntity<?> replaceFriendTag(@RequestBody FriendTagDTO newFriendTag, @PathVariable UUID id) {
        if (id == null) return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        try {
            return new ResponseEntity<>(friendTagService.replaceFriendTag(newFriendTag, id), HttpStatus.OK);
        } catch (BaseNotFoundException e) {
            return new ResponseEntity<>(e.entityType, HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // returns ResponseEntity with void or friend tag entity type (BaseNotFound)
    // full path: /api/v1/friendTags/{id}
    @DeleteMapping("{id}")
    public ResponseEntity<?> deleteFriendTag(@PathVariable UUID id) {
        if (id == null) return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        try {
            boolean isDeleted = friendTagService.deleteFriendTagById(id);
            if (isDeleted) {
                return new ResponseEntity<>(HttpStatus.NO_CONTENT);
            } else {
                return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
            }
        } catch (BaseNotFoundException e) {
            return new ResponseEntity<>(e.entityType, HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // returns ResponseEntity with list of friend tags (can be empty)
    //  or not found entity type (user)
    // full path: /api/v1/friendTags/owner/{ownerId}?full=full
    @GetMapping("owner/{ownerId}")
    public ResponseEntity<?> getFriendTagsByOwnerId(@PathVariable UUID ownerId, @RequestParam(value = "full", required = false) boolean full) {
        if (ownerId == null) return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        try {
            if (full) {
                return new ResponseEntity<>(friendTagService.getFullFriendTagsWithFriendsByOwnerId(ownerId), HttpStatus.OK);
            } else {
                return new ResponseEntity<>(friendTagService.getFriendTagsByOwnerId(ownerId), HttpStatus.OK);
            }
        } catch (BaseNotFoundException e) {
            return new ResponseEntity<>(e.entityType, HttpStatus.NOT_FOUND);
        } catch (BasesNotFoundException e) {
            if (e.entityType == EntityType.FriendTag) {
                return new ResponseEntity<>(new ArrayList<>(), HttpStatus.OK);
            } else {
                return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
            }
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // returns ResponseEntity with void or not found entity type
    // full path: /api/v1/friendTags/{id}?friendTagAction={addFriend/removeFriend}&userId=userId
    @PostMapping("{id}")
    public ResponseEntity<?> modifyFriendTagFriends(@PathVariable UUID id, @RequestParam FriendTagAction friendTagAction, @RequestParam UUID userId) {
        if (id == null) return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        try {
            if (friendTagAction == FriendTagAction.addFriend) {
                friendTagService.saveUserToFriendTag(id, userId);
            } else if (friendTagAction == FriendTagAction.removeFriend) {
                friendTagService.removeUserFromFriendTag(id, userId);
            } else {
                return new ResponseEntity<>(HttpStatus.BAD_REQUEST); // this will handle null/invalid cases
            }
            return new ResponseEntity<>(HttpStatus.OK);
        } catch (BaseNotFoundException e) {
            return new ResponseEntity<>(e.entityType, HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            // this also catches `BaseSaveException`, which we're treating the same way with a 500 error below
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // returns ResponseEntity with list of friend tags (can be empty)
    //  or not found entity type (user)
    // full path: /api/v1/friendTags/friendTagsForFriend?ownerUserId&friendUserId
    /**
     * The purpose of this endpoint is to show which friend tags a user has placed a friend into
     * on mobile -> in the Activity creation view when adding friends to an Activity, or in the friends view
     *
     * @param ownerUserId
     * @param friendUserId
     * @return friend tags that `owner` has placed `friend` into
     */
    @GetMapping("{friendTagsForFriend}")
    public ResponseEntity<?> getFriendTagsForFriend(@RequestParam UUID ownerUserId, @RequestParam UUID friendUserId) {
        if (ownerUserId == null || friendUserId == null) return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        try {
            return new ResponseEntity<>(friendTagService.getPertainingFullFriendTagsForFriend(ownerUserId, friendUserId), HttpStatus.OK);
        } catch (BaseNotFoundException e) {
            return new ResponseEntity<>(e.entityType, HttpStatus.NOT_FOUND);
        } catch (BasesNotFoundException e) {
            if (e.entityType == EntityType.FriendTag) {
                return new ResponseEntity<>(new ArrayList<>(), HttpStatus.OK);
            } else {
                return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
            }
        } catch (Exception e) {
            // this also catches `BaseSaveException`, which we're treating the same way with a 500 error below
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // returns ResponseEntity with list of FullUserDTOs (can be empty)
    //  or not found entity type (friendTag)
    /**
     * The purpose of this endpoint is to return which friends a user can add to a friend tag, since
     * they haven't already been added to that friend tag already. On mobile, this corresponds to the
     * popup from clicking a tag's add friend button
     */
    // full path: /api/v1/friendTags/friendsNotAddedToTag/{friendTagId}
    @GetMapping("friendsNotAddedToTag/{friendTagId}")
    public ResponseEntity<?> getFriendsNotAddedToTag(@PathVariable UUID friendTagId) {
        if (friendTagId == null) return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        try {
            return new ResponseEntity<>(friendTagService.getFriendsNotAddedToTag(friendTagId), HttpStatus.OK);
        } catch (BaseNotFoundException e) {
            return new ResponseEntity<>(e.entityType, HttpStatus.NOT_FOUND);
        } catch (BasesNotFoundException e) {
            if (e.entityType == EntityType.User) {
                return new ResponseEntity<>(new ArrayList<>(), HttpStatus.OK);
            } else {
                return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
            }
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // returns ResponseEntity with list of FriendTagDTOs (can be empty)
    //  or not found entity type (user)
    // full path: /api/v1/friendTags/addUserToTags/{ownerUserId}?friendUserId=friendUserId
    @GetMapping("addUserToTags/{ownerUserId}")
    public ResponseEntity<?> getTagsNotAddedToFriend(@PathVariable UUID ownerUserId, @RequestParam UUID friendUserId) {
        if (ownerUserId == null || friendUserId == null) return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        try {
            return new ResponseEntity<>(friendTagService.getTagsNotAddedToFriend(ownerUserId, friendUserId), HttpStatus.OK);
        } catch (BaseNotFoundException e) {
            return new ResponseEntity<>(e.entityType, HttpStatus.NOT_FOUND);
        } catch (BasesNotFoundException e) {
            if (e.entityType == EntityType.FriendTag) {
                return new ResponseEntity<>(new ArrayList<>(), HttpStatus.OK);
            } else {
                return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
            }
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // returns ResponseEntity with void or not found entity type (friendTag, user)
    // full path: /api/v1/friendTags/addUserToTags?friendUserId=friendUserId
    @PostMapping("addUserToTags")
    public ResponseEntity<?> addUserToTags(@RequestBody List<UUID> friendTagIds, @RequestParam UUID friendUserId) {
        if (friendUserId == null) return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        try {
            if (!friendTagIds.isEmpty()) {
                friendTagService.addFriendToFriendTags(friendTagIds, friendUserId);
            }
            return new ResponseEntity<>(HttpStatus.OK);
        } catch (BaseNotFoundException e) {
            return new ResponseEntity<>(e.entityType, HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            // this also catches `BaseSaveException`, which we're treating the same way with a 500 error below
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    
    // returns ResponseEntity with void or not found entity type (friendTag, user)
    // full path: /api/v1/friendTags/bulkAddFriendsToTag
    @PostMapping("bulkAddFriendsToTag")
    public ResponseEntity<Void> bulkAddFriendsToTag(@RequestBody List<BaseUserDTO> friends, @RequestParam UUID friendTagId) {
        if (friendTagId == null) return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        try {
            friendTagService.bulkAddUsersToFriendTag(friendTagId, friends);
            return new ResponseEntity<>(HttpStatus.OK);
        } catch (BaseNotFoundException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            // this also catches `BaseSaveException`, which we're treating the same way with a 500 error below
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // returns ResponseEntity with list of UserDTOs (can be empty) or not found entity type (friendTag)
    // full path: /api/v1/friendTags/{id}/friends
    @GetMapping("{id}/friends")
    public ResponseEntity<?> getFriendsByFriendTagId(@PathVariable UUID id) {
        if (id == null) return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        try {
            return new ResponseEntity<>(userService.getFriendsByFriendTagId(id), HttpStatus.OK);
        } catch (BaseNotFoundException e) {
            return new ResponseEntity<>(e.entityType, HttpStatus.NOT_FOUND);
        } catch (BasesNotFoundException e) {
            if (e.entityType == EntityType.User) {
                return new ResponseEntity<>(new ArrayList<>(), HttpStatus.OK);
            } else {
                return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
            }
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
