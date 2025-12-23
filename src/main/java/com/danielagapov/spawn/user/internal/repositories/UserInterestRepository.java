package com.danielagapov.spawn.user.internal.repositories;

import com.danielagapov.spawn.user.internal.domain.UserInterest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserInterestRepository extends JpaRepository<UserInterest, UUID> {
    List<UserInterest> findByUserId(UUID userId);
    Optional<UserInterest> findByUserIdAndInterest(UUID userId, String interest);
} 