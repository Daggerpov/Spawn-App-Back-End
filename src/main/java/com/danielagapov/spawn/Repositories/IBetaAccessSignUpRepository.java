package com.danielagapov.spawn.Repositories;

import com.danielagapov.spawn.Models.BetaAccessSignUp;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface IBetaAccessSignUpRepository extends JpaRepository<BetaAccessSignUp, UUID>{
    boolean existsByEmail(String email);
}
