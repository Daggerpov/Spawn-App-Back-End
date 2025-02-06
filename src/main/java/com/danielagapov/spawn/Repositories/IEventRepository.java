package com.danielagapov.spawn.Repositories;

import com.danielagapov.spawn.Exceptions.Models.Event;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface IEventRepository extends JpaRepository<Event, UUID> {
    // JPA (Java Persistence API) should automatically create queries based on these method names:

    // finds events that have been created by a user, by their `creatorId`
    List<Event> findByCreatorId(UUID creatorId);

    // finds events that have been created by users, whose ids are in the `creatorIds` list
    List<Event> findByCreatorIdIn(List<UUID> creatorIds);
}
