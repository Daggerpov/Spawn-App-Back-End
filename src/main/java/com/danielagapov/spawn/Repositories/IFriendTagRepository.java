package com.danielagapov.spawn.Repositories;

import com.danielagapov.spawn.Models.FriendTag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface IFriendTagRepository extends JpaRepository<FriendTag, UUID> {
    // The JpaRepository interface already includes methods like save() and findById()
    @Query("SELECT ft FROM FriendTag ft WHERE ft.owner = :ownerId")
    List<FriendTag> findByOwnerId(@Param("ownerId") UUID ownerId);
}
