package com.danielagapov.spawn.Services.FriendTag;

import com.danielagapov.spawn.DTOs.FriendTag.AbstractFriendTagDTO;
import com.danielagapov.spawn.DTOs.FriendTag.FriendTagDTO;
import com.danielagapov.spawn.DTOs.FriendTag.FullFriendTagDTO;
import com.danielagapov.spawn.DTOs.User.BaseUserDTO;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Service interface for managing friend tags and their relationships with users.
 * Friend tags allow users to organize their friends into labeled groups with colors.
 * Provides CRUD operations, user-tag relationships, and data conversion utilities.
 */
public interface IFriendTagService {
    
    /**
     * Retrieves all friend tags from the database with their owner and friend information.
     * 
     * @return List of FriendTagDTO objects with populated owner and friend user IDs
     * @throws com.danielagapov.spawn.Exceptions.Base.BasesNotFoundException if database access fails
     */
    List<FriendTagDTO> getAllFriendTags();

    /**
     * Retrieves a specific friend tag by its unique identifier.
     * 
     * @param id the unique identifier of the friend tag
     * @return FriendTagDTO object with populated owner and friend user IDs
     * @throws com.danielagapov.spawn.Exceptions.Base.BaseNotFoundException if friend tag with given ID is not found
     */
    FriendTagDTO getFriendTagById(UUID id);

    /**
     * Saves a new friend tag to the database and updates the last modified timestamp.
     * 
     * @param friendTag the friend tag data to save
     * @return the saved FriendTagDTO
     * @throws com.danielagapov.spawn.Exceptions.Base.BaseSaveException if saving fails
     */
    FriendTagDTO saveFriendTag(AbstractFriendTagDTO friendTag);

    /**
     * Updates an existing friend tag or creates a new one if it doesn't exist.
     * Updates the last modified timestamp automatically.
     * 
     * @param friendTag the friend tag data to update with
     * @param tagId the unique identifier of the friend tag to update
     * @return the updated FriendTagDTO
     * @throws com.danielagapov.spawn.Exceptions.Base.BaseSaveException if saving fails
     */
    FriendTagDTO replaceFriendTag(FriendTagDTO friendTag, UUID tagId);

    /**
     * Deletes a friend tag by its unique identifier. The "Everyone" tag cannot be deleted.
     * Also removes all user-friend tag relationships for this tag.
     * 
     * @param id the unique identifier of the friend tag to delete
     * @return true if deletion was successful, false otherwise
     * @throws com.danielagapov.spawn.Exceptions.Base.BaseNotFoundException if friend tag with given ID is not found
     */
    boolean deleteFriendTagById(UUID id);

    /**
     * Retrieves all friend tags owned by a specific user, sorted with "Everyone" tag first.
     * 
     * @param ownerId the unique identifier of the user who owns the friend tags
     * @return List of FriendTagDTO objects owned by the specified user
     * @throws RuntimeException if database access fails
     */
    List<FriendTagDTO> getFriendTagsByOwnerId(UUID ownerId);

    /**
     * Retrieves the IDs of all friend tags owned by a specific user.
     * 
     * @param id the unique identifier of the user
     * @return List of UUID objects representing friend tag IDs owned by the user
     */
    List<UUID> getFriendTagIdsByOwnerUserId(UUID id);

    /**
     * Optimized method to get all friend tags with their friends for a user in a single database query
     *
     * @param ownerId The ID of the user who owns the friend tags
     * @return List of FullFriendTagDTO objects with all friend data included
     */
    List<FullFriendTagDTO> getFullFriendTagsWithFriendsByOwnerId(UUID ownerId);

    /**
     * Finds a friend tag that contains a specific friend user.
     * If the friend is in multiple tags, returns one arbitrarily (excluding "Everyone" tag when possible).
     * 
     * @param ownerUserId the unique identifier of the user who owns the friend tags
     * @param friendUserId the unique identifier of the friend to search for
     * @return Optional containing the FriendTagDTO if found, empty otherwise
     */
    Optional<FriendTagDTO> getPertainingFriendTagBetweenUsers(UUID ownerUserId, UUID friendUserId);

    /**
     * Adds a user to a friend tag relationship.
     * 
     * @param id the unique identifier of the friend tag
     * @param userId the unique identifier of the user to add to the tag
     * @throws com.danielagapov.spawn.Exceptions.Base.BaseNotFoundException if friend tag or user doesn't exist
     * @throws com.danielagapov.spawn.Exceptions.Base.BaseSaveException if saving the relationship fails
     */
    void saveUserToFriendTag(UUID id, UUID userId);

    /**
     * Removes a user from a friend tag relationship.
     * 
     * @param id the unique identifier of the friend tag
     * @param userId the unique identifier of the user to remove from the tag
     * @throws com.danielagapov.spawn.Exceptions.Base.BaseNotFoundException if friend tag or user doesn't exist
     * @throws com.danielagapov.spawn.Exceptions.Base.BaseSaveException if removing the relationship fails
     */
    void removeUserFromFriendTag(UUID id, UUID userId);

