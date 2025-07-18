package com.danielagapov.spawn.Repositories.User;

import com.danielagapov.spawn.Enums.UserStatus;
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

    @Query("SELECT u FROM User u WHERE u.status = :status")
    List<User> findAllUsersByStatus(UserStatus status);

    @Query("SELECT u FROM User u WHERE LOWER(u.name) LIKE CONCAT('%', :query, '%') OR LOWER(u.username) LIKE CONCAT('%', :query, '%')")
    List<User> findUsersWithPartialMatch(String query, Limit limit);

    // Exist
    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    boolean existsByPhoneNumber(String phoneNumber);

    @Query(value = "SELECT MAX(u.last_updated) FROM user u " +
            "JOIN user_friend_tag uft ON u.id = uft.user_id " +
            "JOIN friend_tag ft ON uft.friend_tag_id = ft.id " +
            "WHERE ft.owner_id = :userId AND ft.is_everyone = true", nativeQuery = true)
    Instant findLatestFriendProfileUpdate(@Param("userId") UUID userId);
}
