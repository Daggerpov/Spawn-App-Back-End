package com.danielagapov.spawn.social.internal.services;

import com.danielagapov.spawn.user.api.dto.BlockedUserDTO;
import com.danielagapov.spawn.shared.exceptions.Base.BaseNotFoundException;
import com.danielagapov.spawn.shared.exceptions.Base.BaseSaveException;
import com.danielagapov.spawn.shared.exceptions.Logger.ILogger;
import com.danielagapov.spawn.shared.util.BlockedUserMapper;
import com.danielagapov.spawn.user.internal.domain.BlockedUser;
import com.danielagapov.spawn.user.internal.domain.User;
import com.danielagapov.spawn.user.internal.repositories.IBlockedUserRepository;
import com.danielagapov.spawn.social.internal.repositories.IFriendshipRepository;
import com.danielagapov.spawn.user.internal.services.IUserService;
import com.danielagapov.spawn.shared.util.LoggingUtils;
import com.danielagapov.spawn.shared.util.CacheEvictionHelper;
import com.danielagapov.spawn.shared.util.CacheNames;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.HashSet;
import java.util.Set;

@Service
public class BlockedUserService implements IBlockedUserService {

    private final IBlockedUserRepository repository;
    private final IUserService userService;
    private final IFriendshipRepository friendshipRepository;
    private final ILogger logger;
    private final CacheEvictionHelper cacheEvictionHelper;

    public BlockedUserService(IBlockedUserRepository repository, IUserService userService, IFriendshipRepository friendshipRepository, ILogger logger, CacheEvictionHelper cacheEvictionHelper) {
        this.repository = repository;
        this.userService = userService;
        this.friendshipRepository = friendshipRepository;
        this.logger = logger;
        this.cacheEvictionHelper = cacheEvictionHelper;
    }

