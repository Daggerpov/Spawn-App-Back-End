package com.danielagapov.spawn.activity.internal.repositories;

import com.danielagapov.spawn.activity.internal.domain.Location;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ILocationRepository extends JpaRepository<Location, UUID> {}
