package com.danielagapov.spawn.Services.BlockedUser;

import com.danielagapov.spawn.DTOs.FriendTag.FriendTagDTO;
import com.danielagapov.spawn.Exceptions.Base.BaseNotFoundException;
import com.danielagapov.spawn.Exceptions.Base.BaseSaveException;
import com.danielagapov.spawn.Exceptions.Logger.ILogger;
import com.danielagapov.spawn.Mappers.BlockedUserMapper;
import com.danielagapov.spawn.Models.BlockedUser;
import com.danielagapov.spawn.Models.User;
import com.danielagapov.spawn.Repositories.IBlockedUserRepository;
import com.danielagapov.spawn.Services.FriendTag.IFriendTagService;
import com.danielagapov.spawn.Services.User.IUserService;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import com.danielagapov.spawn.DTOs.BlockedUser.BlockedUserDTO;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class BlockedUserService implements IBlockedUserService {

    private final IBlockedUserRepository repository;
    private final IUserService userService;
    private final IFriendTagService friendTagService;
    private final ILogger logger;

    public BlockedUserService(IBlockedUserRepository repository, IUserService userService, IFriendTagService friendTagService, ILogger logger) {
        this.repository = repository;
        this.userService = userService;
        this.friendTagService = friendTagService;
        this.logger = logger;
    }

    @Override
    @CacheEvict(value = {"blockedUsers", "blockedUserIds"}, key = "#blockerId")
    public void blockUser(UUID blockerId, UUID blockedId, String reason) {
        if (blockerId.equals(blockedId)) return;

        try {
            if (isBlocked(blockerId, blockedId)) {
                return;
            }

            removeFriendshipBetweenUsers(blockerId, blockedId);

            User blocker = userService.getUserEntityById(blockerId);
            User blocked = userService.getUserEntityById(blockedId);

            BlockedUser block = new BlockedUser();
            block.setBlocker(blocker);
            block.setBlocked(blocked);
            block.setReason(reason);

            repository.save(block);
        } catch (DataAccessException e) {
            logger.error("Database error while blocking user: " + e.getMessage());
            throw new BaseSaveException("Failed to block user: " + e.getMessage());
        } catch (BaseNotFoundException e) {
            logger.error("User not found: " + e.getMessage());
            throw e;
        } catch (Exception e) {
            logger.error("Unexpected error in blockUser: " + e.getMessage());
            throw e;
        }
    }

    @Override
    @CacheEvict(value = {"blockedUsers", "blockedUserIds"}, key = "#blockerId")
    public void unblockUser(UUID blockerId, UUID blockedId) {
        try {
            repository.findByBlocker_IdAndBlocked_Id(blockerId, blockedId)
                    .ifPresent(repository::delete);
        } catch (DataAccessException e) {
            logger.error("Database error while unblocking user: " + e.getMessage());
            throw e;
        } catch (Exception e) {
            logger.error("Unexpected error in unblockUser: " + e.getMessage());
            throw e;
        }
    }

    @Override
    @Cacheable(value = "isBlocked", key = "#blockerId.toString() + ':' + #blockedId.toString()")
    public boolean isBlocked(UUID blockerId, UUID blockedId) {
        try {
            return repository.existsByBlocker_IdAndBlocked_Id(blockerId, blockedId);
        } catch (DataAccessException e) {
            logger.error("Database error while checking isBlocked: " + e.getMessage());
            throw e;
        } catch (Exception e) {
            logger.error("Unexpected error in isBlocked: " + e.getMessage());
            throw e;
        }
    }

    @Override
    @Cacheable(value = "blockedUsers", key = "#blockerId")
    public List<BlockedUserDTO> getBlockedUsers(UUID blockerId) {
        try {
            return repository.findAllByBlocker_Id(blockerId).stream()
                    .map(BlockedUserMapper::toDTO)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            logger.error("Error retrieving blocked users for " + blockerId + ": " + e.getMessage());
            throw e;
        }
    }

    @Override
    @Cacheable(value = "blockedUserIds", key = "#blockerId")
    public List<UUID> getBlockedUserIds(UUID blockerId) {
        try {
            return repository.findAllByBlocker_Id(blockerId).stream()
                    .map(BlockedUser::getBlocked)
                    .map(User::getId)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            logger.error("Error retrieving blocked users for " + blockerId + ": " + e.getMessage());
            throw e;
        }
    }

    @Override
    public void removeFriendshipBetweenUsers(UUID userAId, UUID userBId) {
        try {
            List<FriendTagDTO> userATags = friendTagService.getFriendTagsByOwnerId(userAId);
            for (FriendTagDTO tag : userATags) {
                if (tag.getFriendUserIds().contains(userBId)) {
                    friendTagService.removeUserFromFriendTag(tag.getId(), userBId);
                }
            }

            List<FriendTagDTO> userBTags = friendTagService.getFriendTagsByOwnerId(userBId);
            for (FriendTagDTO tag : userBTags) {
                if (tag.getFriendUserIds().contains(userAId)) {
                    friendTagService.removeUserFromFriendTag(tag.getId(), userAId);
                }
            }
        } catch (Exception e) {
            logger.error("Error removing friendship between users " + userAId + " and " + userBId + ": " + e.getMessage());
            throw e;
        }
    }
}
