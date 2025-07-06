package com.danielagapov.spawn.Repositories.User;

import com.danielagapov.spawn.Models.User.User;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
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

    @Query("SELECT u FROM User u WHERE LOWER(u.name) LIKE CONCAT(:prefix, '%') OR LOWER(u.username) LIKE CONCAT(:prefix, '%')")
    List<User> findUsersWithPrefix(String prefix, Limit limit);

    // Exist
    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    boolean existsByPhoneNumber(String phoneNumber);

    // Delete

    /**
     * Used by CleanUnverifiedService to remove expired, unverified users
     */
    @Modifying
    @Transactional
    @Query(value = "DELETE FROM user WHERE verified = false AND date_created <= DATE_SUB(NOW(), INTERVAL 1 DAY)", nativeQuery = true)
    int deleteAllExpiredUnverifiedUsers();

    @Query(value = "SELECT MAX(u.last_updated) FROM user u " +
            "JOIN user_friend_tag uft ON u.id = uft.friend_id " +
            "JOIN friend_tag ft ON uft.tag_id = ft.id " +
            "WHERE ft.owner_id = :userId AND ft.is_everyone = true", nativeQuery = true)
    Instant findLatestFriendProfileUpdate(@Param("userId") UUID userId);
}
