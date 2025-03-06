package com.danielagapov.spawn.Repositories;

import com.danielagapov.spawn.Models.UserIdExternalIdMap;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface IUserIdExternalIdMapRepository extends JpaRepository<UserIdExternalIdMap, String> {
    Optional<UserIdExternalIdMap> findByUserEmail(final String userEmail);
}