    @Override
    @Transactional
    @CacheEvict(value = {
            CacheNames.BLOCKED_USERS,
            CacheNames.BLOCKED_USER_IDS,
            CacheNames.RECOMMENDED_FRIENDS,
            CacheNames.OTHER_PROFILES,
            CacheNames.FRIENDS_LIST
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
    @Transactional
    public void unblockUser(UUID blockerId, UUID blockedId) {
        try {
            User blocker = userService.getUserEntityById(blockerId);
            User blocked = userService.getUserEntityById(blockedId);

            logger.info("Attempting to unblock user: " + LoggingUtils.formatUserInfo(blocked) +
                    " by blocker: " + LoggingUtils.formatUserInfo(blocker));

            repository.findByBlocker_IdAndBlocked_Id(blockerId, blockedId)
                    .ifPresent(blockEntity -> {
                        repository.delete(blockEntity);

                        // Manually evict all relevant caches
                        evictBlockedUserCaches(blockerId);
                        evictBlockedUserCaches(blockedId);
                        
                        // Evict the specific isBlocked cache entry
                        evictIsBlockedCache(blockerId, blockedId);

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

            List<BlockedUser> blockedUserEntities = repository.findAllByBlocker_Id(blockerId);
            logger.info("Found " + blockedUserEntities.size() + " blocked user entities for blocker: " +
                    LoggingUtils.formatUserInfo(blocker));

            List<BlockedUserDTO> blockedUsers = blockedUserEntities.stream()
                    .filter(entity -> {
                        if (entity.getBlocker() == null) {
                            logger.warn("Blocked user entity " + entity.getId() + " has null blocker - skipping");
                            return false;
                        }
                        if (entity.getBlocked() == null) {
                            logger.warn("Blocked user entity " + entity.getId() + " has null blocked user - skipping");
                            return false;
                        }
                        return true;
                    })
                    .map(BlockedUserMapper::toDTO)
                    .collect(Collectors.toList());

            logger.info("Successfully mapped " + blockedUsers.size() + " blocked users for blocker: " +
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
            List<UUID> blockedUserIds = repository.findAllByBlocker_Id(blockerId).stream()
                    .map(BlockedUser::getBlocked)
                    .map(User::getId)
                    .collect(Collectors.toList());

            return blockedUserIds;
        } catch (Exception e) {
            logger.error("Error retrieving blocked user IDs for blocker " + LoggingUtils.formatUserIdInfo(blockerId) +
                    ": " + e.getMessage());
            throw e;
        }
    }

    @Override
    @Transactional
    public void removeFriendshipBetweenUsers(UUID userAId, UUID userBId) {
        try {
            User userA = userService.getUserEntityById(userAId);
            User userB = userService.getUserEntityById(userBId);

            logger.info("Removing friendship between users: " + LoggingUtils.formatUserInfo(userA) +
                    " and " + LoggingUtils.formatUserInfo(userB));

            friendshipRepository.deleteBidirectionally(userAId, userBId);

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
        logger.info("Evicting caches for user ID: " + userId + " after being blocked/unblocked");
        cacheEvictionHelper.evictCaches(userId, 
            CacheNames.RECOMMENDED_FRIENDS,
            CacheNames.OTHER_PROFILES,
            CacheNames.FRIENDS_LIST,
            CacheNames.BLOCKED_USERS,
            CacheNames.BLOCKED_USER_IDS
        );
    }

    /**
     * Helper method to evict the isBlocked cache for a specific blocker-blocked pair
     */
    private void evictIsBlockedCache(UUID blockerId, UUID blockedId) {
        String cacheKey = blockerId.toString() + ":" + blockedId.toString();
        logger.info("Evicting isBlocked cache for key: " + cacheKey);
        cacheEvictionHelper.evictCache("isBlocked", cacheKey);
    }

    @Override
    public <T> List<T> filterOutBlockedUsers(List<T> users, UUID requestingUserId) {
        try {
            if (users == null || users.isEmpty() || requestingUserId == null) {
                return users != null ? users : List.of();
            }

            // Get blocked user IDs for the requesting user (users they blocked)
            List<UUID> blockedByRequestingUser = getBlockedUserIds(requestingUserId);

            // Get users who blocked the requesting user
            List<UUID> usersWhoBlockedRequestingUser = repository.findAllByBlocked_Id(requestingUserId).stream()
                    .map(BlockedUser::getBlocker)
                    .map(User::getId)
                    .collect(Collectors.toList());

            // Combine both lists for filtering
            Set<UUID> usersToFilter = new HashSet<>(blockedByRequestingUser);
            usersToFilter.addAll(usersWhoBlockedRequestingUser);

            // Filter out blocked users using reflection to get the ID
            return users.stream()
                    .filter(user -> {
                        try {
                            // Use reflection to get the ID field/method
                            UUID userId = getUserId(user);
                            return userId != null && !usersToFilter.contains(userId);
                        } catch (Exception e) {
                            logger.warn("Could not extract user ID from object: " + user.getClass().getSimpleName() + 
                                       ". Including in results. Error: " + e.getMessage());
                            return true; // Include in results if we can't determine the ID
                        }
                    })
                    .collect(Collectors.toList());

        } catch (Exception e) {
            logger.error("Error filtering blocked users for requesting user " + 
                        LoggingUtils.formatUserIdInfo(requestingUserId) + ": " + e.getMessage());
            // Return original list if filtering fails
            return users;
        }
    }

    /**
     * Helper method to extract user ID from various user DTO types using reflection
     */
    private <T> UUID getUserId(T user) throws Exception {
        try {
            // Special handling for FetchFriendRequestDTO - try getSenderUser() first
            if (user.getClass().getSimpleName().equals("FetchFriendRequestDTO")) {
                try {
                    java.lang.reflect.Method getSenderUserMethod = user.getClass().getMethod("getSenderUser");
                    Object senderUserObj = getSenderUserMethod.invoke(user);
                    if (senderUserObj != null) {
                        // Try to get ID from the sender user object
                        java.lang.reflect.Method getIdMethod = senderUserObj.getClass().getMethod("getId");
                        Object result = getIdMethod.invoke(senderUserObj);
                        if (result instanceof UUID) {
                            return (UUID) result;
                        }
                    }
                } catch (Exception e) {
                    // If getSenderUser() fails, fall through to other methods
                }
            }
            
            // Special handling for FetchSentFriendRequestDTO - try getReceiverUser() first
            if (user.getClass().getSimpleName().equals("FetchSentFriendRequestDTO")) {
                try {
                    java.lang.reflect.Method getReceiverUserMethod = user.getClass().getMethod("getReceiverUser");
                    Object receiverUserObj = getReceiverUserMethod.invoke(user);
                    if (receiverUserObj != null) {
                        // Try to get ID from the receiver user object
                        java.lang.reflect.Method getIdMethod = receiverUserObj.getClass().getMethod("getId");
                        Object result = getIdMethod.invoke(receiverUserObj);
                        if (result instanceof UUID) {
                            return (UUID) result;
                        }
                    }
                } catch (Exception e) {
                    // If getReceiverUser() fails, fall through to other methods
                }
            }
            
            // Try getId() method first (for most DTOs)
            java.lang.reflect.Method getIdMethod = user.getClass().getMethod("getId");
            Object result = getIdMethod.invoke(user);
            if (result instanceof UUID) {
                return (UUID) result;
            }
        } catch (Exception e) {
            // Try getUserId() method
            try {
                java.lang.reflect.Method getUserIdMethod = user.getClass().getMethod("getUserId");
                Object result = getUserIdMethod.invoke(user);
                if (result instanceof UUID) {
                    return (UUID) result;
                }
            } catch (Exception e2) {
                // Try getUser() method for nested user objects (e.g., SearchResultUserDTO)
                try {
                    java.lang.reflect.Method getUserMethod = user.getClass().getMethod("getUser");
                    Object userObj = getUserMethod.invoke(user);
                    if (userObj != null) {
                        // Try to get ID from the nested user object
                        java.lang.reflect.Method getIdMethod = userObj.getClass().getMethod("getId");
                        Object result = getIdMethod.invoke(userObj);
                        if (result instanceof UUID) {
                            return (UUID) result;
                        }
                    }
                } catch (Exception e3) {
                    // Try getSenderUser() method for FetchFriendRequestDTO (fallback)
                    try {
                        java.lang.reflect.Method getSenderUserMethod = user.getClass().getMethod("getSenderUser");
                        Object senderUserObj = getSenderUserMethod.invoke(user);
                        if (senderUserObj != null) {
                            // Try to get ID from the sender user object
                            java.lang.reflect.Method getIdMethod = senderUserObj.getClass().getMethod("getId");
                            Object result = getIdMethod.invoke(senderUserObj);
                            if (result instanceof UUID) {
                                return (UUID) result;
                            }
                        }
                    } catch (Exception e4) {
                        // Try direct field access
                        try {
                            java.lang.reflect.Field idField = user.getClass().getDeclaredField("id");
                            idField.setAccessible(true);
                            Object result = idField.get(user);
                            if (result instanceof UUID) {
                                return (UUID) result;
                            }
                        } catch (Exception e5) {
                            // Try userId field
                            try {
                                java.lang.reflect.Field userIdField = user.getClass().getDeclaredField("userId");
                                userIdField.setAccessible(true);
                                Object result = userIdField.get(user);
                                if (result instanceof UUID) {
                                    return (UUID) result;
                                }
                            } catch (Exception e6) {
                                throw new Exception("Could not extract user ID from object of type: " + user.getClass().getSimpleName());
                            }
                        }
                    }
                }
            }
        }
        return null;
    }
}
