package com.danielagapov.spawn.user.internal.repositories;

import com.danielagapov.spawn.user.internal.domain.UserSocialMedia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface IUserSocialMediaRepository extends JpaRepository<UserSocialMedia, UUID> {
    Optional<UserSocialMedia> findByUserId(UUID userId);
} 