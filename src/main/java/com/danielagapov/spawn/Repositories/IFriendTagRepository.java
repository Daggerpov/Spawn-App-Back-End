package com.danielagapov.spawn.Repositories;

import com.danielagapov.spawn.Models.FriendTag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface IFriendTagRepository extends JpaRepository<FriendTag, UUID> {
    // The JpaRepository interface already includes methods like save() and findById()
}
