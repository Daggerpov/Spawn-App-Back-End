package com.danielagapov.spawn.Repositories;

import com.danielagapov.spawn.Models.FriendTag;
import com.danielagapov.spawn.Models.User.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface IFriendTagRepository extends JpaRepository<FriendTag, UUID> {
    List<FriendTag> findByOwnerId(UUID ownerId);

    Optional<FriendTag> findByOwnerIdAndIsEveryoneTrue(UUID ownerId);

    @Query("SELECT u FROM User u JOIN UserFriendTag uf ON u.id = uf.friend.id WHERE uf.friendTag.ownerId = :ownerId AND uf.friendTag.isEveryone = true AND u.id != :ownerId")
    List<User> getFriendsFromEveryoneTagByOwnerId(@Param("ownerId") UUID ownerId);

    // Spring Data JPA doesn't directly support aggregate functions in method names, so keeping JPQL
    @Query("SELECT MAX(ft.lastUpdated) FROM FriendTag ft WHERE ft.ownerId = :ownerId")
    Instant findLatestTagActivity(@Param("ownerId") UUID ownerId);
}
