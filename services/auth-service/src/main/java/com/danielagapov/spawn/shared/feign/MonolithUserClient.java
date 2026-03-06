package com.danielagapov.spawn.shared.feign;

import com.danielagapov.spawn.user.api.dto.BaseUserDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.UUID;

/**
 * Feign client for calling the monolith's user endpoints.
 * <p>
 * Once the auth-service has its own database (auth_db), it will no longer have
 * direct access to the user table. This client provides the bridge, allowing
 * the auth-service to look up user data via REST instead of direct DB access.
 * <p>
 * The circuit breaker "monolith" is configured in application.properties
 * via Resilience4j, so a monolith outage does not cascade to the auth service.
 */
@FeignClient(
        name = "monolith-user-client",
        url = "${services.monolith.url}",
        fallbackFactory = MonolithUserClientFallbackFactory.class
)
public interface MonolithUserClient {

    @GetMapping("/api/v1/users/{id}")
    BaseUserDTO getUserById(@PathVariable("id") UUID id);

    @GetMapping("/api/v1/users/by-username")
    BaseUserDTO getUserByUsername(@RequestParam("username") String username);

    @GetMapping("/api/v1/users/by-email")
    BaseUserDTO getUserByEmail(@RequestParam("email") String email);

    @GetMapping("/api/v1/users/exists/by-username")
    boolean existsByUsername(@RequestParam("username") String username);

    @GetMapping("/api/v1/users/exists/by-email")
    boolean existsByEmail(@RequestParam("email") String email);
}
