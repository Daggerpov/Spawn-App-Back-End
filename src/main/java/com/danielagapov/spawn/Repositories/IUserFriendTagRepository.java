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
}
