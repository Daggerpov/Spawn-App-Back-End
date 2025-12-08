package com.danielagapov.spawn.social.internal.repositories;

import com.danielagapov.spawn.social.internal.domain.Friendship;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface IFriendshipRepository extends JpaRepository<Friendship, UUID> {
    boolean existsByUserA_IdAndUserB_Id(UUID userAId, UUID userBId);
    List<Friendship> findByUserA_Id(UUID userAId);
    List<Friendship> findByUserB_Id(UUID userBId);
    void deleteByUserA_IdAndUserB_Id(UUID userAId, UUID userBId);

    // Bidirectional variants (order-agnostic)
    boolean existsByUserA_IdAndUserB_IdOrUserB_IdAndUserA_Id(UUID userAId, UUID userBId, UUID userBId2, UUID userAId2);
    List<Friendship> findByUserA_IdOrUserB_Id(UUID userId1, UUID userId2);
    void deleteByUserA_IdAndUserB_IdOrUserB_IdAndUserA_Id(UUID userAId, UUID userBId, UUID userBId2, UUID userAId2);

    // Convenience helpers
    default boolean existsBidirectionally(UUID userId1, UUID userId2) {
        return existsByUserA_IdAndUserB_IdOrUserB_IdAndUserA_Id(userId1, userId2, userId1, userId2);
    }

    default void deleteBidirectionally(UUID userId1, UUID userId2) {
        deleteByUserA_IdAndUserB_IdOrUserB_IdAndUserA_Id(userId1, userId2, userId1, userId2);
    }

    default List<Friendship> findAllByUserIdBidirectional(UUID userId) {
        return findByUserA_IdOrUserB_Id(userId, userId);
    }
}


