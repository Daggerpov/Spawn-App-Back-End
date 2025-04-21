package com.danielagapov.spawn.Repositories;

import com.danielagapov.spawn.Models.Event;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

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
            "JOIN UserFriendTag f ON f.friend.id = c.id " +  // Get friends of the friend tag owner
            "JOIN EventUser eu ON eu.event.id = e.id " +  // Get users invited to the event
            "WHERE f.friendTag.id = :friendTagId " +
            "AND eu.user.id = :invitedUserId")
    List<Event> getEventsInvitedToWithFriendTagId(@Param("friendTagId") UUID friendTagId, @Param("invitedUserId") UUID invitedUserId);

    Optional<Event> findTopByCreatorIdOrderByCreatedTimestampDesc(UUID creatorId);
}
