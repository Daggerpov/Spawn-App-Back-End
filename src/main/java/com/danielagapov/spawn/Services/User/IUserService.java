package com.danielagapov.spawn.Services.User;

import org.apache.commons.lang3.tuple.Triple;
import com.danielagapov.spawn.DTOs.FriendRequest.FullFriendRequestDTO;
import com.danielagapov.spawn.DTOs.User.FullFriendUserDTO;
import com.danielagapov.spawn.DTOs.User.FullUserDTO;
import com.danielagapov.spawn.DTOs.User.RecommendedFriendUserDTO;
import com.danielagapov.spawn.DTOs.User.UserDTO;
import com.danielagapov.spawn.Models.FriendTag;
import com.danielagapov.spawn.Models.User;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public interface IUserService {
    List<UserDTO> getAllUsers();

    // CRUD operations:
    UserDTO getUserById(UUID id);
    FullUserDTO getFullUserById(UUID id);
    User getUserEntityById(UUID id);
    UserDTO saveUser(UserDTO user);
    UserDTO replaceUser(UserDTO user, UUID id);
    boolean deleteUserById(UUID id);
    User saveEntity(User user);
    UserDTO saveUserWithProfilePicture(UserDTO user, byte[] profilePicture);
    FullUserDTO getFullUserByEmail(String email);

    // For Friends:

    List<UUID> getFriendUserIdsByUserId(UUID id);
    List<FullFriendUserDTO> getFullFriendUsersByUserId(UUID requestingUserId);

    // For Friend Tags:

    List<UserDTO> getUsersByTagId(UUID tagId);
    Map<FriendTag, UUID> getOwnerUserIdsMap();
    Map<FriendTag, List<UUID>> getFriendUserIdsMap();
    List<UserDTO> getFriendsByFriendTagId(UUID friendTagId);
    List<UUID> getFriendUserIdsByFriendTagId(UUID friendTagId);
    List<UserDTO> getFriendsByUserId(UUID userId);
    void removeFriend(UUID userId, UUID friendId);
    void saveFriendToUser(UUID userId, UUID friendId);
    List<RecommendedFriendUserDTO> getRecommendedFriendsForUserId(UUID userId);

    // For Events:

    List<UserDTO> getParticipantsByEventId(UUID eventId);
    List<UserDTO> getInvitedByEventId(UUID eventId) ;
    List<UUID> getParticipantUserIdsByEventId(UUID eventId);
    List<UUID> getInvitedUserIdsByEventId(UUID eventId);

    // Helper
    FullUserDTO getFullUserByUser(UserDTO user, Set<UUID> visitedUsers);

    List<FullUserDTO> convertUsersToFullUsers(List<UserDTO> users, Set<UUID> visitedUsers);
    boolean existsByEmail(String email);
    FullUserDTO getFullUserByUsername(String username);
    boolean existsByUsername(String username);

    // TODO figure out which section this goes in
    Triple<List<FullFriendRequestDTO>, List<RecommendedFriendUserDTO>, List<FullFriendUserDTO>> getRecommendedFriendsBySearch(UUID requestingUserId, String searchQuery);
}
