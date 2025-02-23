package com.danielagapov.spawn.Repositories;

import com.danielagapov.spawn.Models.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface IUserRepository extends JpaRepository<User, UUID> {
    // The JpaRepository interface already includes methods like save() and findById()
    // Find
    User findByEmail(String email);

    User findByUsername(String username);

    // Exist
    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    // Delete

    /**
     * Used by CleanUnverifiedService to remove expired, unverified users
     */
    @Modifying
    @Query(value = "DELETE FROM User WHERE verified = false AND dateCreated <= DATE_SUB(NOW(), INTERVAL 1 DAY)", nativeQuery = true)
    int deleteAllExpiredUnverifiedUsers();
}
