package com.danielagapov.spawn.Services.User;

import com.danielagapov.spawn.DTOs.*;
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
            return getFullUserByUser(getUserById(id));
        } catch (Exception e) {
            logger.log(e.getMessage());
            throw e;
        }
    }

    @Override
    public List<UUID> getFriendUserIdsByUserId(UUID id) {
        try {
            // Fetch FriendTag entities related to the given user (for example, by userId)
            List<FriendTag> friendTags = friendTagRepository.findByOwnerId(id);

            // Retrieve the user IDs associated with those FriendTags
            return friendTags.stream()
                    .flatMap(friendTag -> uftRepository.findFriendIdsByTagId(friendTag.getId()).stream())
                    .distinct() // Remove duplicates
                    .collect(Collectors.toList());
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
        try {
            List<FriendTag> friendTags = friendTagRepository.findAll();
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
        try {
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
        } catch (Exception e) {
            logger.log(e.getMessage());
            throw e;
        }
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
            if (user.profilePicture() == null) {
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
                    .map(UserDTO::id)
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
        try {
            // Get the FriendTags associated with the user (assuming userId represents the owner of friend tags)
            FriendTag everyoneTag = friendTagRepository.findEveryoneTagByOwnerId(userId);
            if (everyoneTag == null) {
                return List.of(); // empty list of friends
            }

            // Retrieve the friends for each FriendTag and return as a flattened list
            return getFriendsByFriendTagId(everyoneTag.getId());
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
                    .map(FriendRequestDTO::receiverUserId)
                    .collect(Collectors.toList());

            // Fetch users who have sent a friend request to the user (pending requests)
            List<UUID> receivedFriendRequestSenderUserIds = friendRequestService.getIncomingFriendRequestsByUserId(userId)
                    .stream()
                    .map(request -> request.getSenderUser().id())
                    .collect(Collectors.toList());

            // Create a set of the requesting user's friends, users they've sent requests to, users they've received requests from, and self for quick lookup
            Set<UUID> excludedUserIds = new HashSet<>(requestingUserFriendIds);
            excludedUserIds.addAll(sentFriendRequestReceiverUserIds);
            excludedUserIds.addAll(receivedFriendRequestSenderUserIds);
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

            // Fetch only users in the mutualFriendCounts map
            List<RecommendedFriendUserDTO> recommendedFriends = mutualFriendCounts.entrySet().stream()
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
                    .sorted(Comparator.comparingInt(RecommendedFriendUserDTO::mutualFriendCount).reversed())
                    .limit(3)
                    .collect(Collectors.toList());

            if (recommendedFriends.size() >= 3) return recommendedFriends;

            // Otherwise, recommend random users not already friends, not sent/received requests, and not self
            List<UserDTO> allUsers = getAllUsers();

            for (UserDTO potentialFriend : allUsers) {
                if (recommendedFriends.size() >= 3) break;

                boolean isExcluded = excludedUserIds.contains(potentialFriend.id());

                if (!isExcluded) {
                    FullUserDTO fullUserDTO = getFullUserById(potentialFriend.id());

                    recommendedFriends.add(new RecommendedFriendUserDTO(
                            fullUserDTO.id(),
                            fullUserDTO.friends(),
                            fullUserDTO.username(),
                            fullUserDTO.profilePicture(),
                            fullUserDTO.firstName(),
                            fullUserDTO.lastName(),
                            fullUserDTO.bio(),
                            fullUserDTO.friendTags(),
                            fullUserDTO.email(),
                            0 // no mutual friends
                    ));
                }
            }

            return recommendedFriends;
        } catch (Exception e) {
            logger.log(e.getMessage());
            throw e;
        }
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
    public FullUserDTO getFullUserByUser(UserDTO user) {
        try {
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
        } catch (Exception e) {
            logger.log(e.getMessage());
            throw e;
        }
    }

    @Override
    public List<FullUserDTO> convertUsersToFullUsers(List<UserDTO> users) {
        try {
            return users.stream()
                    .map(this::getFullUserByUser)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            logger.log(e.getMessage());
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
            List<UserDTO> userFriends = getFriendsByUserId(requestingUserId);
            List<FullUserDTO> fullUserFriends = convertUsersToFullUsers(userFriends);

            List<FullFriendUserDTO> fullFriendUserDTOList = new ArrayList<FullFriendUserDTO>();
            for (FullUserDTO user : fullUserFriends) {
                FullFriendUserDTO fullFriendUserDTO = new FullFriendUserDTO(
                        user.id(),
                        convertUsersToFullUsers(getFriendsByUserId(user.id())),
                        user.username(),
                        user.profilePicture(),
                        user.firstName(),
                        user.lastName(),
                        user.bio(),
                        friendTagService.convertFriendTagsToFullFriendTags(friendTagService.getFriendTagsByOwnerId(user.id())),
                        user.email(),
                        friendTagService.getPertainingFriendTagsForFriend(requestingUserId, user.id())
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
