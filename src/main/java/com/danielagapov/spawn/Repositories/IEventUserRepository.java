package com.danielagapov.spawn.Repositories;

import com.danielagapov.spawn.Enums.ParticipationStatus;
import com.danielagapov.spawn.Models.CompositeKeys.EventUsersId;
import com.danielagapov.spawn.Models.EventUser;
import org.springframework.data.jpa.repository.JpaRepository;

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

    Optional<EventUser> findByEvent_IdAndUser_Id(UUID eventId, UUID userId);

    Optional<EventUser> findTopByUserIdAndStatusOrderByEventCreatedTimestampDesc(UUID userId, ParticipationStatus status);

    Optional<EventUser> findTopByUserIdAndStatusOrderByEventUpdatedTimestampDesc(UUID userId, ParticipationStatus status);
}
