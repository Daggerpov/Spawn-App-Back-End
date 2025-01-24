package com.danielagapov.spawn.Services.User;

import com.danielagapov.spawn.DTOs.FriendTagDTO;
import com.danielagapov.spawn.DTOs.FullUserDTO;
import com.danielagapov.spawn.DTOs.UserDTO;
import com.danielagapov.spawn.Enums.EntityType;
import com.danielagapov.spawn.Exceptions.ApplicationException;
import com.danielagapov.spawn.Exceptions.Base.BaseNotFoundException;
import com.danielagapov.spawn.Exceptions.Base.BaseSaveException;
import com.danielagapov.spawn.Exceptions.Base.BasesNotFoundException;
import com.danielagapov.spawn.Exceptions.DatabaseException;
import com.danielagapov.spawn.Mappers.UserMapper;
import com.danielagapov.spawn.Models.FriendTag;
import com.danielagapov.spawn.Models.User;
import com.danielagapov.spawn.Repositories.IFriendTagRepository;
import com.danielagapov.spawn.Repositories.IUserFriendTagRepository;
import com.danielagapov.spawn.Repositories.IUserRepository;
import com.danielagapov.spawn.Services.FriendTag.IFriendTagService;
import com.danielagapov.spawn.Helpers.Logger.ILogger;
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
    private final ILogger logger;

    @Autowired
    @Lazy // Avoid circular dependency issues with ftService
    public UserService(IUserRepository repository,
                       IUserFriendTagRepository uftRepository, IFriendTagService friendTagService, IFriendTagRepository friendTagRepository, ILogger logger) {
        this.repository = repository;
        this.uftRepository = uftRepository;
        this.friendTagService = friendTagService;
        this.friendTagRepository = friendTagRepository;
        this.logger = logger;
    }

    public List<UserDTO> getAllUsers() {
        try {
            return getUserDTOs();
        } catch (DataAccessException e) {
            logger.log(e.getMessage());
            throw new BasesNotFoundException(EntityType.User);
        } catch (Exception e) {
            logger.log(e.getMessage());
            throw e;
        }
    }

    // CRUD operations:

    public UserDTO getUserById(UUID id) {
        User user = repository.findById(id)
                .orElseThrow(() -> new BaseNotFoundException(EntityType.User, id));

        // Fetch FriendTag IDs based on the user ID
        List<UUID> friendTagIds = friendTagService.getFriendTagIdsByOwnerUserId(user.getId());

        // Fetch FriendTags for the user
        List<FriendTagDTO> friendTags = friendTagService.getFriendTagsByOwnerId(user.getId());

        // Pass in the friendTagIds and friendTags as needed
        return UserMapper.toDTO(user, friendTagIds, friendTagIds);
    }

    public FullUserDTO getFullUserById(UUID id) {
        return getFullUserByUser(getUserById(id));
    }


    public List<UUID> getFriendUserIdsByUserId(UUID id) {
        // Fetch FriendTag entities related to the given user (for example, by userId)
        List<FriendTag> friendTags = friendTagRepository.findByOwnerId(id);

        // Retrieve the user IDs associated with those FriendTags
        return friendTags.stream()
                .flatMap(friendTag -> uftRepository.findFriendIdsByTagId(friendTag.getId()).stream())
                .distinct() // Remove duplicates
                .collect(Collectors.toList());
    }

    public List<UserDTO> getFriendUsersByUserId(UUID id) {
        return getFriendUserIdsByFriendTagId(id).stream().map(this::getUserById).collect(Collectors.toList());
    }


    public User getUserEntityById(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new BaseNotFoundException(EntityType.User, id));
    }

    // For Friend Tags:

    public Map<FriendTag, UUID> getOwnerUserIdsMap() {
        List<FriendTag> friendTags = friendTagRepository.findAll();
        return friendTags.stream()
                .collect(Collectors.toMap(
                        friendTag -> friendTag,
                        FriendTag::getOwnerId
                ));
    }

    public Map<FriendTag, List<UUID>> getFriendUserIdsMap() {
        // Fetch all FriendTags
        List<FriendTag> friendTags = friendTagRepository.findAll();

        // Create a map of FriendTag to a list of associated user IDs
        return friendTags.stream()
                .collect(Collectors.toMap(
                        friendTag -> friendTag, // Use FriendTag as the key
                        friendTag -> uftRepository.findFriendIdsByTagId(friendTag.getId()) // List of user IDs for each FriendTag
                ));
    }

    public List<UserDTO> getUsersByTagId(UUID tagId) {
        try {
            FriendTag friendTag = friendTagRepository.findById(tagId)
                    .orElseThrow(() -> new BaseNotFoundException(EntityType.FriendTag, tagId));

            List<UUID> userIds = uftRepository.findFriendIdsByTagId(tagId);

            if (userIds.isEmpty()) {
                throw new BasesNotFoundException(EntityType.User);
            }

            return userIds.stream()
                    .map(this::getUserById)
                    .collect(Collectors.toList());

        } catch (DataAccessException e) {
            logger.log(e.getMessage());
            throw new DatabaseException("Error accessing database while fetching users by tag ID: " + tagId, e);

        } catch (BaseNotFoundException | BasesNotFoundException e) {
            logger.log(e.getMessage());
            throw e;

        } catch (Exception e) {
            logger.log(e.getMessage());
            throw new ApplicationException("Unexpected error occurred while fetching users by tag ID: " + tagId, e);
        }
    }

    public UserDTO saveUser(UserDTO user) {
        try {
            User userEntity = UserMapper.toEntity(user);
            userEntity = repository.save(userEntity);

            FriendTagDTO everyoneTagDTO = new FriendTagDTO(null, "Everyone",
                    "#1D3D3D", user.id(), List.of(), true);
            friendTagService.saveFriendTag(everyoneTagDTO); // id is generated when saving
            return UserMapper.toDTO(userEntity, List.of(), List.of(everyoneTagDTO.id()));
        } catch (DataAccessException e) {
            logger.log(e.getMessage());
            throw new BaseSaveException("Failed to save user: " + e.getMessage());
        } catch (Exception e) {
            logger.log(e.getMessage());
            throw e;
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

            List<UUID> friendUserIds = getFriendUserIdsByUserId(user.getId());
            List<UUID> friendTagIds = friendTagService.getFriendTagIdsByOwnerUserId(user.getId());
            return UserMapper.toDTO(user, friendUserIds, friendTagIds);
        }).orElseGet(() -> {
            User userEntity = UserMapper.toEntity(newUser);
            repository.save(userEntity);

            List<UUID> friendUserIds = getFriendUserIdsByUserId(userEntity.getId());
            List<UUID> friendTagIds = friendTagService.getFriendTagIdsByOwnerUserId(userEntity.getId());
            return UserMapper.toDTO(userEntity, friendUserIds, friendTagIds);
        });
    }

    public boolean deleteUserById(UUID id) {
        if (!repository.existsById(id)) {
            throw new BaseNotFoundException(EntityType.User, id);
        }

        try {
            repository.deleteById(id);
            return true;
        } catch (Exception e) {
            logger.log(e.getMessage());
            return false;
        }
    }

    public List<UserDTO> getFriendsByFriendTagId(UUID friendTagId) {
        return uftRepository.findFriendIdsByTagId(friendTagId)
                .stream()
                .map(this::getUserById)
                .collect(Collectors.toList());
    }

    public List<UUID> getFriendUserIdsByFriendTagId(UUID friendTagId) {
        // Call the method to get the list of UserDTOs
        List<UserDTO> friends = getFriendsByFriendTagId(friendTagId);

        // Extract the user IDs from the UserDTO list
        return friends.stream()
                .map(UserDTO::id)
                .distinct()
                .collect(Collectors.toList());
    }

    private List<UserDTO> getUserDTOs() {
        List<User> users = repository.findAll();
        Map<User, List<UUID>> friendUserIdsMap = users.stream()
                .collect(Collectors.toMap(
                        user -> user,
                        user -> getFriendUserIdsByUserId(user.getId())
                ));
        Map<User, List<UUID>> friendTagIdsMap = users.stream()
                .collect(Collectors.toMap(
                        user -> user,
                        user -> friendTagService.getFriendTagIdsByOwnerUserId(user.getId())
                ));
        return UserMapper.toDTOList(users, friendUserIdsMap, friendTagIdsMap);
    }

    public List<UserDTO> getFriendsByUserId(UUID userId) {
        // Get the FriendTags associated with the user (assuming userId represents the owner of friend tags)
        FriendTag everyoneTag = friendTagRepository.findEveryoneTagByOwnerId(userId);
        if (everyoneTag == null) {
            return List.of(); // empty list of friends
        }

        // Retrieve the friends for each FriendTag and return as a flattened list
        return getFriendsByFriendTagId(everyoneTag.getId());
    }

    // Adds friend bidirectionally
    public void saveFriendToUser(UUID userId, UUID friendId) {
        UUID userEveryoneTagId = friendTagRepository.findEveryoneTagByOwnerId(userId).getId();
        friendTagService.saveUserToFriendTag(userEveryoneTagId, friendId);
        UUID friendEveryoneTagId = friendTagRepository.findEveryoneTagByOwnerId(userId).getId();
        friendTagService.saveUserToFriendTag(friendEveryoneTagId, userId);
    }

    // Removes friend bidirectionally
    public void removeFriend(UUID userId, UUID friendId) {
        // Deletes all entries in UserFriendTag
        friendTagRepository.findByOwnerId(userId).forEach((friendTag) -> {
            uftRepository.deleteByFriendTagIdAndUserId(friendTag.getId(), friendId);
        });
        friendTagRepository.findByOwnerId(friendId).forEach((friendTag) -> {
            uftRepository.deleteByFriendTagIdAndUserId(friendTag.getId(), userId);
        });
    }

    // TODO: implement this logic later
    public List<UserDTO> getRecommendedFriends(UUID id) {
        // TODO
        return List.of();
    }

    public List<UserDTO> getParticipantsByEventId(UUID eventId) {
        // TODO
        return List.of();
    }

    public List<UserDTO> getInvitedByEventId(UUID eventId) {
        // TODO
        return List.of();
    }

    public List<UUID> getParticipantUserIdsByEventId(UUID eventId) {
        // TODO
        return List.of();
    }

    public List<UUID> getInvitedUserIdsByEventId(UUID eventId) {
        // TODO
        return List.of();
    }

    public FullUserDTO getFullUserByUser(UserDTO user) {
        return new FullUserDTO(
                user.id(),
                getFriendsByUserId(user.id()),
                user.username(),
                user.profilePicture(),
                user.firstName(),
                user.lastName(),
                user.bio(),
                friendTagService.getFriendTagsByOwnerId(user.id()),
                user.email()
        );
    }
}