    /**
     * Retrieves all friend tags that contain a specific friend user as FullFriendTagDTO objects.
     * 
     * @param ownerUserId the unique identifier of the user who owns the friend tags
     * @param friendUserId the unique identifier of the friend to search for
     * @return List of FullFriendTagDTO objects containing the specified friend
     */
    List<FullFriendTagDTO> getPertainingFullFriendTagsForFriend(UUID ownerUserId, UUID friendUserId);

    /**
     * Retrieves all friend tags that contain a specific friend user as FriendTagDTO objects.
     * 
     * @param ownerUserId the unique identifier of the user who owns the friend tags
     * @param friendUserId the unique identifier of the friend to search for
     * @return List of FriendTagDTO objects containing the specified friend
     */
    List<FriendTagDTO> getPertainingFriendTagsForFriend(UUID ownerUserId, UUID friendUserId);

    /**
     * Retrieves all friends of a user that are not yet added to a specific friend tag.
     * 
     * @param friendTagId the unique identifier of the friend tag
     * @return List of BaseUserDTO objects representing friends not in the tag
     * @throws com.danielagapov.spawn.Exceptions.Base.BaseNotFoundException if friend tag doesn't exist
     */
    List<BaseUserDTO> getFriendsNotAddedToTag(UUID friendTagId);

    /**
     * Retrieves all friend tags that don't contain a specific friend user, excluding the "Everyone" tag.
     * 
     * @param ownerUserId the unique identifier of the user who owns the friend tags
     * @param friendUserId the unique identifier of the friend to check against
     * @return List of FullFriendTagDTO objects not containing the specified friend
     * @throws RuntimeException if database access fails
     */
    List<FullFriendTagDTO> getTagsNotAddedToFriend(UUID ownerUserId, UUID friendUserId);

    /**
     * Adds multiple users to a friend tag by calling saveUserToFriendTag for each user.
     * 
     * @param friendTagId the unique identifier of the friend tag
     * @param friends the list of BaseUserDTO objects representing users to add
     * @throws com.danielagapov.spawn.Exceptions.Base.BaseNotFoundException if friend tag or any user doesn't exist
     * @throws com.danielagapov.spawn.Exceptions.Base.BaseSaveException if saving any relationship fails
     */
    void saveUsersToFriendTag(UUID friendTagId, List<BaseUserDTO> friends);

    /**
     * Bulk adds multiple users to a friend tag (functionally identical to saveUsersToFriendTag).
     * 
     * @param friendTagId the unique identifier of the friend tag
     * @param friends the list of BaseUserDTO objects representing users to add
     * @throws com.danielagapov.spawn.Exceptions.Base.BaseNotFoundException if friend tag or any user doesn't exist
     * @throws com.danielagapov.spawn.Exceptions.Base.BaseSaveException if saving any relationship fails
     */
    void bulkAddUsersToFriendTag(UUID friendTagId, List<BaseUserDTO> friends);

    /**
     * Adds a friend user to multiple friend tags.
     * 
     * @param friendTagIds the list of friend tag IDs to add the user to
     * @param friendUserId the unique identifier of the user to add to the tags
     * @throws com.danielagapov.spawn.Exceptions.Base.BaseNotFoundException if any friend tag or user doesn't exist
     * @throws com.danielagapov.spawn.Exceptions.Base.BaseSaveException if saving any relationship fails
     */
    void addFriendToFriendTags(List<UUID> friendTagIds, UUID friendUserId);

    /**
     * Retrieves the IDs of all friends associated with a specific friend tag.
     * 
     * @param tagId the unique identifier of the friend tag
     * @return List of UUID objects representing friend user IDs in the tag
     * @throws com.danielagapov.spawn.Exceptions.Base.BaseNotFoundException if friend tag doesn't exist
     * @throws RuntimeException if database access fails
     */
    List<UUID> getFriendIdsByTagId(UUID tagId);

    /**
     * Converts a list of FriendTagDTO objects to FullFriendTagDTO objects with complete user information.
     * Uses an optimized method when possible to reduce database queries.
     * 
     * @param friendTags the list of FriendTagDTO objects to convert
     * @return List of FullFriendTagDTO objects with populated user details
     */
    List<FullFriendTagDTO> convertFriendTagsToFullFriendTags(List<FriendTagDTO> friendTags);

    /**
     * Retrieves a full friend tag by its unique identifier with complete user information.
     * 
     * @param id the unique identifier of the friend tag
     * @return FullFriendTagDTO object with populated user details
     * @throws com.danielagapov.spawn.Exceptions.Base.BaseNotFoundException if friend tag with given ID is not found
     */
    FullFriendTagDTO getFullFriendTagById(UUID id);

    /**
     * Converts a FriendTagDTO to a FullFriendTagDTO with complete user information.
     * 
     * @param friendTag the FriendTagDTO to convert
     * @return FullFriendTagDTO object with populated user details
     */
    FullFriendTagDTO getFullFriendTagByFriendTag(FriendTagDTO friendTag);
}
