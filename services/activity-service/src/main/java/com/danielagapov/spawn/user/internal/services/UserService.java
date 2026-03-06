package com.danielagapov.spawn.user.internal.services;

import com.danielagapov.spawn.shared.exceptions.Base.BaseNotFoundException;
import com.danielagapov.spawn.shared.exceptions.Logger.ILogger;
import com.danielagapov.spawn.shared.util.EntityType;
import com.danielagapov.spawn.shared.util.UserMapper;
import com.danielagapov.spawn.shared.feign.MonolithUserClient;
import com.danielagapov.spawn.user.api.dto.BaseUserDTO;
import com.danielagapov.spawn.user.api.dto.UserDTO;
import com.danielagapov.spawn.user.api.dto.FriendUser.FullFriendUserDTO;
import com.danielagapov.spawn.user.internal.domain.User;
import com.danielagapov.spawn.user.internal.repositories.IUserRepository;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Service
public class UserService implements IUserService {
    private final IUserRepository userRepository;
    private final MonolithUserClient monolithUserClient;
    private final ILogger logger;

    public UserService(IUserRepository userRepository, MonolithUserClient monolithUserClient, ILogger logger) {
        this.userRepository = userRepository;
        this.monolithUserClient = monolithUserClient;
        this.logger = logger;
    }

    @Override
    public UserDTO getUserById(UUID id) {
        // Try local DB first, then Feign
        try {
            User user = userRepository.findById(id)
                    .orElseThrow(() -> new BaseNotFoundException(EntityType.User, id));
            List<UUID> friendIds = Collections.emptyList();
            try {
                friendIds = monolithUserClient.getFullUserById(id).getFriendUserIds();
            } catch (Exception e) {
                logger.warn("Could not fetch friend IDs from monolith for user " + id + ": " + e.getMessage());
            }
            return UserMapper.toDTO(user, friendIds);
        } catch (BaseNotFoundException e) {
            // Fall back to Feign client
            try {
                return monolithUserClient.getFullUserById(id);
            } catch (Exception feignEx) {
                throw new BaseNotFoundException(EntityType.User, id);
            }
        }
    }

    @Override
    public User getUserEntityById(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new BaseNotFoundException(EntityType.User, id));
    }

    @Override
    public BaseUserDTO getBaseUserById(UUID id) {
        try {
            User user = userRepository.findById(id)
                    .orElseThrow(() -> new BaseNotFoundException(EntityType.User, id));
            return UserMapper.toDTO(user);
        } catch (BaseNotFoundException e) {
            try {
                return monolithUserClient.getUserById(id);
            } catch (Exception feignEx) {
                throw new BaseNotFoundException(EntityType.User, id);
            }
        }
    }

    @Override
    public List<FullFriendUserDTO> getFullFriendUsersByUserId(UUID requestingUserId) {
        try {
            return monolithUserClient.getFriendsByUserId(requestingUserId);
        } catch (Exception e) {
            logger.warn("Could not fetch friends from monolith for user " + requestingUserId + ": " + e.getMessage());
            return Collections.emptyList();
        }
    }
}
