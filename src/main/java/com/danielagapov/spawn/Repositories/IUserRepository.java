package com.danielagapov.spawn.Repositories;

import com.danielagapov.spawn.Models.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface IUserRepository extends JpaRepository<User, UUID> {
    // The JpaRepository interface already includes methods like save() and findById()
    // Find
    User findByEmail(String email);
<<<<<<< HEAD
=======
    User findByUsername(String username);
    // Exist
    boolean existsByUsername(String username);
>>>>>>> 01e125f (refactor with email + exception handling)
    boolean existsByEmail(String email);
}
