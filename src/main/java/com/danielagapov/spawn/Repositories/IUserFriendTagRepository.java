package com.danielagapov.spawn.Repositories;

import com.danielagapov.spawn.Models.UserFriendTag;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Query;

import java.util.UUID;
import java.util.List;

@Repository
public interface IUserFriendTagRepository extends JpaRepository<UserFriendTag, UUID> {
    @Query(value = "SELECT * FROM user_id WHERE friend_tag_id = ?1", nativeQuery = true)
    List<UUID> findFriendsByTagId(UUID tagId);
}
