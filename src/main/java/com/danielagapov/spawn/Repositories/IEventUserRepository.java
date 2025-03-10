package com.danielagapov.spawn.Repositories;

import com.danielagapov.spawn.Models.CompositeKeys.EventUsersId;
import com.danielagapov.spawn.Models.EventUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface IEventUserRepository extends JpaRepository<EventUser, EventUsersId> {
    Optional<List<EventUser>> findByEvent_Id(UUID eventId);
    Optional<List<EventUser>> findByUser_Id(UUID userId);
}
