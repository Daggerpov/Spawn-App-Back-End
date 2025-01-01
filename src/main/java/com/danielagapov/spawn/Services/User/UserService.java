package com.danielagapov.spawn.Services.User;

import com.danielagapov.spawn.DTOs.FriendTagDTO;
import com.danielagapov.spawn.DTOs.UserDTO;
import com.danielagapov.spawn.Enums.EntityType;
import com.danielagapov.spawn.Exceptions.Base.*;
import com.danielagapov.spawn.Mappers.UserMapper;
import com.danielagapov.spawn.Models.FriendTag;
import com.danielagapov.spawn.Models.User;
import com.danielagapov.spawn.Repositories.IFriendTagRepository;
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
    private final IFriendTagRepository friendTagRepository;

    @Autowired
    @Lazy // Avoid circular dependency issues with ftService
    public UserService(IUserRepository repository,
                       IUserFriendTagRepository uftRepository, IFriendTagService friendTagService, IFriendTagRepository friendTagRepository) {
        this.repository = repository;
        this.uftRepository = uftRepository;
        this.friendTagService = friendTagService;
        this.friendTagRepository = friendTagRepository;
    }

    public List<UserDTO> getAllUsers() {
        try {
            return getUserDTOs();
        } catch (DataAccessException e) {
            throw new BasesNotFoundException(EntityType.User);
        }
    }

    public Map<FriendTag, UserDTO> getOwnerMap() {
        List<FriendTag> friendTags = friendTagRepository.findAll();
        return friendTags.stream()
                .collect(Collectors.toMap(
                        friendTag -> friendTag,
                        friendTag -> getUserById(friendTag.getOwnerId()) // Map each FriendTag to its owner (UserDTO)
                ));
    }

    public Map<FriendTag, List<UserDTO>> getFriendsMap() {
        List<FriendTag> friendTags = friendTagRepository.findAll();
        return friendTags.stream()
                .collect(Collectors.toMap(
                        friendTag -> friendTag,
                        friendTag -> getUserFriends(friendTag.getId()) // Map each FriendTag to its friends list (List<UserDTO>)
                ));
    }

    public List<UserDTO> getFriendsByFriendTagId(UUID friendTagId) {
        // Assuming you have a method to retrieve friends for a given FriendTag
        return uftRepository.findFriendIdsByTagId(friendTagId)
                .stream()
                .map(this::getUserById)
                .collect(Collectors.toList());
    }

    public UserDTO getUserById(UUID id) {
        User user = repository.findById(id)
                .orElseThrow(() -> new BaseNotFoundException(id));
        List<UserDTO> friends = getUserFriends(user.getId());
        List<FriendTagDTO> friendTags = friendTagService.getFriendTagsByOwnerId(user.getId());
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

    public UserDTO saveUser(UserDTO user) {
        try {
            User userEntity = UserMapper.toEntity(user);
            repository.save(userEntity);

            // Fetch friends and friend tags after saving the user
            List<UserDTO> friends = getUserFriends(userEntity.getId());
            List<FriendTagDTO> friendTags = friendTagService.getFriendTagsByOwnerId(userEntity.getId());

            userEntity = repository.findById(userEntity.getId()).orElseThrow(() ->
                    new BaseSaveException("Failed to retrieve saved user"));

            repository.save(userEntity);
            return UserMapper.toDTO(userEntity, friends, friendTags);
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
            List<UserDTO> friends = getUserFriends(user.getId());
            List<FriendTagDTO> friendTags = friendTagService.getFriendTagsByOwnerId(user.getId());
            return UserMapper.toDTO(user, friends, friendTags);
        }).orElseGet(() -> {
            User userEntity = UserMapper.toEntity(newUser);
            repository.save(userEntity);
            List<UserDTO> friends = getUserFriends(userEntity.getId());
            List<FriendTagDTO> friendTags = friendTagService.getFriendTagsByOwnerId(userEntity.getId());
            return UserMapper.toDTO(userEntity, friends, friendTags);
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
                        user -> friendTagService.getFriendTagsByOwnerId(user.getId())
                ));
        return UserMapper.toDTOList(users, friendsMap, friendTagsMap);
    }
}
