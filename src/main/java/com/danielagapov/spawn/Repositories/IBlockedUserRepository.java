package com.danielagapov.spawn.Repositories;

import com.danielagapov.spawn.Models.BlockedUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface IBlockedUserRepository extends JpaRepository<BlockedUser, UUID> {

    boolean existsByBlocker_IdAndBlocked_Id(UUID blockerId, UUID blockedId);
    Optional<BlockedUser> findByBlocker_IdAndBlocked_Id(UUID blockerId, UUID blockedId);
    List<BlockedUser> findAllByBlocker_Id(UUID blockerId);
}
