package com.danielagapov.spawn.Repositories;

import com.danielagapov.spawn.Models.EventInvited;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface IEventInvitedRepository extends JpaRepository<EventInvited, UUID> { }
