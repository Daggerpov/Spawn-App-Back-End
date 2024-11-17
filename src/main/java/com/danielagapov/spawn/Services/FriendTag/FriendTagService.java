package com.danielagapov.spawn.Services.FriendTag;

import com.danielagapov.spawn.Exceptions.Base.BasesNotFoundException;
import com.danielagapov.spawn.Exceptions.Base.BaseNotFoundException;
import com.danielagapov.spawn.Exceptions.Base.BaseSaveException;
import com.danielagapov.spawn.Models.FriendTag.FriendTag;
import com.danielagapov.spawn.Repositories.IFriendTagRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class FriendTagService implements IFriendTagService {
    private final IFriendTagRepository repository;

    @Autowired
    public FriendTagService(IFriendTagRepository repository) {
        this.repository = repository;
    }

    public List<FriendTag> getAllFriendTags() {
        try {
            return repository.findAll();
        } catch (DataAccessException e) {
            throw new BasesNotFoundException();
        }
    }

    public FriendTag getFriendTagById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new BaseNotFoundException(id));
    }

    public List<FriendTag> getFriendTagsByTagId(Long tagId) {
        // TODO: change this logic later, once tags are setup.
        try {
            return repository.findAll();
        } catch (DataAccessException e) {
            throw new RuntimeException("Error retrieving friendTags", e);
        }
    }

    public FriendTag saveFriendTag(FriendTag friendTag) {
        try {
            return repository.save(friendTag);
        } catch (DataAccessException e) {
            throw new BaseSaveException("Failed to save friendTag: " + e.getMessage());
        }
    }
}
