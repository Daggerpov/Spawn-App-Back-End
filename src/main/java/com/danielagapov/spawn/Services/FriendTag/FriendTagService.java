package com.danielagapov.spawn.Services.FriendTag;

import com.danielagapov.spawn.DTOs.FriendTagDTO;
import com.danielagapov.spawn.Exceptions.Base.BasesNotFoundException;
import com.danielagapov.spawn.Exceptions.Base.BaseNotFoundException;
import com.danielagapov.spawn.Exceptions.Base.BaseSaveException;
import com.danielagapov.spawn.Mappers.FriendTagMapper;
import com.danielagapov.spawn.Models.FriendTag;
import com.danielagapov.spawn.Repositories.IFriendTagRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class FriendTagService implements IFriendTagService {
    private final IFriendTagRepository repository;

    @Autowired
    public FriendTagService(IFriendTagRepository repository) {
        this.repository = repository;
    }

    public List<FriendTagDTO> getAllFriendTags() {
        try {
            return FriendTagMapper.toDTOList(repository.findAll());
        } catch (DataAccessException e) {
            throw new BasesNotFoundException();
        }
    }

    public FriendTagDTO getFriendTagById(UUID id) {
        return FriendTagMapper.toDTO(repository.findById(id)
                .orElseThrow(() -> new BaseNotFoundException(id)));
    }

    public List<FriendTagDTO> getFriendTagsByTagId(UUID tagId) {
        // TODO: change this logic later, once tags are setup.
        try {
            return FriendTagMapper.toDTOList(repository.findAll());
        } catch (DataAccessException e) {
            throw new RuntimeException("Error retrieving friendTags", e);
        }
    }

    public FriendTagDTO saveFriendTag(FriendTagDTO friendTag) {
        try {
            FriendTag friendTagEntity = FriendTagMapper.toEntity(friendTag);
            repository.save(friendTagEntity);
            return FriendTagMapper.toDTO(friendTagEntity);
        } catch (DataAccessException e) {
            throw new BaseSaveException("Failed to save friendTag: " + e.getMessage());
        }
    }

    // basically 'upserting' (a.k.a. inserting if not already in DB, otherwise, updating)
    public FriendTagDTO replaceFriendTag(FriendTagDTO newFriendTag, UUID id) {
        // TODO: we may want to make this function easier to read in the future,
        // but for now, I left the logic the same as what Seabert wrote.
        return repository.findById(id).map(friendTag -> {
            friendTag.setColor(newFriendTag.colorHexCode());
            friendTag.setDisplayName(newFriendTag.displayName());
            repository.save(friendTag);
            return FriendTagMapper.toDTO(friendTag);
        }).orElseGet(() -> {
            FriendTag friendTagEntity = FriendTagMapper.toEntity(newFriendTag);
            repository.save(friendTagEntity);
            return FriendTagMapper.toDTO(friendTagEntity);
        });
    }

}
