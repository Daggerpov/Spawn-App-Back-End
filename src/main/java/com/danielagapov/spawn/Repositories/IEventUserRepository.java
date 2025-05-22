package com.danielagapov.spawn.Repositories;

import com.danielagapov.spawn.DTOs.UserIdEventTimeDTO;
import com.danielagapov.spawn.Enums.ParticipationStatus;
import com.danielagapov.spawn.Models.CompositeKeys.EventUsersId;
import com.danielagapov.spawn.Models.EventUser;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface IEventUserRepository extends JpaRepository<EventUser, EventUsersId> {
    List<EventUser> findByEvent_Id(UUID eventId);

    List<EventUser> findByUser_Id(UUID userId);

    // Find event users by event ID and participation status
    List<EventUser> findByEvent_IdAndStatus(UUID eventId, ParticipationStatus status);

    // Find event users by user ID and participation status
    List<EventUser> findByUser_IdAndStatus(UUID userId, ParticipationStatus status);

    List<EventUser> findEventsByEvent_IdAndStatus(UUID eventId, ParticipationStatus status);

    @Query("SELECT eu.event.id FROM EventUser eu WHERE eu.user.id = :userId AND eu.status = :status AND eu.event.endTime <= current_time")
    List<UUID> findPastEventIdsForUser(UUID userId, ParticipationStatus status, Limit limit);

    @Query("SELECT DISTINCT new com.danielagapov.spawn.DTOs.UserIdEventTimeDTO(eu.user.id, MAX(eu.event.startTime)) FROM EventUser eu WHERE eu.event.id IN :eventIds AND eu.status = :status AND eu.user.id != :userId GROUP BY eu.user.id ORDER BY MAX(eu.event.startTime) DESC")
    List<UserIdEventTimeDTO> findOtherUserIdsByEventIds(List<UUID> eventIds, UUID userId, ParticipationStatus status);

    Optional<EventUser> findByEvent_IdAndUser_Id(UUID eventId, UUID userId);

    @Query("SELECT eu FROM EventUser eu JOIN eu.event e WHERE eu.user.id = :userId AND eu.status = :status ORDER BY e.lastUpdated DESC")
    Optional<EventUser> findTopByUserIdAndStatusOrderByEventLastUpdatedDesc(@Param("userId") UUID userId, @Param("status") ParticipationStatus status);
}
