package com.danielagapov.spawn.shared.feign;

import com.danielagapov.spawn.user.api.dto.BaseUserDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Fallback factory for {@link MonolithUserClient}.
 * <p>
 * When the monolith is unreachable or the circuit breaker is open, these
 * fallback methods are invoked. They log the failure and throw a runtime
 * exception so the caller can handle the error appropriately.
 */
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
            public BaseUserDTO getUserByUsername(String username) {
                log.error("Fallback: monolith unreachable when fetching user by username {}. Cause: {}", username, cause.getMessage());
                throw new RuntimeException("User service unavailable", cause);
            }

            @Override
            public BaseUserDTO getUserByEmail(String email) {
                log.error("Fallback: monolith unreachable when fetching user by email {}. Cause: {}", email, cause.getMessage());
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
