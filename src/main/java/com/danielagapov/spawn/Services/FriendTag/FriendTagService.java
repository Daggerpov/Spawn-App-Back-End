package com.danielagapov.spawn.Services.FriendTag;

import com.danielagapov.spawn.DTOs.FriendTagDTO;
import com.danielagapov.spawn.DTOs.UserDTO;
import com.danielagapov.spawn.Enums.EntityType;
import com.danielagapov.spawn.Exceptions.Base.BaseNotFoundException;
import com.danielagapov.spawn.Exceptions.Base.BaseSaveException;
import com.danielagapov.spawn.Exceptions.Base.BasesNotFoundException;
import com.danielagapov.spawn.Exceptions.Base.DatabaseException;
import com.danielagapov.spawn.Mappers.FriendTagMapper;
import com.danielagapov.spawn.Models.FriendTag;
import com.danielagapov.spawn.Models.User;
import com.danielagapov.spawn.Models.UserFriendTag;
import com.danielagapov.spawn.Repositories.IFriendTagRepository;
import com.danielagapov.spawn.Repositories.IUserFriendTagRepository;
import com.danielagapov.spawn.Repositories.IUserRepository;
import com.danielagapov.spawn.Services.User.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class FriendTagService implements IFriendTagService {
    private final IFriendTagRepository repository;
    private final UserService userService;
    private final IUserFriendTagRepository uftRepository;
    private final IUserRepository userRepository;

    @Autowired
    public FriendTagService(IFriendTagRepository repository, UserService userService,
                            IUserFriendTagRepository uftRepository, IUserRepository userRepository) {
        this.repository = repository;
        this.userService = userService;
        this.uftRepository = uftRepository;
        this.userRepository = userRepository;
    }

    public List<FriendTagDTO> getAllFriendTags() {
        try {
            // Use the helper methods you created
            Map<FriendTag, UserDTO> ownerMap = userService.getOwnerMap();
            Map<FriendTag, List<UserDTO>> friendsMap = userService.getFriendsMap();
            return FriendTagMapper.toDTOList(repository.findAll(), ownerMap, friendsMap);
        } catch (DataAccessException e) {
            throw new BasesNotFoundException(EntityType.FriendTag);
        }
    }

    public FriendTagDTO getFriendTagById(UUID id) {
        return repository.findById(id)
                .map(friendTag -> {
                    UserDTO owner = userService.getUserById(friendTag.getOwnerId());
                    List<UserDTO> friends = userService.getFriendsByFriendTagId(friendTag.getId());
                    return FriendTagMapper.toDTO(friendTag, owner, friends);
                })
                .orElseThrow(() -> new BaseNotFoundException(id));
    }

    public List<FriendTagDTO> getFriendTagsByOwnerId(UUID ownerId) {
        try {
            Map<FriendTag, UserDTO> ownerMap = userService.getOwnerMap();
            Map<FriendTag, List<UserDTO>> friendsMap = userService.getFriendsMap();
            return FriendTagMapper.toDTOList(repository.findByOwnerId(ownerId), ownerMap, friendsMap);
        } catch (DataAccessException e) {
            throw new RuntimeException("Error retrieving friendTags", e);
        }
    }

    public FriendTagDTO saveFriendTag(FriendTagDTO friendTag) {
        try {
            FriendTag friendTagEntity = FriendTagMapper.toEntity(friendTag);
            repository.save(friendTagEntity);
            return FriendTagMapper.toDTO(friendTagEntity, userService.getUserById(friendTag.owner().id()), List.of());
        } catch (DataAccessException e) {
            throw new BaseSaveException("Failed to save friendTag: " + e.getMessage());
        }
    }

    public FriendTagDTO replaceFriendTag(FriendTagDTO newFriendTag, UUID id) {
        return repository.findById(id).map(friendTag -> {
            friendTag.setColorHexCode(newFriendTag.colorHexCode());
            friendTag.setDisplayName(newFriendTag.displayName());
            repository.save(friendTag);
            return FriendTagMapper.toDTO(friendTag, userService.getUserById(newFriendTag.owner().id()), List.of());
        }).orElseGet(() -> {
            FriendTag friendTagEntity = FriendTagMapper.toEntity(newFriendTag);
            repository.save(friendTagEntity);
            return FriendTagMapper.toDTO(friendTagEntity, userService.getUserById(newFriendTag.owner().id()), List.of());
        });
    }

    public boolean deleteFriendTagById(UUID id) {
        if (!repository.existsById(id)) {
            throw new BaseNotFoundException(id);
        }

        try {
            repository.deleteById(id);
            return true;
        } catch (DataAccessException e) {
            return false;
        }
    }

    public void saveUserToFriendTag(UUID id, UUID userId) {
        if (!repository.existsById(id)) {
            throw new BaseNotFoundException(id);
        }
        if (!repository.existsById(userId)) {
            throw new BaseNotFoundException(userId);
        }
        // TODO consider adding a more descriptive error
        FriendTag friendTag = repository.findById(id).orElseThrow(() -> new BaseNotFoundException(id));
        User user = userRepository.findById(userId).orElseThrow(() -> new BaseNotFoundException(id));
        UserFriendTag uft = new UserFriendTag();
        uft.setFriend(user);
        uft.setFriendTag(friendTag);
        try {
            uftRepository.save(uft);
        } catch (DataAccessException e) {
            throw new BaseSaveException("Failed to save new UserFriendTag");
        }
    }
}
