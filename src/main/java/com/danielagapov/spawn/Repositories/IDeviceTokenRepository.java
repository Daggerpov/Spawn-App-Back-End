package com.danielagapov.spawn.Repositories;

import com.danielagapov.spawn.Models.DeviceToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface IDeviceTokenRepository extends JpaRepository<DeviceToken, UUID> {
    List<DeviceToken> findByUserId(UUID userId);

    boolean existsByToken(String token);

    void deleteByToken(String token);
    
    List<DeviceToken> findByToken(String token);
} 