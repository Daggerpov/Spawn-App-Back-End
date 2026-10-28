package com.danielagapov.spawn.shared.feign;

import com.danielagapov.spawn.user.api.dto.BaseUserDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class MonolithUserClientFallbackFactory implements FallbackFactory<MonolithUserClient> {

    private static final Logger log = LoggerFactory.getLogger(MonolithUserClientFallbackFactory.class);

    @Override
    public MonolithUserClient create(Throwable cause) {
        return id -> {
            log.warn("Fallback: monolith unreachable when fetching user {}. Cause: {}", id, cause.getMessage());
            throw new RuntimeException("User service unavailable", cause);
        };
    }
}
