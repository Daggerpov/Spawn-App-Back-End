package com.danielagapov.spawn.Repositories;

import com.danielagapov.spawn.Models.UserSocialMedia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface IUserSocialMediaRepository extends JpaRepository<UserSocialMedia, UUID> {
    Optional<UserSocialMedia> findByUserId(UUID userId);
} 