package com.danielagapov.spawn.Services.User;

import com.danielagapov.spawn.DTOs.FriendTag.FriendTagDTO;
import com.danielagapov.spawn.DTOs.User.BaseUserDTO;
import com.danielagapov.spawn.DTOs.User.FriendUser.FullFriendUserDTO;
import com.danielagapov.spawn.DTOs.User.FriendUser.RecommendedFriendUserDTO;
import com.danielagapov.spawn.DTOs.User.Profile.UserProfileInfoDTO;
import com.danielagapov.spawn.DTOs.User.RecentlySpawnedUserDTO;
import com.danielagapov.spawn.DTOs.User.UserDTO;
import com.danielagapov.spawn.DTOs.User.UserUpdateDTO;
import com.danielagapov.spawn.DTOs.UserIdActivityTimeDTO;
import com.danielagapov.spawn.Enums.EntityType;
import com.danielagapov.spawn.Enums.ParticipationStatus;
import com.danielagapov.spawn.Exceptions.ApplicationException;
import com.danielagapov.spawn.Exceptions.Base.BaseNotFoundException;
import com.danielagapov.spawn.Exceptions.Base.BaseSaveException;
import com.danielagapov.spawn.Exceptions.Base.BasesNotFoundException;
import com.danielagapov.spawn.Exceptions.Logger.ILogger;
import com.danielagapov.spawn.Mappers.FriendTagMapper;
import com.danielagapov.spawn.Mappers.UserMapper;
import com.danielagapov.spawn.Models.ActivityUser;
import com.danielagapov.spawn.Models.FriendTag;
import com.danielagapov.spawn.Models.User.User;
import com.danielagapov.spawn.Models.UserFriendTag;
import com.danielagapov.spawn.Repositories.IActivityUserRepository;
import com.danielagapov.spawn.Repositories.IFriendTagRepository;
import com.danielagapov.spawn.Repositories.IUserFriendTagRepository;
import com.danielagapov.spawn.Repositories.User.IUserRepository;
import com.danielagapov.spawn.Services.ActivityType.IActivityTypeService;
import com.danielagapov.spawn.Services.FriendRequest.IFriendRequestService;
import com.danielagapov.spawn.Services.FriendTag.IFriendTagService;
import com.danielagapov.spawn.Services.S3.IS3Service;
import com.danielagapov.spawn.Services.UserSearch.IUserSearchService;
import com.danielagapov.spawn.Util.LoggingUtils;
import com.danielagapov.spawn.Util.SearchedUserResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.context.annotation.Lazy;
import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;


@Service
public class UserService implements IUserService {
    private final IUserRepository repository;
    private final IActivityUserRepository activityUserRepository;
    private final IUserFriendTagRepository uftRepository;
    private final IFriendTagService friendTagService;
    private final IFriendTagRepository friendTagRepository;
    private final IS3Service s3Service;
    private final ILogger logger;
    private final IUserSearchService userSearchService;
    private final CacheManager cacheManager;
    private final IActivityTypeService activityTypeService;

