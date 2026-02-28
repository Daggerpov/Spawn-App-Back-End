package com.danielagapov.spawn.shared.feign;

import com.danielagapov.spawn.user.api.dto.BaseUserDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

@FeignClient(
        name = "monolith-user-client",
        url = "${services.monolith.url}",
        fallbackFactory = MonolithUserClientFallbackFactory.class
)
public interface MonolithUserClient {

    @GetMapping("/api/v1/users/{id}")
    BaseUserDTO getUserById(@PathVariable("id") UUID id);
}
