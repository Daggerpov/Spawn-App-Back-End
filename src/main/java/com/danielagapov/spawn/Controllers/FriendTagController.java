package com.danielagapov.spawn.Controllers;

import com.danielagapov.spawn.DTOs.FriendTag.FriendTagCreationDTO;
import com.danielagapov.spawn.DTOs.FriendTag.FriendTagDTO;
import com.danielagapov.spawn.DTOs.User.BaseUserDTO;
import com.danielagapov.spawn.Enums.EntityType;
import com.danielagapov.spawn.Enums.FriendTagAction;
import com.danielagapov.spawn.Exceptions.Base.BaseNotFoundException;
import com.danielagapov.spawn.Exceptions.Base.BasesNotFoundException;
import com.danielagapov.spawn.Exceptions.Logger.ILogger;
import com.danielagapov.spawn.Services.BlockedUser.IBlockedUserService;
import com.danielagapov.spawn.Services.FriendTag.IFriendTagService;
import com.danielagapov.spawn.Services.User.IUserService;
import com.danielagapov.spawn.Util.LoggingUtils;
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
    private final IBlockedUserService blockedUserService;
    private final ILogger logger;

    public FriendTagController(IFriendTagService friendTagService, IUserService userService, IBlockedUserService blockedUserService, ILogger logger) {
        this.friendTagService = friendTagService;
        this.userService = userService;
        this.blockedUserService = blockedUserService;
        this.logger = logger;
    }

    // full path: /api/v1/friendTags
    @PostMapping
    public ResponseEntity<FriendTagDTO> createFriendTag(@RequestBody FriendTagCreationDTO newFriendTag) {
        try {
            FriendTagDTO createdTag = friendTagService.saveFriendTag(newFriendTag);
            return new ResponseEntity<>(createdTag, HttpStatus.CREATED);
        } catch (Exception e) {
            logger.error("Error creating friend tag: " + e.getMessage());
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // returns ResponseEntity with FriendTagDTO or friend tag entity type (BaseNotFound)
    // full path: /api/v1/friendTags/{id}
    @PutMapping("{id}")
    public ResponseEntity<?> replaceFriendTag(@RequestBody FriendTagDTO newFriendTag, @PathVariable UUID id) {
        if (id == null) {
            logger.error("Invalid parameter: friend tag ID is null");
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
        try {
            return new ResponseEntity<>(friendTagService.replaceFriendTag(newFriendTag, id), HttpStatus.OK);
        } catch (BaseNotFoundException e) {
            logger.error("Friend tag not found for replacement: " + id + ": " + e.getMessage());
            return new ResponseEntity<>(e.entityType, HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            logger.error("Error replacing friend tag: " + id + ": " + e.getMessage());
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // returns ResponseEntity with void or friend tag entity type (BaseNotFound)
    // full path: /api/v1/friendTags/{id}
    @DeleteMapping("{id}")
    public ResponseEntity<?> deleteFriendTag(@PathVariable UUID id) {
        if (id == null) {
            logger.error("Invalid parameter: friend tag ID is null");
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
        try {
            boolean isDeleted = friendTagService.deleteFriendTagById(id);
            if (isDeleted) {
                return new ResponseEntity<>(HttpStatus.NO_CONTENT);
            } else {
                logger.error("Failed to delete friend tag: " + id);
                return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
            }
        } catch (BaseNotFoundException e) {
            logger.error("Friend tag not found for deletion: " + id + ": " + e.getMessage());
            return new ResponseEntity<>(e.entityType, HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            logger.error("Error deleting friend tag: " + id + ": " + e.getMessage());
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // returns ResponseEntity with list of friend tags (can be empty)
    //  or not found entity type (user)
    // full path: /api/v1/friendTags/owner/{ownerId}?full=full
    @GetMapping("owner/{ownerId}")
    public ResponseEntity<?> getFriendTagsByOwnerId(@PathVariable UUID ownerId, @RequestParam(value = "full", required = false) boolean full) {
        if (ownerId == null) {
            logger.error("Invalid parameter: ownerId is null");
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
        try {
            if (full) {
                return new ResponseEntity<>(friendTagService.getFullFriendTagsWithFriendsByOwnerId(ownerId), HttpStatus.OK);
            } else {
                return new ResponseEntity<>(friendTagService.getFriendTagsByOwnerId(ownerId), HttpStatus.OK);
            }
        } catch (BaseNotFoundException e) {
            logger.error("Owner not found for friend tags: " + LoggingUtils.formatUserIdInfo(ownerId) + ": " + e.getMessage());
            return new ResponseEntity<>(e.entityType, HttpStatus.NOT_FOUND);
        } catch (BasesNotFoundException e) {
            if (e.entityType == EntityType.FriendTag) {
                return new ResponseEntity<>(new ArrayList<>(), HttpStatus.OK);
            } else {
                logger.error("Bad request for friend tags by owner: " + e.getMessage());
                return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
            }
        } catch (Exception e) {
            logger.error("Error getting friend tags for owner: " + LoggingUtils.formatUserIdInfo(ownerId) + ": " + e.getMessage());
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // returns ResponseEntity with void or not found entity type
    // full path: /api/v1/friendTags/{id}?friendTagAction={addFriend/removeFriend}&userId=userId
    @PostMapping("{id}")
    public ResponseEntity<?> modifyFriendTagFriends(@PathVariable UUID id, @RequestParam FriendTagAction friendTagAction, @RequestParam UUID userId) {
        if (id == null) {
            logger.error("Invalid parameter: friend tag ID is null");
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
        try {
            if (friendTagAction == FriendTagAction.addFriend) {
                friendTagService.saveUserToFriendTag(id, userId);
            } else if (friendTagAction == FriendTagAction.removeFriend) {
                friendTagService.removeUserFromFriendTag(id, userId);
            } else {
                logger.error("Invalid friend tag action: " + friendTagAction);
                return new ResponseEntity<>(HttpStatus.BAD_REQUEST); // this will handle null/invalid cases
            }
            return new ResponseEntity<>(HttpStatus.OK);
        } catch (BaseNotFoundException e) {
            logger.error("Entity not found for friend tag modification: " + e.getMessage());
            return new ResponseEntity<>(e.entityType, HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            // this also catches `BaseSaveException`, which we're treating the same way with a 500 error below
            logger.error("Error modifying friend tag: " + id + " with action: " + friendTagAction + ": " + e.getMessage());
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
        if (ownerUserId == null || friendUserId == null) {
            logger.error("Invalid parameters: ownerUserId or friendUserId is null");
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
        try {
            return new ResponseEntity<>(friendTagService.getPertainingFullFriendTagsForFriend(ownerUserId, friendUserId), HttpStatus.OK);
        } catch (BaseNotFoundException e) {
            logger.error("User not found for friend tags: " + e.getMessage());
            return new ResponseEntity<>(e.entityType, HttpStatus.NOT_FOUND);
        } catch (BasesNotFoundException e) {
            if (e.entityType == EntityType.FriendTag) {
                return new ResponseEntity<>(new ArrayList<>(), HttpStatus.OK);
            } else {
                logger.error("Bad request for friend tags for friend: " + e.getMessage());
                return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
            }
        } catch (Exception e) {
            // this also catches `BaseSaveException`, which we're treating the same way with a 500 error below
            logger.error("Error getting friend tags for friend: " + LoggingUtils.formatUserIdInfo(friendUserId) + " owned by: " + LoggingUtils.formatUserIdInfo(ownerUserId) + ": " + e.getMessage());
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
        if (friendTagId == null) {
            logger.error("Invalid parameter: friendTagId is null");
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
        try {
            List<BaseUserDTO> friends = friendTagService.getFriendsNotAddedToTag(friendTagId);
            // Get the owner of the friend tag to use as the requesting user for filtering
            FriendTagDTO friendTag = friendTagService.getFriendTagById(friendTagId);
            List<BaseUserDTO> filteredFriends = blockedUserService.filterOutBlockedUsers(friends, friendTag.getOwnerUserId());
            return new ResponseEntity<>(filteredFriends, HttpStatus.OK);
        } catch (BaseNotFoundException e) {
            logger.error("Friend tag not found: " + friendTagId + ": " + e.getMessage());
            return new ResponseEntity<>(e.entityType, HttpStatus.NOT_FOUND);
        } catch (BasesNotFoundException e) {
            if (e.entityType == EntityType.User) {
                return new ResponseEntity<>(new ArrayList<>(), HttpStatus.OK);
            } else {
                logger.error("Bad request for friends not added to tag: " + e.getMessage());
                return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
            }
        } catch (Exception e) {
            logger.error("Error getting friends not added to tag: " + friendTagId + ": " + e.getMessage());
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // returns ResponseEntity with list of FriendTagDTOs (can be empty)
    //  or not found entity type (user)
    // full path: /api/v1/friendTags/addUserToTags/{ownerUserId}?friendUserId=friendUserId
    @GetMapping("addUserToTags/{ownerUserId}")
    public ResponseEntity<?> getTagsNotAddedToFriend(@PathVariable UUID ownerUserId, @RequestParam UUID friendUserId) {
        if (ownerUserId == null || friendUserId == null) {
            logger.error("Invalid parameters: ownerUserId or friendUserId is null");
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
        try {
            return new ResponseEntity<>(friendTagService.getTagsNotAddedToFriend(ownerUserId, friendUserId), HttpStatus.OK);
        } catch (BaseNotFoundException e) {
            logger.error("User not found for tags not added to friend: " + e.getMessage());
            return new ResponseEntity<>(e.entityType, HttpStatus.NOT_FOUND);
        } catch (BasesNotFoundException e) {
            if (e.entityType == EntityType.FriendTag) {
                return new ResponseEntity<>(new ArrayList<>(), HttpStatus.OK);
            } else {
                logger.error("Bad request for tags not added to friend: " + e.getMessage());
                return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
            }
        } catch (Exception e) {
            logger.error("Error getting tags not added to friend: " + LoggingUtils.formatUserIdInfo(friendUserId) + " for owner: " + LoggingUtils.formatUserIdInfo(ownerUserId) + ": " + e.getMessage());
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // returns ResponseEntity with void or not found entity type (friendTag, user)
    // full path: /api/v1/friendTags/addUserToTags?friendUserId=friendUserId
    @PostMapping("addUserToTags")
    public ResponseEntity<?> addUserToTags(@RequestBody List<UUID> friendTagIds, @RequestParam UUID friendUserId) {
        try {
            friendTagService.addFriendToFriendTags(friendTagIds, friendUserId);
            return new ResponseEntity<>(HttpStatus.OK);
        } catch (BaseNotFoundException e) {
            logger.error("Entity not found for adding user to tags: " + e.getMessage());
            return new ResponseEntity<>(e.entityType, HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            logger.error("Error adding user to tags: " + LoggingUtils.formatUserIdInfo(friendUserId) + ": " + e.getMessage());
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping("bulkAddFriendsToTag")
    public ResponseEntity<Void> bulkAddFriendsToTag(@RequestBody List<BaseUserDTO> friends, @RequestParam UUID friendTagId) {
        try {
            friendTagService.bulkAddUsersToFriendTag(friendTagId, friends);
            return new ResponseEntity<>(HttpStatus.OK);
        } catch (BaseNotFoundException e) {
            logger.error("Friend tag not found for bulk add: " + friendTagId + ": " + e.getMessage());
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            logger.error("Error bulk adding friends to tag: " + friendTagId + ": " + e.getMessage());
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("{id}/friends")
    public ResponseEntity<?> getFriendsByFriendTagId(@PathVariable UUID id) {
        if (id == null) {
            logger.error("Invalid parameter: friend tag ID is null");
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
        try {
            List<BaseUserDTO> friends = userService.getFriendsByFriendTagId(id);
            // Get the owner of the friend tag to use as the requesting user for filtering
            FriendTagDTO friendTag = friendTagService.getFriendTagById(id);
            List<BaseUserDTO> filteredFriends = blockedUserService.filterOutBlockedUsers(friends, friendTag.getOwnerUserId());
            return new ResponseEntity<>(filteredFriends, HttpStatus.OK);
        } catch (BaseNotFoundException e) {
            logger.error("Friend tag not found: " + id + ": " + e.getMessage());
            return new ResponseEntity<>(e.entityType, HttpStatus.NOT_FOUND);
        } catch (BasesNotFoundException e) {
            if (e.entityType == EntityType.User) {
                return new ResponseEntity<>(new ArrayList<>(), HttpStatus.OK);
            } else {
                logger.error("Bad request for friends by friend tag: " + e.getMessage());
                return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
            }
        } catch (Exception e) {
            logger.error("Error getting friends by friend tag ID: " + id + ": " + e.getMessage());
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
