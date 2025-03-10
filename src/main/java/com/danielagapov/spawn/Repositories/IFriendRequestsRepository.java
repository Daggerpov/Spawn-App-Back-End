package com.danielagapov.spawn.Repositories;

import com.danielagapov.spawn.Models.FriendRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface IFriendRequestsRepository extends JpaRepository<FriendRequest, UUID> {
    Optional<List<FriendRequest>> findByReceiverId(UUID receiverId);

    Optional<List<FriendRequest>> findBySenderId(UUID userId);
}
