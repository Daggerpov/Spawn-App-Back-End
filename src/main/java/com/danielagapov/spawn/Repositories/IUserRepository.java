package com.danielagapov.spawn.Repositories;

import com.danielagapov.spawn.Models.User;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface IUserRepository extends JpaRepository<User, UUID> {
    // The JpaRepository interface already includes methods like save() and findById()
    // Find
    Optional<User> findByEmail(String email);

    Optional<User> findByUsername(String username);

    Optional<User> findUserByEmail(String email);

    @Query("SELECT u FROM User u WHERE LOWER(u.firstName) LIKE CONCAT(:prefix, '%') OR LOWER(u.lastName) LIKE CONCAT(:prefix, '%') OR LOWER(u.username) LIKE CONCAT(:prefix, '%')")
    List<User> findUsersWithPrefix(String prefix, Limit limit);

    // Exist
    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    // Delete

    /**
     * Used by CleanUnverifiedService to remove expired, unverified users
     */
    @Modifying
    @Transactional
    @Query("DELETE FROM User u WHERE u.verified = false AND u.dateCreated <= :expirationDate")
    int deleteAllExpiredUnverifiedUsers(@Param("expirationDate") Instant expirationDate);

    @Query("SELECT MAX(u.lastUpdated) FROM User u JOIN u.friends f WHERE f.id = :userId")
    Instant findLatestFriendProfileUpdate(@Param("userId") UUID userId);
}
