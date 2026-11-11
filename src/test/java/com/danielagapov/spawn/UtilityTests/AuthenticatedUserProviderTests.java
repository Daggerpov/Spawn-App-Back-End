package com.danielagapov.spawn.UtilityTests;

import com.danielagapov.spawn.shared.util.AuthenticatedUserProvider;
import com.danielagapov.spawn.shared.util.UserStatus;
import com.danielagapov.spawn.user.internal.domain.User;
import com.danielagapov.spawn.user.internal.domain.UserInfo;
import com.danielagapov.spawn.user.internal.repositories.IUserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AuthenticatedUserProviderTests {

    private final IUserRepository userRepository = mock(IUserRepository.class);
    private final AuthenticatedUserProvider provider = new AuthenticatedUserProvider(userRepository);

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    private void signIn(String username) {
        UserInfo userInfo = new UserInfo(username, "", UserStatus.ACTIVE);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(userInfo, null, userInfo.getAuthorities()));
    }

    private User userWithId(UUID id) {
        User user = new User();
        user.setId(id);
        return user;
    }

    @Test
    void getCurrentUserId_ReturnsEmpty_WhenNotSignedIn() {
        assertTrue(provider.getCurrentUserId().isEmpty());
    }

    @Test
    void getCurrentUserId_FindsUserByUsername() {
        UUID id = UUID.randomUUID();
        signIn("haley");
        when(userRepository.findByUsername("haley")).thenReturn(Optional.of(userWithId(id)));

        assertEquals(Optional.of(id), provider.getCurrentUserId());
    }

    @Test
    void getCurrentUserId_FallsBackToEmail_ForOAuthUsersWithoutUsername() {
        UUID id = UUID.randomUUID();
        signIn("haley@example.com");
        when(userRepository.findByUsername("haley@example.com")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("haley@example.com")).thenReturn(Optional.of(userWithId(id)));

        assertEquals(Optional.of(id), provider.getCurrentUserId());
    }
}
