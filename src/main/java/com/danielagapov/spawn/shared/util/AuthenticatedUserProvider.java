package com.danielagapov.spawn.shared.util;

import com.danielagapov.spawn.user.internal.domain.User;
import com.danielagapov.spawn.user.internal.repositories.IUserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/**
 * Looks up the user who sent the current request, from the JWT that {@code JWTFilterConfig} authenticated.
 * Use it when an endpoint takes a user ID that has to belong to the caller, instead of trusting the ID in the URL.
 */
@Component
public class AuthenticatedUserProvider {
    private final IUserRepository userRepository;

    public AuthenticatedUserProvider(IUserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * @return the signed-in user's ID, or empty if the request isn't authenticated
     */
    public Optional<UUID> getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || !(authentication.getPrincipal() instanceof UserDetails userDetails)) {
            return Optional.empty();
        }

        // The JWT filter authenticates by username, or by email for OAuth users without one
        String name = userDetails.getUsername();
        return userRepository.findByUsername(name)
                .or(() -> userRepository.findByEmail(name))
                .map(User::getId);
    }
}
