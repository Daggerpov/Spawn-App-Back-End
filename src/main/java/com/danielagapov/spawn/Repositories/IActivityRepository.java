package com.danielagapov.spawn.Repositories;

import com.danielagapov.spawn.Models.Activity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface IActivityRepository extends JpaRepository<Activity, UUID> {
    // JPA (Java Persistence API) should automatically create queries based on these method names:

    // finds activities that have been created by a user, by their `creatorId`
    List<Activity> findByCreatorId(UUID creatorId);
    
    // finds activities that have been created by any of the users in the list
    List<Activity> findByCreatorIdIn(List<UUID> creatorIds);
    
    /**
     * Finds past activities created by the inviter where the requesting user was invited
     * Either the end time is in the past or the start time is in the past (if no end time)
     */
    @Query("SELECT a FROM Activity a " +
           "JOIN ActivityUser au ON au.activity.id = a.id " +
           "WHERE a.creator.id = :inviterUserId " +
           "AND au.user.id = :requestingUserId " +
           "AND ((a.endTime IS NOT NULL AND a.endTime < :now) OR " +
           "(a.endTime IS NULL AND a.startTime < :now)) " +
           "ORDER BY a.startTime DESC")
    List<Activity> getPastActivitiesWhereUserInvited(
        @Param("inviterUserId") UUID inviterUserId, 
        @Param("requestingUserId") UUID requestingUserId,
        @Param("now") OffsetDateTime now);

    Optional<Activity> findTopByCreatorIdOrderByLastUpdatedDesc(UUID creatorId);
}
