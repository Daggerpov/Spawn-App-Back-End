package com.danielagapov.spawn.activity.internal.repositories;

import com.danielagapov.spawn.activity.internal.domain.Activity;
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

    // finds activities that have been created by a user, by their `creator.id`
    // Note: The Activity entity has a 'creator' field (User object), not a 'creatorId' field
    // Spring Data JPA will automatically resolve this to creator.id
    @Query("SELECT a FROM Activity a WHERE a.creator.id = :creatorId")
    List<Activity> findByCreatorId(@Param("creatorId") UUID creatorId);
    
    // finds activities that have been created by any of the users in the list
    // Note: The Activity entity has a 'creator' field (User object), not a 'creatorId' field
    // Spring Data JPA will automatically resolve this to creator.id
    @Query("SELECT a FROM Activity a WHERE a.creator.id IN :creatorIds")
    List<Activity> findByCreatorIdIn(@Param("creatorIds") List<UUID> creatorIds);
    
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

    // finds the most recently updated activity created by a user
    @Query("SELECT a FROM Activity a WHERE a.creator.id = :creatorId ORDER BY a.lastUpdated DESC LIMIT 1")
    Optional<Activity> findTopByCreatorIdOrderByLastUpdatedDesc(@Param("creatorId") UUID creatorId);
}
