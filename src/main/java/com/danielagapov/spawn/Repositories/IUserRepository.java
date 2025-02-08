package com.danielagapov.spawn.Repositories;

import com.danielagapov.spawn.Models.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface IUserRepository extends JpaRepository<User, UUID> {
    // The JpaRepository interface already includes methods like save() and findById()
    User findByEmail(String email);
    boolean existsByEmail(String email);
    User findByUsername(String username);
}
