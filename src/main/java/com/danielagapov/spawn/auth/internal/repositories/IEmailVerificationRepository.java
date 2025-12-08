package com.danielagapov.spawn.auth.internal.repositories;

import com.danielagapov.spawn.auth.internal.domain.EmailVerification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface IEmailVerificationRepository extends JpaRepository<EmailVerification, UUID> {
    
    EmailVerification findByEmail(String email);
    
    boolean existsByEmail(String email);

    boolean existsByVerificationCode(String verificationCode);
} 