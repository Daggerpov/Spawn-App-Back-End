package com.danielagapov.spawn.Services.FriendTag;

import com.danielagapov.spawn.DTOs.FriendTagDTO;
import com.danielagapov.spawn.Enums.EntityType;
import com.danielagapov.spawn.Exceptions.Base.BasesNotFoundException;
import com.danielagapov.spawn.Exceptions.Base.BaseNotFoundException;
import com.danielagapov.spawn.Exceptions.Base.BaseSaveException;
import com.danielagapov.spawn.Mappers.FriendTagMapper;
import com.danielagapov.spawn.Models.FriendTag;
import com.danielagapov.spawn.Repositories.IFriendTagRepository;
import com.danielagapov.spawn.Services.User.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class FriendTagService implements IFriendTagService {
    private final IFriendTagRepository repository;
    private final UserService userService;

    @Autowired
    public FriendTagService(IFriendTagRepository repository, UserService userService) {
        this.repository = repository;
        this.userService = userService;
    }

    public List<FriendTagDTO> getAllFriendTags() {
        try {
            return FriendTagMapper.toDTOList(repository.findAll(), userService);
        } catch (DataAccessException e) {
            throw new BasesNotFoundException(EntityType.FriendTag);
        }
    }

    public FriendTagDTO getFriendTagById(UUID id) {
        return FriendTagMapper.toDTO(repository.findById(id)
                .orElseThrow(() -> new BaseNotFoundException(id)), userService);
    }

    public List<FriendTagDTO> getFriendTagsByOwnerId(UUID ownerId) {
        try {
            return repository.findByOwnerId(ownerId).stream()
                    .map(this::getFriendTagById)
                    .collect(Collectors.toList());
        } catch (DataAccessException e) {
            throw new RuntimeException("Error retrieving friendTags", e);
        }
    }

    public FriendTagDTO saveFriendTag(FriendTagDTO friendTag) {
        try {
            FriendTag friendTagEntity = FriendTagMapper.toEntity(friendTag);
            repository.save(friendTagEntity);
            return FriendTagMapper.toDTO(friendTagEntity, userService);
        } catch (DataAccessException e) {
            throw new BaseSaveException("Failed to save friendTag: " + e.getMessage());
        }
    }

    // basically 'upserting' (a.k.a. inserting if not already in DB, otherwise, updating)
    public FriendTagDTO replaceFriendTag(FriendTagDTO newFriendTag, UUID id) {
        // TODO: we may want to make this function easier to read in the future,
        // but for now, I left the logic the same as what Seabert wrote.
        return repository.findById(id).map(friendTag -> {
            friendTag.setColorHexCode(newFriendTag.colorHexCode());
            friendTag.setDisplayName(newFriendTag.displayName());
            repository.save(friendTag);
            return FriendTagMapper.toDTO(friendTag, userService);
        }).orElseGet(() -> {
            FriendTag friendTagEntity = FriendTagMapper.toEntity(newFriendTag);
            repository.save(friendTagEntity);
            return FriendTagMapper.toDTO(friendTagEntity, userService);
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

}
