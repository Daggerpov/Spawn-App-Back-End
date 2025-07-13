package com.danielagapov.spawn.Repositories.User.Profile;

import com.danielagapov.spawn.Models.User.Profile.UserInterest;
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