package com.danielagapov.spawn.Repositories;

import com.danielagapov.spawn.Models.ActivityType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface IActivityTypeRepository extends JpaRepository<ActivityType, UUID> {

    @Query("SELECT at FROM ActivityType at WHERE at.creator.id = :creatorId")
    List<ActivityType> findActivityTypesByCreatorId(UUID creatorId);

    Integer findMaxOrderNumberByCreatorId(UUID creatorId);
}
