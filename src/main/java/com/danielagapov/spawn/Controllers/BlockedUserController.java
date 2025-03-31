package com.danielagapov.spawn.Controllers;

import com.danielagapov.spawn.DTOs.BlockedUserDTO;
import com.danielagapov.spawn.Services.BlockedUser.IBlockedUserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("api/v1/blocked-users")
public class BlockedUserController {

    private final IBlockedUserService blockedUserService;

    public BlockedUserController(IBlockedUserService blockedUserService) {
        this.blockedUserService = blockedUserService;
    }

    // POST /api/v1/blocked-users/{blockerId}/block/{blockedId}
    @PostMapping("{blockerId}/block/{blockedId}")
    public ResponseEntity<Void> blockUser(@PathVariable UUID blockerId,
                                          @PathVariable UUID blockedId,
                                          @RequestParam(required = false) String reason) {
        if (blockerId == null || blockedId == null) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }

        try {
            blockedUserService.blockUser(blockerId, blockedId, reason);
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // DELETE /api/v1/blocked-users/{blockerId}/unblock/{blockedId}
    @DeleteMapping("{blockerId}/unblock/{blockedId}")
    public ResponseEntity<Void> unblockUser(@PathVariable UUID blockerId,
                                            @PathVariable UUID blockedId) {
        if (blockerId == null || blockedId == null) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }

        try {
            blockedUserService.unblockUser(blockerId, blockedId);
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("is-blocked")
    public ResponseEntity<Boolean> isBlocked(@RequestParam UUID blockerId, @RequestParam UUID blockedId) {
        try {
            return ResponseEntity.ok(blockedUserService.isBlocked(blockerId, blockedId));
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }


    // GET /api/v1/blocked-users/{blockerId}
    @GetMapping("{blockerId}")
    public ResponseEntity<List<BlockedUserDTO>> getBlockedUsers(@PathVariable UUID blockerId) {
        if (blockerId == null) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }

        try {
            List<BlockedUserDTO> blockedUsers = blockedUserService.getBlockedUsers(blockerId);
            return new ResponseEntity<>(blockedUsers, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // GET /api/v1/blocked-users/{blockerId}/ids
    @GetMapping("{blockerId}/ids")
    public ResponseEntity<List<UUID>> getBlockedUserIds(@PathVariable UUID blockerId) {
        if (blockerId == null) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }

        try {
            List<UUID> blockedUserIds = blockedUserService.getBlockedUserIds(blockerId);
            return new ResponseEntity<>(blockedUserIds, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
