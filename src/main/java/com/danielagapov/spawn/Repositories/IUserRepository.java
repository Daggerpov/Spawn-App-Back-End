package com.danielagapov.spawn.Repositories;

import com.danielagapov.spawn.Models.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface IUserRepository extends JpaRepository<User, UUID> {
    // The JpaRepository interface already includes methods like save() and findById()
    User findByEmail(String email);
    // TODO: replace with more effective search methods
    List<User> findByFirstName(String firstName);
    List<User> findByLastName(String lastName);
    List<User> findByUsername(String username);
}
