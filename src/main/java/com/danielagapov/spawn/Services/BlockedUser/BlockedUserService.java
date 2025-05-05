package com.danielagapov.spawn.Services.BlockedUser;

import com.danielagapov.spawn.DTOs.FriendTag.FriendTagDTO;
import com.danielagapov.spawn.Exceptions.Base.BaseNotFoundException;
import com.danielagapov.spawn.Exceptions.Base.BaseSaveException;
import com.danielagapov.spawn.Exceptions.Logger.ILogger;
import com.danielagapov.spawn.Mappers.BlockedUserMapper;
import com.danielagapov.spawn.Models.User.BlockedUser;
import com.danielagapov.spawn.Models.User.User;
import com.danielagapov.spawn.Repositories.User.IBlockedUserRepository;
import com.danielagapov.spawn.Services.FriendTag.IFriendTagService;
import com.danielagapov.spawn.Services.User.IUserService;
import com.danielagapov.spawn.Utils.LoggingUtils;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import com.danielagapov.spawn.DTOs.BlockedUser.BlockedUserDTO;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class BlockedUserService implements IBlockedUserService {

    private final IBlockedUserRepository repository;
    private final IUserService userService;
    private final IFriendTagService friendTagService;
    private final ILogger logger;
    private final CacheManager cacheManager;

    public BlockedUserService(IBlockedUserRepository repository, IUserService userService, IFriendTagService friendTagService, ILogger logger, CacheManager cacheManager) {
        this.repository = repository;
        this.userService = userService;
        this.friendTagService = friendTagService;
        this.logger = logger;
        this.cacheManager = cacheManager;
    }

    @Override
    @CacheEvict(value = {
        "blockedUsers", 
        "blockedUserIds", 
        "recommendedFriends", 
        "otherProfiles", 
        "friendsList"
    }, key = "#blockerId")
    public void blockUser(UUID blockerId, UUID blockedId, String reason) {
        if (blockerId.equals(blockedId)) return;

        try {
            User blocker = userService.getUserEntityById(blockerId);
            User blocked = userService.getUserEntityById(blockedId);
            
            logger.info("Attempting to block user: " + LoggingUtils.formatUserInfo(blocked) + 
                         " by blocker: " + LoggingUtils.formatUserInfo(blocker));
            
            if (isBlocked(blockerId, blockedId)) {
                logger.info("User " + LoggingUtils.formatUserInfo(blocked) + 
                           " is already blocked by " + LoggingUtils.formatUserInfo(blocker));
                return;
            }

            removeFriendshipBetweenUsers(blockerId, blockedId);

            BlockedUser block = new BlockedUser();
            block.setBlocker(blocker);
            block.setBlocked(blocked);
            block.setReason(reason);

            repository.save(block);
            
            // Also evict caches for the blocked user to ensure they don't see the blocker in recommendations
            evictBlockedUserCaches(blockedId);
            
            logger.info("Successfully blocked user: " + LoggingUtils.formatUserInfo(blocked) + 
                         " by blocker: " + LoggingUtils.formatUserInfo(blocker));
        } catch (DataAccessException e) {
            logger.error("Database error while blocking user " + LoggingUtils.formatUserIdInfo(blockedId) + 
                         " by blocker " + LoggingUtils.formatUserIdInfo(blockerId) + ": " + e.getMessage());
            throw new BaseSaveException("Failed to block user: " + e.getMessage());
        } catch (BaseNotFoundException e) {
            logger.error("User not found while blocking: " + e.getMessage());
            throw e;
        } catch (Exception e) {
            logger.error("Unexpected error while blocking user " + LoggingUtils.formatUserIdInfo(blockedId) + 
                         " by blocker " + LoggingUtils.formatUserIdInfo(blockerId) + ": " + e.getMessage());
            throw e;
        }
    }

    @Override
    @CacheEvict(value = {
        "blockedUsers", 
        "blockedUserIds", 
        "recommendedFriends", 
        "otherProfiles", 
        "friendsList"
    }, key = "#blockerId")
    public void unblockUser(UUID blockerId, UUID blockedId) {
        try {
            User blocker = userService.getUserEntityById(blockerId);
            User blocked = userService.getUserEntityById(blockedId);
            
            logger.info("Attempting to unblock user: " + LoggingUtils.formatUserInfo(blocked) + 
                       " by blocker: " + LoggingUtils.formatUserInfo(blocker));
                       
            repository.findByBlocker_IdAndBlocked_Id(blockerId, blockedId)
                    .ifPresent(blockEntity -> {
                        repository.delete(blockEntity);
                        
                        // Also evict caches for the previously blocked user
                        evictBlockedUserCaches(blockedId);
                        
                        logger.info("Successfully unblocked user: " + LoggingUtils.formatUserInfo(blocked) + 
                                  " by blocker: " + LoggingUtils.formatUserInfo(blocker));
                    });
        } catch (DataAccessException e) {
            logger.error("Database error while unblocking user " + LoggingUtils.formatUserIdInfo(blockedId) + 
                         " by blocker " + LoggingUtils.formatUserIdInfo(blockerId) + ": " + e.getMessage());
            throw e;
        } catch (Exception e) {
            logger.error("Unexpected error while unblocking user " + LoggingUtils.formatUserIdInfo(blockedId) + 
                         " by blocker " + LoggingUtils.formatUserIdInfo(blockerId) + ": " + e.getMessage());
            throw e;
        }
    }

    @Override
    @Cacheable(value = "isBlocked", key = "#blockerId.toString() + ':' + #blockedId.toString()")
    public boolean isBlocked(UUID blockerId, UUID blockedId) {
        try {
            logger.info("Checking if user " + LoggingUtils.formatUserIdInfo(blockedId) + 
                       " is blocked by " + LoggingUtils.formatUserIdInfo(blockerId));
            return repository.existsByBlocker_IdAndBlocked_Id(blockerId, blockedId);
        } catch (DataAccessException e) {
            logger.error("Database error while checking if user " + LoggingUtils.formatUserIdInfo(blockedId) + 
                         " is blocked by " + LoggingUtils.formatUserIdInfo(blockerId) + ": " + e.getMessage());
            throw e;
        } catch (Exception e) {
            logger.error("Unexpected error while checking if user " + LoggingUtils.formatUserIdInfo(blockedId) + 
                         " is blocked by " + LoggingUtils.formatUserIdInfo(blockerId) + ": " + e.getMessage());
            throw e;
        }
    }

    @Override
    @Cacheable(value = "blockedUsers", key = "#blockerId")
    public List<BlockedUserDTO> getBlockedUsers(UUID blockerId) {
        try {
            User blocker = userService.getUserEntityById(blockerId);
            logger.info("Getting blocked users for blocker: " + LoggingUtils.formatUserInfo(blocker));
            
            List<BlockedUserDTO> blockedUsers = repository.findAllByBlocker_Id(blockerId).stream()
                    .map(BlockedUserMapper::toDTO)
                    .collect(Collectors.toList());
                    
            logger.info("Found " + blockedUsers.size() + " blocked users for blocker: " + 
                       LoggingUtils.formatUserInfo(blocker));
            return blockedUsers;
        } catch (Exception e) {
            logger.error("Error retrieving blocked users for blocker " + LoggingUtils.formatUserIdInfo(blockerId) + 
                        ": " + e.getMessage());
            throw e;
        }
    }

    @Override
    @Cacheable(value = "blockedUserIds", key = "#blockerId")
    public List<UUID> getBlockedUserIds(UUID blockerId) {
        try {
            User blocker = userService.getUserEntityById(blockerId);
            logger.info("Getting blocked user IDs for blocker: " + LoggingUtils.formatUserInfo(blocker));
            
            List<UUID> blockedUserIds = repository.findAllByBlocker_Id(blockerId).stream()
                    .map(BlockedUser::getBlocked)
                    .map(User::getId)
                    .collect(Collectors.toList());
                    
            logger.info("Found " + blockedUserIds.size() + " blocked user IDs for blocker: " + 
                       LoggingUtils.formatUserInfo(blocker));
            return blockedUserIds;
        } catch (Exception e) {
            logger.error("Error retrieving blocked user IDs for blocker " + LoggingUtils.formatUserIdInfo(blockerId) + 
                        ": " + e.getMessage());
            throw e;
        }
    }

    @Override
    public void removeFriendshipBetweenUsers(UUID userAId, UUID userBId) {
        try {
            User userA = userService.getUserEntityById(userAId);
            User userB = userService.getUserEntityById(userBId);
            
            logger.info("Removing friendship between users: " + LoggingUtils.formatUserInfo(userA) + 
                       " and " + LoggingUtils.formatUserInfo(userB));
                       
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
            
            logger.info("Successfully removed friendship between users: " + LoggingUtils.formatUserInfo(userA) + 
                       " and " + LoggingUtils.formatUserInfo(userB));
        } catch (Exception e) {
            logger.error("Error removing friendship between users " + LoggingUtils.formatUserIdInfo(userAId) + 
                        " and " + LoggingUtils.formatUserIdInfo(userBId) + ": " + e.getMessage());
            throw e;
        }
    }

    /**
     * Helper method to evict relevant caches for the blocked user
     */
    private void evictBlockedUserCaches(UUID userId) {
        try {
            logger.info("Evicting caches for user ID: " + userId + " after being blocked/unblocked");

            Cache recommendedFriendsCache = cacheManager.getCache("recommendedFriends");
            if (recommendedFriendsCache != null) {
                recommendedFriendsCache.evict(userId);
            }

            Cache otherProfilesCache = cacheManager.getCache("otherProfiles");
            if (otherProfilesCache != null) {
                otherProfilesCache.evict(userId);
            }

            Cache friendsListCache = cacheManager.getCache("friendsList");
            if (friendsListCache != null) {
                friendsListCache.evict(userId);
            }

        } catch (Exception e) {
            logger.error("Error evicting caches for blocked user: " + e.getMessage());
            // Don't throw here - this is a best-effort operation
        }
    }
}
