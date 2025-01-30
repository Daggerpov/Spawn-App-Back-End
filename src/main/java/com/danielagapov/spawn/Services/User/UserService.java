package com.danielagapov.spawn.Services.User;

import com.danielagapov.spawn.DTOs.FriendTagDTO;
import com.danielagapov.spawn.DTOs.FullUserDTO;
import com.danielagapov.spawn.DTOs.RecommendedFriendUserDTO;
import com.danielagapov.spawn.DTOs.UserDTO;
import com.danielagapov.spawn.Enums.EntityType;
import com.danielagapov.spawn.Exceptions.ApplicationException;
import com.danielagapov.spawn.Exceptions.Base.BaseNotFoundException;
import com.danielagapov.spawn.Exceptions.Base.BaseSaveException;
import com.danielagapov.spawn.Exceptions.Base.BasesNotFoundException;
import com.danielagapov.spawn.Exceptions.DatabaseException;
import com.danielagapov.spawn.Helpers.Logger.ILogger;
import com.danielagapov.spawn.Mappers.UserMapper;
import com.danielagapov.spawn.Models.FriendTag;
import com.danielagapov.spawn.Models.User;
import com.danielagapov.spawn.Repositories.IFriendTagRepository;
import com.danielagapov.spawn.Repositories.IUserFriendTagRepository;
import com.danielagapov.spawn.Repositories.IUserRepository;
import com.danielagapov.spawn.Services.FriendTag.IFriendTagService;
import com.danielagapov.spawn.Services.S3.IS3Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class UserService implements IUserService {
    private final IUserRepository repository;
    private final IUserFriendTagRepository uftRepository;
    private final IFriendTagService friendTagService;
    private final IFriendTagRepository friendTagRepository;
    private final IS3Service s3Service;
    private final ILogger logger;

    @Autowired
    @Lazy // Avoid circular dependency issues with ftService
    public UserService(IUserRepository repository,
                       IUserFriendTagRepository uftRepository, IFriendTagService friendTagService, IFriendTagRepository friendTagRepository, IS3Service s3Service, ILogger logger) {
        this.repository = repository;
        this.uftRepository = uftRepository;
        this.friendTagService = friendTagService;
        this.friendTagRepository = friendTagRepository;
        this.s3Service = s3Service;
        this.logger = logger;
    }

    @Override
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

    @Override
    public UserDTO getUserById(UUID id) {
        User user = repository.findById(id)
                .orElseThrow(() -> new BaseNotFoundException(EntityType.User, id));

        List<UUID> friendUserIds = getFriendUserIdsByUserId(id);

        // Fetch FriendTag IDs based on the user ID
        List<UUID> friendTagIds = friendTagService.getFriendTagIdsByOwnerUserId(user.getId());

        // Pass in the friendTagIds and friendTags as needed
        return UserMapper.toDTO(user, friendUserIds , friendTagIds);
    }

    @Override
    public FullUserDTO getFullUserById(UUID id) {
        return getFullUserByUser(getUserById(id));
    }

    @Override
    public List<UUID> getFriendUserIdsByUserId(UUID id) {
        // Fetch FriendTag entities related to the given user (for example, by userId)
        List<FriendTag> friendTags = friendTagRepository.findByOwnerId(id);

        // Retrieve the user IDs associated with those FriendTags
        return friendTags.stream()
                .flatMap(friendTag -> uftRepository.findFriendIdsByTagId(friendTag.getId()).stream())
                .distinct() // Remove duplicates
                .collect(Collectors.toList());
    }

    @Override
    public User getUserEntityById(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new BaseNotFoundException(EntityType.User, id));
    }

    // For Friend Tags:

    @Override
    public Map<FriendTag, UUID> getOwnerUserIdsMap() {
        List<FriendTag> friendTags = friendTagRepository.findAll();
        return friendTags.stream()
                .collect(Collectors.toMap(
                        friendTag -> friendTag,
                        FriendTag::getOwnerId
                ));
    }

    @Override
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

    @Override
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

    @Override
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
    @Override
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

    @Override
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

    @Override
    public User saveEntity(User user) {
        return repository.save(user);
    }

    @Override
    public UserDTO saveUserWithProfilePicture(UserDTO user, byte[] profilePicture) {
        if (user.profilePicture() == null) {
            user = s3Service.putProfilePictureWithUser(profilePicture, user);
        }
        return saveUser(user);
    }

    @Override
    public FullUserDTO getUserByEmail(String email) {
        User user = repository.findByEmail(email);
        return user == null ? null : getFullUserById(user.getId());
    }

    public List<UserDTO> getFriendsByFriendTagId(UUID friendTagId) {
        return uftRepository.findFriendIdsByTagId(friendTagId)
                .stream()
                .map(this::getUserById)
                .collect(Collectors.toList());
    }

    @Override
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

    @Override
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
    @Override
    public void saveFriendToUser(UUID userId, UUID friendId) {
        UUID userEveryoneTagId = friendTagRepository.findEveryoneTagByOwnerId(userId).getId();
        friendTagService.saveUserToFriendTag(userEveryoneTagId, friendId);
        UUID friendEveryoneTagId = friendTagRepository.findEveryoneTagByOwnerId(userId).getId();
        friendTagService.saveUserToFriendTag(friendEveryoneTagId, userId);
    }

    // Removes friend bidirectionally
    @Override
    public void removeFriend(UUID userId, UUID friendId) {
        // Deletes all entries in UserFriendTag
        friendTagRepository.findByOwnerId(userId).forEach((friendTag) -> {
            uftRepository.deleteByFriendTagIdAndUserId(friendTag.getId(), friendId);
        });
        friendTagRepository.findByOwnerId(friendId).forEach((friendTag) -> {
            uftRepository.deleteByFriendTagIdAndUserId(friendTag.getId(), userId);
        });
    }

    // returns top 3 friends with most mutuals with user (with `userId`) as
    // `RecommendedFriendUserDTO`s, to include the `mutualFriendCount`
    @Override
    public List<RecommendedFriendUserDTO> getRecommendedFriendsForUserId(UUID userId) {
        // Fetch the requesting user's friends
        List<UUID> requestingUserFriendIds = getFriendUserIdsByUserId(userId);

        // Create a set of the requesting user's friends for quick lookup
        Set<UUID> requestingUserFriendSet = new HashSet<>(requestingUserFriendIds);

        // Collect friends of friends (excluding already existing friends and the user itself)
        Map<UUID, Integer> mutualFriendCounts = new HashMap<>();
        for (UUID friendId : requestingUserFriendIds) {
            List<UUID> friendOfFriendIds = getFriendUserIdsByUserId(friendId);

            for (UUID friendOfFriendId : friendOfFriendIds) {
                if (!friendOfFriendId.equals(userId) && !requestingUserFriendSet.contains(friendOfFriendId)) {
                    mutualFriendCounts.merge(friendOfFriendId, 1, Integer::sum);
                }
            }
        }

        // Fetch only users in the mutualFriendCounts map
        List<RecommendedFriendUserDTO> recommendedFriends = mutualFriendCounts.entrySet().stream()
                // Get detailed user information for each friend of friend
                .map(entry -> {
                    UUID mutualFriendId = entry.getKey();
                    int mutualFriendCount = entry.getValue();
                    FullUserDTO fullUser = getFullUserById(mutualFriendId);

                    return new RecommendedFriendUserDTO(
                            fullUser.id(),
                            fullUser.friends(),
                            fullUser.username(),
                            fullUser.profilePicture(),
                            fullUser.firstName(),
                            fullUser.lastName(),
                            fullUser.bio(),
                            fullUser.friendTags(),
                            fullUser.email(),
                            mutualFriendCount
                    );
                })
                // Sort by mutual friend count in descending order
                .sorted(Comparator.comparingInt(RecommendedFriendUserDTO::mutualFriendCount).reversed())
                // Limit to top 3 recommendations
                .limit(3)
                .collect(Collectors.toList());

        return recommendedFriends;
    }

    @Override
    public List<UserDTO> getParticipantsByEventId(UUID eventId) {
        // TODO
        return List.of();
    }

    @Override
    public List<UserDTO> getInvitedByEventId(UUID eventId) {
        // TODO
        return List.of();
    }

    @Override
    public List<UUID> getParticipantUserIdsByEventId(UUID eventId) {
        // TODO
        return List.of();
    }

    @Override
    public List<UUID> getInvitedUserIdsByEventId(UUID eventId) {
        // TODO
        return List.of();
    }

    @Override
    public FullUserDTO getFullUserByUser(UserDTO user) {
        return new FullUserDTO(
                user.id(),
                convertUsersToFullUsers(getFriendsByUserId(user.id())),
                user.username(),
                user.profilePicture(),
                user.firstName(),
                user.lastName(),
                user.bio(),
                friendTagService.convertFriendTagsToFullFriendTags(friendTagService.getFriendTagsByOwnerId(user.id())),
                user.email()
        );
    }

    @Override
    public List<FullUserDTO> convertUsersToFullUsers(List<UserDTO> users) {
        return users.stream()
                .map(this::getFullUserByUser)
                .collect(Collectors.toList());
    }
}
