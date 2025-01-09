package com.danielagapov.spawn.Services.User;

import com.danielagapov.spawn.DTOs.FriendTagDTO;
import com.danielagapov.spawn.DTOs.UserDTO;
import com.danielagapov.spawn.Enums.EntityType;
import com.danielagapov.spawn.Exceptions.ApplicationException;
import com.danielagapov.spawn.Exceptions.Base.*;
import com.danielagapov.spawn.Exceptions.DatabaseException;
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

    // CRUD operations:

    public UserDTO getUserById(UUID id) {
        User user = repository.findById(id)
                .orElseThrow(() -> new BaseNotFoundException(EntityType.User, id));
        List<UserDTO> friends = getFriendsByFriendTagId(user.getId());
        List<FriendTagDTO> friendTags = friendTagService.getFriendTagsByOwnerId(user.getId());
        return UserMapper.toDTO(user, friends, friendTags);
    }

    // For Friend Tags:

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
                        friendTag -> getFriendsByFriendTagId(friendTag.getId()) // Map each FriendTag to its friends list (List<UserDTO>)
                ));
    }

    public List<UserDTO> getUsersByTagId(UUID tagId) {
        // TODO: adjust this stub implementation
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
            userEntity = repository.save(userEntity);

            // TODO: resolve circular entity-DTO conversions
            FriendTagDTO everyoneTagDTO = new FriendTagDTO(null, "everyone",
                    "#ffffff", user, List.of(), true);
            friendTagService.saveFriendTag(everyoneTagDTO); // id is generated when saving
            return UserMapper.toDTO(userEntity, List.of(), List.of(everyoneTagDTO));
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
            List<UserDTO> friends = getFriendsByFriendTagId(user.getId());
            List<FriendTagDTO> friendTags = friendTagService.getFriendTagsByOwnerId(user.getId());
            return UserMapper.toDTO(user, friends, friendTags);
        }).orElseGet(() -> {
            User userEntity = UserMapper.toEntity(newUser);
            repository.save(userEntity);
            List<UserDTO> friends = getFriendsByFriendTagId(userEntity.getId());
            List<FriendTagDTO> friendTags = friendTagService.getFriendTagsByOwnerId(userEntity.getId());
            return UserMapper.toDTO(userEntity, friends, friendTags);
        });
    }

    public boolean deleteUserById(UUID id) {
        if (!repository.existsById(id)) {
            throw new BaseNotFoundException(EntityType.User, id);
        }

        try {
            repository.deleteById(id);
            return true;
        } catch (DataAccessException e) {
            return false;
        }
    }

    public List<UserDTO> getFriendsByFriendTagId(UUID friendTagId) {
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
                        user -> getFriendsByFriendTagId(user.getId())
                ));
        Map<User, List<FriendTagDTO>> friendTagsMap = users.stream()
                .collect(Collectors.toMap(
                        user -> user,
                        user -> friendTagService.getFriendTagsByOwnerId(user.getId())
                ));
        return UserMapper.toDTOList(users, friendsMap, friendTagsMap);
    }

    public List<UserDTO> getFriendsByUserId(UUID userId) {
        // Get the FriendTags associated with the user (assuming userId represents the owner of friend tags)
        FriendTag everyoneTag;
        try {
            everyoneTag = friendTagRepository.findEveryoneTagByOwnerId(userId);
        } catch (Exception e) {
            // if everyoneTag is not found the User doesn't exist
            throw new BaseNotFoundException(EntityType.User, userId);
        }

        // Retrieve the friends for each FriendTag and return as a flattened list
        return getFriendsByFriendTagId(everyoneTag.getId());
    }

    public void saveFriendToUser(UUID userId, UUID friendId) {
        UUID everyoneTagId = friendTagRepository.findEveryoneTagByOwnerId(userId).getId();
        friendTagService.saveUserToFriendTag(everyoneTagId, friendId);
    }

    public void removeFriend(UUID userId, UUID friendId) {
    }

    public List<UserDTO> getRecommendedFriends(UUID id) {
        return List.of();
    }
}
