package com.danielagapov.spawn.Repositories;

import com.danielagapov.spawn.Models.Event;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface IEventRepository extends JpaRepository<Event, UUID> {
    // The JpaRepository interface already includes methods like save() and findById()
}
