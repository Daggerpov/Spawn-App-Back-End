package com.danielagapov.spawn.Services.User;

import com.danielagapov.spawn.DTOs.FriendRequestDTO;
import com.danielagapov.spawn.DTOs.UserDTO;
import com.danielagapov.spawn.Enums.EntityType;
import com.danielagapov.spawn.Exceptions.Base.BaseNotFoundException;
import com.danielagapov.spawn.Exceptions.Base.BaseSaveException;
import com.danielagapov.spawn.Exceptions.Base.BasesNotFoundException;
import com.danielagapov.spawn.Exceptions.Base.ApplicationException;
import com.danielagapov.spawn.Exceptions.Base.DatabaseException;
import com.danielagapov.spawn.Mappers.FriendRequestMapper;
import com.danielagapov.spawn.Mappers.UserMapper;
import com.danielagapov.spawn.Models.FriendRequests;
import com.danielagapov.spawn.Models.User;
import com.danielagapov.spawn.Repositories.IFriendRequestsRepository;
import com.danielagapov.spawn.Repositories.IUserFriendTagRepository;
import com.danielagapov.spawn.Repositories.IUserRepository;
import com.danielagapov.spawn.Services.FriendTag.FriendTagService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class UserService implements IUserService {
    private final IUserRepository repository;
    private final IFriendRequestsRepository friendRequestsRepository;
    private final IUserFriendTagRepository uftRepository;
    private final FriendTagService ftService;

    @Autowired
    @Lazy // otherwise ftService will try to initialize this (which is already initializing)
    public UserService(IUserRepository repository, IFriendRequestsRepository friendRequestsRepository,
                       IUserFriendTagRepository uftRepository, FriendTagService ftService) {
        this.repository = repository;
        this.friendRequestsRepository = friendRequestsRepository;
        this.uftRepository = uftRepository;
        this.ftService = ftService;
    }

    public List<UserDTO> getAllUsers() {
        try {
            return UserMapper.toDTOList(repository.findAll(), this, ftService);
        } catch (DataAccessException e) {
            throw new BasesNotFoundException(EntityType.User);
        }
    }

    public UserDTO getUserById(UUID id) {
        return UserMapper.toDTO(repository.findById(id)
                .orElseThrow(() -> new BaseNotFoundException(id)), this, ftService);
    }

    public List<UserDTO> getUsersByTagId(UUID tagId) {
        // TODO: change this logic later, once tags are setup.
        try {
            return UserMapper.toDTOList(repository.findAll(), this, ftService);
        } catch (DataAccessException e) {
            throw new DatabaseException("Failed to get users by tag ID: " + e.getMessage(), e);
        } catch (Exception e) {
            throw new ApplicationException("Unexpected error occurred while getting users by tag ID: " + e.getMessage(), e);
        }
    }

    public UserDTO saveUser(UserDTO user) {
        try {
            User userEntity = UserMapper.toEntity(user); // friends field might be null
            repository.save(userEntity);
            userEntity = repository.findById(userEntity.getId()).orElseThrow(() ->
                    new BaseSaveException("if this error is thrown GG. it should never be thrown lol"));
            if (user.friends() == null) {
                userEntity.setFriends(ftService.generateNewUserFriendTag(userEntity.getId()));
            } else {
                userEntity.setFriends(ftService.generateNewUserFriendTag(user.friendTags().get(0), userEntity.getId())); //assumes first element is "everyone" tag
            }
            repository.save(userEntity);
            return UserMapper.toDTO(userEntity, this, ftService);
        } catch (DataAccessException e) {
            throw new BaseSaveException("Failed to save user: " + e.getMessage());
        }
    }

    // basically 'upserting' (a.k.a. inserting if not already in DB, otherwise, updating)
    public UserDTO replaceUser(UserDTO newUser, UUID id) {
        // TODO: we may want to make this function easier to read in the future,
        // but for now, I left the logic the same as what Seabert wrote.
        return repository.findById(id).map(user -> {
            user.setBio(newUser.bio());
            user.setFirstName(newUser.firstName());
            user.setLastName(newUser.lastName());
            user.setUsername(newUser.username());
            repository.save(user);
            return UserMapper.toDTO(user, this, ftService);
        }).orElseGet(() -> {
            User userEntity = UserMapper.toEntity(newUser);
            repository.save(userEntity);
            return UserMapper.toDTO(userEntity, this, ftService);
        });
    }

    public boolean deleteUserById(UUID id) {
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
    public FriendRequestDTO saveFriendRequest(FriendRequestDTO friendRequestDTO) {
        try {
            FriendRequests friendRequest = FriendRequestMapper.toEntity(friendRequestDTO);
            friendRequestsRepository.save(friendRequest);
            return FriendRequestMapper.toDTO(friendRequest, this, ftService);
        } catch (DataAccessException e) {
            throw new BaseSaveException("Failed to save friend request: " + e.getMessage());
        }
    }

    // TODO: Decide whether to make a new service for this
    public List<UUID> getUserFriendsId(UUID friendTagId) {
        return uftRepository.findFriendIdsByTagId(friendTagId);
    }

    public List<UserDTO> getUserFriends(UUID friendTagId) {
        return uftRepository.findFriendIdsByTagId(friendTagId)
                .stream()
                .map(this::getUserById)
                .collect(Collectors.toList());
    }
}