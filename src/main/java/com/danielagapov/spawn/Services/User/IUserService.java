package com.danielagapov.spawn.Services.User;

import com.danielagapov.spawn.DTOs.FullUserDTO;
import com.danielagapov.spawn.DTOs.RecommendedFriendUserDTO;
import com.danielagapov.spawn.DTOs.UserDTO;
import com.danielagapov.spawn.Models.FriendTag;
import com.danielagapov.spawn.Models.User;

import java.util.List;
import java.util.Map;
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

    // For Friends:

    List<UUID> getFriendUserIdsByUserId(UUID id);
    List<UserDTO> getFriendUsersByUserId(UUID id);

    // For Friend Tags:

    List<UserDTO> getUsersByTagId(UUID tagId);
    Map<FriendTag, UUID> getOwnerUserIdsMap();
    Map<FriendTag, List<UUID>> getFriendUserIdsMap();
    List<UserDTO> getFriendsByFriendTagId(UUID friendTagId);
    List<UUID> getFriendUserIdsByFriendTagId(UUID friendTagId);
    List<UserDTO> getFriendsByUserId(UUID userId);
    void removeFriend(UUID userId, UUID friendId);
    void saveFriendToUser(UUID userId, UUID friendId);
    List<RecommendedFriendUserDTO> getRecommendedFriendsForUserId(UUID userIdd);

    // For Events:

    List<UserDTO> getParticipantsByEventId(UUID eventId);
    List<UserDTO> getInvitedByEventId(UUID eventId) ;
    List<UUID> getParticipantUserIdsByEventId(UUID eventId);
    List<UUID> getInvitedUserIdsByEventId(UUID eventId);

    // Helper
    FullUserDTO getFullUserByUser(UserDTO user);
}
