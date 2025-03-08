package com.danielagapov.spawn.Repositories;

import com.danielagapov.spawn.Models.DeviceToken;
import com.danielagapov.spawn.Models.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface DeviceTokenRepository extends JpaRepository<DeviceToken, UUID> {
    List<DeviceToken> findByUser(User user);
    List<DeviceToken> findByUserId(UUID userId);
    boolean existsByToken(String token);
    void deleteByToken(String token);
} 