package com.danielagapov.spawn.Repositories;

import com.danielagapov.spawn.Models.NotificationPreferences;
import com.danielagapov.spawn.Models.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for notification preferences
 */
@Repository
public interface INotificationPreferencesRepository extends JpaRepository<NotificationPreferences, Long> {
    
    /**
     * Find notification preferences by user
     * @param user The user
     * @return Optional containing the preferences if found
     */
    Optional<NotificationPreferences> findByUser(User user);
} 