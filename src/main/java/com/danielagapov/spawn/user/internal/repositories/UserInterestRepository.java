package com.danielagapov.spawn.user.internal.repositories;

import com.danielagapov.spawn.user.internal.domain.UserInterest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserInterestRepository extends JpaRepository<UserInterest, UUID> {
    List<UserInterest> findByUserId(UUID userId);
    Optional<UserInterest> findByUserIdAndInterest(UUID userId, String interest);

    @Query("SELECT ui FROM UserInterest ui WHERE ui.user.id = :userId AND LOWER(ui.interest) = LOWER(:interest)")
    Optional<UserInterest> findByUserIdAndInterestIgnoreCase(@Param("userId") UUID userId, @Param("interest") String interest);

    @Modifying
    @Query("DELETE FROM UserInterest ui WHERE ui.user.id = :userId")
    void deleteAllByUserId(@Param("userId") UUID userId);
} 