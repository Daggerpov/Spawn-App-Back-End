package com.danielagapov.spawn.Repositories;

import com.danielagapov.spawn.Models.Event;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface IEventRepository extends JpaRepository<Event, Long> {
    // The JpaRepository interface already includes methods like save() and findById()
}
