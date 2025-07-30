package com.danielagapov.spawn.Repositories;

import com.danielagapov.spawn.Models.UserFriendTag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface IUserFriendTagRepository extends JpaRepository<UserFriendTag, UUID> {
    @Query("SELECT uft.friend.id FROM UserFriendTag uft WHERE uft.friendTag.id = :tagId")
    List<UUID> findFriendIdsByTagId(@Param("tagId") UUID tagId);

    @Modifying
    @Transactional
    void deleteByFriendTagIdAndFriendId(UUID friendTagId, UUID friendId);

    boolean existsByFriendTagIdAndFriendId(UUID friendTagId, UUID friendId);

    Instant findTopByFriendTag_OwnerIdOrderByLastUpdatedDesc(UUID ownerId);

    @Query("SELECT uft.friend.id FROM UserFriendTag uft WHERE uft.friendTag.isEveryone = true AND uft.friendTag.ownerId = :ownerId")
    List<UUID> findFriendIdsByUserId(UUID ownerId);

    /**
     * Retrieves all UserFriendTag entries for a specific owner's friends in a single query.
     * This query fetches all the data needed to build FullFriendUserDTOs efficiently.
     *
     * @param ownerId The ID of the user who owns the friend tags
     * @return List of UserFriendTag objects with their associated User and FriendTag data
     */
    @Query("SELECT uft FROM UserFriendTag uft " +
           "JOIN FETCH uft.friend " +
           "JOIN FETCH uft.friendTag " +
           "WHERE uft.friendTag.ownerId = :ownerId " +
           "ORDER BY uft.friend.id")
    List<UserFriendTag> findAllFriendsWithTagsByOwnerId(@Param("ownerId") UUID ownerId);

    /**
     * Retrieves all friend tags with their associated friends for a specific owner in a single query.
     * This optimized query groups friends by their tags to build FullFriendTagDTOs efficiently.
     * 
     * @param ownerId The ID of the user who owns the friend tags
     * @return List of UserFriendTag objects grouped by FriendTag
     */
    @Query("SELECT uft FROM UserFriendTag uft " +
           "JOIN FETCH uft.friend " +
           "JOIN FETCH uft.friendTag " +
           "WHERE uft.friendTag.ownerId = :ownerId " +
           "ORDER BY uft.friendTag.id, uft.friend.id")
    List<UserFriendTag> findAllTagsWithFriendsByOwnerId(@Param("ownerId") UUID ownerId);
    
    /**
     * Directly checks if a potential friend is in a user's "Everyone" tag (i.e., is a friend)
     * 
     * @param userId The ID of the user
     * @param potentialFriendId The ID of the potential friend
     * @return true if the users are friends, false otherwise
     */
    @Query("SELECT COUNT(uft) > 0 FROM UserFriendTag uft " +
           "WHERE uft.friendTag.ownerId = :userId " +
           "AND uft.friendTag.isEveryone = true " +
           "AND uft.friend.id = :potentialFriendId")
    boolean isUserFriendOfUser(@Param("userId") UUID userId, @Param("potentialFriendId") UUID potentialFriendId);
    
    /**
     * Efficiently calculates mutual friend count between two users using database-level operations.
     * This is much more efficient than fetching friend lists and calculating intersection in memory.
     * 
     * @param userId1 The first user's ID
     * @param userId2 The second user's ID
     * @return Count of mutual friends between the two users
     */
    @Query("SELECT COUNT(DISTINCT uft1.friend.id) FROM UserFriendTag uft1, UserFriendTag uft2 " +
           "WHERE uft1.friendTag.ownerId = :userId1 AND uft1.friendTag.isEveryone = true " +
           "AND uft2.friendTag.ownerId = :userId2 AND uft2.friendTag.isEveryone = true " +
           "AND uft1.friend.id = uft2.friend.id")
    int getMutualFriendCount(@Param("userId1") UUID userId1, @Param("userId2") UUID userId2);
}
