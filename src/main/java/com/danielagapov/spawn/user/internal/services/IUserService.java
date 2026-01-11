package com.danielagapov.spawn.user.internal.services;

import com.danielagapov.spawn.user.api.dto.*;
import com.danielagapov.spawn.user.api.dto.FullFriendUserDTO;
import com.danielagapov.spawn.user.api.dto.RecommendedFriendUserDTO;
import com.danielagapov.spawn.user.api.dto.UserProfileInfoDTO;
import com.danielagapov.spawn.shared.util.UserStatus;
import com.danielagapov.spawn.user.internal.domain.User;
import com.danielagapov.spawn.shared.util.SearchedUserResult;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Service interface for managing users and their related operations.
 * Provides CRUD operations, friend management, profile handling, and search functionality.
 */
public interface IUserService {

    /**
     * Retrieves all users from the database.
     *
     * @return List of UserDTO objects representing all users
     * @throws com.danielagapov.spawn.Exceptions.Base.BasesNotFoundException if database access fails
     */
    List<UserDTO> getAllUsers();

    /**
     * Retrieves a specific user by their unique identifier.
     *
     * @param id the unique identifier of the user
     * @return UserDTO object representing the requested user
     * @throws com.danielagapov.spawn.Exceptions.Base.BaseNotFoundException if user with given ID is not found
     */
    UserDTO getUserById(UUID id);

    /**
     * Retrieves a user entity by their unique identifier.
     *
     * @param id the unique identifier of the user
     * @return User entity object
     * @throws com.danielagapov.spawn.Exceptions.Base.BaseNotFoundException if user with given ID is not found
     */
    User getUserEntityById(UUID id);

    /**
     * Saves a user to the database.
     *
     * @param user the UserDTO to save
     * @return the saved UserDTO
     * @throws com.danielagapov.spawn.Exceptions.Base.BaseSaveException if saving fails
     */
    UserDTO saveUser(UserDTO user);

    /**
     * Deletes a user by their unique identifier.
     *
     * @param id the unique identifier of the user to delete
     * @throws com.danielagapov.spawn.Exceptions.Base.BaseNotFoundException if user with given ID is not found
     */
    void deleteUserById(UUID id);

    /**
     * Saves a user entity to the database.
     *
     * @param user the User entity to save
     * @return the saved User entity
     * @throws com.danielagapov.spawn.Exceptions.Base.BaseSaveException if saving fails
     */
    User saveEntity(User user);

    /**
     * Saves a user with a profile picture to the database and S3.
     *
     * @param user the UserDTO to save
     * @param profilePicture byte array representation of the profile picture
     * @return the saved UserDTO with profile picture URL
     * @throws com.danielagapov.spawn.Exceptions.Base.BaseSaveException if saving fails
     */
    UserDTO saveUserWithProfilePicture(UserDTO user, byte[] profilePicture);

    /**
     * Converts a User entity to a UserDTO with populated friend and tag information.
     *
     * @param user the User entity to convert
     * @return UserDTO with populated friend and tag information
     */
    UserDTO getUserDTOByEntity(User user);

    /**
     * Creates and saves a new user entity to the database.
     *
     * @param user the User entity to create and save
     * @return the created User entity
     * @throws com.danielagapov.spawn.Exceptions.Base.BaseSaveException if saving fails
     */
    User createAndSaveUser(User user);

    /**
     * Creates and saves a new user with a profile picture.
     *
     * @param user the UserDTO to create and save
     * @param profilePicture byte array representation of the profile picture
     * @return the created UserDTO with profile picture URL
     * @throws com.danielagapov.spawn.Exceptions.Base.BaseSaveException if saving fails
     */
    UserDTO createAndSaveUserWithProfilePicture(UserDTO user, byte[] profilePicture);

    /**
     * Retrieves the user IDs of all friends of a specific user.
     *
     * @param id the unique identifier of the user
     * @return List of UUID objects representing friend user IDs
     * @throws com.danielagapov.spawn.Exceptions.Base.BaseNotFoundException if user doesn't exist
     */
    List<UUID> getFriendUserIdsByUserId(UUID id);

    /**
     * Retrieves all friends of a user as FullFriendUserDTO objects with complete information.
     *
     * @param requestingUserId the unique identifier of the user requesting their friends
     * @return List of FullFriendUserDTO objects representing the user's friends
     * @throws com.danielagapov.spawn.Exceptions.Base.BaseNotFoundException if user doesn't exist
     */
    List<FullFriendUserDTO> getFullFriendUsersByUserId(UUID requestingUserId);

    /**
     * Retrieves all friends of a user as User entities.
     *
     * @param requestingUserId the unique identifier of the user requesting their friends
     * @return List of User entities representing the user's friends
     * @throws com.danielagapov.spawn.Exceptions.Base.BaseNotFoundException if user doesn't exist
     */
    List<User> getFriendUsersByUserId(UUID requestingUserId);

    /**
     * Checks if a user is a friend of another user.
     *
     * @param userId The ID of the user to check
     * @param potentialFriendId The ID of the potential friend
     * @return True if the users are friends, false otherwise
     */
    boolean isUserFriendOfUser(UUID userId, UUID potentialFriendId);

    // Friend-tag APIs removed; friendships now model relationships directly

    /**
     * Saves a bidirectional friendship between two users.
     *
     * @param userId the unique identifier of the first user
     * @param friendId the unique identifier of the second user
     * @throws com.danielagapov.spawn.Exceptions.Base.BaseNotFoundException if either user doesn't exist
     * @throws com.danielagapov.spawn.Exceptions.Base.BaseSaveException if saving the friendship fails
     */
    void saveFriendToUser(UUID userId, UUID friendId);

