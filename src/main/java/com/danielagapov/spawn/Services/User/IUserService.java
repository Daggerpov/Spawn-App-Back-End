package com.danielagapov.spawn.Services.User;

import com.danielagapov.spawn.DTOs.User.BaseUserDTO;
import com.danielagapov.spawn.DTOs.User.FriendUser.FullFriendUserDTO;
import com.danielagapov.spawn.DTOs.User.FriendUser.RecommendedFriendUserDTO;
import com.danielagapov.spawn.DTOs.User.Profile.UserProfileInfoDTO;
import com.danielagapov.spawn.DTOs.User.RecentlySpawnedUserDTO;
import com.danielagapov.spawn.DTOs.User.UserDTO;
import com.danielagapov.spawn.DTOs.User.UserUpdateDTO;
import com.danielagapov.spawn.Models.FriendTag;
import com.danielagapov.spawn.Models.User.User;
import com.danielagapov.spawn.Util.SearchedUserResult;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface IUserService {
    List<UserDTO> getAllUsers();

    // CRUD operations:
    UserDTO getUserById(UUID id);

    User getUserEntityById(UUID id);

    UserDTO saveUser(UserDTO user);

    void deleteUserById(UUID id);

    User saveEntity(User user);

    UserDTO saveUserWithProfilePicture(UserDTO user, byte[] profilePicture);

    UserDTO getUserDTOByEntity(User user);

    User createAndSaveUser(User user);

    UserDTO createAndSaveUserWithProfilePicture(UserDTO user, byte[] profilePicture);


    // For Friends:

    List<UUID> getFriendUserIdsByUserId(UUID id);

    List<FullFriendUserDTO> getFullFriendUsersByUserId(UUID requestingUserId);

    List<User> getFriendUsersByUserId(UUID requestingUserId);

    /**
     * Checks if a user is a friend of another user.
     *
     * @param userId The ID of the user to check
     * @param potentialFriendId The ID of the potential friend
     * @return True if the users are friends, false otherwise
     */
    boolean isUserFriendOfUser(UUID userId, UUID potentialFriendId);

    // For Friend Tags:

    Map<FriendTag, UUID> getOwnerUserIdsMap();

    Map<FriendTag, List<UUID>> getFriendUserIdsMap();

    List<BaseUserDTO> getFriendsByFriendTagId(UUID friendTagId);

    List<UUID> getFriendUserIdsByFriendTagId(UUID friendTagId);

    void saveFriendToUser(UUID userId, UUID friendId);

    List<RecommendedFriendUserDTO> getLimitedRecommendedFriendsForUserId(UUID userId);

    /**
     * Gets the timestamp of the latest profile update from any of the user's friends.
     *
     * @param userId The user ID to get the latest friend profile update for
     * @return The timestamp of the latest friend profile update, or null if none found
     */
    Instant getLatestFriendProfileUpdateTimestamp(UUID userId);

    // For Activities:

    List<BaseUserDTO> getParticipantsByActivityId(UUID ActivityId);

    List<BaseUserDTO> getInvitedByActivityId(UUID ActivityId);

    List<UUID> getParticipantUserIdsByActivityId(UUID ActivityId);

    List<UUID> getInvitedUserIdsByActivityId(UUID ActivityId);

    // Helper
    boolean existsByEmail(String email);

    boolean existsByUsername(String username);

    boolean existsByPhoneNumber(String phoneNumber);

    boolean existsByUserId(UUID userId);

    void verifyUserByUsername(String username);

    int getMutualFriendCount(UUID receiverId, UUID id);

    BaseUserDTO getBaseUserById(UUID id);

    BaseUserDTO updateUser(UUID id, UserUpdateDTO updateUserDTO);

    SearchedUserResult getRecommendedFriendsBySearch(UUID requestingUserId, String searchQuery);

    /**
     * Get the User entity by username
     */
    User getUserEntityByUsername(String username);

    List<BaseUserDTO> searchByQuery(String searchQuery);

    /**
     * Get the User entity by email
     */
    User getUserByEmail(String email);

    List<RecentlySpawnedUserDTO> getRecentlySpawnedWithUsers(UUID requestingUserId);

    BaseUserDTO getBaseUserByUsername(String username);

    /**
     * Get user profile information
     */
    UserProfileInfoDTO getUserProfileInfo(UUID userId);
}
