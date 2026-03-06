package com.danielagapov.spawn.shared.feign;

import com.danielagapov.spawn.user.api.dto.BaseUserDTO;
import com.danielagapov.spawn.user.api.dto.UserDTO;
import com.danielagapov.spawn.user.api.dto.FriendUser.FullFriendUserDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
public class MonolithUserClientFallbackFactory implements FallbackFactory<MonolithUserClient> {

    private static final Logger log = LoggerFactory.getLogger(MonolithUserClientFallbackFactory.class);

    @Override
    public MonolithUserClient create(Throwable cause) {
        return new MonolithUserClient() {

            @Override
            public BaseUserDTO getUserById(UUID id) {
                log.error("Fallback: monolith unreachable when fetching user by id {}. Cause: {}", id, cause.getMessage());
                throw new RuntimeException("User service unavailable", cause);
            }

            @Override
            public UserDTO getFullUserById(UUID id) {
                log.error("Fallback: monolith unreachable when fetching full user by id {}. Cause: {}", id, cause.getMessage());
                throw new RuntimeException("User service unavailable", cause);
            }

            @Override
            public List<FullFriendUserDTO> getFriendsByUserId(UUID userId) {
                log.error("Fallback: monolith unreachable when fetching friends for user {}. Cause: {}", userId, cause.getMessage());
                throw new RuntimeException("User service unavailable", cause);
            }

            @Override
            public BaseUserDTO getUserByUsername(String username) {
                log.error("Fallback: monolith unreachable when fetching user by username {}. Cause: {}", username, cause.getMessage());
                throw new RuntimeException("User service unavailable", cause);
            }

            @Override
            public boolean existsByUsername(String username) {
                log.error("Fallback: monolith unreachable when checking username exists {}. Cause: {}", username, cause.getMessage());
                throw new RuntimeException("User service unavailable", cause);
            }

            @Override
            public boolean existsByEmail(String email) {
                log.error("Fallback: monolith unreachable when checking email exists {}. Cause: {}", email, cause.getMessage());
                throw new RuntimeException("User service unavailable", cause);
            }
        };
    }
}
