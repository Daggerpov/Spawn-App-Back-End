package com.danielagapov.spawn.Repositories;

import com.danielagapov.spawn.Exceptions.Models.EventTag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface IEventTagRepository extends JpaRepository<EventTag, UUID> { }
