package com.danielagapov.spawn.Repositories;

import com.danielagapov.spawn.Models.FriendTag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Query;

import java.util.UUID;
import java.util.List;

@Repository
public interface IFriendTagRepository extends JpaRepository<FriendTag, UUID> {
    // The JpaRepository interface already includes methods like save() and findById()
    @Query(value = "SELECT id FROM friend_tag WHERE owner_id = ?1", nativeQuery = true)
    List<UUID> findByOwnerId(UUID ownerId);
}
