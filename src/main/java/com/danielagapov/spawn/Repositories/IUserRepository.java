package com.danielagapov.spawn.Repositories;

import com.danielagapov.spawn.Models.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface IUserRepository extends JpaRepository<User, Long> {
    // The JpaRepository interface already includes methods like save() and findById()

    // You can add custom query methods if needed, for example:
    Optional<User> findById(Long id);
}
