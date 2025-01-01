package com.danielagapov.spawn.Services.User;

import com.danielagapov.spawn.DTOs.FriendTagDTO;
import com.danielagapov.spawn.DTOs.UserDTO;
import com.danielagapov.spawn.Enums.EntityType;
import com.danielagapov.spawn.Exceptions.Base.*;
import com.danielagapov.spawn.Mappers.UserMapper;
import com.danielagapov.spawn.Models.User;
import com.danielagapov.spawn.Repositories.IUserFriendTagRepository;
import com.danielagapov.spawn.Repositories.IUserRepository;
import com.danielagapov.spawn.Services.FriendTag.IFriendTagService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class UserService implements IUserService {
    private final IUserRepository repository;
    private final IUserFriendTagRepository uftRepository;
    private final IFriendTagService friendTagService;

    @Autowired
    @Lazy // Avoid circular dependency issues with ftService
    public UserService(IUserRepository repository,
                       IUserFriendTagRepository uftRepository, IFriendTagService friendTagService) {
        this.repository = repository;
        this.uftRepository = uftRepository;
        this.friendTagService = friendTagService;
    }

    public List<UserDTO> getAllUsers() {
        try {
            return getUserDTOs();
        } catch (DataAccessException e) {
            throw new BasesNotFoundException(EntityType.User);
        }
    }

    public UserDTO getUserById(UUID id) {
        User user = repository.findById(id)
                .orElseThrow(() -> new BaseNotFoundException(id));
        List<UserDTO> friends = getUserFriends(user.getId());
        List<FriendTagDTO> friendTags = friendTagService.getFriendTagsByUserId(user.getId());
        return UserMapper.toDTO(user, friends, friendTags);
    }

    public List<UserDTO> getUsersByTagId(UUID tagId) {
        try {
            return getUserDTOs();
        } catch (DataAccessException e) {
            throw new DatabaseException("Failed to get users by tag ID: " + e.getMessage(), e);
        } catch (Exception e) {
            throw new ApplicationException("Unexpected error occurred while getting users by tag ID: " + e.getMessage(), e);
        }
    }

    public List<UserDTO> getUserFriends(UUID friendTagId) {
        return uftRepository.findFriendIdsByTagId(friendTagId)
                .stream()
                .map(this::getUserById)
                .collect(Collectors.toList());
    }

    public UserDTO saveUser(UserDTO user) {
        try {
            User userEntity = UserMapper.toEntity(user); // Handle nullable `friends` field
            repository.save(userEntity);
            userEntity = repository.findById(userEntity.getId()).orElseThrow(() ->
                    new BaseSaveException("Failed to retrieve saved user"));
            if (user.friends() == null) {
                userEntity.setFriends(friendTagService.generateNewUserFriendTag(userEntity.getId()));
            } else {
                userEntity.setFriends(friendTagService.generateNewUserFriendTag(user.friendTags().get(0), userEntity.getId())); //assumes first element is "everyone" tag
            }
            repository.save(userEntity);
            return UserMapper.toDTO(userEntity, this, friendTagService);
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
            return UserMapper.toDTO(user, this, friendTagService);
        }).orElseGet(() -> {
            User userEntity = UserMapper.toEntity(newUser);
            repository.save(userEntity);
            return UserMapper.toDTO(userEntity, this, friendTagService);
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

    public List<UUID> getUserFriendsId(UUID friendTagId) {
        return uftRepository.findFriendIdsByTagId(friendTagId);
    }

    public List<UserDTO> getUserFriends(UUID friendTagId) {
        return uftRepository.findFriendIdsByTagId(friendTagId)
                .stream()
                .map(this::getUserById)
                .collect(Collectors.toList());
    }

    private List<UserDTO> getUserDTOs() {
        List<User> users = repository.findAll();
        Map<User, List<UserDTO>> friendsMap = users.stream()
                .collect(Collectors.toMap(
                        user -> user,
                        user -> getUserFriends(user.getId())
                ));
        Map<User, List<FriendTagDTO>> friendTagsMap = users.stream()
                .collect(Collectors.toMap(
                        user -> user,
                        user -> friendTagService.getFriendTagsByUserId(user.getId())
                ));
        return UserMapper.toDTOList(users, friendsMap, friendTagsMap);
    }
}
