package com.danielagapov.spawn.Repositories;

import com.danielagapov.spawn.Models.FriendTag.FriendTag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface IFriendTagRepository extends JpaRepository<FriendTag, Long> {
    // The JpaRepository interface already includes methods like save() and findById()
}