    @Autowired
    @Lazy // Avoid circular dependency issues with ftService
    public UserService(IUserRepository repository,
                       IActivityUserRepository activityUserRepository,
                       IUserFriendTagRepository uftRepository,
                       IFriendTagService friendTagService,
                       IFriendTagRepository friendTagRepository,
                       IS3Service s3Service, ILogger logger,
                       IUserSearchService userSearchService,
                       CacheManager cacheManager,
                       IFriendRequestService friendRequestService, IActivityTypeService activityTypeService) {
        this.repository = repository;
        this.activityUserRepository = activityUserRepository;
        this.uftRepository = uftRepository;
        this.friendTagService = friendTagService;
        this.friendTagRepository = friendTagRepository;
        this.s3Service = s3Service;
        this.logger = logger;
        this.userSearchService = userSearchService;
        this.cacheManager = cacheManager;
        this.activityTypeService = activityTypeService;
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
            return uftRepository.findFriendIdsByUserId(id);
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

    @CacheEvict(value = "friendsByUserId", key = "#id")
    @Override
    public void deleteUserById(UUID id) {
        try {
            User user = repository.findById(id).orElseThrow(() -> new BaseNotFoundException(EntityType.User, id));
            logger.info("Deleting user: " + LoggingUtils.formatUserInfo(user));

            List<UUID> friendIds = getFriendUserIdsByUserId(id);
            for (UUID friendId : friendIds) {
                if (cacheManager.getCache("friendsByUserId") != null) {
                    cacheManager.getCache("friendsByUserId").evict(friendId);
                    cacheManager.getCache("recommendedFriends").evict(friendId);
                }
            }

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
            if (user.getProfilePicture() == null) {
                user = s3Service.putProfilePictureWithUser(profilePicture, user);
            }
            user = saveUser(user);
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
    public UserDTO createAndSaveUserWithProfilePicture(UserDTO user, byte[] profilePicture) {
        user = saveUserWithProfilePicture(user, profilePicture);
        createAndSaveUser(UserMapper.toEntity(user));
        return user;
    }

    @Override
    public User createAndSaveUser(User user) {
        // Save the user first so it has a valid ID for foreign key relationships
        user = repository.save(user);
        // Now initialize default activity types with the persisted user
        activityTypeService.initializeDefaultActivityTypesForUser(user);
        return user;
    }

    @Override
    public List<BaseUserDTO> getFriendsByFriendTagId(UUID friendTagId) {
        try {
            return UserMapper.toBaseDTOList(uftRepository.findFriendIdsByTagId(friendTagId)
                    .stream()
                    .map(this::getUserById)
                    .collect(Collectors.toList()));
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

    // Adds friend bidirectionally
    @Caching(evict = {
            @CacheEvict(value = "friendsByUserId", key = "#userId"),
            @CacheEvict(value = "friendsByUserId", key = "#friendId"),
            @CacheEvict(value = "recommendedFriends", key = "#userId"),
            @CacheEvict(value = "recommendedFriends", key = "#friendId")
    })
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
    @Cacheable(value = "recommendedFriends", key = "#userId")
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
    public List<BaseUserDTO> getParticipantsByActivityId(UUID activityId) {
        try {
            List<ActivityUser> activityUsers = activityUserRepository.findByActivity_IdAndStatus(activityId, ParticipationStatus.participating);

            return activityUsers.stream()
                    .map(activityUser -> UserMapper.toDTO(activityUser.getUser()))
                    .collect(Collectors.toList());
        } catch (Exception e) {
            logger.error("Error retrieving participants for activityId " + activityId + ": " + e.getMessage());
            throw new ApplicationException("Error retrieving participants for activityId " + activityId, e);
        }
    }

    @Override
    public List<BaseUserDTO> getInvitedByActivityId(UUID activityId) {
        try {
            List<ActivityUser> activityUsers = activityUserRepository.findByActivity_IdAndStatus(activityId, ParticipationStatus.invited);

            return activityUsers.stream()
                    .map(activityUser -> UserMapper.toDTO(activityUser.getUser()))
                    .collect(Collectors.toList());
        } catch (Exception e) {
            logger.error("Error retrieving invited users for activityId " + activityId + ": " + e.getMessage());
            throw new ApplicationException("Error retrieving invited users for activityId " + activityId, e);
        }
    }

    @Override
    public List<UUID> getParticipantUserIdsByActivityId(UUID activityId) {
        try {
            List<ActivityUser> activityUsers = activityUserRepository.findByActivity_IdAndStatus(activityId, ParticipationStatus.participating);

            return activityUsers.stream()
                    .map(activityUser -> activityUser.getUser().getId())
                    .collect(Collectors.toList());
        } catch (Exception e) {
            logger.error("Error retrieving participant user IDs for activityId " + activityId + ": " + e.getMessage());
            throw new ApplicationException("Error retrieving participant user IDs for activityId " + activityId, e);
        }
    }

    @Override
    public List<UUID> getInvitedUserIdsByActivityId(UUID activityId) {
        try {
            List<ActivityUser> activityUsers = activityUserRepository.findByActivity_IdAndStatus(activityId, ParticipationStatus.invited);

            return activityUsers.stream()
                    .map(activityUser -> activityUser.getUser().getId())
                    .collect(Collectors.toList());
        } catch (Exception e) {
            logger.error("Error retrieving invited user IDs for activityId " + activityId + ": " + e.getMessage());
            throw new ApplicationException("Error retrieving invited user IDs for activityId " + activityId, e);
        }
    }





    @Override
    public boolean existsByUsername(String username) {
        return repository.existsByUsername(username);
    }

    @Override
    public boolean existsByPhoneNumber(String phoneNumber) {
        return repository.existsByPhoneNumber(phoneNumber);
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
     *                         from either the activity creation view or friends view.
     * @return `FullFriendUserDTO` list of friends for the requesting user,
     * which includes all that's in `FullUserDTO`, plus the friend tags that
     * the requesting user has associated to their friends
     */
    @Override
    public List<FullFriendUserDTO> getFullFriendUsersByUserId(UUID requestingUserId) {
        try {
            // Retrieve all UserFriendTag entries for this owner in a single query
            List<UserFriendTag> userFriendTags = uftRepository.findAllFriendsWithTagsByOwnerId(requestingUserId);

            // If no entries were found, we might still have friends in the "Everyone" tag
            if (userFriendTags.isEmpty()) {
                return getFallbackFriendsList(requestingUserId);
            }

            // Create a map to efficiently group tags by user
            Map<UUID, List<FriendTagDTO>> friendToTagsMap = new HashMap<>();
            Map<UUID, User> friendUsersMap = new HashMap<>();

            // Process all UserFriendTag entries to build our maps
            for (UserFriendTag uft : userFriendTags) {
                User friend = uft.getFriend();
                FriendTag tag = uft.getFriendTag();

                // Skip if this is the owner (not a friend)
                if (friend.getId().equals(requestingUserId)) {
                    continue;
                }

                // Add user to our map if not already there
                friendUsersMap.putIfAbsent(friend.getId(), friend);

                // Add this tag to the user's tag list
                friendToTagsMap.computeIfAbsent(friend.getId(), k -> new ArrayList<>())
                        .add(FriendTagMapper.toDTO(tag, requestingUserId, List.of()));
            }

            // Create FullFriendUserDTO objects for each unique friend
            List<FullFriendUserDTO> result = new ArrayList<>();
            for (User friend : friendUsersMap.values()) {
                List<FriendTagDTO> tags = friendToTagsMap.getOrDefault(friend.getId(), List.of());

                FullFriendUserDTO dto = new FullFriendUserDTO(
                        friend.getId(),
                        friend.getUsername(),
                        friend.getProfilePictureUrlString(),
                        friend.getName(),
                        friend.getBio(),
                        friend.getEmail(),
                        tags
                );

                result.add(dto);
            }

            // If no friends were found, try the fallback method
            if (result.isEmpty()) {
                return getFallbackFriendsList(requestingUserId);
            }

            return result;
        } catch (Exception e) {
            logger.error("Error retrieving full friend users: " + e.getMessage());
            throw e;
        }
    }

    /**
     * Fallback method to get friends from the "Everyone" tag when the optimized query returns no results
     */
    private List<FullFriendUserDTO> getFallbackFriendsList(UUID requestingUserId) {
        try {
            // Get friends from the "Everyone" tag using the existing method
            List<User> userFriends = getFriendUsersByUserId(requestingUserId);

            // Try to get the "Everyone" tag
            UUID everyoneTagId = friendTagRepository.getEveryoneTagIdByOwnerId(requestingUserId);
            FriendTagDTO everyoneTag = null;

            if (everyoneTagId != null) {
                FriendTag tag = friendTagRepository.findById(everyoneTagId)
                        .orElse(null);
                if (tag != null) {
                    everyoneTag = FriendTagMapper.toDTO(tag, requestingUserId, List.of());
                }
            }

            // Create DTO list
            List<FullFriendUserDTO> result = new ArrayList<>();
            for (User friend : userFriends) {
                List<FriendTagDTO> tags = new ArrayList<>();
                if (everyoneTag != null) {
                    tags.add(everyoneTag);
                }

                FullFriendUserDTO dto = new FullFriendUserDTO(
                        friend.getId(),
                        friend.getUsername(),
                        friend.getProfilePictureUrlString(),
                        friend.getName(),
                        friend.getBio(),
                        friend.getEmail(),
                        tags
                );

                result.add(dto);
            }

            return result;
        } catch (Exception e) {
            logger.error("Error in fallback friends list: " + e.getMessage());
            throw e;
        }
    }

    @Override
    public List<User> getFriendUsersByUserId(UUID requestingUserId) {
        try {
            // Get the friend IDs
            List<UUID> friendIds = getFriendUserIdsByUserId(requestingUserId);
            
            // Fetch and return the user entities
            if (!friendIds.isEmpty()) {
                return repository.findAllById(friendIds);
            }
            
            return List.of();
        } catch (Exception e) {
            logger.error("Error retrieving friend users by user ID: " + LoggingUtils.formatUserIdInfo(requestingUserId) + ": " + e.getMessage());
            throw e;
        }
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
    public BaseUserDTO updateUser(UUID id, UserUpdateDTO updateDTO) {
        try {
            User user = repository.findById(id)
                    .orElseThrow(() -> new BaseNotFoundException(EntityType.User, id));

            user.setBio(updateDTO.getBio());
            user.setUsername(updateDTO.getUsername());
            user.setName(updateDTO.getName());

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
    public List<RecentlySpawnedUserDTO> getRecentlySpawnedWithUsers(UUID requestingUserId) {
        try {
            final int activityLimit = 10;
            final int userLimit = 40;
            List<UUID> pastActivityIds = activityUserRepository.findPastActivityIdsForUser(requestingUserId, ParticipationStatus.participating, Limit.of(activityLimit));
            List<UserIdActivityTimeDTO> pastActivityParticipantIds = activityUserRepository.findOtherUserIdsByActivityIds(pastActivityIds, requestingUserId, ParticipationStatus.participating);
            Set<UUID> excludedIds = userSearchService.getExcludedUserIds(requestingUserId);

            return pastActivityParticipantIds.stream()
                    .filter(e -> !excludedIds.contains(e.getUserId()))
                    .map(e -> new RecentlySpawnedUserDTO(getBaseUserById(e.getUserId()), e.getStartTime()))
                    .limit(userLimit)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            logger.error("Error fetching recently spawned-with users for user: " + LoggingUtils.formatUserIdInfo(requestingUserId) + ". " + e.getMessage());
            throw e;
        }
    }

    @Override
    public BaseUserDTO getBaseUserByUsername(String username) {
        try {
            return UserMapper.toDTO(getUserEntityByUsername(username));
        } catch (Exception e) {
            logger.error("Error getting BaseUserDTO by username (" + username + "): " + e.getMessage());
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

    @Override
    public boolean isUserFriendOfUser(UUID userId, UUID potentialFriendId) {
        try {
            // Use the direct query method
            return uftRepository.isUserFriendOfUser(userId, potentialFriendId);
        } catch (Exception e) {
            logger.error("Error checking if user is friend of user: " + 
                         LoggingUtils.formatUserIdInfo(userId) + " and " + 
                         LoggingUtils.formatUserIdInfo(potentialFriendId) + ": " + e.getMessage());
            throw e;
        }
    }

    @Override
    public UserProfileInfoDTO getUserProfileInfo(UUID userId) {
        try {
            User user = repository.findById(userId)
                    .orElseThrow(() -> new BaseNotFoundException(EntityType.User, userId));

            return new UserProfileInfoDTO(
                    user.getId(),
                    user.getName(),
                    user.getUsername(),
                    user.getBio(),
                    user.getProfilePictureUrlString(),
                    user.getDateCreated()
            );
        } catch (Exception e) {
            logger.error("Error getting user profile info for user: " + LoggingUtils.formatUserIdInfo(userId) + ": " + e.getMessage());
            throw e;
        }
    }
}
