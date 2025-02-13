package com.danielagapov.spawn.Services.FriendTag;

import com.danielagapov.spawn.DTOs.FriendTagDTO;
import com.danielagapov.spawn.DTOs.FullFriendTagDTO;
import com.danielagapov.spawn.DTOs.FullUserDTO;
import com.danielagapov.spawn.DTOs.UserDTO;
import com.danielagapov.spawn.Enums.EntityType;
import com.danielagapov.spawn.Exceptions.Base.BaseNotFoundException;
import com.danielagapov.spawn.Exceptions.Base.BaseSaveException;
import com.danielagapov.spawn.Exceptions.Base.BasesNotFoundException;
import com.danielagapov.spawn.Exceptions.Logger.ILogger;
import com.danielagapov.spawn.Mappers.FriendTagMapper;
import com.danielagapov.spawn.Models.FriendTag;
import com.danielagapov.spawn.Models.User;
import com.danielagapov.spawn.Models.UserFriendTag;
import com.danielagapov.spawn.Repositories.IFriendTagRepository;
import com.danielagapov.spawn.Repositories.IUserFriendTagRepository;
import com.danielagapov.spawn.Repositories.IUserRepository;
import com.danielagapov.spawn.Services.User.IUserService;
import org.springframework.beans.factory.annotation.Autowired;
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

    @Autowired
    public FriendTagService(IFriendTagRepository repository, IUserService userService,
                            IUserFriendTagRepository uftRepository, IUserRepository userRepository, ILogger logger) {
        this.repository = repository;
        this.userService = userService;
        this.uftRepository = uftRepository;
        this.userRepository = userRepository;
        this.logger = logger;
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
            logger.log(e.getMessage());
            throw new BasesNotFoundException(EntityType.FriendTag);
        } catch (Exception e) {
            logger.log(e.getMessage());
            throw e;
        }
    }

    @Override
    public List<FullFriendTagDTO> getAllFullFriendTags() {
        return getAllFriendTags().stream()
                .map(this::getFullFriendTagByFriendTag)
                .collect(Collectors.toList());
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
        List<FriendTag> friendTags = repository.findByOwnerId(id);

        // Extract and return the FriendTag IDs
        return friendTags.stream()
                .map(FriendTag::getId) // Get the ID of each FriendTag
                .collect(Collectors.toList());
    }

    @Override
    public List<FriendTagDTO> getFriendTagsByOwnerId(UUID ownerId) {
        try {
            Map<FriendTag, UUID> ownerUserIdsMap = userService.getOwnerUserIdsMap();
            Map<FriendTag, List<UUID>> friendUserIdsMap = userService.getFriendUserIdsMap();
            return FriendTagMapper.toDTOList(repository.findByOwnerId(ownerId), ownerUserIdsMap, friendUserIdsMap);
        } catch (DataAccessException e) {
            logger.log(e.getMessage());
            throw new RuntimeException("Error retrieving friendTags", e);
        } catch (Exception e) {
            logger.log(e.getMessage());
            throw e;
        }
    }

    @Override
    public List<FullFriendTagDTO> getFullFriendTagsByOwnerId(UUID ownerId) {
        return getFriendTagsByOwnerId(ownerId).stream().map(this::getFullFriendTagByFriendTag).collect(Collectors.toList());
    }

    @Override
    public FriendTagDTO saveFriendTag(FriendTagDTO friendTag) {
        try {
            FriendTag friendTagEntity = FriendTagMapper.toEntity(friendTag);
            friendTagEntity = repository.save(friendTagEntity);
            return FriendTagMapper.toDTO(friendTagEntity, friendTag.ownerUserId(), List.of());
        } catch (DataAccessException e) {
            logger.log(e.getMessage());
            throw new BaseSaveException("Failed to save friendTag: " + e.getMessage());
        } catch (Exception e) {
            logger.log(e.getMessage());
            throw e;
        }
    }

    @Override
    public FriendTagDTO replaceFriendTag(FriendTagDTO newFriendTag, UUID id) {
        return repository.findById(id).map(friendTag -> {
            friendTag.setColorHexCode(newFriendTag.colorHexCode());
            friendTag.setDisplayName(newFriendTag.displayName());
            repository.save(friendTag);
            return FriendTagMapper.toDTO(friendTag, newFriendTag.ownerUserId(), List.of());
        }).orElseGet(() -> {
            FriendTag friendTagEntity = FriendTagMapper.toEntity(newFriendTag);
            repository.save(friendTagEntity);
            return FriendTagMapper.toDTO(friendTagEntity, newFriendTag.ownerUserId(), List.of());
        });
    }

    @Override
    public boolean deleteFriendTagById(UUID id) {
        if (!repository.existsById(id)) {
            throw new BaseNotFoundException(EntityType.FriendTag, id);
        }

        try {
            uftRepository.findAllById(List.of(id)).forEach((UserFriendTag uftEntry) -> uftRepository.deleteById(uftEntry.getId()));
            repository.deleteById(id);
            return true;
        } catch (Exception e) {
            logger.log(e.getMessage());
            return false;
        }
    }

    @Override
    public void saveUserToFriendTag(UUID id, UUID userId) {
        if (!repository.existsById(id)) {
            throw new BaseNotFoundException(EntityType.FriendTag, id);
        }
        if (!userRepository.existsById(userId)) {
            throw new BaseNotFoundException(EntityType.User, userId);
        }
        // TODO consider adding a more descriptive error
        FriendTag friendTag = repository.findById(id).orElseThrow(() -> new BaseNotFoundException(EntityType.FriendTag, id));
        User user = userRepository.findById(userId).orElseThrow(() -> new BaseNotFoundException(EntityType.User, userId));
        UserFriendTag uft = new UserFriendTag();
        uft.setFriend(user);
        uft.setFriendTag(friendTag);
        try {
            uftRepository.save(uft);
        } catch (DataAccessException e) {
            logger.log(e.getMessage());
            throw new BaseSaveException("Failed to save new UserFriendTag");
        } catch (Exception e) {
            logger.log(e.getMessage());
            throw e;
        }
    }

    @Override
    public void removeUserFromFriendTag(UUID id, UUID userId) {
        if (!repository.existsById(id)) {
            throw new BaseNotFoundException(EntityType.FriendTag, id);
        }
        if (!userRepository.existsById(userId)) {
            throw new BaseNotFoundException(EntityType.User, userId);
        }
        // TODO consider adding a more descriptive error
        FriendTag friendTag = repository.findById(id).orElseThrow(() -> new BaseNotFoundException(EntityType.FriendTag, id));
        User user = userRepository.findById(userId).orElseThrow(() -> new BaseNotFoundException(EntityType.User, userId));
        UserFriendTag uft = new UserFriendTag();
        uft.setFriend(user);
        uft.setFriendTag(friendTag);

        try {
            uftRepository.save(uft);
        } catch (DataAccessException e) {
            logger.log(e.getMessage());
            throw new BaseSaveException("Failed to save new UserFriendTag");
        } catch (Exception e) {
            logger.log(e.getMessage());
            throw e;
        }
    }

    @Override
    public void saveUsersToFriendTag(UUID friendTagId, List<FullUserDTO> friends) {
        for (FullUserDTO friend : friends) {
            saveUserToFriendTag(friendTagId, friend.id());
        }
    }

    @Override
    public FullFriendTagDTO getFullFriendTagByFriendTag(FriendTagDTO friendTag) {
        return new FullFriendTagDTO(
                friendTag.id(),
                friendTag.displayName(),
                friendTag.colorHexCode(),
                friendTag.friendUserIds().stream().map(userService::getUserById).collect(Collectors.toList()),
                friendTag.isEveryone());
    }

    @Override
    public List<FullFriendTagDTO> convertFriendTagsToFullFriendTags(List<FriendTagDTO> friendTags) {
        return friendTags.stream()
                .map(this::getFullFriendTagByFriendTag)
                .collect(Collectors.toList());
    }

    /// this function takes the owner user id of a friend tag, and the friend's user id
    /// it will return precisely one of the friend tags that the owner has placed this
    /// friend inside, even if they've placed them in multiple friend tags
    /// -> currently, on the product side, we don't specify a rule for which should take precedence.
    @Override
    public FriendTagDTO getPertainingFriendTagByUserIds(UUID ownerUserId, UUID friendUserId) {
        // Fetch all friend tags for the owner
        ArrayList<FriendTagDTO> friendTags = new ArrayList<>(getFriendTagsByOwnerId(ownerUserId).stream()
                // Filter to include only friend tags where friendUserId exists in friendUserIds
                .filter(friendTag -> friendTag.friendUserIds().contains(friendUserId))
                .toList());

        // if there's more than just the 'everyone' tag, don't use that one
        if (friendTags.size() > 1) friendTags.removeIf(FriendTagDTO::isEveryone);

        // arbitrarily grab the first tag that isn't the 'everyone' tag
        try {
            Optional<FriendTagDTO> friendTag = friendTags.stream().findFirst();
            if (friendTag.isPresent()) { // just null-checking
                return friendTag.get();
            } else {
                throw new BaseNotFoundException(EntityType.FriendTag, friendUserId);
            }
        } catch (Exception e) {
            logger.log(e.getMessage());
            throw e;
        }
    }

    @Override
    public List<FullFriendTagDTO> getPertainingFriendTagsForFriend(UUID ownerUserId, UUID friendUserId) {
        // Fetch all friend tags for the owner
        List<FriendTagDTO> friendTags = getFriendTagsByOwnerId(ownerUserId).stream()
                // Filter to include only friend tags where friendUserId exists in friendUserIds
                .filter(friendTag -> friendTag.friendUserIds().contains(friendUserId))
                .collect(Collectors.toList());
        return convertFriendTagsToFullFriendTags(friendTags);
    }

    @Override
    public List<FullUserDTO> getFriendsNotAddedToTag(UUID friendTagId) {
        FriendTagDTO friendTagDTO = getFriendTagById(friendTagId);
        UUID requestingUserId = friendTagDTO.ownerUserId();

        List<UserDTO> friends = userService.getFriendsByUserId(requestingUserId);

        List<UserDTO> friendsAddedToTag = userService.getFriendsByFriendTagId(friendTagId);

        List<UserDTO> friendsNotAddedToTag = friends.stream()
                        .filter(friend -> !friendsAddedToTag.contains(friend))
                        .toList();

        return friendsNotAddedToTag.stream()
                .map(userDTO -> userService.getFullUserById(requestingUserId))
                .collect(Collectors.toList());
    }
}
