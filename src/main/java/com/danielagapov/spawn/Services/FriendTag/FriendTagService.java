package com.danielagapov.spawn.Services.FriendTag;

import com.danielagapov.spawn.DTOs.FriendTag.AbstractFriendTagDTO;
import com.danielagapov.spawn.DTOs.FriendTag.FriendTagDTO;
import com.danielagapov.spawn.DTOs.User.BaseUserDTO;
import com.danielagapov.spawn.Enums.EntityType;
import com.danielagapov.spawn.Exceptions.Base.BaseNotFoundException;
import com.danielagapov.spawn.Exceptions.Base.BaseSaveException;
import com.danielagapov.spawn.Exceptions.Base.BasesNotFoundException;
import com.danielagapov.spawn.Exceptions.Logger.ILogger;
import com.danielagapov.spawn.Mappers.FriendTagMapper;
import com.danielagapov.spawn.Models.FriendTag;
import com.danielagapov.spawn.Models.User.User;
import com.danielagapov.spawn.Models.UserFriendTag;
import com.danielagapov.spawn.Repositories.IFriendTagRepository;
import com.danielagapov.spawn.Repositories.IUserFriendTagRepository;
import com.danielagapov.spawn.Repositories.User.IUserRepository;
import com.danielagapov.spawn.Services.User.IUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class FriendTagService implements IFriendTagService {
    private final IFriendTagRepository repository;
    private final IUserService userService;
    private final IUserFriendTagRepository uftRepository;
    private final IUserRepository userRepository;
    private final ILogger logger;
    private final CacheManager cacheManager;
    
    @Autowired
    public FriendTagService(IFriendTagRepository repository, IUserService userService,
                            IUserFriendTagRepository uftRepository, IUserRepository userRepository, ILogger logger, CacheManager cacheManager) {
        this.repository = repository;
        this.userService = userService;
        this.uftRepository = uftRepository;
        this.userRepository = userRepository;
        this.logger = logger;
        this.cacheManager = cacheManager;
    }

    @Override
    public FriendTagDTO getFriendTagById(UUID id) {
        return repository.findById(id)
                .map(friendTag -> {
                    UUID ownerId = friendTag.getOwnerId();
                    List<UUID> friendUserIds = userService.getFriendUserIdsByFriendTagId(friendTag.getId());
                    return FriendTagMapper.toDTO(friendTag, ownerId, friendUserIds);
                })
                .orElseThrow(() -> new BaseNotFoundException(EntityType.FriendTag, id));
    }

    @Override
    public List<UUID> getFriendTagIdsByOwnerUserId(UUID id) {
        // Fetch FriendTag entities related to the given user (for example, by userId)
        return repository.findByOwnerId(id)
                .stream()
                .filter(Objects::nonNull)
                .map(FriendTag::getId)
                .collect(Collectors.toList());
    }

    @Override
    @Cacheable(value = "friendTagsByOwnerId", key = "#ownerId")
    public List<FriendTagDTO> getFriendTagsByOwnerId(UUID ownerId) {
        try {
            // Fetch the raw data
            Map<FriendTag, UUID> ownerUserIdsMap = userService.getOwnerUserIdsMap();
            Map<FriendTag, List<UUID>> friendUserIdsMap = userService.getFriendUserIdsMap();
            List<FriendTag> friendTags = repository.findByOwnerId(ownerId);

            // Sort the list with "Everyone" tag first
            friendTags.sort((tag1, tag2) -> {
                if (tag1.isEveryone()) return -1; // "Everyone" comes first
                if (tag2.isEveryone()) return 1;
                return 0; // Maintain order for other tags
            });

            // Convert to DTOs
            return FriendTagMapper.toDTOList(friendTags, ownerUserIdsMap, friendUserIdsMap);
        } catch (DataAccessException e) {
            logger.error(e.getMessage());
            throw new RuntimeException("Error retrieving friendTags", e);
        } catch (Exception e) {
            logger.error(e.getMessage());
            throw e;
        }
    }

    @Override
    @Caching(evict = {
            @CacheEvict(value = "activitiesByFriendTagId", key = "#friendTag.id", condition = "#friendTag.id != null"),
            @CacheEvict(value = "filteredFeedActivities", key = "#friendTag.ownerUserId"),
            @CacheEvict(value = "friendTagsByOwnerId", key = "#friendTag.ownerUserId")
    })
    public FriendTagDTO saveFriendTag(AbstractFriendTagDTO friendTag) {
        FriendTag friendTagEntity = FriendTagMapper.toEntity(friendTag);
        friendTagEntity = saveFriendTagEntity(friendTagEntity);
        return FriendTagMapper.toDTO(friendTagEntity, friendTagEntity.getOwnerId(), List.of());
    }

    private FriendTag saveFriendTagEntity(FriendTag friendTag) {
        try {
            // Set the lastModified field to the current timestamp
            friendTag.setLastUpdated(java.time.Instant.now());
            return repository.save(friendTag);
        } catch (DataAccessException e) {
            logger.error(e.getMessage());
            throw new BaseSaveException("Failed to save friendTag: " + e.getMessage());
        } catch (Exception e) {
            logger.error(e.getMessage());
            throw e;
        }
    }

    @Override
    @Caching(evict = {
            @CacheEvict(value = "activitiesByFriendTagId", key = "#id")
    })
    public boolean deleteFriendTagById(UUID id) {
        if (!repository.existsById(id)) {
            throw new BaseNotFoundException(EntityType.FriendTag, id);
        }

        try {
            // Alter to check if the tag is an "Everyone" tag before deleting
            FriendTagDTO friendTag = getFriendTagById(id);
            UUID ownerId = friendTag.getOwnerUserId();

            if (cacheManager.getCache("friendTagsByOwnerId") != null) {
                cacheManager.getCache("friendTagsByOwnerId").evict(ownerId);
            }

            if (friendTag.isEveryone()) {
                logger.warn("Cannot delete the 'Everyone' tag");
                return true;
            }

            uftRepository.findAllById(List.of(id)).forEach((UserFriendTag uftEntry) -> uftRepository.deleteById(uftEntry.getId()));
            repository.deleteById(id);

            if (cacheManager.getCache("filteredFeedActivities") != null) {
                cacheManager.getCache("filteredFeedActivities").evict(ownerId);
            }

            return true;
        } catch (Exception e) {
            logger.error(e.getMessage());
            return false;
        }
    }

    @Override
    @Caching(evict = {
            @CacheEvict(value = "activitiesByFriendTagId", key = "#id"),
            @CacheEvict(value = "filteredFeedActivities", key = "#id")
    })
    public void saveUserToFriendTag(UUID id, UUID userId) {
        if (!repository.existsById(id)) {
            throw new BaseNotFoundException(EntityType.FriendTag, id);
        }
        if (!userRepository.existsById(userId)) {
            throw new BaseNotFoundException(EntityType.User, userId);
        }

        FriendTag friendTag = repository.findById(id)
                .orElseThrow(() -> new BaseNotFoundException(EntityType.FriendTag, id));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BaseNotFoundException(EntityType.User, userId));

        boolean exists = uftRepository.existsByFriendTagIdAndFriendId(id, userId);
        if (exists) {
            logger.info("User " + userId + " is already in FriendTag " + id);
            return;
        }

        UserFriendTag uft = new UserFriendTag();
        uft.setFriend(user);
        uft.setFriendTag(friendTag);
        try {
            uftRepository.save(uft);
        } catch (DataAccessException e) {
            logger.error(e.getMessage());
            throw new BaseSaveException("Failed to save new UserFriendTag");
        } catch (Exception e) {
            logger.error(e.getMessage());
            throw e;
        }
    }

    @Override
    @Caching(evict = {
            @CacheEvict(value = "activitiesByFriendTagId", key = "#id"),
            @CacheEvict(value = "filteredFeedActivities", key = "#id")
    })
    public void removeUserFromFriendTag(UUID id, UUID userId) {
        // Check if the FriendTag exists
        if (!repository.existsById(id)) {
            throw new BaseNotFoundException(EntityType.FriendTag, id);
        }
        // Check if the User exists
        if (!userRepository.existsById(userId)) {
            throw new BaseNotFoundException(EntityType.User, userId);
        }

        try {
            // Remove the UserFriendTag entity
            uftRepository.deleteByFriendTagIdAndFriendId(id, userId);
        } catch (DataAccessException e) {
            logger.error(e.getMessage());
            throw new BaseSaveException("Failed to remove UserFriendTag (friend from friend tag)");
        } catch (Exception e) {
            logger.error(e.getMessage());
            throw e;
        }
    }

    @Override
    public Optional<FriendTagDTO> getPertainingFriendTagBetweenUsers(UUID ownerUserId, UUID friendUserId) {
        // Fetch all friend tags for the owner
        ArrayList<FriendTagDTO> friendTags = new ArrayList<>(getFriendTagsByOwnerId(ownerUserId).stream()
                // Filter to include only friend tags where friendUserId exists in friendUserIds
                .filter(friendTag -> friendTag.getFriendUserIds().contains(friendUserId))
                .toList());

        // if there's more than just the 'everyone' tag, don't use that one
        if (friendTags.size() > 1) friendTags.removeIf(FriendTagDTO::isEveryone);

        // arbitrarily grab the first tag that isn't the 'everyone' tag
        try {
            return friendTags.stream().findFirst();
        } catch (Exception e) {
            logger.error(e.getMessage());
            throw e;
        }
    }

    @Override
    public List<UUID> getFriendIdsByTagId(UUID tagId) {
        try {
            // Check if the tag exists
            if (!repository.existsById(tagId)) {
                throw new BaseNotFoundException(EntityType.FriendTag, tagId);
            }
            
            // Use the repository method to get friend IDs directly
            return uftRepository.findFriendIdsByTagId(tagId);
        } catch (DataAccessException e) {
            logger.error("Database error retrieving friend IDs for tag " + tagId + ": " + e.getMessage());
            throw new RuntimeException("Error retrieving friend IDs", e);
        } catch (Exception e) {
            logger.error("Error retrieving friend IDs for tag " + tagId + ": " + e.getMessage());
            throw e;
        }
    }
}
