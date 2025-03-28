package com.danielagapov.spawn.Repositories;

import com.danielagapov.spawn.Models.FriendTag;
import com.danielagapov.spawn.Models.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface IFriendTagRepository extends JpaRepository<FriendTag, UUID> {
    // The JpaRepository interface already includes methods like save() and findById()
    @Query("SELECT ft FROM FriendTag ft WHERE ft.ownerId = :ownerId")
    List<FriendTag> findByOwnerId(@Param("ownerId") UUID ownerId);

    @Query("SELECT ft From FriendTag ft WHERE ft.ownerId = :ownerId AND ft.isEveryone = true")
    Optional<FriendTag> findEveryoneTagByOwnerId(@Param("ownerId") UUID ownerId);

    @Query("SELECT u FROM User u JOIN UserFriendTag uf ON uf.friend.id = u.id WHERE uf.friendTag.ownerId = :ownerId AND uf.friendTag.isEveryone = true AND u.id != :ownerId")
    List<User> getFriendsFromEveryoneTagByOwnerId(@Param("ownerId") UUID ownerId);
}
