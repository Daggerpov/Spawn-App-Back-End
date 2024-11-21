package com.danielagapov.spawn.Repositories;

import com.danielagapov.spawn.Models.UserFriendTag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface IUserFriendTagRepository extends JpaRepository<UserFriendTag, UUID> { }
