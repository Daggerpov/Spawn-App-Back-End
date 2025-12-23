package com.danielagapov.spawn.user.internal.services;

import com.danielagapov.spawn.user.api.dto.*;
import com.danielagapov.spawn.activity.api.dto.UserIdActivityTimeDTO;
import com.danielagapov.spawn.shared.util.EntityType;
import com.danielagapov.spawn.shared.util.ParticipationStatus;
import com.danielagapov.spawn.shared.util.UserStatus;
import com.danielagapov.spawn.shared.exceptions.ApplicationException;
import com.danielagapov.spawn.shared.exceptions.Base.BaseNotFoundException;
import com.danielagapov.spawn.shared.exceptions.Base.BaseSaveException;
import com.danielagapov.spawn.shared.exceptions.Base.BasesNotFoundException;
import com.danielagapov.spawn.shared.exceptions.Logger.ILogger;
import com.danielagapov.spawn.shared.util.UserMapper;
import com.danielagapov.spawn.activity.internal.domain.ActivityUser;
import com.danielagapov.spawn.social.internal.domain.Friendship;
import com.danielagapov.spawn.user.internal.domain.User;
import com.danielagapov.spawn.activity.internal.repositories.IActivityUserRepository;
import com.danielagapov.spawn.social.internal.repositories.IFriendshipRepository;
import com.danielagapov.spawn.auth.internal.repositories.IUserIdExternalIdMapRepository;
import com.danielagapov.spawn.user.internal.repositories.IUserRepository;
import com.danielagapov.spawn.shared.events.UserActivityTypeEvents.UserCreatedEvent;
import com.danielagapov.spawn.media.internal.services.IS3Service;
import com.danielagapov.spawn.user.internal.services.IUserSearchService;
import com.danielagapov.spawn.shared.util.LoggingUtils;
import com.danielagapov.spawn.shared.util.PhoneNumberMatchingUtil;
import com.danielagapov.spawn.shared.util.SearchedUserResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;


@Service
public class UserService implements IUserService {
    private final IUserRepository repository;
    private final IActivityUserRepository activityUserRepository;
    private final IFriendshipRepository friendshipRepository;

    private final IS3Service s3Service;
    private final ILogger logger;
    private final IUserSearchService userSearchService;
    private final CacheManager cacheManager;
    private final ApplicationEventPublisher eventPublisher;
    private final IUserIdExternalIdMapRepository userIdExternalIdMapRepository;

    @Value("${ADMIN_USERNAME:admin}")
    private String adminUsername;

    @Autowired
    public UserService(IUserRepository repository,
                       IActivityUserRepository activityUserRepository,
                       IFriendshipRepository friendshipRepository,

                       IS3Service s3Service, ILogger logger,
                       @Lazy IUserSearchService userSearchService,
                       CacheManager cacheManager,
                       ApplicationEventPublisher eventPublisher,
                       IUserIdExternalIdMapRepository userIdExternalIdMapRepository) {
        this.repository = repository;
        this.activityUserRepository = activityUserRepository;
        this.friendshipRepository = friendshipRepository;
        this.s3Service = s3Service;
        this.logger = logger;
        this.userSearchService = userSearchService;
        this.cacheManager = cacheManager;
        this.eventPublisher = eventPublisher;
        this.userIdExternalIdMapRepository = userIdExternalIdMapRepository;
    }

    /**
     * Helper method to check if a user is the admin user
     */
    private boolean isAdminUser(User user) {
        return user != null && adminUsername.equals(user.getUsername());
    }

    /**
     * Helper method to filter out admin users from a list of users
     */
    private <T> List<T> filterOutAdminUsers(List<T> users, java.util.function.Function<T, User> userExtractor) {
        return users.stream()
                .filter(item -> !isAdminUser(userExtractor.apply(item)))
                .collect(Collectors.toList());
    }

    /**
     * Helper method to filter out admin users from BaseUserDTO lists
     */
    private List<BaseUserDTO> filterOutAdminFromBaseUserDTOs(List<BaseUserDTO> users) {
        return users.stream()
                .filter(user -> !adminUsername.equals(user.getUsername()))
                .collect(Collectors.toList());
    }

