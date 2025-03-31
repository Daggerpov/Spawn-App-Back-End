package com.danielagapov.spawn.Services.BlockedUser;

import com.danielagapov.spawn.Exceptions.Base.BaseNotFoundException;
import com.danielagapov.spawn.Exceptions.Base.BaseSaveException;
import com.danielagapov.spawn.Exceptions.Logger.ILogger;
import com.danielagapov.spawn.Mappers.BlockedUserMapper;
import com.danielagapov.spawn.Models.BlockedUser;
import com.danielagapov.spawn.Models.User;
import com.danielagapov.spawn.Repositories.IBlockedUserRepository;
import com.danielagapov.spawn.Services.FriendRequest.IFriendRequestService;
import com.danielagapov.spawn.Services.User.IUserService;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import com.danielagapov.spawn.DTOs.BlockedUserDTO;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class BlockedUserService implements IBlockedUserService {

    private final IBlockedUserRepository repository;
    private final IUserService userService;

    private final ILogger logger;

    public BlockedUserService(IBlockedUserRepository repository, IUserService userService, ILogger logger) {
        this.repository = repository;
        this.userService = userService;
        this.logger = logger;
    }

    @Override
    public void blockUser(UUID blockerId, UUID blockedId, String reason) {
        if (blockerId.equals(blockedId)) return;

        try {
            if (isBlocked(blockerId, blockedId)) {
                return;
            }

            userService.removeFriendshipBetweenUsers(blockerId, blockedId);

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
}
