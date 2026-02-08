package com.danielagapov.spawn.shared.feign;

import com.danielagapov.spawn.user.api.dto.BaseUserDTO;
import com.danielagapov.spawn.user.api.dto.UserDTO;
import com.danielagapov.spawn.user.api.dto.FriendUser.FullFriendUserDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.UUID;

@FeignClient(
        name = "monolith-user-client",
        url = "${services.monolith.url}",
        fallbackFactory = MonolithUserClientFallbackFactory.class
)
public interface MonolithUserClient {

    @GetMapping("/api/v1/users/{id}")
    BaseUserDTO getUserById(@PathVariable("id") UUID id);

    @GetMapping("/api/v1/users/{id}/full")
    UserDTO getFullUserById(@PathVariable("id") UUID id);

    @GetMapping("/api/v1/users/{userId}/friends")
    List<FullFriendUserDTO> getFriendsByUserId(@PathVariable("userId") UUID userId);

    @GetMapping("/api/v1/users/by-username")
    BaseUserDTO getUserByUsername(@RequestParam("username") String username);

    @GetMapping("/api/v1/users/exists/by-username")
    boolean existsByUsername(@RequestParam("username") String username);

    @GetMapping("/api/v1/users/exists/by-email")
    boolean existsByEmail(@RequestParam("email") String email);
}
