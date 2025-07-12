package com.danielagapov.spawn.Repositories;

import com.danielagapov.spawn.Models.EmailVerification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface IEmailVerificationRepository extends JpaRepository<EmailVerification, UUID> {
    
    EmailVerification findByEmail(String email);
    
    boolean existsByEmail(String email);

    boolean existsByVerificationCode(String verificationCode);
} 