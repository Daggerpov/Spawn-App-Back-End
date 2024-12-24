package com.danielagapov.spawn.Repositories;

import com.danielagapov.spawn.Models.UserFriendTagMapping;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface IUserFriendTagMappingRepository extends JpaRepository<UserFriendTagMapping, UUID> {
    List<UserFriendTagMapping> findByFriendTagId(UUID friendTagId);
}
