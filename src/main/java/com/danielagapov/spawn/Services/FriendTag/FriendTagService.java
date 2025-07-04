package com.danielagapov.spawn.Services.FriendTag;

import com.danielagapov.spawn.DTOs.FriendTag.AbstractFriendTagDTO;
import com.danielagapov.spawn.DTOs.FriendTag.FriendTagDTO;
import com.danielagapov.spawn.DTOs.FriendTag.FullFriendTagDTO;
import com.danielagapov.spawn.DTOs.User.BaseUserDTO;
import com.danielagapov.spawn.Enums.EntityType;
import com.danielagapov.spawn.Exceptions.Base.BaseNotFoundException;
import com.danielagapov.spawn.Exceptions.Base.BaseSaveException;
import com.danielagapov.spawn.Exceptions.Base.BasesNotFoundException;
import com.danielagapov.spawn.Exceptions.Logger.ILogger;
import com.danielagapov.spawn.Mappers.FriendTagMapper;
import com.danielagapov.spawn.Mappers.UserMapper;
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
    public List<FriendTagDTO> getAllFriendTags() {
        try {
            // Use the helper methods you created
            Map<FriendTag, UUID> ownerUserIdsMap = userService.getOwnerUserIdsMap();
            Map<FriendTag, List<UUID>> friendUserIdsMap = userService.getFriendUserIdsMap();

            // Corrected the argument names to match the method signature
            return FriendTagMapper.toDTOList(repository.findAll(), ownerUserIdsMap, friendUserIdsMap);
        } catch (DataAccessException e) {
            logger.error(e.getMessage());
            throw new BasesNotFoundException(EntityType.FriendTag);
        } catch (Exception e) {
            logger.error(e.getMessage());
            throw e;
        }
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
    public FullFriendTagDTO getFullFriendTagById(UUID id) {
        FriendTagDTO ft = getFriendTagById(id);
        return getFullFriendTagByFriendTag(ft);
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
            @CacheEvict(value = "activitiesByFriendTagId", key = "#id"),
            @CacheEvict(value = "filteredFeedActivities", key = "#newFriendTag.ownerUserId"),
            @CacheEvict(value = "friendTagsByOwnerId", key = "#newFriendTag.ownerUserId")
    })
    public FriendTagDTO replaceFriendTag(FriendTagDTO newFriendTag, UUID id) {
        return repository.findById(id).map(friendTag -> {
            friendTag.setColorHexCode(newFriendTag.getColorHexCode());
            friendTag.setDisplayName(newFriendTag.getDisplayName());
            friendTag.setLastUpdated(java.time.Instant.now());
            repository.save(friendTag);
            return FriendTagMapper.toDTO(friendTag, newFriendTag.getOwnerUserId(), List.of());
        }).orElseGet(() -> {
            FriendTag friendTagEntity = FriendTagMapper.toEntity(newFriendTag);
            friendTagEntity.setLastUpdated(java.time.Instant.now());
            repository.save(friendTagEntity);
            return FriendTagMapper.toDTO(friendTagEntity, newFriendTag.getOwnerUserId(), List.of());
        });
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
    @Caching(evict = {
            @CacheEvict(value = "activitiesByFriendTagId", key = "#friendTagId"),
            @CacheEvict(value = "filteredFeedActivities", key = "#friendTagId")
    })
    public void saveUsersToFriendTag(UUID friendTagId, List<BaseUserDTO> friends) {
        for (BaseUserDTO friend : friends) {
            saveUserToFriendTag(friendTagId, friend.getId());
        }
    }

    @Override
    @CacheEvict(value = "activitiesByFriendTagId", key = "#friendTagId")
    public void bulkAddUsersToFriendTag(UUID friendTagId, List<BaseUserDTO> friends) {
        for (BaseUserDTO friend : friends) {
            saveUserToFriendTag(friendTagId, friend.getId());
        }
    }

    @Override
    public FullFriendTagDTO getFullFriendTagByFriendTag(FriendTagDTO friendTag) {
        return new FullFriendTagDTO(
                friendTag.getId(),
                friendTag.getDisplayName(),
                friendTag.getColorHexCode(),
                friendTag.getFriendUserIds().stream().map(userService::getUserById).collect(Collectors.toList()),
                friendTag.isEveryone());
    }

    @Override
    public List<FullFriendTagDTO> convertFriendTagsToFullFriendTags(List<FriendTagDTO> friendTags) {
        // For large number of tags, use the optimized method instead
        if (friendTags.size() > 0) {
            try {
                // Get the owner ID from the first tag to use with the optimized method
                UUID ownerId = friendTags.get(0).getOwnerUserId();
                return getFullFriendTagsWithFriendsByOwnerId(ownerId);
            } catch (Exception e) {
                logger.warn("Failed to use optimized method, falling back to standard conversion: " + e.getMessage());
                // Fall back to the original implementation if there's an error
            }
        }
        
        // Original implementation as fallback
        return friendTags.stream()
                .map(this::getFullFriendTagByFriendTag)
                .collect(Collectors.toList());
    }
    
    /**
     * Optimized method to get all friend tags with their friends for a user in a single database query
     * This replaces the need to do separate queries for each tag's friends
     *
     * @param ownerId The ID of the user who owns the friend tags
     * @return List of FullFriendTagDTO objects with all friend data included
     */
    @Override
    public List<FullFriendTagDTO> getFullFriendTagsWithFriendsByOwnerId(UUID ownerId) {
        try {
            // Fetch all tags for this owner first (empty friends will be populated later)
            List<FriendTag> allTags = repository.findByOwnerId(ownerId);
            
            // If no tags found, return empty list
            if (allTags.isEmpty()) {
                return new ArrayList<>();
            }
            
            // Sort with "Everyone" tag first for consistency
            allTags.sort((tag1, tag2) -> {
                if (tag1.isEveryone()) return -1;
                if (tag2.isEveryone()) return 1;
                return 0;
            });
            
            // Create map of empty FullFriendTagDTOs keyed by their ID
            Map<UUID, FullFriendTagDTO> tagDtoMap = new HashMap<>();
            for (FriendTag tag : allTags) {
                tagDtoMap.put(tag.getId(), new FullFriendTagDTO(
                    tag.getId(),
                    tag.getDisplayName(),
                    tag.getColorHexCode(),
                    new ArrayList<>(),  // Empty friends list to be populated
                    tag.isEveryone()
                ));
            }
            
            // Fetch all UserFriendTag entries in a single optimized query
            List<UserFriendTag> userFriendTags = uftRepository.findAllTagsWithFriendsByOwnerId(ownerId);
            
            // Populate friend data into the appropriate tag DTOs
            for (UserFriendTag uft : userFriendTags) {
                UUID tagId = uft.getFriendTag().getId();
                User friend = uft.getFriend();
                
                // Skip if this is the owner (not a friend) or tag doesn't exist in map (shouldn't happen)
                if (friend.getId().equals(ownerId) || !tagDtoMap.containsKey(tagId)) {
                    continue;
                }
                
                // Add friend to the appropriate tag's friends list
                FullFriendTagDTO tagDto = tagDtoMap.get(tagId);
                tagDto.getFriends().add(UserMapper.toDTO(friend));
            }
            
            // Return list of fully populated DTOs, maintaining the sort order
            return allTags.stream()
                .map(tag -> tagDtoMap.get(tag.getId()))
                .collect(Collectors.toList());
        } catch (Exception e) {
            logger.error("Error retrieving full friend tags with friends: " + e.getMessage());
            throw e;
        }
    }

    /// this function takes the owner user id of a friend tag, and the friend's user id
    /// it will return precisely one of the friend tags that the owner has placed this
    /// it will return precisely one of the friend tags that the owner has placed this
    /// friend inside, even if they've placed them in multiple friend tags
    /// -> currently, on the product side, we don't specify a rule for which should take precedence.
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
    public List<FullFriendTagDTO> getPertainingFullFriendTagsForFriend(UUID ownerUserId, UUID friendUserId) {
        // Fetch all friend tags for the owner
        List<FriendTagDTO> friendTags = getPertainingFriendTagsForFriend(ownerUserId, friendUserId);
        return convertFriendTagsToFullFriendTags(friendTags);
    }

    @Override
    public List<FriendTagDTO> getPertainingFriendTagsForFriend(UUID ownerUserId, UUID friendUserId) {
        // Fetch all friend tags for the owner
        return getFriendTagsByOwnerId(ownerUserId).stream()
                // Filter to include only friend tags where friendUserId exists in friendUserIds
                .filter(friendTag -> friendTag.getFriendUserIds().contains(friendUserId))
                .collect(Collectors.toList());
    }

    @Override
    public List<BaseUserDTO> getFriendsNotAddedToTag(UUID friendTagId) {
        FriendTagDTO friendTagDTO = getFriendTagById(friendTagId);
        UUID requestingUserId = friendTagDTO.getOwnerUserId();

        List<UUID> friendUserIds = userService.getFriendUserIdsByUserId(requestingUserId);

        List<UUID> friendIdsNotAddedToTag = friendUserIds.stream()
                .filter(friendUserId -> !friendTagDTO.getFriendUserIds().contains(friendUserId))
                .toList();

        return friendIdsNotAddedToTag.stream()
                .map(userService::getBaseUserById)
                .collect(Collectors.toList());
    }

    @Override
    public List<FullFriendTagDTO> getTagsNotAddedToFriend(UUID ownerUserId, UUID friendUserId) {
        try {
            // Get all friend tags owned by the owner
            List<FriendTagDTO> allOwnerTags = getFriendTagsByOwnerId(ownerUserId);

            // Get all friend tags that the friend is currently in
            List<FriendTagDTO> friendsCurrentTags = allOwnerTags.stream()
                    .filter(tag -> tag.getFriendUserIds().contains(friendUserId))
                    .toList();

            // Filter out the tags the friend is already in and convert to FullFriendTagDTO
            return allOwnerTags.stream()
                    .filter(tag -> !friendsCurrentTags.contains(tag))
                    // Don't include the "Everyone" tag as an option
                    .filter(tag -> !tag.isEveryone())
                    .map(this::getFullFriendTagByFriendTag)
                    .collect(Collectors.toList());
        } catch (DataAccessException e) {
            logger.error(e.getMessage());
            throw new RuntimeException("Error retrieving tags not added to friend", e);
        } catch (Exception e) {
            logger.error(e.getMessage());
            throw e;
        }
    }

    @Override
    public void addFriendToFriendTags(List<UUID> friendTagIds, UUID friendUserId) {
        for (UUID friendTagId : friendTagIds) {
            saveUserToFriendTag(friendTagId, friendUserId);
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