    /**
     * Helper method to filter out admin users from FullFriendUserDTO lists
     */
    private List<FullFriendUserDTO> filterOutAdminFromFullFriendUserDTOs(List<FullFriendUserDTO> users) {
        return users.stream()
                .filter(user -> !adminUsername.equals(user.getUsername()))
                .collect(Collectors.toList());
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

            // Hide admin user from front-end
            if (isAdminUser(user)) {
                throw new BaseNotFoundException(EntityType.User, id);
            }

            List<UUID> friendUserIds = getFriendUserIdsByUserId(id);
            return UserMapper.toDTO(user, friendUserIds);
        } catch (Exception e) {
            logger.error("Error getting user: " + LoggingUtils.formatUserIdInfo(id) + ": " + e.getMessage());
            throw e;
        }
    }

    @Override
    public List<UUID> getFriendUserIdsByUserId(UUID id) {
        try {
            return friendshipRepository.findAllByUserIdBidirectional(id)
                    .stream()
                    .map(f -> f.getUserA().getId().equals(id) ? f.getUserB().getId() : f.getUserA().getId())
                    .collect(Collectors.toList());
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

    // Friend tags removed

    @Override
    public UserDTO saveUser(UserDTO user) {
        try {
            logger.info("Saving user with username: " + user.getUsername());
            User userEntity = UserMapper.toEntity(user);
            userEntity = repository.save(userEntity);
            logger.info("User saved successfully: " + LoggingUtils.formatUserInfo(userEntity));
            return UserMapper.toDTO(userEntity, List.of());
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

            // OAuth mappings will be automatically deleted by database cascade deletion
            // Removing explicit deletion to avoid race conditions during concurrent OAuth operations

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
        return UserMapper.toDTO(user, friendUserIds);
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
        
        // Publish event for Activity module to initialize default activity types
        // This breaks the circular dependency between User and ActivityType
        eventPublisher.publishEvent(new UserCreatedEvent(
            user.getId(),
            user.getUsername() != null ? user.getUsername() : "unknown"
        ));
        
        return user;
    }

    // Friend-tag based methods removed in friendship refactor

    private List<UserDTO> getUserDTOs() {
        try {
            List<User> users = repository.findAllUsersByStatus(UserStatus.ACTIVE);
            Map<User, List<UUID>> friendUserIdsMap = users.stream()
                    .collect(Collectors.toMap(
                            user -> user,
                            user -> getFriendUserIdsByUserId(user.getId())
                    ));
            return UserMapper.toDTOList(users, friendUserIdsMap);
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

            // Canonical ordering (smallest UUID first)
            UUID aId = userId;
            UUID bId = friendId;
            if (aId.compareTo(bId) > 0) {
                UUID tmp = aId;
                aId = bId;
                bId = tmp;
            }

            // If already friends, no-op
            if (friendshipRepository.existsByUserA_IdAndUserB_Id(aId, bId)) {
                return;
            }

            // Load users and save friendship
            User userA = getUserEntityById(aId);
            User userB = getUserEntityById(bId);
            Friendship friendship = new Friendship();
            friendship.setUserA(userA);
            friendship.setUserB(userB);
            friendshipRepository.save(friendship);

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
    public List<BaseUserDTO> searchByQuery(String searchQuery, UUID requestingUserId) {
        return userSearchService.searchByQuery(searchQuery, requestingUserId);
    }

    @Override
    public List<BaseUserDTO> getParticipantsByActivityId(UUID activityId) {
        try {
            List<ActivityUser> activityUsers = activityUserRepository.findByActivity_IdAndStatus(activityId, ParticipationStatus.participating);

            List<BaseUserDTO> participants = activityUsers.stream()
                    .map(activityUser -> UserMapper.toDTO(activityUser.getUser()))
                    .collect(Collectors.toList());

            // Filter out admin user from activity participants
            return filterOutAdminFromBaseUserDTOs(participants);
        } catch (Exception e) {
            logger.error("Error retrieving participants for activityId " + activityId + ": " + e.getMessage());
            throw new ApplicationException("Error retrieving participants for activityId " + activityId, e);
        }
    }

    @Override
    public List<BaseUserDTO> getInvitedByActivityId(UUID activityId) {
        try {
            List<ActivityUser> activityUsers = activityUserRepository.findByActivity_IdAndStatus(activityId, ParticipationStatus.invited);

            List<BaseUserDTO> invitedUsers = activityUsers.stream()
                    .map(activityUser -> UserMapper.toDTO(activityUser.getUser()))
                    .collect(Collectors.toList());

            // Filter out admin user from activity invitees
            return filterOutAdminFromBaseUserDTOs(invitedUsers);
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
    public boolean existsByUserId(UUID userId) {
        return repository.existsById(userId);
    }

    @Override
    public void verifyUserByUsername(String username) {
        try {
            logger.info("Marking user as verified " + username);
            User user = repository.findByUsername(username).orElseThrow(() -> new BaseNotFoundException(EntityType.User, username, "username"));
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

    @Override
    public boolean existsByEmailAndStatus(String email, UserStatus status) {
        return repository.existsByEmailAndStatus(email, status);
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
            List<UUID> friendIds = getFriendUserIdsByUserId(requestingUserId);
            if (friendIds.isEmpty()) {
                return List.of();
            }
            List<User> friendUsers = repository.findAllById(friendIds);
            List<FullFriendUserDTO> result = new ArrayList<>();
            for (User friend : friendUsers) {
                FullFriendUserDTO dto = new FullFriendUserDTO(
                        friend.getId(),
                        friend.getUsername(),
                        friend.getProfilePictureUrlString(),
                        friend.getName(),
                        friend.getBio(),
                        friend.getEmail()
                );
                result.add(dto);
            }
            return filterOutAdminFromFullFriendUserDTOs(result);
        } catch (Exception e) {
            logger.error("Error retrieving full friend users: " + e.getMessage());
            throw e;
        }
    }

    /**
     * Fallback method to get friends from the "Everyone" tag when the optimized query returns no results
     */
    private List<FullFriendUserDTO> getFallbackFriendsList(UUID requestingUserId) {
        List<User> userFriends = getFriendUsersByUserId(requestingUserId);
        List<FullFriendUserDTO> result = new ArrayList<>();
        for (User friend : userFriends) {
            FullFriendUserDTO dto = new FullFriendUserDTO(
                    friend.getId(),
                    friend.getUsername(),
                    friend.getProfilePictureUrlString(),
                    friend.getName(),
                    friend.getBio(),
                    friend.getEmail()
            );
            result.add(dto);
        }
        return filterOutAdminFromFullFriendUserDTOs(result);
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
        try {
            List<UUID> user1Friends = new ArrayList<>(getFriendUserIdsByUserId(userId1));
            List<UUID> user2Friends = getFriendUserIdsByUserId(userId2);

            // Create a mutable copy of user1Friends and retain only elements that are also in user2Friends
            user1Friends.retainAll(user2Friends);
            return user1Friends.size();
        } catch (Exception e) {
            logger.error(e.getMessage());
            throw e;
        }
    }

    @Override
    public BaseUserDTO getBaseUserById(UUID id) {
        try {
            User user = repository.findById(id)
                    .orElseThrow(() -> new BaseNotFoundException(EntityType.User, id));

            // Hide admin user from front-end
            if (isAdminUser(user)) {
                throw new BaseNotFoundException(EntityType.User, id);
            }

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
            // Use UTC for consistent timezone comparison across server and client timezones
            OffsetDateTime now = OffsetDateTime.now(java.time.ZoneOffset.UTC);
            List<UUID> pastActivityIds = activityUserRepository.findPastActivityIdsForUser(requestingUserId, ParticipationStatus.participating, now, Limit.of(activityLimit));
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
            return friendshipRepository.existsBidirectionally(userId, potentialFriendId);
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
            User user = getUserEntityById(userId);
            return new UserProfileInfoDTO(
                    user.getId(),
                    user.getName(),
                    user.getUsername(),
                    user.getBio(),
                    user.getProfilePictureUrlString(),
                    user.getDateCreated()
            );
        } catch (Exception e) {
            logger.error("Error getting user profile info: " + LoggingUtils.formatUserIdInfo(userId) + ": " + e.getMessage());
            throw e;
        }
    }

    @Override
    public BaseUserDTO setOptionalDetails(UUID userId, OptionalDetailsDTO optionalDetailsDTO) {
        try {
            User user = getUserEntityById(userId);

            if (optionalDetailsDTO.getName() != null) {
                user.setName(optionalDetailsDTO.getName());
            }
            user.setProfilePictureUrlString(s3Service.updateProfilePictureWithUserId(optionalDetailsDTO.getProfilePictureData(), user.getId()));
            user.setStatus(UserStatus.NAME_AND_PHOTO);
            user = repository.save(user);
            return UserMapper.toDTO(user);
        } catch (Exception e) {
            logger.error("Error getting user profile info: " + LoggingUtils.formatUserIdInfo(userId) + ": " + e.getMessage());
            throw e;
        }
    }

    @Override
    public List<BaseUserDTO> findUsersByPhoneNumbers(List<String> phoneNumbers, UUID requestingUserId) {
        try {
            logger.info("Finding users by phone numbers for contact cross-reference. Phone numbers count: " + phoneNumbers.size() + ", requesting user: " + LoggingUtils.formatUserIdInfo(requestingUserId));

            // Debug: Log all incoming phone numbers
            logger.info("🔍 INCOMING PHONE NUMBERS:");
            for (int i = 0; i < phoneNumbers.size(); i++) {
                logger.info("  [" + i + "] Original: '" + phoneNumbers.get(i) + "'");
            }

            // Generate all possible search variants for flexible matching
            // This allows us to find matches even when numbers are stored in different formats
            List<String> searchVariants = PhoneNumberMatchingUtil.getSearchVariants(phoneNumbers);

            logger.info("🔄 GENERATED " + searchVariants.size() + " SEARCH VARIANTS:");
            searchVariants.forEach(variant -> logger.info("  - '" + variant + "'"));

            if (searchVariants.isEmpty()) {
                logger.info("❌ No valid phone number variants to search for");
                return Collections.emptyList();
            }

            // Use database query with all variants instead of loading all users into memory
            List<User> matchingUsers = repository.findByPhoneNumberIn(searchVariants);

            logger.info("📞 FOUND " + matchingUsers.size() + " USERS WITH MATCHING PHONE NUMBERS");
            matchingUsers.forEach(user -> {
                String displayPhone = user.getOptionalPhoneNumber().orElse("(no phone)");
                logger.info("  - User: " + user.getDisplayName() + " (" + user.getOptionalUsername().orElse("no username") + ") with phone: " + displayPhone);
            });

            // Filter out the requesting user, admin users, and inactive users
            List<User> filteredUsers = matchingUsers.stream()
                    .distinct()
                    .filter(user -> {
                        boolean keep = !user.getId().equals(requestingUserId) &&
                                !isAdminUser(user) &&
                                user.getStatus() == UserStatus.ACTIVE &&
                                user.getOptionalPhoneNumber().isPresent(); // Ensure phone number is actually present

                        if (!keep) {
                            String reason = user.getId().equals(requestingUserId) ? "requesting user" :
                                    isAdminUser(user) ? "admin user" :
                                            user.getStatus() != UserStatus.ACTIVE ? "inactive status: " + user.getStatus() :
                                                    !user.getOptionalPhoneNumber().isPresent() ? "no valid phone number" : "unknown";
                            logger.info("🚫 FILTERED OUT USER: " + user.getDisplayName() + " (reason: " + reason + ")");
                        }
                        return keep;
                    })
                    .collect(Collectors.toList());

            logger.info("✅ FINAL RESULT: Found " + filteredUsers.size() + " matching users for contact cross-reference");
            filteredUsers.forEach(user -> {
                String displayPhone = user.getOptionalPhoneNumber().orElse("(no phone)");
                logger.info("  - FINAL: " + user.getDisplayName() + " (" + user.getOptionalUsername().orElse("no username") + ") with phone: " + displayPhone);
            });

            // Convert to DTOs
            return UserMapper.toDTOList(filteredUsers);

        } catch (Exception e) {
            logger.error("Error finding users by phone numbers for user: " + LoggingUtils.formatUserIdInfo(requestingUserId) + ": " + e.getMessage());
            throw e;
        }
    }

}
