package com.danielagapov.spawn.Repositories;

import com.danielagapov.spawn.Models.UserInterest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface UserInterestRepository extends JpaRepository<UserInterest, UUID> {
    List<UserInterest> findByUserId(UUID userId);
} 