package com.danielagapov.spawn.activity.internal.repositories;

import com.danielagapov.spawn.activity.internal.domain.ActivityType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface IActivityTypeRepository extends JpaRepository<ActivityType, UUID> {

    @Query("SELECT DISTINCT at FROM ActivityType at LEFT JOIN FETCH at.associatedFriends WHERE at.creator.id = :creatorId")
    List<ActivityType> findActivityTypesByCreatorId(UUID creatorId);

    @Query("SELECT MAX(at.orderNum) FROM ActivityType at WHERE at.creator.id = :creatorId")
    Integer findMaxOrderNumberByCreatorId(UUID creatorId);
    
    Long countByCreatorIdAndIsPinnedTrue(UUID creatorId);
    
    Long countByCreatorId(UUID creatorId);
}
