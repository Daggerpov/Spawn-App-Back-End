package com.danielagapov.spawn.Repositories;

import com.danielagapov.spawn.Models.EventUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface IEventUserRepository extends JpaRepository<EventUser, UUID> {
    // TODO: queries?
}
