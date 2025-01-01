package com.danielagapov.spawn.Repositories;

import com.danielagapov.spawn.Models.FriendTag;
import com.danielagapov.spawn.Models.UserFriendTag;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Query;

import java.util.UUID;
import java.util.List;

@Repository
public interface IUserFriendTagRepository extends JpaRepository<UserFriendTag, UUID> {
    @Query("SELECT uft.friend.id FROM UserFriendTag uft WHERE uft.friendTag.id = :tagId")
    List<UUID> findFriendIdsByTagId(@Param("tagId") UUID tagId);
}
