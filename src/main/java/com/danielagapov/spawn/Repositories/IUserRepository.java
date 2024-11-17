package com.danielagapov.spawn.Repositories;

import com.danielagapov.spawn.Models.User.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface IUserRepository extends JpaRepository<User, Long> {
    // The JpaRepository interface already includes methods like save() and findById()
}
