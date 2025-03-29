package com.danielagapov.spawn.Services.User;

import com.danielagapov.spawn.DTOs.User.BaseUserDTO;
import com.danielagapov.spawn.DTOs.User.FriendUser.FullFriendUserDTO;
import com.danielagapov.spawn.DTOs.User.FriendUser.RecommendedFriendUserDTO;
import com.danielagapov.spawn.DTOs.User.FullUserDTO;
import com.danielagapov.spawn.DTOs.User.UserDTO;
import com.danielagapov.spawn.Models.FriendTag;
import com.danielagapov.spawn.Models.User;
import com.danielagapov.spawn.Util.SearchedUserResult;

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

    boolean deleteUserById(UUID id);

    User saveEntity(User user);

    UserDTO saveUserWithProfilePicture(UserDTO user, byte[] profilePicture);
    
    UserDTO getUserDTOByEntity(User user);

    // For Friends:

    List<UUID> getFriendUserIdsByUserId(UUID id);

    List<FullFriendUserDTO> getFullFriendUsersByUserId(UUID requestingUserId);

    List<User> getFriendUsersByUserId(UUID requestingUserId);

    // For Friend Tags:

    List<UserDTO> getUsersByTagId(UUID tagId);

    Map<FriendTag, UUID> getOwnerUserIdsMap();

    Map<FriendTag, List<UUID>> getFriendUserIdsMap();

    List<UserDTO> getFriendsByFriendTagId(UUID friendTagId);

    List<UUID> getFriendUserIdsByFriendTagId(UUID friendTagId);

    List<UserDTO> getFriendsByUserId(UUID userId);

    void saveFriendToUser(UUID userId, UUID friendId);

    List<RecommendedFriendUserDTO> getLimitedRecommendedFriendsForUserId(UUID userId);

    // Helper
    FullUserDTO getFullUserByUser(UserDTO user, Set<UUID> visitedUsers);

    List<FullUserDTO> convertUsersToFullUsers(List<UserDTO> users, Set<UUID> visitedUsers);

    boolean existsByEmail(String email);

    FullUserDTO getFullUserByUsername(String username);

    boolean existsByUsername(String username);

    void verifyUserByUsername(String username);

    int getMutualFriendCount(UUID receiverId, UUID id);

    BaseUserDTO getBaseUserById(UUID id);

    BaseUserDTO updateUser(UUID id, String bio, String username, String firstName, String lastName);

    SearchedUserResult getRecommendedFriendsBySearch(UUID requestingUserId, String searchQuery);
}