    /**
     * Retrieves a limited number of recommended friends for a user.
     *
     * @param userId the unique identifier of the user requesting recommendations
     * @return List of RecommendedFriendUserDTO objects representing recommended friends
     * @throws com.danielagapov.spawn.Exceptions.Base.BaseNotFoundException if user doesn't exist
     */
    List<RecommendedFriendUserDTO> getLimitedRecommendedFriendsForUserId(UUID userId);

    /**
     * Gets the timestamp of the latest profile update from any of the user's friends.
     *
     * @param userId The user ID to get the latest friend profile update for
     * @return The timestamp of the latest friend profile update, or null if none found
     */
    Instant getLatestFriendProfileUpdateTimestamp(UUID userId);

    /**
     * Checks if a user exists with the given email address.
     *
     * @param email the email address to check
     * @return true if a user exists with the given email, false otherwise
     */
    boolean existsByEmail(String email);

    /**
     * Checks if a user exists with the given email address and status.
     *
     * @param email the email address to check
     * @return true if a user exists with the given email, false otherwise
     */
    boolean existsByEmailAndStatus(String email, UserStatus status);

    /**
     * Checks if a user exists with the given username.
     *
     * @param username the username to check
     * @return true if a user exists with the given username, false otherwise
     */
    boolean existsByUsername(String username);

    boolean existsByPhoneNumber(String phoneNumber);

    boolean existsByUserId(UUID userId);

    /**
     * Verifies a user account by username.
     *
     * @param username the username of the user to verify
     * @throws com.danielagapov.spawn.Exceptions.Base.BaseNotFoundException if user doesn't exist
     */
    void verifyUserByUsername(String username);

    /**
     * Calculates the number of mutual friends between two users.
     *
     * @param receiverId the unique identifier of the first user
     * @param id the unique identifier of the second user
     * @return the number of mutual friends between the users
     */
    int getMutualFriendCount(UUID receiverId, UUID id);

    /**
     * Retrieves a user as a BaseUserDTO by their unique identifier.
     *
     * @param id the unique identifier of the user
     * @return BaseUserDTO object representing the user
     * @throws com.danielagapov.spawn.Exceptions.Base.BaseNotFoundException if user doesn't exist
     */
    BaseUserDTO getBaseUserById(UUID id);

    /**
     * Updates a user's information with the provided update data.
     *
     * @param id the unique identifier of the user to update
     * @param updateUserDTO the DTO containing updated user information
     * @return updated BaseUserDTO object
     * @throws com.danielagapov.spawn.Exceptions.Base.BaseNotFoundException if user doesn't exist
     * @throws com.danielagapov.spawn.Exceptions.Base.BaseSaveException if updating fails
     */
    BaseUserDTO updateUser(UUID id, UserUpdateDTO updateUserDTO);

    /**
     * Searches for recommended friends based on a search query.
     *
     * @param requestingUserId the unique identifier of the user performing the search
     * @param searchQuery the search query string
     * @return SearchedUserResult containing search results and metadata
     * @throws com.danielagapov.spawn.Exceptions.Base.BaseNotFoundException if requesting user doesn't exist
     */
    SearchedUserResult getRecommendedFriendsBySearch(UUID requestingUserId, String searchQuery);

    /**
     * Get the User entity by username
     *
     * @param username the username to search for
     * @return User entity object
     * @throws com.danielagapov.spawn.Exceptions.Base.BaseNotFoundException if user doesn't exist
     */
    User getUserEntityByUsername(String username);

    /**
     * Searches for users by a query string for a requesting user.
     *
     * @param searchQuery the search query string
     * @param requestingUserId the unique identifier of the user performing the search
     * @return List of BaseUserDTO objects matching the search query
     * @throws com.danielagapov.spawn.Exceptions.Base.BaseNotFoundException if requesting user doesn't exist
     */
    List<BaseUserDTO> searchByQuery(String searchQuery, UUID requestingUserId);

    /**
     * Get the User entity by email
     *
     * @param email the email address to search for
     * @return User entity object
     * @throws com.danielagapov.spawn.Exceptions.Base.BaseNotFoundException if user doesn't exist
     */
    User getUserByEmail(String email);

    /**
     * Retrieves a user as a BaseUserDTO by their username.
     *
     * @param username the username to search for
     * @return BaseUserDTO object representing the user
     * @throws com.danielagapov.spawn.Exceptions.Base.BaseNotFoundException if user doesn't exist
     */
    BaseUserDTO getBaseUserByUsername(String username);

    /**
     * Get user profile information
     *
     * @param userId the unique identifier of the user
     * @return UserProfileInfoDTO containing user profile information
     * @throws com.danielagapov.spawn.Exceptions.Base.BaseNotFoundException if user doesn't exist
     */
    UserProfileInfoDTO getUserProfileInfo(UUID userId);

    BaseUserDTO setOptionalDetails(UUID userId, OptionalDetailsDTO optionalDetailsDTO);

    /**
     * Finds users by their phone numbers for contact cross-referencing.
     *
     * @param phoneNumbers list of phone numbers to search for
     * @param requestingUserId the unique identifier of the user making the request (to exclude from results)
     * @return List of BaseUserDTO objects for users with matching phone numbers
     */
    List<BaseUserDTO> findUsersByPhoneNumbers(List<String> phoneNumbers, UUID requestingUserId);
}
