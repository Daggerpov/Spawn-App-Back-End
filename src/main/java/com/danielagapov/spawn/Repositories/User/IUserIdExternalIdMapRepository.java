package com.danielagapov.spawn.Repositories.User;

import com.danielagapov.spawn.Models.User.UserIdExternalIdMap;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface IUserIdExternalIdMapRepository extends JpaRepository<UserIdExternalIdMap, String> {
    Optional<UserIdExternalIdMap> findByUserEmail(final String userEmail);
}
