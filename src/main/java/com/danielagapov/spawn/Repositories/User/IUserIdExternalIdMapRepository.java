package com.danielagapov.spawn.Repositories.User;

import com.danielagapov.spawn.Models.User.UserIdExternalIdMap;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface IUserIdExternalIdMapRepository extends JpaRepository<UserIdExternalIdMap, String> {
    Optional<UserIdExternalIdMap> findByUserEmail(final String userEmail);
    
    @Modifying
    @Transactional
    @Query("DELETE FROM UserIdExternalIdMap m WHERE m.user.id = :userId")
    void deleteAllByUserId(@Param("userId") UUID userId);
}
