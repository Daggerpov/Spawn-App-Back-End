package com.danielagapov.spawn.Repositories;

import com.danielagapov.spawn.Models.User;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface IUserRepository extends JpaRepository<User, UUID> {
    // The JpaRepository interface already includes methods like save() and findById()
    // Find
    Optional<User> findByEmail(String email);

    Optional<User> findByUsername(String username);

    Optional<User> findUserByEmail(String email);

    // Exist
    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    // Delete

    /**
     * Used by CleanUnverifiedService to remove expired, unverified users
     */
    @Modifying
    @Transactional
    @Query(value = "DELETE FROM user WHERE verified = false AND date_created <= DATE_SUB(NOW(), INTERVAL 1 DAY)", nativeQuery = true)
    int deleteAllExpiredUnverifiedUsers();
}
