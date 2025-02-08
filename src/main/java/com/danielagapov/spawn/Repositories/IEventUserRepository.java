package com.danielagapov.spawn.Repositories;

import com.danielagapov.spawn.Models.EventUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface IEventUserRepository extends JpaRepository<EventUser, UUID> {
    List<EventUser> findByEvent_Id(UUID eventId);

    List<EventUser> findByUser_Id(UUID userId);
}
