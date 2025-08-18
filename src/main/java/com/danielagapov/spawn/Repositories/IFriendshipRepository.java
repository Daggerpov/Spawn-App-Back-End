package com.danielagapov.spawn.Repositories;

import com.danielagapov.spawn.Models.Friendship;
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
}


