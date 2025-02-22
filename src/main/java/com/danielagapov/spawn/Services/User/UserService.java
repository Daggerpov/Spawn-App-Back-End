package com.danielagapov.spawn.Services.User;

import com.danielagapov.spawn.DTOs.FriendRequest.FriendRequestDTO;
import com.danielagapov.spawn.DTOs.FriendRequest.FullFriendRequestDTO;
import com.danielagapov.spawn.DTOs.FriendTag.FriendTagDTO;
import com.danielagapov.spawn.DTOs.User.FullFriendUserDTO;
import com.danielagapov.spawn.DTOs.User.FullUserDTO;
import com.danielagapov.spawn.DTOs.User.RecommendedFriendUserDTO;
import com.danielagapov.spawn.DTOs.User.UserDTO;
import com.danielagapov.spawn.Enums.EntityType;
import com.danielagapov.spawn.Enums.ParticipationStatus;
import com.danielagapov.spawn.Exceptions.ApplicationException;
import com.danielagapov.spawn.Exceptions.Base.BaseNotFoundException;
import com.danielagapov.spawn.Exceptions.Base.BaseSaveException;
import com.danielagapov.spawn.Exceptions.Base.BasesNotFoundException;
import com.danielagapov.spawn.Exceptions.DatabaseException;
import com.danielagapov.spawn.Exceptions.Logger.ILogger;
import com.danielagapov.spawn.Mappers.UserMapper;
import com.danielagapov.spawn.Models.EventUser;
import com.danielagapov.spawn.Models.FriendTag;
import com.danielagapov.spawn.Models.User;
import com.danielagapov.spawn.Repositories.IEventUserRepository;
import com.danielagapov.spawn.Repositories.IFriendTagRepository;
import com.danielagapov.spawn.Repositories.IUserFriendTagRepository;
import com.danielagapov.spawn.Repositories.IUserRepository;
import com.danielagapov.spawn.Services.FriendRequestService.IFriendRequestService;
import com.danielagapov.spawn.Services.FriendTag.IFriendTagService;
import com.danielagapov.spawn.Services.S3.IS3Service;
import com.danielagapov.spawn.Utils.SearchedUserResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

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
    private final IFriendRequestService friendRequestService;
    private final ILogger logger;

    @Autowired
    @Lazy // Avoid circular dependency issues with ftService
    public UserService(IUserRepository repository,
                       IEventUserRepository eventUserRepository, IUserFriendTagRepository uftRepository, IFriendTagService friendTagService, IFriendTagRepository friendTagRepository, IS3Service s3Service, IFriendRequestService friendRequestService, ILogger logger) {
        this.repository = repository;
        this.eventUserRepository = eventUserRepository;
        this.uftRepository = uftRepository;
        this.friendTagService = friendTagService;
        this.friendTagRepository = friendTagRepository;
        this.s3Service = s3Service;
        this.friendRequestService = friendRequestService;
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
        logger.log("Getting user by id with id " + id);
        try {
            User user = repository.findById(id)
                    .orElseThrow(() -> new BaseNotFoundException(EntityType.User, id));

            List<UUID> friendUserIds = getFriendUserIdsByUserId(id);

            // Fetch FriendTag IDs based on the user ID
            List<UUID> friendTagIds = friendTagService.getFriendTagIdsByOwnerUserId(user.getId());

            // Pass in the friendTagIds and friendTags as needed
            return UserMapper.toDTO(user, friendUserIds, friendTagIds);
        } catch (Exception e) {
            logger.log(e.getMessage());
            throw e;
        }
    }

    @Override
    public FullUserDTO getFullUserById(UUID id) {
        try {
            logger.log("Getting full user by id for user with id " + id);
            return getFullUserByUser(getUserById(id), new HashSet<>());
        } catch (Exception e) {
            logger.log(e.getMessage());
            throw e;
        }
    }

    @Override
    public List<UUID> getFriendUserIdsByUserId(UUID id) {
        logger.log("Getting friend user ids for user with id " + id);
        try {
            // Fetch FriendTag entities related to the given user (for example, by userId)
            List<FriendTag> friendTags = friendTagRepository.findByOwnerId(id);

            // Retrieve the user IDs associated with those FriendTags
            List<UUID> friendIds = friendTags.stream()
                    .flatMap(friendTag -> uftRepository.findFriendIdsByTagId(friendTag.getId()).stream())
                    .distinct() // Remove duplicates
                    .toList();
            logger.log("Found friend ids " + friendIds);
            return friendIds;
        } catch (Exception e) {
            logger.log(e.getMessage());
            throw e;
        }
    }

    @Override
    public User getUserEntityById(UUID id) {
        try {
            return repository.findById(id)
                    .orElseThrow(() -> new BaseNotFoundException(EntityType.User, id));
        } catch (Exception e) {
            logger.log(e.getMessage());
            throw e;
        }
    }

    // For Friend Tags:

    @Override
    public Map<FriendTag, UUID> getOwnerUserIdsMap() {
        logger.log("Getting owner user ids map");
        try {
            List<FriendTag> friendTags = friendTagRepository.findAll(); // TODO: don't find by all
            logger.log("Friend tags found: " + friendTags.size());
            return friendTags.stream()
                    .collect(Collectors.toMap(
                            friendTag -> friendTag,
                            FriendTag::getOwnerId
                    ));
        } catch (Exception e) {
            logger.log(e.getMessage());
            throw e;
        }
    }

    @Override
    public Map<FriendTag, List<UUID>> getFriendUserIdsMap() {
        logger.log("Getting friend user ids map");
        try {
            // Fetch all FriendTags
            List<FriendTag> friendTags = friendTagRepository.findAll();

            // Create a map of FriendTag to a list of associated user IDs
            return friendTags.stream()
                    .collect(Collectors.toMap(
                            friendTag -> friendTag, // Use FriendTag as the key
                            friendTag -> uftRepository.findFriendIdsByTagId(friendTag.getId()) // List of user IDs for each FriendTag
                    ));
        } catch (Exception e) {
            logger.log(e.getMessage());
            throw e;
        }
    }

    @Override
    public List<UserDTO> getUsersByTagId(UUID tagId) {
        try {
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
                    "#1D3D3D", userEntity.getId(), List.of(), true);
            FriendTagDTO everyoneTagDTOAfterPersisting = friendTagService.saveFriendTag(everyoneTagDTO);
            // id is generated when saving
            return UserMapper.toDTO(userEntity, List.of(), List.of(everyoneTagDTOAfterPersisting.getId()));
        } catch (DataAccessException e) {
            logger.log(e.getMessage());
            throw new BaseSaveException("Failed to save user: " + e.getMessage()); // TODO: fix throwing
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
        try {
            return repository.findById(id).map(user -> {
                user.setBio(newUser.getBio());
                user.setFirstName(newUser.getFirstName());
                user.setLastName(newUser.getLastName());
                user.setUsername(newUser.getUsername());
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
        } catch (Exception e) {
            logger.log(e.getMessage());
            throw e;
        }
    }

    @Override
    public boolean deleteUserById(UUID id) {
        User user = repository.findById(id).orElseThrow(() -> new BaseNotFoundException(EntityType.User, id));
        try {
            repository.deleteById(id);
            s3Service.deleteObjectByURL(user.getProfilePictureUrlString());
            return true;
        } catch (Exception e) {
            logger.log(e.getMessage());
            return false;
        }
    }

    @Override
    public User saveEntity(User user) {
        try {
            return repository.save(user);
        } catch (Exception e) {
            logger.log(e.getMessage());
            throw e;
        }
    }

    @Override
    public UserDTO saveUserWithProfilePicture(UserDTO user, byte[] profilePicture) {
        try {
            logger.log(String.format("Entering saveUserWithProfilePicture: {user: %s}", user));
            if (user.getProfilePicture() == null) {
                logger.log("Profile picture is null, user either chose their profile picture or has default");
                user = s3Service.putProfilePictureWithUser(profilePicture, user);
            }
            user = saveUser(user);
            logger.log(String.format("Exiting saveUserWithProfilePicture: {user: %s}", user));
            return user;
        } catch (Exception e) {
            logger.log(e.getMessage());
            throw e;
        }
    }

    @Override
    public FullUserDTO getFullUserByEmail(String email) {
        try {
            User user = repository.findByEmail(email);
            return user == null ? null : getFullUserById(user.getId());
        } catch (Exception e) {
            logger.log(e.getMessage());
            throw e;
        }
    }

    public List<UserDTO> getFriendsByFriendTagId(UUID friendTagId) {
        try {
            return uftRepository.findFriendIdsByTagId(friendTagId)
                    .stream()
                    .map(this::getUserById)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            logger.log(e.getMessage());
            throw e;
        }
    }

    @Override
    public List<UUID> getFriendUserIdsByFriendTagId(UUID friendTagId) {
        try {
            // Call the method to get the list of UserDTOs
            List<UserDTO> friends = getFriendsByFriendTagId(friendTagId);

            // Extract the user IDs from the UserDTO list
            return friends.stream()
                    .map(UserDTO::getId)
                    .distinct()
                    .collect(Collectors.toList());
        } catch (Exception e) {
            logger.log(e.getMessage());
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
            logger.log(e.getMessage());
            throw e;
        }
    }

    @Override
    public List<UserDTO> getFriendsByUserId(UUID userId) {
        logger.log("Getting friends (user entities) by user id " + userId);
        try {
            // Get the FriendTags associated with the user (assuming userId represents the owner of friend tags)
            FriendTag everyoneTag = friendTagRepository.findEveryoneTagByOwnerId(userId);
            if (everyoneTag == null) {
                return List.of(); // empty list of friends
            }

            // Retrieve the friends for each FriendTag and return as a flattened list
            List<UserDTO> friends = getFriendsByFriendTagId(everyoneTag.getId());

            // Filter out the friend whose ID matches the userId
            List<UserDTO> filteredFriends = friends.stream()
                    .filter(friend -> !friend.getId().equals(userId)) // Exclude the user themselves
                    .collect(Collectors.toList());

            return filteredFriends;
        } catch (Exception e) {
            logger.log(e.getMessage());
            throw e;
        }
    }

    // Adds friend bidirectionally
    @Override
    public void saveFriendToUser(UUID userId, UUID friendId) {
        try {
            UUID userEveryoneTagId = friendTagRepository.findEveryoneTagByOwnerId(userId).getId();
            friendTagService.saveUserToFriendTag(userEveryoneTagId, friendId);
            UUID friendEveryoneTagId = friendTagRepository.findEveryoneTagByOwnerId(userId).getId();
            friendTagService.saveUserToFriendTag(friendEveryoneTagId, userId);
        } catch (Exception e) {
            logger.log(e.getMessage());
            throw e;
        }
    }

    // Removes friend bidirectionally
    @Override
    public void removeFriend(UUID userId, UUID friendId) {
        try {
            // Deletes all entries in UserFriendTag
            friendTagRepository.findByOwnerId(userId).forEach((friendTag) -> {
                uftRepository.deleteByFriendTagIdAndUserId(friendTag.getId(), friendId);
            });
            friendTagRepository.findByOwnerId(friendId).forEach((friendTag) -> {
                uftRepository.deleteByFriendTagIdAndUserId(friendTag.getId(), userId);
            });
        } catch (Exception e) {
            logger.log(e.getMessage());
            throw e;
        }
    }

    // returns top 3 friends with most mutuals with user (with `userId`) as
    // `RecommendedFriendUserDTO`s, to include the `mutualFriendCount`
    @Override
    public List<RecommendedFriendUserDTO> getRecommendedFriendsForUserId(UUID userId) {
        try {
            // Fetch the requesting user's friends
            List<UUID> requestingUserFriendIds = getFriendUserIdsByUserId(userId);

            // Fetch users who have already received a friend request from the user
            List<UUID> sentFriendRequestReceiverUserIds = friendRequestService.getSentFriendRequestsByUserId(userId)
                    .stream()
                    .map(FriendRequestDTO::getReceiverUserId)
                    .toList();

            // Map mutual friends to RecommendedFriendUserDTO
            List<UUID> receivedFriendRequestSenderUserIds = friendRequestService.getIncomingFriendRequestsByUserId(userId)
                    .stream()
                    .map(request -> request.getSenderUser().getId())
                    .toList();

            List<UUID> existingFriendUserIds = getFriendUserIdsByUserId(userId);

            // Create a set of the requesting user's friends, users they've sent requests to, users they've received requests from, and self for quick lookup
            Set<UUID> excludedUserIds = new HashSet<>(requestingUserFriendIds);
            excludedUserIds.addAll(sentFriendRequestReceiverUserIds);
            excludedUserIds.addAll(receivedFriendRequestSenderUserIds);
            excludedUserIds.addAll(existingFriendUserIds);
            excludedUserIds.add(userId); // Exclude self

            // Collect friends of friends (excluding already existing friends, sent/received requests, and self)
            Map<UUID, Integer> mutualFriendCounts = new HashMap<>();
            for (UUID friendId : requestingUserFriendIds) {
                List<UUID> friendOfFriendIds = getFriendUserIdsByUserId(friendId);

                for (UUID friendOfFriendId : friendOfFriendIds) {
                    if (!excludedUserIds.contains(friendOfFriendId)) {
                        mutualFriendCounts.merge(friendOfFriendId, 1, Integer::sum);
                    }
                }
            }

            // Map mutual friends to RecommendedFriendUserDTO
            List<RecommendedFriendUserDTO> recommendedFriends = mutualFriendCounts.entrySet().stream()
                    .map(entry -> {
                        UUID mutualFriendId = entry.getKey();
                        int mutualFriendCount = entry.getValue();
                        FullUserDTO fullUser = getFullUserById(mutualFriendId);

                        return new RecommendedFriendUserDTO(
                                fullUser.getId(),
                                fullUser.getFriends(),
                                fullUser.getUsername(),
                                fullUser.getProfilePicture(),
                                fullUser.getFirstName(),
                                fullUser.getLastName(),
                                fullUser.getBio(),
                                fullUser.getFriendTags(),
                                fullUser.getEmail(),
                                mutualFriendCount
                        );
                    })
                    .sorted(Comparator.comparingInt(RecommendedFriendUserDTO::getMutualFriendCount).reversed())
                    .limit(3)
                    .collect(Collectors.toList());

            if (recommendedFriends.size() >= 3) {
                return recommendedFriends;
            }

            // Otherwise, recommend random users not already friends, not sent/received requests, and not self
            List<UserDTO> allUsers = getAllUsers();

            for (UserDTO potentialFriend : allUsers) {
                if (recommendedFriends.size() >= 3) break;

                UUID potentialFriendId = potentialFriend.getId();
                boolean isExcluded = excludedUserIds.contains(potentialFriendId);

                if (!isExcluded) {
                    boolean hasAlreadySentFriendRequest = false;

                    try {
                        List<FullFriendRequestDTO> potentialFriendIncomingFriendRequests = friendRequestService.getIncomingFriendRequestsByUserId(potentialFriendId);

                        for (FullFriendRequestDTO friendRequestDTO : potentialFriendIncomingFriendRequests) {
                            if ((friendRequestDTO.getSenderUser().getId().equals(userId) && friendRequestDTO.getReceiverUser().getId().equals(potentialFriendId)) ||
                                    (friendRequestDTO.getSenderUser().getId().equals(potentialFriendId) && friendRequestDTO.getReceiverUser().getId().equals(userId))) {
                                hasAlreadySentFriendRequest = true;
                                break;
                            }
                        }
                    } catch (BaseNotFoundException e) {
                        // No incoming friend requests, safe to ignore
                    } catch (Exception e) {
                        logger.log(e.getMessage());
                        throw e;
                    }

                    if (!hasAlreadySentFriendRequest) {
                        FullUserDTO fullUserDTO = getFullUserById(potentialFriendId);

                        recommendedFriends.add(new RecommendedFriendUserDTO(
                                fullUserDTO.getId(),
                                fullUserDTO.getFriends(),
                                fullUserDTO.getUsername(),
                                fullUserDTO.getProfilePicture(),
                                fullUserDTO.getFirstName(),
                                fullUserDTO.getLastName(),
                                fullUserDTO.getBio(),
                                fullUserDTO.getFriendTags(),
                                fullUserDTO.getEmail(),
                                0 // No mutual friends
                        ));

                        // Add to excluded list to prevent duplicates
                        excludedUserIds.add(potentialFriendId);
                    }
                }
            }

            return recommendedFriends;
        } catch (Exception e) {
            logger.log(e.getMessage());
            throw e;
        }
    }

    @Override
    public SearchedUserResult getRecommendedFriendsBySearch(UUID requestingUserId, String searchQuery) {

        // Step 1. Find all incoming friend Requests
        List<FullFriendRequestDTO> incomingFriendRequests = friendRequestService.getIncomingFriendRequestsByUserId(requestingUserId);
        List<RecommendedFriendUserDTO> recommendedFriends;
        List<FullFriendUserDTO> friends;

        // If searchQuery is empty:
        if (searchQuery.isEmpty()) {
            // Step 2. Get recommended friends
            recommendedFriends = getRecommendedFriendsForUserId(requestingUserId);
            // Step 3. Get all friends
            friends = getFullFriendUsersByUserId(requestingUserId);
        } else { // If searchQuery is not empty:
            // Step 2. List all recommended friends who match based on searchQuery
            // TODO need to refactor getRecommendedFriendsForUserId() to decouple what its doing so we can filter not just by top 3
            recommendedFriends = getRecommendedFriendsForUserId(requestingUserId);
            // Step 3. List all friends who match based on searchQuery
            friends = getFullFriendUsersByUserId(requestingUserId).stream().filter(user -> Objects.equals(user.getUsername(), searchQuery) || Objects.equals(user.getFirstName(), searchQuery) || Objects.equals(user.getLastName(), searchQuery)).collect(Collectors.toList());
        }

        return new SearchedUserResult(incomingFriendRequests, recommendedFriends, friends);
    }

    @Override
    public List<UserDTO> getParticipantsByEventId(UUID eventId) {
        try {
            List<EventUser> eventUsers = eventUserRepository.findByEvent_Id(eventId);

            List<UserDTO> participants = eventUsers.stream()
                    .filter(eventUser -> eventUser.getStatus() == ParticipationStatus.participating)
                    .map(eventUser -> getUserById(eventUser.getUser().getId()))
                    .collect(Collectors.toList());

            return participants;
        } catch (Exception e) {
            logger.log("Error retrieving participants for eventId " + eventId + ": " + e.getMessage());
            throw new ApplicationException("Error retrieving participants for eventId " + eventId, e);
        }
    }

    @Override
    public List<UserDTO> getInvitedByEventId(UUID eventId) {
        try {
            List<EventUser> eventUsers = eventUserRepository.findByEvent_Id(eventId);

            List<UserDTO> invitedUsers = eventUsers.stream()
                    .filter(eventUser -> eventUser.getStatus() == ParticipationStatus.invited)
                    .map(eventUser -> getUserById(eventUser.getUser().getId()))
                    .collect(Collectors.toList());

            return invitedUsers;
        } catch (Exception e) {
            logger.log("Error retrieving invited users for eventId " + eventId + ": " + e.getMessage());
            throw new ApplicationException("Error retrieving invited users for eventId " + eventId, e);
        }
    }


    @Override
    public List<UUID> getParticipantUserIdsByEventId(UUID eventId) {
        try {
            List<EventUser> eventUsers = eventUserRepository.findByEvent_Id(eventId);

            List<UUID> participantUserIds = eventUsers.stream()
                    .filter(eventUser -> eventUser.getStatus() == ParticipationStatus.participating)
                    .map(eventUser -> eventUser.getUser().getId())
                    .collect(Collectors.toList());

            return participantUserIds;
        } catch (Exception e) {
            logger.log("Error retrieving participant user IDs for eventId " + eventId + ": " + e.getMessage());
            throw new ApplicationException("Error retrieving participant user IDs for eventId " + eventId, e);
        }
    }


    @Override
    public List<UUID> getInvitedUserIdsByEventId(UUID eventId) {
        try {
            List<EventUser> eventUsers = eventUserRepository.findByEvent_Id(eventId);

            List<UUID> invitedUserIds = eventUsers.stream()
                    .filter(eventUser -> eventUser.getStatus() == ParticipationStatus.invited)
                    .map(eventUser -> eventUser.getUser().getId())
                    .collect(Collectors.toList());

            return invitedUserIds;
        } catch (Exception e) {
            logger.log("Error retrieving invited user IDs for eventId " + eventId + ": " + e.getMessage());
            throw new ApplicationException("Error retrieving invited user IDs for eventId " + eventId, e);
        }
    }


    @Override
    public FullUserDTO getFullUserByUser(UserDTO user, Set<UUID> visitedUsers) {
        logger.log("Getting full user by user: " + user.toString());
        try {
            if (visitedUsers.contains(user.getId())) {
                return null; // Skip already visited users
            }
            visitedUsers.add(user.getId());

            return new FullUserDTO(
                    user.getId(),
                    convertUsersToFullUsers(getFriendsByUserId(user.getId()), visitedUsers),
                    user.getUsername(),
                    user.getProfilePicture(),
                    user.getFirstName(),
                    user.getLastName(),
                    user.getBio(),
                    friendTagService.convertFriendTagsToFullFriendTags(friendTagService.getFriendTagsByOwnerId(user.getId())),
                    user.getEmail()
            );
        } catch (Exception e) {
            logger.log(e.getMessage());
            throw e;
        }
    }

    @Override
    public List<FullUserDTO> convertUsersToFullUsers(List<UserDTO> users, Set<UUID> visitedUsers) {
        logger.log("Converting users to full users: " + users.toString());
        try {
            return users.stream()
                    .map(user -> getFullUserByUser(user, visitedUsers))
                    .filter(Objects::nonNull) // Filter out null values (already visited users)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            logger.log(e.getMessage());
            throw e;
        }
    }

    @Override
    public FullUserDTO getFullUserByUsername(String username) {
        try {
            logger.log("Getting full user for " + username);
            User user = repository.findByUsername(username);
            if (user == null) {
                logger.log("Could not find user " + username);
                throw new BaseNotFoundException(EntityType.User, username);
            } else {
                logger.log("Found user " + username);
            }
            return getFullUserById(user.getId());
        } catch (Exception e) {
            logger.log(e.getMessage());
            throw e;
        }
    }

    @Override
    public boolean existsByUsername(String username) {
        return repository.existsByUsername(username);
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
            List<UserDTO> userFriends = getFriendsByUserId(requestingUserId);
            List<FullUserDTO> fullUserFriends = convertUsersToFullUsers(userFriends, new HashSet<>());

            List<FullFriendUserDTO> fullFriendUserDTOList = new ArrayList<>();
            for (FullUserDTO user : fullUserFriends) {
                FullFriendUserDTO fullFriendUserDTO = new FullFriendUserDTO(
                        user.getId(),
                        user.getFriends(),
                        user.getUsername(),
                        user.getProfilePicture(),
                        user.getFirstName(),
                        user.getLastName(),
                        user.getBio(),
                        user.getFriendTags(),
                        user.getEmail(),
                        // only added property from `FullUserDTO`:
                        friendTagService.getPertainingFriendTagsForFriend(requestingUserId, user.getId())
                );
                fullFriendUserDTOList.add(fullFriendUserDTO);
            }

            return fullFriendUserDTOList;
        } catch (Exception e) {
            logger.log(e.getMessage());
            throw e;
        }
    }
}
