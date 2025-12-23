package com.danielagapov.spawn.user.internal.services;

import com.danielagapov.spawn.shared.exceptions.Base.BaseNotFoundException;
import com.danielagapov.spawn.shared.exceptions.Logger.ILogger;
import com.danielagapov.spawn.shared.util.EntityType;
import com.danielagapov.spawn.shared.util.LoggingUtils;
import com.danielagapov.spawn.shared.util.UserStatus;
import com.danielagapov.spawn.social.internal.repositories.IFriendshipRepository;
import com.danielagapov.spawn.user.api.dto.FullFriendUserDTO;
import com.danielagapov.spawn.user.internal.domain.User;
import com.danielagapov.spawn.user.internal.repositories.IUserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for querying friendship-related data.
 * This service breaks the circular dependency between UserService and UserSearchService
 * by extracting common friendship query operations.
 * 
 * Following CQRS principles, this service handles only READ operations for friendships.
 * Write operations (like creating friendships) remain in UserService.
 */
@Service
public class UserFriendshipQueryService implements IUserFriendshipQueryService {
    
    private final IUserRepository userRepository;
    private final IFriendshipRepository friendshipRepository;
    private final ILogger logger;
    
    @Value("${ADMIN_USERNAME:admin}")
    private String adminUsername;
    
    public UserFriendshipQueryService(
            IUserRepository userRepository,
            IFriendshipRepository friendshipRepository,
            ILogger logger) {
        this.userRepository = userRepository;
        this.friendshipRepository = friendshipRepository;
        this.logger = logger;
    }
    
    /**
     * Helper method to check if a user is the admin user
     */
    private boolean isAdminUser(User user) {
        return user != null && adminUsername.equals(user.getUsername());
    }
    
    /**
     * Helper method to filter out admin users from FullFriendUserDTO lists
     */
    private List<FullFriendUserDTO> filterOutAdminFromFullFriendUserDTOs(List<FullFriendUserDTO> users) {
        return users.stream()
                .filter(user -> !adminUsername.equals(user.getUsername()))
                .collect(Collectors.toList());
    }
    
    @Override
    public List<UUID> getFriendUserIdsByUserId(UUID userId) {
        try {
            return friendshipRepository.findAllByUserIdBidirectional(userId)
                    .stream()
                    .map(f -> f.getUserA().getId().equals(userId) ? f.getUserB().getId() : f.getUserA().getId())
                    .collect(Collectors.toList());
        } catch (Exception e) {
            logger.error("Error getting friend user IDs for user: " + LoggingUtils.formatUserIdInfo(userId) + ": " + e.getMessage());
            throw e;
        }
    }
    
    @Override
    public List<User> getFriendUsersByUserId(UUID userId) {
        try {
            // Get the friend IDs
            List<UUID> friendIds = getFriendUserIdsByUserId(userId);
            
            // Fetch and return the user entities
            if (!friendIds.isEmpty()) {
                return userRepository.findAllById(friendIds);
            }
            
            return List.of();
        } catch (Exception e) {
            logger.error("Error retrieving friend users by user ID: " + LoggingUtils.formatUserIdInfo(userId) + ": " + e.getMessage());
            throw e;
        }
    }
    
    @Override
    public List<FullFriendUserDTO> getFullFriendUsersByUserId(UUID requestingUserId) {
        try {
            List<UUID> friendIds = getFriendUserIdsByUserId(requestingUserId);
            if (friendIds.isEmpty()) {
                return List.of();
            }
            List<User> friendUsers = userRepository.findAllById(friendIds);
            List<FullFriendUserDTO> result = new ArrayList<>();
            for (User friend : friendUsers) {
                FullFriendUserDTO dto = new FullFriendUserDTO(
                        friend.getId(),
                        friend.getUsername(),
                        friend.getProfilePictureUrlString(),
                        friend.getName(),
                        friend.getBio(),
                        friend.getEmail()
                );
                result.add(dto);
            }
            return filterOutAdminFromFullFriendUserDTOs(result);
        } catch (Exception e) {
            logger.error("Error retrieving full friend users: " + e.getMessage());
            throw e;
        }
    }
    
    @Override
    public boolean isUserFriendOfUser(UUID userId, UUID potentialFriendId) {
        try {
            return friendshipRepository.existsBidirectionally(userId, potentialFriendId);
        } catch (Exception e) {
            logger.error("Error checking if user is friend of user: " +
                    LoggingUtils.formatUserIdInfo(userId) + " and " +
                    LoggingUtils.formatUserIdInfo(potentialFriendId) + ": " + e.getMessage());
            throw e;
        }
    }
    
    @Override
    public int getMutualFriendCount(UUID userId1, UUID userId2) {
        try {
            List<UUID> user1Friends = new ArrayList<>(getFriendUserIdsByUserId(userId1));
            List<UUID> user2Friends = getFriendUserIdsByUserId(userId2);
            
            // Create a mutable copy of user1Friends and retain only elements that are also in user2Friends
            user1Friends.retainAll(user2Friends);
            return user1Friends.size();
        } catch (Exception e) {
            logger.error("Error calculating mutual friend count: " + e.getMessage());
            throw e;
        }
    }
    
    @Override
    public User getUserEntityById(UUID userId) {
        try {
            return userRepository.findById(userId)
                    .orElseThrow(() -> new BaseNotFoundException(EntityType.User, userId));
        } catch (Exception e) {
            logger.error("Error retrieving user entity: " + LoggingUtils.formatUserIdInfo(userId) + ": " + e.getMessage());
            throw e;
        }
    }
    
    @Override
    public List<User> getAllActiveUsers() {
        try {
            return userRepository.findAllUsersByStatus(UserStatus.ACTIVE);
        } catch (Exception e) {
            logger.error("Error retrieving all active users: " + e.getMessage());
            throw e;
        }
    }
}

