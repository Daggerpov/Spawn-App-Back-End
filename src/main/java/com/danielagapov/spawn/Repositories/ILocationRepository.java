package com.danielagapov.spawn.Repositories;

import com.danielagapov.spawn.Models.Location.Location;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ILocationRepository extends JpaRepository<Location, UUID> {}
