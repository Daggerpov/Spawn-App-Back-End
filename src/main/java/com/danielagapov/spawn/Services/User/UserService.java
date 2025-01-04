package com.danielagapov.spawn.Services.User;

import com.danielagapov.spawn.DTOs.FriendRequestDTO;
import com.danielagapov.spawn.DTOs.UserDTO;
import com.danielagapov.spawn.Exceptions.Base.*;
import com.danielagapov.spawn.Mappers.FriendRequestMapper;
import com.danielagapov.spawn.Mappers.UserMapper;
import com.danielagapov.spawn.Models.FriendRequests;

import com.danielagapov.spawn.Models.User;
import com.danielagapov.spawn.Models.UserFriendTagMapping;
import com.danielagapov.spawn.Repositories.IFriendRequestsRepository;

import com.danielagapov.spawn.Repositories.IUserFriendTagMappingRepository;
import com.danielagapov.spawn.Repositories.IUserRepository;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class UserService implements IUserService {
    private final IUserRepository repository;
    private final IFriendRequestsRepository friendRequestsRepository;
    private final IUserFriendTagMappingRepository userTagMappingRepository;

    public UserService(IUserRepository repository, IFriendRequestsRepository friendRequestsRepository, IUserFriendTagMappingRepository tagRepository) {
        this.repository = repository;
        this.friendRequestsRepository = friendRequestsRepository;
        this.userTagMappingRepository = tagRepository;
    }

    public List<UserDTO> getAllUsers() {
        try {
            return UserMapper.toDTOList(repository.findAll());
        } catch (DataAccessException e) {
            throw new BasesNotFoundException("users");
        }
    }

    public UserDTO getUserById(UUID id) {
        return UserMapper.toDTO(repository.findById(id)
                .orElseThrow(() -> new BaseNotFoundException(id)));
    }

    public List<UserDTO> getUsersByTagId(UUID tagId) {
        try {
            List<UserFriendTagMapping> mappings = userTagMappingRepository.findByFriendTagId(tagId);
            List<User> users = new ArrayList<>();
            for (UserFriendTagMapping mapping : mappings) {
                users.add(mapping.getUser2());
            }
            return UserMapper.toDTOList(users);
        } catch (DataAccessException e) {
            throw new DatabaseException("Failed to get users by tag ID: " + e.getMessage(), e);
        } catch (Exception e) {
            throw new ApplicationException("Unexpected error occurred while getting users by tag ID: " + e.getMessage(), e);
        }
    }

    public UserDTO saveUser(UserDTO user) {
        try {
            User userEntity = UserMapper.toEntity(user);
            repository.save(userEntity);
            return UserMapper.toDTO(userEntity);
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
            return UserMapper.toDTO(user);
        }).orElseGet(() -> {
            User userEntity = UserMapper.toEntity(newUser);
            repository.save(userEntity);
            return UserMapper.toDTO(userEntity);
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
            return FriendRequestMapper.toDTO(friendRequest);
        } catch (DataAccessException e) {
            throw new BaseSaveException("Failed to save friend request: " + e.getMessage());
        }
    }

    // if user exists, do nothing ?
    // if user does not exist, create user account
    public UserDTO verifyUserOAuth(UserDTO user, UUID id) {
        if (!repository.existsById(id)) {
            return saveUser(user);
        }
        return getUserById(id);
    }


}