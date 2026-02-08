package com.danielagapov.spawn.auth.internal.repositories;

import com.danielagapov.spawn.auth.internal.domain.EmailVerification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface IEmailVerificationRepository extends JpaRepository<EmailVerification, UUID> {
    
    EmailVerification findByEmail(String email);
    
    boolean existsByEmail(String email);

    boolean existsByVerificationCode(String verificationCode);
} 