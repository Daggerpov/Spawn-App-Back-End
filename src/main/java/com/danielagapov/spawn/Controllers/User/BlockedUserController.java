package com.danielagapov.spawn.Controllers.User;

import com.danielagapov.spawn.DTOs.BlockedUser.BlockedUserCreationDTO;
import com.danielagapov.spawn.DTOs.BlockedUser.BlockedUserDTO;
import com.danielagapov.spawn.Exceptions.Logger.ILogger;
import com.danielagapov.spawn.Services.BlockedUser.IBlockedUserService;
import com.danielagapov.spawn.Services.FriendRequest.IFriendRequestService;
import com.danielagapov.spawn.Util.LoggingUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("api/v1/blocked-users")
public class BlockedUserController {

    private final IBlockedUserService blockedUserService;
    private final IFriendRequestService friendRequestService;
    private final ILogger logger;

    public BlockedUserController(IBlockedUserService blockedUserService, IFriendRequestService friendRequestService, ILogger logger) {
        this.blockedUserService = blockedUserService;
        this.friendRequestService = friendRequestService;
        this.logger = logger;
    }

    // POST /api/v1/blocked-users/block
    @PostMapping("block")
    public ResponseEntity<Void> blockUser(@RequestBody BlockedUserCreationDTO dto) {
        try {
            friendRequestService.deleteFriendRequestBetweenUsersIfExists(dto.getBlockerId(), dto.getBlockedId());
            blockedUserService.blockUser(dto.getBlockerId(), dto.getBlockedId(), dto.getReason());
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        } catch (Exception e) {
            logger.error("Error blocking user: " + LoggingUtils.formatUserIdInfo(dto.getBlockedId()) + " by user: " + LoggingUtils.formatUserIdInfo(dto.getBlockerId()) + ": " + e.getMessage());
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // DELETE /api/v1/blocked-users/unblock?blockerId=...&blockedId=...
    @DeleteMapping("unblock")
    public ResponseEntity<Void> unblockUser(@RequestParam UUID blockerId,
                                            @RequestParam UUID blockedId) {
        try {
            blockedUserService.unblockUser(blockerId, blockedId);
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        } catch (Exception e) {
            logger.error("Error unblocking user: " + LoggingUtils.formatUserIdInfo(blockedId) + " by user: " + LoggingUtils.formatUserIdInfo(blockerId) + ": " + e.getMessage());
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // GET /api/v1/blocked-users/{blockerId}?returnOnlyIds=true
    @GetMapping("{blockerId}")
    public ResponseEntity<?> getBlockedUsers(@PathVariable UUID blockerId,
                                             @RequestParam(required = false, defaultValue = "false") boolean returnOnlyIds) {
        try {
            if (returnOnlyIds) {
                List<UUID> blockedUserIds = blockedUserService.getBlockedUserIds(blockerId);
                return new ResponseEntity<>(blockedUserIds, HttpStatus.OK);
            } else {
                List<BlockedUserDTO> blockedUsers = blockedUserService.getBlockedUsers(blockerId);
                return new ResponseEntity<>(blockedUsers, HttpStatus.OK);
            }
        } catch (Exception e) {
            logger.error("Error getting blocked users for user: " + LoggingUtils.formatUserIdInfo(blockerId) + ": " + e.getMessage());
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // GET /api/v1/blocked-users/is-blocked?blockerId=...&blockedId=...
    @GetMapping("is-blocked")
    public ResponseEntity<Boolean> isBlocked(@RequestParam UUID blockerId,
                                             @RequestParam UUID blockedId) {
        try {
            boolean isBlocked = blockedUserService.isBlocked(blockerId, blockedId);
            return ResponseEntity.ok(isBlocked);
        } catch (Exception e) {
            logger.error("Error checking if user is blocked: " + LoggingUtils.formatUserIdInfo(blockedId) + " by user: " + LoggingUtils.formatUserIdInfo(blockerId) + ": " + e.getMessage());
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
