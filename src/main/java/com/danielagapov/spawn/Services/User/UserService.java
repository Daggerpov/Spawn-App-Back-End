package com.danielagapov.spawn.Services.User;

import com.danielagapov.spawn.DTOs.FriendTag.FriendTagDTO;
import com.danielagapov.spawn.DTOs.User.BaseUserDTO;
import com.danielagapov.spawn.DTOs.User.FriendUser.FullFriendUserDTO;
import com.danielagapov.spawn.DTOs.User.FriendUser.RecommendedFriendUserDTO;
import com.danielagapov.spawn.DTOs.User.UserDTO;
import com.danielagapov.spawn.Enums.EntityType;
import com.danielagapov.spawn.Enums.ParticipationStatus;
import com.danielagapov.spawn.Exceptions.ApplicationException;
import com.danielagapov.spawn.Exceptions.Base.BaseNotFoundException;
import com.danielagapov.spawn.Exceptions.Base.BaseSaveException;
import com.danielagapov.spawn.Exceptions.Base.BasesNotFoundException;
import com.danielagapov.spawn.Exceptions.Logger.ILogger;
import com.danielagapov.spawn.Mappers.UserMapper;
import com.danielagapov.spawn.Models.EventUser;
import com.danielagapov.spawn.Models.FriendTag;
import com.danielagapov.spawn.Models.User;
import com.danielagapov.spawn.Repositories.IEventUserRepository;
import com.danielagapov.spawn.Repositories.IFriendTagRepository;
import com.danielagapov.spawn.Repositories.IUserFriendTagRepository;
import com.danielagapov.spawn.Repositories.IUserRepository;
import com.danielagapov.spawn.Services.BlockedUser.IBlockedUserService;
import com.danielagapov.spawn.Services.FriendRequest.IFriendRequestService;
import com.danielagapov.spawn.Services.FriendTag.IFriendTagService;
import com.danielagapov.spawn.Services.S3.IS3Service;
import com.danielagapov.spawn.Services.UserSearch.IUserSearchService;
import com.danielagapov.spawn.Services.UserSearch.UserSearchService;
import com.danielagapov.spawn.Util.SearchedUserResult;
import com.danielagapov.spawn.Utils.LoggingUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class UserService implements IUserService {
    private final IUserRepository repository;
    private final IEventUserRepository eventUserRepository;
    private final IUserFriendTagRepository uftRepository;
    private final IFriendTagService friendTagService;
    private final IFriendTagRepository friendTagRepository;
    private final IS3Service s3Service;
    private final ILogger logger;
    private final IUserSearchService userSearchService;

    @Autowired
    @Lazy // Avoid circular dependency issues with ftService
    public UserService(IUserRepository repository,
                       IEventUserRepository eventUserRepository,
                       IUserFriendTagRepository uftRepository,
                       IFriendTagService friendTagService,
                       IFriendTagRepository friendTagRepository,
                       IS3Service s3Service, ILogger logger,
                       UserSearchService userSearchService){
        this.repository = repository;
        this.eventUserRepository = eventUserRepository;
        this.uftRepository = uftRepository;
        this.friendTagService = friendTagService;
        this.friendTagRepository = friendTagRepository;
        this.s3Service = s3Service;
        this.logger = logger;
        this.userSearchService = userSearchService;
    }

    @Override
    public List<UserDTO> getAllUsers() {
        try {
            return getUserDTOs();
        } catch (DataAccessException e) {
            logger.error(e.getMessage());
            throw new BasesNotFoundException(EntityType.User);
        } catch (Exception e) {
            logger.error(e.getMessage());
            throw e;
        }
    }

    // CRUD operations:

    @Override
    public UserDTO getUserById(UUID id) {
        try {
            User user = repository.findById(id)
                    .orElseThrow(() -> new BaseNotFoundException(EntityType.User, id));

            logger.info("Retrieved user: " + LoggingUtils.formatUserInfo(user));

            List<UUID> friendUserIds = getFriendUserIdsByUserId(id);

            // Fetch FriendTag IDs based on the user ID
            List<UUID> friendTagIds = friendTagService.getFriendTagIdsByOwnerUserId(user.getId());

            // Pass in the friendTagIds and friendTags as needed
            return UserMapper.toDTO(user, friendUserIds, friendTagIds);
        } catch (Exception e) {
            logger.error("Error getting user: " + LoggingUtils.formatUserIdInfo(id) + ": " + e.getMessage());
            throw e;
        }
    }

    @Override
    public List<UUID> getFriendUserIdsByUserId(UUID id) {
        try {
            logger.info("Getting friend user IDs for user: " + LoggingUtils.formatUserIdInfo(id));
            // Get all friend tags for the user
            List<FriendTag> friendTags = friendTagRepository.findByOwnerId(id);

            // Get the "Everyone" tag
            Optional<FriendTag> everyoneTag = friendTags.stream()
                    .filter(FriendTag::isEveryone)
                    .findFirst();

            // If "Everyone" tag exists, get all friend IDs from it
            if (everyoneTag.isPresent()) {
                return uftRepository.findFriendIdsByTagId(everyoneTag.get().getId());
            }

            // If no "Everyone" tag, return empty list
            return Collections.emptyList();
        } catch (Exception e) {
            logger.error("Error getting friend user IDs for user: " + LoggingUtils.formatUserIdInfo(id) + ": " + e.getMessage());
            throw e;
        }
    }

    @Override
    public User getUserEntityById(UUID id) {
        try {
            User user = repository.findById(id)
                    .orElseThrow(() -> new BaseNotFoundException(EntityType.User, id));
            logger.info("Retrieved user entity: " + LoggingUtils.formatUserInfo(user));
            return user;
        } catch (Exception e) {
            logger.error("Error retrieving user entity: " + LoggingUtils.formatUserIdInfo(id) + ": " + e.getMessage());
            throw e;
        }
    }

    // For Friend Tags:

    @Override
    public Map<FriendTag, UUID> getOwnerUserIdsMap() {
        try {
            List<FriendTag> friendTags = friendTagRepository.findAll(); // TODO: don't find by all
            return friendTags.stream()
                    .collect(Collectors.toMap(
                            friendTag -> friendTag,
                            FriendTag::getOwnerId
                    ));
        } catch (Exception e) {
            logger.error(e.getMessage());
            throw e;
        }
    }

    @Override
    public Map<FriendTag, List<UUID>> getFriendUserIdsMap() {
        try {
            // Fetch all FriendTags
            List<FriendTag> friendTags = friendTagRepository.findAll();

            // Create a map of FriendTag to a list of associated user IDs
            return friendTags.stream()
                    .collect(Collectors.toMap(
                            friendTag -> friendTag, // Use FriendTag as the key
                            friendTag -> uftRepository.findFriendIdsByTagId(friendTag.getId())
                    ));
        } catch (Exception e) {
            logger.error(e.getMessage());
            throw e;
        }
    }

    @Override
    public UserDTO saveUser(UserDTO user) {
        try {
            logger.info("Saving user with username: " + user.getUsername());
            User userEntity = UserMapper.toEntity(user);
            userEntity = repository.save(userEntity);
            logger.info("User saved successfully: " + LoggingUtils.formatUserInfo(userEntity));

            FriendTagDTO everyoneTagDTO = new FriendTagDTO(null, "Everyone",
                    "#8693FF", userEntity.getId(), List.of(), true);
            FriendTagDTO everyoneTagDTOAfterPersisting = friendTagService.saveFriendTag(everyoneTagDTO);
            // id is generated when saving
            return UserMapper.toDTO(userEntity, List.of(), List.of(everyoneTagDTOAfterPersisting.getId()));
        } catch (DataAccessException e) {
            logger.error("Failed to save user: " + e.getMessage());
            throw new BaseSaveException("Failed to save user: " + e.getMessage()); 
        } catch (Exception e) {
            logger.error("Error saving user: " + e.getMessage());
            throw e;
        }
    }

    @Override
    public void deleteUserById(UUID id) {
        try {
            User user = repository.findById(id).orElseThrow(() -> new BaseNotFoundException(EntityType.User, id));
            logger.info("Deleting user: " + LoggingUtils.formatUserInfo(user));
            repository.deleteById(id);
            s3Service.deleteObjectByURL(user.getProfilePictureUrlString());
        } catch (Exception e) {
            logger.error(e.getMessage());
            throw e;
        }
    }

    @Override
    public User saveEntity(User user) {
        try {
            return repository.save(user);
        } catch (Exception e) {
            logger.error(e.getMessage());
            throw e;
        }
    }

    @Override
    public UserDTO saveUserWithProfilePicture(UserDTO user, byte[] profilePicture) {
        try {
            logger.info(String.format("Entering saveUserWithProfilePicture: {user: %s}", user));
            if (user.getProfilePicture() == null) {
                logger.info("Profile picture is null, user either chose their profile picture or has default");
                user = s3Service.putProfilePictureWithUser(profilePicture, user);
            }
            user = saveUser(user);
            logger.info(String.format("Exiting saveUserWithProfilePicture: {user: %s}", user));
            return user;
        } catch (Exception e) {
            logger.error(e.getMessage());
            throw e;
        }
    }

    @Override
    public UserDTO getUserDTOByEntity(User user) {
        List<UUID> friendUserIds = getFriendUserIdsByUserId(user.getId());

        // Fetch FriendTag IDs based on the user ID
        List<UUID> friendTagIds = friendTagService.getFriendTagIdsByOwnerUserId(user.getId());

        // Pass in the friendTagIds and friendTags as needed
        return UserMapper.toDTO(user, friendUserIds, friendTagIds);
    }

    @Override
    public List<UserDTO> getFriendsByFriendTagId(UUID friendTagId) {
        try {
            return uftRepository.findFriendIdsByTagId(friendTagId)
                    .stream()
                    .map(this::getUserById)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            logger.error(e.getMessage());
            throw e;
        }
    }

    @Override
    public List<UUID> getFriendUserIdsByFriendTagId(UUID friendTagId) {
        try {
            return uftRepository.findFriendIdsByTagId(friendTagId);
        } catch (Exception e) {
            logger.error(e.getMessage());
            throw e;
        }
    }

    private List<UserDTO> getUserDTOs() {
        try {
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
        } catch (Exception e) {
            logger.error(e.getMessage());
            throw e;
        }
    }

    @Override
    public List<UserDTO> getFriendsByUserId(UUID userId) {
        try {
            // Get the FriendTags associated with the user (assuming userId represents the owner of friend tags)
            Optional<FriendTag> optionalEveryoneTag = friendTagRepository.findByOwnerIdAndIsEveryoneTrue(userId);

            if (optionalEveryoneTag.isEmpty()) {
                return List.of(); // empty list of friends
            }

            FriendTag everyoneTag = optionalEveryoneTag.get();

            // Retrieve the friends for each FriendTag and return as a flattened list
            List<UserDTO> friends = getFriendsByFriendTagId(everyoneTag.getId());

            // Filter out the friend whose ID matches the userId
            // Exclude the user themselves

            return friends.stream()
                    .filter(friend -> !friend.getId().equals(userId)) // Exclude the user themselves
                    .collect(Collectors.toList());
        } catch (Exception e) {
            logger.error(e.getMessage());
            throw e;
        }
    }

    // Adds friend bidirectionally
    @Override
    public void saveFriendToUser(UUID userId, UUID friendId) {
        try {
            if (userId.equals(friendId)) {
                logger.info("Attempted to add self to Everyone tag. Skipping.");
                return;
            }

            Optional<FriendTag> userEveryoneTag = friendTagRepository.findByOwnerIdAndIsEveryoneTrue(userId);
            userEveryoneTag.ifPresent(tag ->
                    friendTagService.saveUserToFriendTag(tag.getId(), friendId));

            Optional<FriendTag> friendEveryoneTag = friendTagRepository.findByOwnerIdAndIsEveryoneTrue(friendId);
            friendEveryoneTag.ifPresent(tag ->
                    friendTagService.saveUserToFriendTag(tag.getId(), userId));
        } catch (Exception e) {
            logger.error(e.getMessage());
            throw e;
        }
    }

    // returns top x (specified by limit) friends with most mutuals with user (with `userId`) as
    // `RecommendedFriendUserDTO`s, to include the `mutualFriendCount`
    @Override
    public List<RecommendedFriendUserDTO> getLimitedRecommendedFriendsForUserId(UUID userId) {
        return userSearchService.getLimitedRecommendedFriendsForUserId(userId);
    }

    @Override
    public SearchedUserResult getRecommendedFriendsBySearch(UUID requestingUserId, String searchQuery) {
        return userSearchService.getRecommendedFriendsBySearch(requestingUserId, searchQuery);
    }

    @Override
    public List<BaseUserDTO> searchByQuery(String searchQuery) {
        return userSearchService.searchByQuery(searchQuery);
    }

    @Override
    public List<BaseUserDTO> getParticipantsByEventId(UUID eventId) {
        try {
            List<EventUser> eventUsers = eventUserRepository.findByEvent_IdAndStatus(eventId, ParticipationStatus.participating);

            return eventUsers.stream()
                    .map(eventUser -> UserMapper.toDTO(eventUser.getUser()))
                    .collect(Collectors.toList());
        } catch (Exception e) {
            logger.error("Error retrieving participants for eventId " + eventId + ": " + e.getMessage());
            throw new ApplicationException("Error retrieving participants for eventId " + eventId, e);
        }
    }

    @Override
    public List<BaseUserDTO> getInvitedByEventId(UUID eventId) {
        try {
            List<EventUser> eventUsers = eventUserRepository.findByEvent_IdAndStatus(eventId, ParticipationStatus.invited);

            return eventUsers.stream()
                    .map(eventUser -> UserMapper.toDTO(eventUser.getUser()))
                    .collect(Collectors.toList());
        } catch (Exception e) {
            logger.error("Error retrieving invited users for eventId " + eventId + ": " + e.getMessage());
            throw new ApplicationException("Error retrieving invited users for eventId " + eventId, e);
        }
    }

    @Override
    public List<UUID> getParticipantUserIdsByEventId(UUID eventId) {
        try {
            List<EventUser> eventUsers = eventUserRepository.findByEvent_IdAndStatus(eventId, ParticipationStatus.participating);

            return eventUsers.stream()
                    .map(eventUser -> eventUser.getUser().getId())
                    .collect(Collectors.toList());
        } catch (Exception e) {
            logger.error("Error retrieving participant user IDs for eventId " + eventId + ": " + e.getMessage());
            throw new ApplicationException("Error retrieving participant user IDs for eventId " + eventId, e);
        }
    }

    @Override
    public List<UUID> getInvitedUserIdsByEventId(UUID eventId) {
        try {
            List<EventUser> eventUsers = eventUserRepository.findByEvent_IdAndStatus(eventId, ParticipationStatus.invited);

            return eventUsers.stream()
                    .map(eventUser -> eventUser.getUser().getId())
                    .collect(Collectors.toList());
        } catch (Exception e) {
            logger.error("Error retrieving invited user IDs for eventId " + eventId + ": " + e.getMessage());
            throw new ApplicationException("Error retrieving invited user IDs for eventId " + eventId, e);
        }
    }

    @Override
    public boolean existsByUsername(String username) {
        return repository.existsByUsername(username);
    }

    @Override
    public void verifyUserByUsername(String username) {
        try {
            logger.info("Marking user as verified " + username);
            User user = repository.findByUsername(username).orElseThrow(() -> new BaseNotFoundException(EntityType.User, username, "username"));
            user.setVerified(true);
            repository.save(user);
        } catch (Exception e) {
            logger.error("Unexpected error while marking user as verified: " + e.getMessage());
            throw e;
        }
    }

    @Override
    public boolean existsByEmail(String email) {
        return repository.existsByEmail(email);
    }

    /**
     * @param requestingUserId the user who's requesting this from the mobile app,
     *                         from either the event creation view or friends view.
     * @return `FullFriendUserDTO` list of friends for the requesting user,
     * which includes all that's in `FullUserDTO`, plus the friend tags that
     * the requesting user has associated to their friends
     */
    @Override
    public List<FullFriendUserDTO> getFullFriendUsersByUserId(UUID requestingUserId) {
        try {
            List<User> userFriends = getFriendUsersByUserId(requestingUserId);

            List<FullFriendUserDTO> fullFriendUserDTOList = new ArrayList<>();
            for (User user : userFriends) {
                FullFriendUserDTO fullFriendUserDTO = new FullFriendUserDTO(
                        user.getId(),
                        user.getUsername(),
                        user.getProfilePictureUrlString(),
                        user.getFirstName(),
                        user.getLastName(),
                        user.getBio(),
                        user.getEmail(),
                        friendTagService.getPertainingFriendTagsForFriend(requestingUserId, user.getId())
                );
                fullFriendUserDTOList.add(fullFriendUserDTO);
            }

            return fullFriendUserDTOList;
        } catch (Exception e) {
            logger.error(e.getMessage());
            throw e;
        }
    }

    @Override
    public List<User> getFriendUsersByUserId(UUID requestingUserId) {
        try {
            return friendTagRepository.getFriendsFromEveryoneTagByOwnerId(requestingUserId);
        } catch (BaseNotFoundException e) {
            logger.warn("Could not find user with id: " + requestingUserId);
            throw e;
        } catch (Exception e) {
            logger.error("Unexpected error: " + e.getMessage());
        }

        return List.of();
    }

    @Override
    public int getMutualFriendCount(UUID userId1, UUID userId2) {
        List<UUID> user1Friends = new ArrayList<>(getFriendUserIdsByUserId(userId1));
        List<UUID> user2Friends = getFriendUserIdsByUserId(userId2);

        // Create a mutable copy of user1Friends and retain only elements that are also in user2Friends
        user1Friends.retainAll(user2Friends);
        return user1Friends.size();
    }

    @Override
    public BaseUserDTO getBaseUserById(UUID id) {
        try {
            User user = repository.findById(id)
                    .orElseThrow(() -> new BaseNotFoundException(EntityType.User, id));
            return UserMapper.toDTO(user);
        } catch (Exception e) {
            logger.error(e.getMessage());
            throw e;
        }
    }

    @Override
    public BaseUserDTO updateUser(UUID id, String bio, String username, String firstName, String lastName) {
        try {
            User user = repository.findById(id)
                    .orElseThrow(() -> new BaseNotFoundException(EntityType.User, id));

            user.setBio(bio);
            user.setUsername(username);
            user.setFirstName(firstName);
            user.setLastName(lastName);

            user = repository.save(user);

            return UserMapper.toDTO(user);
        } catch (Exception e) {
            logger.error("Error updating user " + id + ": " + e.getMessage());
            throw e;
        }
    }

    @Override
    public User getUserEntityByUsername(String username) {
        try {
            return repository.findByUsername(username)
                    .orElseThrow(() -> new BaseNotFoundException(EntityType.User, username, "username"));
        } catch (Exception e) {
            logger.error(e.getMessage());
            throw e;
        }
    }

    @Override
    public User getUserByEmail(String email) {
        try {
            return repository.findByEmail(email)
                    .orElseThrow(() -> new BaseNotFoundException(EntityType.User, email, "email"));
        } catch (Exception e) {
            logger.error(e.getMessage());
            throw e;
        }
    }

    @Override
    public Instant getLatestFriendProfileUpdateTimestamp(UUID userId) {
        try {
            return repository.findLatestFriendProfileUpdate(userId);
        } catch (Exception e) {
            logger.error("Error getting latest friend profile update timestamp for user: " + userId + ": " + e.getMessage());
            throw e;
        }
    }
}
