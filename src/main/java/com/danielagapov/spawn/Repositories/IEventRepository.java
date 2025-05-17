package com.danielagapov.spawn.Repositories;

import com.danielagapov.spawn.Models.Event;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface IEventRepository extends JpaRepository<Event, UUID> {
    // JPA (Java Persistence API) should automatically create queries based on these method names:

    // finds events that have been created by a user, by their `creatorId`
    List<Event> findByCreatorId(UUID creatorId);
    
    // finds events that have been created by any of the users in the list
    List<Event> findByCreatorIdIn(List<UUID> creatorIds);

    @Query("SELECT e FROM Event e " +
            "JOIN e.creator c " +
            "JOIN UserFriendTag uft ON uft.friend.id = c.id " +  // Proper entity name
            "JOIN EventUser eu ON eu.event.id = e.id " +  // Get users invited to the event
            "WHERE uft.friendTag.id = :friendTagId " +
            "AND eu.user.id = :invitedUserId")
    List<Event> getEventsInvitedToWithFriendTagId(@Param("friendTagId") UUID friendTagId, @Param("invitedUserId") UUID invitedUserId);
    
    /**
     * Finds past events created by the inviter where the requesting user was invited
     * Either the end time is in the past or the start time is in the past (if no end time)
     */
    @Query("SELECT e FROM Event e " +
           "JOIN EventUser eu ON eu.event.id = e.id " +
           "WHERE e.creator.id = :inviterUserId " +
           "AND eu.user.id = :requestingUserId " +
           "AND ((e.endTime IS NOT NULL AND e.endTime < :now) OR " +
           "(e.endTime IS NULL AND e.startTime < :now)) " +
           "ORDER BY e.startTime DESC")
    List<Event> getPastEventsWhereUserInvited(
        @Param("inviterUserId") UUID inviterUserId, 
        @Param("requestingUserId") UUID requestingUserId,
        @Param("now") OffsetDateTime now);

    Optional<Event> findTopByCreatorIdOrderByLastUpdatedDesc(UUID creatorId);
}
