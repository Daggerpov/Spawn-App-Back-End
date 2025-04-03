package com.danielagapov.spawn.Repositories;

import com.danielagapov.spawn.Models.FriendRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface IFriendRequestsRepository extends JpaRepository<FriendRequest, UUID> {
    List<FriendRequest> findByReceiverId(UUID receiverId);

    List<FriendRequest> findBySenderId(UUID userId);

    List<FriendRequest> findBySenderIdAndReceiverId(UUID senderId, UUID receiverId);
}
