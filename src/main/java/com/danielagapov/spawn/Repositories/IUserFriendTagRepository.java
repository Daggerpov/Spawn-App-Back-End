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

    @Query("SELECT MAX(uft.lastModified) FROM UserFriendTag uft WHERE uft.friendTag.ownerId = :ownerId")
    Instant findLatestTagFriendActivity(@Param("ownerId") UUID ownerId);
}
