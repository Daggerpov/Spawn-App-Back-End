package com.danielagapov.spawn.Repositories;

import com.danielagapov.spawn.Models.UserIdExternalIdMap;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface IUserIdExternalIdMapRepository extends JpaRepository<UserIdExternalIdMap, String> {
}
