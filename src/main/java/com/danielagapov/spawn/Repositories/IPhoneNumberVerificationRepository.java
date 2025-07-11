package com.danielagapov.spawn.Repositories;

import com.danielagapov.spawn.Models.PhoneNumberVerification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface IPhoneNumberVerificationRepository extends JpaRepository<PhoneNumberVerification, UUID> {

    PhoneNumberVerification findByUserId(UUID userId);

    boolean existsByUserId(UUID userId);
}
