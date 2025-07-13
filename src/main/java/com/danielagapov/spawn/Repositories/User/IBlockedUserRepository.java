package com.danielagapov.spawn.Repositories.User;

import com.danielagapov.spawn.Models.User.BlockedUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface IBlockedUserRepository extends JpaRepository<BlockedUser, UUID> {

    boolean existsByBlocker_IdAndBlocked_Id(UUID blockerId, UUID blockedId);
    Optional<BlockedUser> findByBlocker_IdAndBlocked_Id(UUID blockerId, UUID blockedId);
    
    @Query("SELECT b FROM BlockedUser b JOIN FETCH b.blocker JOIN FETCH b.blocked WHERE b.blocker.id = :blockerId")
    List<BlockedUser> findAllByBlocker_Id(@Param("blockerId") UUID blockerId);
    
    @Query("SELECT b FROM BlockedUser b JOIN FETCH b.blocker JOIN FETCH b.blocked WHERE b.blocked.id = :blockedId")
    List<BlockedUser> findAllByBlocked_Id(@Param("blockedId") UUID blockedId);
}
